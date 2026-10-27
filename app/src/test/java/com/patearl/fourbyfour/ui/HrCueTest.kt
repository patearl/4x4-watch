package com.patearl.fourbyfour.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HrCueTest {
    // maxHr = 200 so bpm / 2 is the percentage.
    private fun cue(pct: Int) = HrCue.of(bpm = pct * 2, maxHr = 200)

    @Test
    fun cuesFollowTargetBands() {
        assertEquals(HrCue.BIG_UP, cue(84))
        assertEquals(HrCue.SMALL_UP, cue(85))
        assertEquals(HrCue.SMALL_UP, cue(87))
        assertEquals(HrCue.ON_TARGET, cue(88))
        assertEquals(HrCue.ON_TARGET, cue(90))
        assertEquals(HrCue.ON_TARGET, cue(92))
        assertEquals(HrCue.SMALL_DOWN, cue(93))
        assertEquals(HrCue.SMALL_DOWN, cue(95))
        assertEquals(HrCue.BIG_DOWN, cue(96))
    }
}
