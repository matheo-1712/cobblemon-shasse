package rerebleue.cobblemon_shasse.method

import rerebleue.cobblemon_shasse.config.HuntConfig
import rerebleue.cobblemon_shasse.data.SpeciesStatistics

/** Methods contribute independent rolls; the registry combines them once per generation. */
interface HuntingMethod {
    val id: String
    fun enabled(config: HuntConfig): Boolean
    fun extraRolls(stats: SpeciesStatistics, config: HuntConfig, specialRoll: Double): Int
}
class MethodRegistry {
    private val methods = linkedMapOf<String, HuntingMethod>()
    fun register(method: HuntingMethod) {
        require(methods.putIfAbsent(method.id, method) == null) { "Méthode dupliquée : ${method.id}" }
    }
    fun all(): List<HuntingMethod> = methods.values.toList()
    fun extraRolls(stats: SpeciesStatistics, config: HuntConfig, specialRoll: Double) =
        methods.values.filter { it.enabled(config) }.sumOf { it.extraRolls(stats, config, specialRoll) }.coerceAtMost(64)
}
