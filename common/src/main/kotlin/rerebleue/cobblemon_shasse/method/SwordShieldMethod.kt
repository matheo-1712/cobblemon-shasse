package rerebleue.cobblemon_shasse.method

import rerebleue.cobblemon_shasse.config.HuntConfig
import rerebleue.cobblemon_shasse.config.HuntMode
import rerebleue.cobblemon_shasse.data.SpeciesStatistics
import kotlin.math.expm1
import kotlin.math.ln1p

class SwordShieldMethod : HuntingMethod {
    override val id = "sword_shield_ko"
    override fun enabled(config: HuntConfig) = config.enabled && config.swordShield.enabled
    fun tier(count: Long, config: HuntConfig): Int {
        val capped = count.coerceIn(0, config.swordShield.maxCountForBonus)
        return config.swordShield.rollTiers.last { it.count <= capped }.extraRolls
    }
    fun brilliantProbability(count: Long, config: HuntConfig): Double {
        val capped = count.coerceIn(0, config.swordShield.maxCountForBonus)
        return config.swordShield.brilliantTiers.last { it.count <= capped }.probability
    }
    override fun extraRolls(stats: SpeciesStatistics, config: HuntConfig, specialRoll: Double): Int {
        if (!enabled(config)) return 0
        if (config.swordShield.mode == HuntMode.FAITHFUL &&
            specialRoll >= brilliantProbability(stats.totalBattledCount, config)) return 0
        return tier(stats.totalBattledCount, config)
    }
}
object ShinyProbability {
    /** Cobblemon uses a denominator (1 in rate), not a probability. Preserve prior modifiers
     * and append n independent rolls using Cobblemon's configured base denominator. */
    fun withExtraRolls(currentRate: Float, baseRate: Float, rolls: Int): Float {
        if (rolls <= 0 || !currentRate.isFinite() || !baseRate.isFinite() ||
            currentRate <= 0 || baseRate <= 0) return currentRate
        if (currentRate <= 1 || baseRate <= 1) return 1f
        val logFailure = ln1p(-1.0 / currentRate) + rolls * ln1p(-1.0 / baseRate)
        return (1.0 / -expm1(logFailure)).toFloat().coerceAtLeast(1f)
    }
}
