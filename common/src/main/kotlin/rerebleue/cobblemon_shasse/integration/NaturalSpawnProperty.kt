package rerebleue.cobblemon_shasse.integration

import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.pokemon.Pokemon
import java.util.UUID

/** Copied and serialized by PokemonProperties before roll(), using the public property API. */
class NaturalSpawnProperty(private val source: UUID) : CustomPokemonProperty {
    override fun asString() = "$KEY=$source"
    override fun matches(pokemon: Pokemon) = pokemon.persistentData.hasUUID(ORIGIN) &&
        pokemon.persistentData.getUUID(ORIGIN) == source
    override fun apply(pokemon: Pokemon) {
        if (!pokemon.persistentData.hasUUID(ORIGIN)) {
            pokemon.persistentData.putUUID(ORIGIN, source)
            pokemon.onChange()
        }
    }
    companion object {
        const val KEY = "shasse_natural_origin"
        const val ORIGIN = "cobblemon_shasse:natural_origin"
        const val ROLLED = "cobblemon_shasse:rolled"
        fun register() {
            CustomPokemonProperty.register(KEY, true, { value ->
                value?.let { runCatching { NaturalSpawnProperty(UUID.fromString(it)) }.getOrNull() }
            }, { emptyList() })
        }
    }
}
class NaturalSpawnInfluence(private val source: UUID) : SpawningInfluence {
    override fun affectAction(action: SpawnAction<*>) {
        if (action !is PokemonSpawnAction) return
        val props = action.props.copy()
        props.customProperties.removeAll { it is NaturalSpawnProperty }
        props.customProperties.add(NaturalSpawnProperty(source))
        action.updatePokemon(props)
    }
}
