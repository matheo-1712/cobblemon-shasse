package rerebleue.cobblemon_shasse.integration

import java.util.UUID

object BattleRules {
    fun eligible(playerOnSide1: Boolean, playerOnSide2: Boolean, hasNpc: Boolean, allowTrainerBattles: Boolean) =
        !(playerOnSide1 && playerOnSide2) && (!hasNpc || allowTrainerBattles)

    fun responsiblePlayer(contextOrigin: UUID?, opposingPlayers: Set<UUID>): UUID? =
        contextOrigin?.takeIf { it in opposingPlayers } ?: opposingPlayers.singleOrNull()
}
