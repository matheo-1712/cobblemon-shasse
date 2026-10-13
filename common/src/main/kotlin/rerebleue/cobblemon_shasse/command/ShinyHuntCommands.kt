package rerebleue.cobblemon_shasse.command

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import rerebleue.cobblemon_shasse.ShasseRuntime
import rerebleue.cobblemon_shasse.data.SpeciesStatistics
import rerebleue.cobblemon_shasse.method.SwordShieldMethod
import java.util.UUID

object ShinyHuntCommands {
    private val error = DynamicCommandExceptionType { Component.literal(it.toString()) }
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, runtime: () -> ShasseRuntime?) {
        fun requireRuntime() = runtime() ?: throw error.create("Le serveur démarre encore.")
        fun speciesArgument() = argument("pokemon", StringArgumentType.word())
            .suggests { _, builder -> SharedSuggestionProvider.suggest(
                PokemonSpecies.species.map { it.resourceIdentifier.toString() }, builder) }
        fun playerArgument() = argument("player", StringArgumentType.word())
            .suggests { ctx, builder -> SharedSuggestionProvider.suggest(ctx.source.server.playerNames, builder) }
        dispatcher.register(literal("shinyhunt")
            .then(literal("stats").executes { ctx ->
                val r = requireRuntime()
                val data = r.storage.player(ctx.source.playerOrException.uuid)
                if (data.speciesStatistics.isEmpty()) reply(ctx.source, "Aucune rencontre éligible comptabilisée.")
                else {
                    data.speciesStatistics.toSortedMap().entries.take(20).forEach { (id, stats) ->
                        reply(ctx.source, describe(id, stats, r))
                    }
                    if (data.speciesStatistics.size > 20) reply(ctx.source, "20 espèces affichées. /shinyhunt stats <pokemon> pour les autres.")
                }
                1
            }.then(speciesArgument().executes { ctx ->
                val r = requireRuntime()
                val id = species(ctx)
                val stats = r.storage.player(ctx.source.playerOrException.uuid).speciesStatistics[id] ?: SpeciesStatistics()
                reply(ctx.source, describe(id, stats, r))
                1
            }))
            .then(literal("methods").executes { ctx ->
                val r = requireRuntime()
                r.methods.all().forEach { method ->
                    reply(ctx.source, "${method.id} : ${if (method.enabled(r.config)) "activée" else "désactivée"} (${r.config.swordShield.mode})")
                }
                reply(ctx.source, "Chaîne de captures : ${if (r.config.captureChain.enabled) "suivi activé" else "désactivée"} ; aucun bonus shiny.")
                1
            })
            .then(literal("reload").requires { it.hasPermission(2) }.executes { ctx ->
                try {
                    requireRuntime().reload()
                    reply(ctx.source, "Configuration rechargée.")
                    1
                } catch (e: Exception) {
                    ShasseRuntime.LOGGER.warn("Configuration rejetée lors du rechargement", e)
                    ctx.source.sendFailure(Component.literal("Configuration invalide : ${e.message}. Les paramètres précédents sont conservés."))
                    0
                }
            })
            .then(literal("admin").requires { it.hasPermission(2) }
                .then(literal("reset").then(playerArgument().then(speciesArgument().executes { ctx ->
                    val r = requireRuntime()
                    val player = targetPlayer(ctx)
                    val id = species(ctx)
                    r.storage.player(player).reset(id)
                    r.changed()
                    reply(ctx.source, "Statistiques de $id réinitialisées pour $player.")
                    1
                })))
                .then(literal("set").then(playerArgument().then(speciesArgument()
                    .then(argument("count", LongArgumentType.longArg(0)).executes { ctx ->
                        val r = requireRuntime()
                        val player = targetPlayer(ctx)
                        val id = species(ctx)
                        val count = LongArgumentType.getLong(ctx, "count")
                        r.storage.player(player).statistics(id).totalBattledCount = count
                        r.changed()
                        reply(ctx.source, "Compteur éligible de $id : $count pour $player.")
                        1
                    }))))
            )
        )
    }
    private fun species(ctx: CommandContext<CommandSourceStack>): String {
        val input = StringArgumentType.getString(ctx, "pokemon").lowercase()
        val id = ResourceLocation.tryParse(if (':' in input) input else "cobblemon:$input")
            ?: throw error.create("Identifiant d'espèce invalide : $input")
        if (PokemonSpecies.getByIdentifier(id) == null) throw error.create("Espèce inconnue : $id")
        return id.toString()
    }
    private fun targetPlayer(ctx: CommandContext<CommandSourceStack>): UUID {
        val input = StringArgumentType.getString(ctx, "player")
        return ctx.source.server.playerList.getPlayerByName(input)?.uuid
            ?: runCatching { UUID.fromString(input) }.getOrNull()
            ?: throw error.create("Utiliser le nom d'un joueur connecté ou son UUID.")
    }
    private fun describe(id: String, s: SpeciesStatistics, r: ShasseRuntime): String {
        val method = SwordShieldMethod()
        return "$id : KO=${s.defeatedCount}, captures=${s.capturedCount}, total=${s.totalBattledCount}, " +
            "chaîne=${s.captureStreak}, bonus=+${method.tier(s.totalBattledCount, r.config)} tirages" +
            if (r.config.swordShield.mode == rerebleue.cobblemon_shasse.config.HuntMode.FAITHFUL)
                " sur ${method.brilliantProbability(s.totalBattledCount, r.config) * 100}% d'apparitions spéciales." else "."
    }
    private fun reply(source: CommandSourceStack, text: String) =
        source.sendSuccess({ Component.literal("[Shasse] $text") }, false)
}
