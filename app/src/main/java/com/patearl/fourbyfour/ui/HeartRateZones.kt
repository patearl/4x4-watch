package com.patearl.fourbyfour.ui

import androidx.compose.ui.graphics.Color

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

/**
 * Which way to push during a hard interval. The 85–95% target has an inner 88–92% sweet spot
 * around its 90% midpoint: inside it you're on target, elsewhere in 85–95% nudge gently,
 * and outside 85–95% push firmly.
 */
enum class HrCue {
    BIG_UP, SMALL_UP, ON_TARGET, SMALL_DOWN, BIG_DOWN;

    companion object {
        fun of(bpm: Int, maxHr: Int): HrCue {
            val pct = bpm * 100f / maxHr
            return when {
                pct < 85f -> BIG_UP
                pct < 88f -> SMALL_UP
                pct <= 92f -> ON_TARGET
                pct <= 95f -> SMALL_DOWN
                else -> BIG_DOWN
            }
        }
    }
}
