package rerebleue.cobblemon_shasse.storage

import rerebleue.cobblemon_shasse.data.PlayerShinyHuntData
import java.util.UUID

interface HuntStorage {
    fun findPlayer(uuid: UUID): PlayerShinyHuntData?
    fun player(uuid: UUID): PlayerShinyHuntData
    fun changed()
}
