package rerebleue.cobblemon_shasse

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import rerebleue.cobblemon_shasse.config.*
import rerebleue.cobblemon_shasse.data.*
import rerebleue.cobblemon_shasse.method.*
import rerebleue.cobblemon_shasse.storage.HuntSavedData
import java.nio.file.Path
import java.util.UUID
import kotlin.math.pow

class HuntingTests {
    private val config = HuntConfig()
    private val method = SwordShieldMethod()
    private val player = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test fun thresholdsAndBoundaries() {
        listOf(0L to 0, 1L to 1, 19L to 1, 20L to 1, 49L to 1, 50L to 2,
            99L to 2, 100L to 3, 199L to 3, 200L to 4, 299L to 4,
            300L to 5, 499L to 5, 500L to 6, Long.MAX_VALUE to 6).forEach { (count, rolls) ->
            assertEquals(rolls, method.tier(count, config), "count=$count")
        }
    }
    @Test fun faithfulOnlyBoostsSpecialEncounters() {
        val stats = SpeciesStatistics(totalBattledCount = 500)
        assertEquals(6, method.extraRolls(stats, config, 0.029999))
        assertEquals(0, method.extraRolls(stats, config, 0.03))
        assertEquals(0, method.extraRolls(SpeciesStatistics(), config, 0.0))
        assertEquals(0.015, method.brilliantProbability(1, config))
        assertEquals(0.02, method.brilliantProbability(20, config))
        assertEquals(0.025, method.brilliantProbability(50, config))
        assertEquals(0.03, method.brilliantProbability(100, config))
    }
    @Test fun simplifiedAndIndependentDisableSwitches() {
        val simple = config.copy(swordShield = config.swordShield.copy(mode = HuntMode.SIMPLIFIED))
        assertEquals(6, method.extraRolls(SpeciesStatistics(totalBattledCount = 500), simple, 0.99))
        assertEquals(0, method.extraRolls(SpeciesStatistics(totalBattledCount = 500), config.copy(enabled = false), 0.0))
        assertEquals(0, method.extraRolls(SpeciesStatistics(totalBattledCount = 500),
            config.copy(swordShield = config.swordShield.copy(enabled = false)), 0.0))
        assertEquals(3, method.tier(500, config.copy(swordShield = config.swordShield.copy(maxCountForBonus = 100))))
    }
    @Test fun probabilityUsesIndependentRollsAndPreservesExistingBonus() {
        for (rolls in 0..6) {
            val denominator = ShinyProbability.withExtraRolls(4096f, 4096f, rolls)
            val expected = 1 - (1 - 1.0 / 4096).pow(1 + rolls)
            assertEquals(expected, 1.0 / denominator, 1e-9)
        }
        val modified = ShinyProbability.withExtraRolls(1000f, 4096f, 6)
        val expected = 1 - (1 - 1.0 / 1000) * (1 - 1.0 / 4096).pow(6)
        assertEquals(expected, 1.0 / modified, 1e-9)
        assertTrue(modified < 1000)
    }
    @Test fun invalidAndCertainBaseRatesStaySafe() {
        assertEquals(0f, ShinyProbability.withExtraRolls(0f, 4096f, 6))
        assertEquals(-1f, ShinyProbability.withExtraRolls(-1f, 4096f, 6))
        assertEquals(1000f, ShinyProbability.withExtraRolls(1000f, 0f, 6))
        assertEquals(1f, ShinyProbability.withExtraRolls(1f, 4096f, 6))
        assertEquals(1f, ShinyProbability.withExtraRolls(4096f, 1f, 6))
        assertEquals(4096f, ShinyProbability.withExtraRolls(4096f, 4096f, 0))
        assertTrue(ShinyProbability.withExtraRolls(Float.NaN, 4096f, 6).isNaN())
    }
    @Test fun duplicatesAndResurrectedCaptureDoNotDoubleTotal() {
        val tags = CompoundTag()
        assertEquals(true, EncounterLedger.claim(tags, player, Outcome.DEFEAT, true))
        assertNull(EncounterLedger.claim(tags, player, Outcome.DEFEAT, true))
        assertEquals(false, EncounterLedger.claim(tags.copy(), player, Outcome.CAPTURE, true))
        assertNull(EncounterLedger.claim(tags, player, Outcome.DEFEAT, true))
        val second = UUID.randomUUID()
        assertEquals(true, EncounterLedger.claim(tags, second, Outcome.DEFEAT, true))
    }
    @Test fun excludedOutcomeDoesNotConsumeEligibleTotal() {
        val tags = CompoundTag()
        assertEquals(false, EncounterLedger.claim(tags, player, Outcome.DEFEAT, false))
        assertEquals(true, EncounterLedger.claim(tags, player, Outcome.CAPTURE, true))
    }
    @Test fun playerAndSpeciesCountersAreIndependent() {
        val storage = HuntSavedData()
        storage.player(player).statistics("cobblemon:pikachu").record(Outcome.DEFEAT, true)
        storage.player(player).statistics("cobblemon:pikachu").record(Outcome.CAPTURE, true)
        val p = storage.player(player).statistics("cobblemon:pikachu")
        assertEquals(1, p.defeatedCount)
        assertEquals(1, p.capturedCount)
        assertEquals(2, p.totalBattledCount)
        assertEquals(0, storage.player(UUID.randomUUID()).statistics("cobblemon:pikachu").totalBattledCount)
        assertEquals(0, storage.player(player).statistics("cobblemon:eevee").totalBattledCount)
    }
    @Test fun captureChainResetsPreviousSpeciesAndIsPersisted() {
        val data = PlayerShinyHuntData(player)
        data.recordCaptureChain("cobblemon:pikachu")
        data.recordCaptureChain("cobblemon:pikachu")
        assertEquals(2, data.statistics("cobblemon:pikachu").captureStreak)
        data.recordCaptureChain("cobblemon:eevee")
        assertEquals(0, data.statistics("cobblemon:pikachu").captureStreak)
        assertEquals(1, data.statistics("cobblemon:eevee").captureStreak)
        data.recordCaptureChain("cobblemon:pikachu")
        assertEquals(1, data.statistics("cobblemon:pikachu").captureStreak)
        assertEquals(0, data.statistics("cobblemon:eevee").captureStreak)
        assertFalse(config.captureChain.enabled)
    }
    @Test fun diskRoundTripPreservesStatisticsAndChains(@TempDir dir: Path) {
        val storage = HuntSavedData()
        val data = storage.player(player)
        data.statistics("cobblemon:pikachu").record(Outcome.DEFEAT, true)
        data.statistics("cobblemon:pikachu").record(Outcome.CAPTURE, true)
        data.recordCaptureChain("cobblemon:pikachu")
        val envelope = CompoundTag().also { it.put("data", storage.encode()) }
        val path = dir.resolve("stats.dat")
        NbtIo.writeCompressed(envelope, path)
        val decoded = HuntSavedData.decode(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()).getCompound("data"))
        assertEquals(data, decoded.player(player))
        decoded.player(player).recordCaptureChain("cobblemon:pikachu")
        assertEquals(2, decoded.player(player).statistics("cobblemon:pikachu").captureStreak)
    }
    @Test fun rejectsUnknownFormatAndNegativePersistedCounts() {
        val tag = HuntSavedData().encode()
        tag.putInt("formatVersion", 2)
        assertThrows(IllegalArgumentException::class.java) { HuntSavedData.decode(tag) }
        val storage = HuntSavedData()
        storage.player(player).statistics("cobblemon:pikachu").defeatedCount = -1
        assertThrows(IllegalArgumentException::class.java) { HuntSavedData.decode(storage.encode()) }
    }
    @Test fun resetAndOverflowAreSafe() {
        val data = PlayerShinyHuntData(player)
        data.recordCaptureChain("cobblemon:pikachu")
        data.reset("cobblemon:pikachu")
        assertNull(data.lastCapturedSpecies)
        assertTrue(data.speciesStatistics.isEmpty())
        val s = SpeciesStatistics(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
        s.record(Outcome.DEFEAT, true)
        s.record(Outcome.CAPTURE, true)
        assertEquals(Long.MAX_VALUE, s.totalBattledCount)
        assertEquals(Long.MAX_VALUE, s.capturedCount)
        assertEquals(Long.MAX_VALUE, s.defeatedCount)
    }
    @Test fun configDefaultsPartialOverridesAndValidation() {
        assertEquals(config, ConfigFile.parse("{}"))
        assertEquals(HuntMode.SIMPLIFIED, ConfigFile.parse("""{"swordShield":{"mode":"SIMPLIFIED"}}""").swordShield.mode)
        listOf("""{"enabled":null}""", """{"enabled":"false"}""",
            """{"swordShield":{"mode":"unknown"}}""", """{"swordShield":{"maxCountForBonus":-1}}""",
            """{"swordShield":{"maxCountForBonus":1.5}}""", """{"swordShield":{"rollTiers":[]}}""",
            """{"swordShield":{"rollTiers":[{"count":0,"extraRolls":65}]}}""",
            """{"swordShield":{"brilliantTiers":[{"count":0,"probability":2}]}}""",
            """{"swordShield":{"rollTiers":[{"count":0,"extraRolls":0},{"count":0,"extraRolls":1}]}}""")
            .forEach { json -> assertThrows(Exception::class.java) { ConfigFile.parse(json) } }
    }
    @Test fun generatedConfigLoadsAgain(@TempDir dir: Path) {
        val file = ConfigFile(dir.resolve("config.json"))
        assertEquals(config, file.load())
        assertEquals(config, file.load())
    }
    @Test fun registryRejectsDuplicateMethodsAndCombinesIndependentRolls() {
        val registry = MethodRegistry()
        registry.register(method)
        assertThrows(IllegalArgumentException::class.java) { registry.register(SwordShieldMethod()) }
        registry.register(object : HuntingMethod {
            override val id = "test_method"
            override fun enabled(config: HuntConfig) = true
            override fun extraRolls(stats: SpeciesStatistics, config: HuntConfig, specialRoll: Double) = 2
        })
        assertEquals(8, registry.extraRolls(SpeciesStatistics(totalBattledCount = 500), config, 0.0))
    }
}
