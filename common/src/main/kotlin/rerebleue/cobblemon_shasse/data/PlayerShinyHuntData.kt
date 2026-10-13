package rerebleue.cobblemon_shasse.data

import java.util.UUID

enum class Outcome { DEFEAT, CAPTURE }
data class SpeciesStatistics(
    var defeatedCount: Long = 0,
    var capturedCount: Long = 0,
    var totalBattledCount: Long = 0,
    var captureStreak: Long = 0
) {
    fun record(outcome: Outcome, eligibleForTotal: Boolean) {
        when (outcome) {
            Outcome.DEFEAT -> defeatedCount = increment(defeatedCount)
            Outcome.CAPTURE -> capturedCount = increment(capturedCount)
        }
        if (eligibleForTotal) totalBattledCount = increment(totalBattledCount)
    }
    companion object {
        fun increment(value: Long) = if (value == Long.MAX_VALUE) value else value + 1
    }
}
data class PlayerShinyHuntData(
    val playerUUID: UUID,
    val speciesStatistics: MutableMap<String, SpeciesStatistics> = linkedMapOf(),
    var lastCapturedSpecies: String? = null,
    var currentHuntSpecies: String? = null
) {
    fun statistics(species: String) = speciesStatistics.getOrPut(species) { SpeciesStatistics() }
    fun currentHunt(): Pair<String, SpeciesStatistics>? {
        val species = currentHuntSpecies ?: return null
        return speciesStatistics[species]?.let { species to it }
    }
    fun recordEncounter(species: String, outcome: Outcome, eligibleForTotal: Boolean) {
        statistics(species).record(outcome, eligibleForTotal)
        currentHuntSpecies = species
    }
    fun recordCaptureChain(species: String) {
        if (lastCapturedSpecies != species) {
            lastCapturedSpecies?.let { speciesStatistics[it]?.captureStreak = 0 }
            statistics(species).captureStreak = 0
        }
        val stats = statistics(species)
        stats.captureStreak = SpeciesStatistics.increment(stats.captureStreak)
        lastCapturedSpecies = species
        currentHuntSpecies = species
    }
    fun reset(species: String) {
        speciesStatistics.remove(species)
        if (lastCapturedSpecies == species) lastCapturedSpecies = null
        if (currentHuntSpecies == species) currentHuntSpecies = null
    }
}
