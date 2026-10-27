package com.patearl.fourbyfour.ui

import androidx.compose.ui.graphics.Color
import com.patearl.fourbyfour.workout.PhaseType

/** Heart-rate zones relative to max HR. The 4×4 target for hard intervals is 85–95%. */
enum class HrZone(val color: Color, val label: String) {
    EASY(Color(0xFF9E9E9E), "Easy"),
    MODERATE(Color(0xFF42A5F5), "Moderate"),
    STEADY(Color(0xFFFFCA28), "Steady"),
    TARGET(Color(0xFF66BB6A), "Target"),
    ABOVE(Color(0xFFEF5350), "Above target"),
    ;

    companion object {
        fun of(bpm: Int, maxHr: Int): HrZone {
            val pct = bpm * 100f / maxHr
            return when {
                pct < 60f -> EASY
                pct < 70f -> MODERATE
                pct < 85f -> STEADY
                pct <= 95f -> TARGET
                else -> ABOVE
            }
        }
    }
}

/** Which way to steer heart rate: the heart means "hold it here". */
enum class HrCue { BIG_UP, SMALL_UP, ON_TARGET, SMALL_DOWN, BIG_DOWN }

/**
 * Per-phase heart-rate targets, as % of max HR:
 * - Warm-up: build toward the top of 60–70%. The bar for "on target" rises from 60% to 70% over
 *   the warm-up, so it pushes harder as you go. Being above it is fine.
 * - Hard: 85–95%, steering to the 88–92% middle.
 * - Recovery: 65–75%, steering to 68–72% (NTNU recommends ~70% to clear lactate).
 * - Cool-down: 60% or below. Going easier is never wrong.
 */
object HrTargets {
    fun cue(phase: PhaseType, bpm: Int, maxHr: Int, phaseProgress: Float): HrCue {
        val pct = bpm * 100f / maxHr
        return when (phase) {
            PhaseType.WARM_UP -> {
                val floor = 60f + 10f * phaseProgress.coerceIn(0f, 1f)
                when {
                    pct >= floor -> HrCue.ON_TARGET
                    pct >= floor - 5f -> HrCue.SMALL_UP
                    else -> HrCue.BIG_UP
                }
            }
            PhaseType.HARD -> banded(pct, low = 85f, high = 95f)
            PhaseType.RECOVERY -> banded(pct, low = 65f, high = 75f)
            PhaseType.COOL_DOWN -> when {
                pct <= 60f -> HrCue.ON_TARGET
                pct <= 65f -> HrCue.SMALL_DOWN
                else -> HrCue.BIG_DOWN
            }
        }
    }

    /** Heart in the inner 4 points around the midpoint, small arrows elsewhere in range, big outside. */
    private fun banded(pct: Float, low: Float, high: Float): HrCue {
        val mid = (low + high) / 2
        return when {
            pct < low -> HrCue.BIG_UP
            pct < mid - 2f -> HrCue.SMALL_UP
            pct <= mid + 2f -> HrCue.ON_TARGET
            pct <= high -> HrCue.SMALL_DOWN
            else -> HrCue.BIG_DOWN
        }
    }
}
