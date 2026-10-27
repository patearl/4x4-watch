package com.patearl.fourbyfour.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutPlanTest {
    private val minute = 60_000L

    @Test
    fun totalIs43Minutes() {
        assertEquals(43 * minute, WorkoutPlan.totalMs)
        assertEquals(10, WorkoutPlan.phases.size)
    }

    @Test
    fun positionsFollowTheProtocol() {
        assertEquals(PhaseType.WARM_UP, WorkoutPlan.positionAt(0)!!.phase.type)
        assertEquals(10 * minute, WorkoutPlan.positionAt(0)!!.remainingMs)

        val firstHard = WorkoutPlan.positionAt(10 * minute)!!
        assertEquals(PhaseType.HARD, firstHard.phase.type)
        assertEquals(1, firstHard.phase.round)
        assertEquals(4 * minute, firstHard.remainingMs)

        val firstRecovery = WorkoutPlan.positionAt(14 * minute)!!
        assertEquals(PhaseType.RECOVERY, firstRecovery.phase.type)
        assertEquals(1, firstRecovery.phase.round)

        val lastHard = WorkoutPlan.positionAt(31 * minute + 1)!!
        assertEquals(PhaseType.HARD, lastHard.phase.type)
        assertEquals(4, lastHard.phase.round)

        assertEquals(PhaseType.COOL_DOWN, WorkoutPlan.positionAt(38 * minute)!!.phase.type)
        assertEquals(1, WorkoutPlan.positionAt(43 * minute - 1)!!.remainingMs)
        assertNull(WorkoutPlan.positionAt(43 * minute))
    }
}
