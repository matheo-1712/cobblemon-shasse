package rerebleue.cobblemon_shasse.integration

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.reactive.ObservableSubscription
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.server.level.ServerPlayer
import rerebleue.cobblemon_shasse.ShasseRuntime
import rerebleue.cobblemon_shasse.config.Attribution
import rerebleue.cobblemon_shasse.data.Outcome
import rerebleue.cobblemon_shasse.data.EncounterLedger
import rerebleue.cobblemon_shasse.method.ShinyProbability
import java.util.UUID
import kotlin.random.Random

class CobblemonHooks(private val runtime: ShasseRuntime) : AutoCloseable {
    private val subscriptions = mutableListOf<ObservableSubscription<*>>()
    private data class CaptureCandidate(val pokemon: UUID, val player: UUID, val expires: Long)
    private val captureCandidates = hashMapOf<UUID, CaptureCandidate>()
    private val builder: (ServerPlayer) -> SpawningInfluence? = { NaturalSpawnInfluence(it.uuid) }

    fun install() {
        PlayerSpawnerFactory.influenceBuilders.add(builder)
        subscriptions += CobblemonEvents.SHINY_CHANCE_CALCULATION.subscribe(priority = Priority.LOWEST, handler = { event ->
            var cachedRolls: Int? = null
            event.addModificationFunction { rate, contextPlayer, pokemon ->
                val server = runtime.server
                val config = runtime.config
                val tags = pokemon.persistentData
                // Generation only: never modify shiny flags or reroll existing Pokémon.
                if (!server.isSameThread || !config.enabled || !config.swordShield.enabled ||
                    pokemon.shiny || !pokemon.isWild() ||
                    (tags.getBoolean(NaturalSpawnProperty.ROLLED) && cachedRolls == null) ||
                    !tags.hasUUID(NaturalSpawnProperty.ORIGIN)) {
                    rate
                } else {
                    val source = tags.getUUID(NaturalSpawnProperty.ORIGIN)
                    val player = when (config.swordShield.attribution) {
                        Attribution.COBBLEMON_CONTEXT -> contextPlayer?.takeIf { it.uuid == source }
                        Attribution.CONTEXT_OR_SPAWNER -> contextPlayer?.takeIf { it.uuid == source }
                            ?: server.playerList.getPlayer(source)
                    }
                    if (player == null) rate else {
                        val stats = runtime.storage.player(player.uuid).statistics(pokemon.species.resourceIdentifier.toString())
                        val rolls = cachedRolls ?: runtime.methods.extraRolls(stats, config, Random.nextDouble()).also {
                            cachedRolls = it
                            tags.putBoolean(NaturalSpawnProperty.ROLLED, true)
                        }
                        ShinyProbability.withExtraRolls(rate, event.baseChance, rolls)
                    }
                }
            }
        })
        subscriptions += CobblemonEvents.BATTLE_FAINTED.subscribe(priority = Priority.NORMAL, handler = { event ->
            runtime.onServerThread {
                val killed = event.killed
                val pokemon = killed.originalPokemon
                if (killed.actor.type != ActorType.WILD || !pokemon.isWild() ||
                    killed.entity?.ownerUUID != null || !eligibleBattle(event.battle) || !eligibleOrigin(pokemon)) return@onServerThread
                val opponents = event.battle.actors.filter {
                    it.type == ActorType.PLAYER && it.getSide() != killed.actor.getSide()
                }
                // Damage/status context handles multi battles; a sole opponent is an unambiguous fallback.
                val origin = event.context.origin?.actor?.takeIf { it in opponents }?.getPlayerUUIDs()?.singleOrNull()
                val player = BattleRules.responsiblePlayer(origin,
                    opponents.flatMap { it.getPlayerUUIDs() }.toSet()) ?: return@onServerThread
                record(player, pokemon, Outcome.DEFEAT, runtime.config.swordShield.countWildDefeats)
            }
        })
        subscriptions += CobblemonEvents.POKE_BALL_CAPTURE_CALCULATED.subscribe(priority = Priority.NORMAL, handler = { event ->
            runtime.onServerThread {
                val player = event.thrower as? ServerPlayer ?: return@onServerThread
                val entity = event.pokemonEntity
                val battle = entity.battleId?.let { com.cobblemon.mod.common.battles.BattleRegistry.getBattle(it) }
                if (entity.ownerUUID != null || !entity.pokemon.isWild() || !eligibleOrigin(entity.pokemon) ||
                    (battle != null && !eligibleBattle(battle))) return@onServerThread
                // Do not trust the preliminary success result: only POKEMON_CAPTURED records success.
                captureCandidates[event.pokeBallEntity.uuid] =
                    CaptureCandidate(entity.pokemon.uuid, player.uuid, runtime.ticks + 1200)
            }
        })
        subscriptions += CobblemonEvents.POKEMON_CAPTURED.subscribe(priority = Priority.NORMAL, handler = { event ->
            runtime.onServerThread {
                val candidate = captureCandidates.remove(event.pokeBallEntity.uuid) ?: return@onServerThread
                if (candidate.pokemon != event.pokemon.uuid || candidate.player != event.player.uuid) return@onServerThread
                record(event.player.uuid, event.pokemon, Outcome.CAPTURE, runtime.config.swordShield.countWildCaptures)
            }
        })
    }
    private fun eligibleOrigin(pokemon: Pokemon) = runtime.config.swordShield.allowArtificialSpawns ||
        pokemon.persistentData.hasUUID(NaturalSpawnProperty.ORIGIN)

    private fun eligibleBattle(battle: PokemonBattle): Boolean {
        return BattleRules.eligible(
            battle.side1.actors.any { it.type == ActorType.PLAYER },
            battle.side2.actors.any { it.type == ActorType.PLAYER },
            battle.actors.any { it.type == ActorType.NPC },
            runtime.config.swordShield.allowTrainerBattles)
    }

    private fun record(player: UUID, pokemon: Pokemon, outcome: Outcome, countForMethod: Boolean) {
        val config = runtime.config
        if (!config.enabled || (!config.swordShield.enabled && !config.captureChain.enabled)) return
        val countOutcome = config.swordShield.enabled && countForMethod
        if (!countOutcome && (outcome != Outcome.CAPTURE || !config.captureChain.enabled)) return
        val addTotal = EncounterLedger.claim(pokemon.persistentData, player, outcome,
            config.swordShield.enabled && countForMethod) ?: return
        val species = pokemon.species.resourceIdentifier.toString()
        val data = runtime.storage.player(player)
        if (countOutcome) data.statistics(species).record(outcome, addTotal)
        if (outcome == Outcome.CAPTURE && config.captureChain.enabled) data.recordCaptureChain(species)
        pokemon.onChange()
        runtime.changed()
    }
    fun tick() {
        if (runtime.ticks % 200L == 0L) captureCandidates.entries.removeIf { it.value.expires < runtime.ticks }
    }
    override fun close() {
        subscriptions.forEach { it.unsubscribe() }
        subscriptions.clear()
        PlayerSpawnerFactory.influenceBuilders.remove(builder)
        captureCandidates.clear()
    }
}
