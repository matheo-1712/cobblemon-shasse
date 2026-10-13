package rerebleue.cobblemon_shasse.data

import net.minecraft.nbt.CompoundTag
import java.util.UUID

/** Persistent per-Pokémon markers survive duplicate events and chunk unloads.
 * null means duplicate; otherwise the value determines whether totalBattledCount increases. */
object EncounterLedger {
    fun claim(tags: CompoundTag, player: UUID, outcome: Outcome, eligibleForTotal: Boolean): Boolean? {
        val eventKey = "cobblemon_shasse:${outcome.name.lowercase()}_$player"
        if (tags.getBoolean(eventKey)) return null
        tags.putBoolean(eventKey, true)
        val totalKey = "cobblemon_shasse:total_$player"
        val incrementTotal = eligibleForTotal && !tags.getBoolean(totalKey)
        if (incrementTotal) tags.putBoolean(totalKey, true)
        return incrementTotal
    }
}
