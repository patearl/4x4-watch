package com.patearl.fourbyfour.ui

import com.patearl.fourbyfour.workout.PhaseType
import org.junit.Assert.assertEquals
import org.junit.Test

class HrCueTest {
    // maxHr = 200 so bpm / 2 is the percentage.
    private fun cue(phase: PhaseType, pct: Int, progress: Float = 0.5f) =
        HrTargets.cue(phase, bpm = pct * 2, maxHr = 200, phaseProgress = progress)

    @Test
    fun hardSteersToMiddleOf85to95() {
        assertEquals(HrCue.BIG_UP, cue(PhaseType.HARD, 84))
        assertEquals(HrCue.SMALL_UP, cue(PhaseType.HARD, 85))
        assertEquals(HrCue.SMALL_UP, cue(PhaseType.HARD, 87))
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.HARD, 88))
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.HARD, 92))
        assertEquals(HrCue.SMALL_DOWN, cue(PhaseType.HARD, 93))
        assertEquals(HrCue.SMALL_DOWN, cue(PhaseType.HARD, 95))
        assertEquals(HrCue.BIG_DOWN, cue(PhaseType.HARD, 96))
    }

    @Test
    fun recoverySteersTo70() {
        assertEquals(HrCue.BIG_UP, cue(PhaseType.RECOVERY, 64))
        assertEquals(HrCue.SMALL_UP, cue(PhaseType.RECOVERY, 67))
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.RECOVERY, 70))
        assertEquals(HrCue.SMALL_DOWN, cue(PhaseType.RECOVERY, 73))
        assertEquals(HrCue.BIG_DOWN, cue(PhaseType.RECOVERY, 76))
    }

    @Test
    fun warmUpBarRisesAndNeverSaysSlowDown() {
        // Start: 60% is enough.
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.WARM_UP, 60, progress = 0f))
        assertEquals(HrCue.SMALL_UP, cue(PhaseType.WARM_UP, 56, progress = 0f))
        assertEquals(HrCue.BIG_UP, cue(PhaseType.WARM_UP, 50, progress = 0f))
        // Halfway: needs 65%.
        assertEquals(HrCue.SMALL_UP, cue(PhaseType.WARM_UP, 62, progress = 0.5f))
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.WARM_UP, 65, progress = 0.5f))
        // End: needs 70%, and the same 60% that was fine at the start is now well short.
        assertEquals(HrCue.BIG_UP, cue(PhaseType.WARM_UP, 60, progress = 1f))
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.WARM_UP, 70, progress = 1f))
        // Over the range is fine.
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.WARM_UP, 90, progress = 1f))
    }

    @Test
    fun coolDownCeilingFallsAndNeverSaysSpeedUp() {
        // Start: up to 75% is fine.
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.COOL_DOWN, 75, progress = 0f))
        assertEquals(HrCue.SMALL_DOWN, cue(PhaseType.COOL_DOWN, 78, progress = 0f))
        assertEquals(HrCue.BIG_DOWN, cue(PhaseType.COOL_DOWN, 90, progress = 0f))
        // Halfway: ceiling is 67.5%.
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.COOL_DOWN, 67, progress = 0.5f))
        assertEquals(HrCue.SMALL_DOWN, cue(PhaseType.COOL_DOWN, 70, progress = 0.5f))
        // End: down to 60%, and the 75% that was fine at the start is now well over.
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.COOL_DOWN, 60, progress = 1f))
        assertEquals(HrCue.SMALL_DOWN, cue(PhaseType.COOL_DOWN, 63, progress = 1f))
        assertEquals(HrCue.BIG_DOWN, cue(PhaseType.COOL_DOWN, 75, progress = 1f))
        // Under is always fine.
        assertEquals(HrCue.ON_TARGET, cue(PhaseType.COOL_DOWN, 40, progress = 0f))
    }
}
