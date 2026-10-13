package rerebleue.cobblemon_shasse.storage

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.saveddata.SavedData
import rerebleue.cobblemon_shasse.data.PlayerShinyHuntData
import rerebleue.cobblemon_shasse.data.SpeciesStatistics
import java.util.UUID

/** Stored in the overworld data directory. Minecraft owns the save lifecycle. No disk IO per KO. */
class HuntSavedData : SavedData(), HuntStorage {
    private val players = linkedMapOf<UUID, PlayerShinyHuntData>()
    override fun findPlayer(uuid: UUID) = players[uuid]
    override fun player(uuid: UUID) = players.getOrPut(uuid) { PlayerShinyHuntData(uuid) }
    override fun changed() = setDirty()
    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag = encode(tag)

    fun encode(tag: CompoundTag = CompoundTag()): CompoundTag {
        tag.putInt("formatVersion", FORMAT_VERSION)
        val playerTags = CompoundTag()
        players.forEach { (uuid, data) ->
            val playerTag = CompoundTag()
            data.lastCapturedSpecies?.let { playerTag.putString("lastCapturedSpecies", it) }
            // Empty explicitly means no current hunt; absent is a legacy save.
            playerTag.putString("currentHuntSpecies", data.currentHuntSpecies ?: "")
            val speciesTags = CompoundTag()
            data.speciesStatistics.forEach { (id, stats) ->
                val s = CompoundTag()
                s.putLong("defeatedCount", stats.defeatedCount)
                s.putLong("capturedCount", stats.capturedCount)
                s.putLong("totalBattledCount", stats.totalBattledCount)
                s.putLong("captureStreak", stats.captureStreak)
                speciesTags.put(id, s)
            }
            playerTag.put("speciesStatistics", speciesTags)
            playerTags.put(uuid.toString(), playerTag)
        }
        tag.put("players", playerTags)
        return tag
    }
    companion object {
        const val FORMAT_VERSION = 1
        const val NAME = "cobblemon_shasse"
        fun decode(tag: CompoundTag): HuntSavedData {
            require(tag.getInt("formatVersion") == FORMAT_VERSION) { "Format de statistiques non pris en charge" }
            require(tag.contains("players", Tag.TAG_COMPOUND.toInt())) { "Données de joueurs absentes" }
            val result = HuntSavedData()
            val playerTags = tag.getCompound("players")
            playerTags.allKeys.forEach { key ->
                val uuid = UUID.fromString(key)
                val p = playerTags.getCompound(key)
                val data = result.player(uuid)
                if (p.contains("lastCapturedSpecies", Tag.TAG_STRING.toInt())) data.lastCapturedSpecies = p.getString("lastCapturedSpecies")
                val species = p.getCompound("speciesStatistics")
                species.allKeys.forEach { id ->
                    val s = species.getCompound(id)
                    val stats = SpeciesStatistics(s.getLong("defeatedCount"), s.getLong("capturedCount"),
                        s.getLong("totalBattledCount"), s.getLong("captureStreak"))
                    require(listOf(stats.defeatedCount, stats.capturedCount, stats.totalBattledCount, stats.captureStreak).all { it >= 0 })
                    data.speciesStatistics[id] = stats
                }
                data.currentHuntSpecies = if (p.contains("currentHuntSpecies", Tag.TAG_STRING.toInt())) {
                    p.getString("currentHuntSpecies").takeIf { it.isNotEmpty() && it in data.speciesStatistics }
                } else {
                    // Older files did not track event order. Infer only when one species has progress.
                    data.speciesStatistics.entries.singleOrNull { (_, s) ->
                        s.defeatedCount > 0 || s.capturedCount > 0 || s.totalBattledCount > 0 || s.captureStreak > 0
                    }?.key
                }
            }
            return result
        }
    }
}
