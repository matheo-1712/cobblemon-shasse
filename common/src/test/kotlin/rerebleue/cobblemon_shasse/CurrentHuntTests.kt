package rerebleue.cobblemon_shasse

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rerebleue.cobblemon_shasse.data.Outcome
import rerebleue.cobblemon_shasse.data.PlayerShinyHuntData
import rerebleue.cobblemon_shasse.storage.HuntSavedData
import java.util.UUID

class CurrentHuntTests {
    private val player = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val pikachu = "cobblemon:pikachu"
    private val eevee = "cobblemon:eevee"

    @Test fun lastEligibleEncounterSelectsOneSpeciesWithoutResettingHistory() {
        val data = PlayerShinyHuntData(player)
        data.recordEncounter(pikachu, Outcome.DEFEAT, true)
        assertEquals(pikachu, data.currentHunt()?.first)
        data.recordEncounter(eevee, Outcome.CAPTURE, true)
        assertEquals(eevee, data.currentHunt()?.first)
        assertEquals(1L, data.speciesStatistics[pikachu]?.defeatedCount)
        assertEquals(1L, data.currentHunt()?.second?.capturedCount)
        data.recordEncounter(pikachu, Outcome.DEFEAT, true)
        assertEquals(pikachu, data.currentHunt()?.first)
        assertEquals(2L, data.currentHunt()?.second?.totalBattledCount)
        assertEquals(1L, data.speciesStatistics[eevee]?.capturedCount)
    }

    @Test fun readOnlyLookupAndEmptyStatisticsNeverStartAHunt() {
        val storage = HuntSavedData()
        assertNull(storage.findPlayer(player))
        assertTrue(storage.encode().getCompound("players").isEmpty)
        val data = storage.player(player)
        data.statistics(pikachu)
        data.statistics(eevee)
        assertNull(data.currentHunt())
    }

    @Test fun currentHuntSurvivesSavingAndLoading() {
        val storage = HuntSavedData()
        val data = storage.player(player)
        data.recordEncounter(pikachu, Outcome.DEFEAT, true)
        data.recordEncounter(eevee, Outcome.CAPTURE, true)
        val restored = HuntSavedData.decode(storage.encode()).player(player)
        assertEquals(data, restored)
        assertEquals(eevee, restored.currentHunt()?.first)
    }

    @Test fun resetCurrentHuntDoesNotPromoteAnOldHuntAfterRestart() {
        val storage = HuntSavedData()
        val data = storage.player(player)
        data.recordEncounter(pikachu, Outcome.DEFEAT, true)
        data.recordEncounter(eevee, Outcome.CAPTURE, true)
        data.reset(eevee)
        assertNull(data.currentHunt())
        assertEquals(1L, data.speciesStatistics[pikachu]?.totalBattledCount)
        assertNull(HuntSavedData.decode(storage.encode()).player(player).currentHunt())
    }

    @Test fun legacyFilesIgnoreZeroCountersAndDoNotGuessBetweenMultipleHunts() {
        val storage = HuntSavedData()
        val data = storage.player(player)
        data.statistics("cobblemon:buneary")
        data.statistics("cobblemon:dustox")
        data.statistics(pikachu).record(Outcome.DEFEAT, true)
        val single = storage.encode()
        single.getCompound("players").getCompound(player.toString()).remove("currentHuntSpecies")
        assertEquals(pikachu, HuntSavedData.decode(single).player(player).currentHunt()?.first)
        data.statistics(eevee).record(Outcome.CAPTURE, true)
        val multiple = storage.encode()
        multiple.getCompound("players").getCompound(player.toString()).remove("currentHuntSpecies")
        assertNull(HuntSavedData.decode(multiple).player(player).currentHunt())
    }

    @Test fun displayedHuntAndCaptureStreakRemainIndependent() {
        val data = PlayerShinyHuntData(player)
        data.recordCaptureChain(pikachu)
        data.recordEncounter(eevee, Outcome.DEFEAT, true)
        assertEquals(eevee, data.currentHunt()?.first)
        assertEquals(pikachu, data.lastCapturedSpecies)
        assertEquals(1L, data.speciesStatistics[pikachu]?.captureStreak)
        data.recordCaptureChain(pikachu)
        assertEquals(pikachu, data.currentHunt()?.first)
        assertEquals(2L, data.currentHunt()?.second?.captureStreak)
    }
}
