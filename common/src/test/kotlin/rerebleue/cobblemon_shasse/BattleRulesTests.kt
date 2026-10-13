package rerebleue.cobblemon_shasse

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import rerebleue.cobblemon_shasse.integration.BattleRules
import java.util.UUID

class BattleRulesTests {
    private val first = UUID.randomUUID()
    private val second = UUID.randomUUID()
    @Test fun wildBattleIsEligible() {
        assertTrue(BattleRules.eligible(true, false, false, false))
        assertTrue(BattleRules.eligible(false, true, false, false))
    }
    @Test fun pvpNeverCountsEvenWithTrainerOption() {
        assertFalse(BattleRules.eligible(true, true, false, false))
        assertFalse(BattleRules.eligible(true, true, true, true))
    }
    @Test fun trainerBattlesRequireExplicitOptIn() {
        assertFalse(BattleRules.eligible(true, false, true, false))
        assertTrue(BattleRules.eligible(true, false, true, true))
    }
    @Test fun multiplePlayersUseDamageContextWithoutArbitraryFallback() {
        assertEquals(second, BattleRules.responsiblePlayer(second, setOf(first, second)))
        assertNull(BattleRules.responsiblePlayer(null, setOf(first, second)))
        assertNull(BattleRules.responsiblePlayer(UUID.randomUUID(), setOf(first, second)))
    }
    @Test fun singleOpponentCanReceiveIndirectOrUnattributedKO() {
        assertEquals(first, BattleRules.responsiblePlayer(null, setOf(first)))
        assertEquals(first, BattleRules.responsiblePlayer(UUID.randomUUID(), setOf(first)))
        assertNull(BattleRules.responsiblePlayer(null, emptySet()))
    }
}
