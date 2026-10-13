package rerebleue.cobblemon_shasse.config

enum class HuntMode { FAITHFUL, SIMPLIFIED }
enum class Attribution { COBBLEMON_CONTEXT, CONTEXT_OR_SPAWNER }
data class RollTier(val count: Long, val extraRolls: Int)
data class BrilliantTier(val count: Long, val probability: Double)

data class SwordShieldConfig(
    val enabled: Boolean = true,
    val mode: HuntMode = HuntMode.FAITHFUL,
    val maxCountForBonus: Long = 500,
    val countWildDefeats: Boolean = true,
    val countWildCaptures: Boolean = true,
    val allowTrainerBattles: Boolean = false,
    val allowArtificialSpawns: Boolean = false,
    val attribution: Attribution = Attribution.COBBLEMON_CONTEXT,
    val rollTiers: List<RollTier> = listOf(
        RollTier(0, 0), RollTier(1, 1), RollTier(20, 1), RollTier(50, 2),
        RollTier(100, 3), RollTier(200, 4), RollTier(300, 5), RollTier(500, 6)
    ),
    val brilliantTiers: List<BrilliantTier> = listOf(
        BrilliantTier(0, 0.0), BrilliantTier(1, 0.015), BrilliantTier(20, 0.02),
        BrilliantTier(50, 0.025), BrilliantTier(100, 0.03)
    )
)
data class CaptureChainConfig(val enabled: Boolean = false)
data class PersistenceConfig(val autosaveEnabled: Boolean = true)
data class HuntConfig(
    val enabled: Boolean = true,
    val swordShield: SwordShieldConfig = SwordShieldConfig(),
    val captureChain: CaptureChainConfig = CaptureChainConfig(),
    val persistence: PersistenceConfig = PersistenceConfig()
) {
    fun validate(): HuntConfig {
        val s = swordShield
        require(s.maxCountForBonus in 1..1_000_000) { "maxCountForBonus doit être entre 1 et 1000000" }
        require(s.rollTiers.isNotEmpty() && s.rollTiers.first().count == 0L)
        require(s.brilliantTiers.isNotEmpty() && s.brilliantTiers.first().count == 0L)
        require(s.rollTiers.all { it.count >= 0 && it.extraRolls in 0..64 })
        require(s.brilliantTiers.all { it.count >= 0 && it.probability.isFinite() && it.probability in 0.0..1.0 })
        require(s.rollTiers.zipWithNext().all { (a, b) -> a.count < b.count })
        require(s.brilliantTiers.zipWithNext().all { (a, b) -> a.count < b.count })
        return this
    }
}
