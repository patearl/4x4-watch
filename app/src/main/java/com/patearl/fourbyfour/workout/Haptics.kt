package com.patearl.fourbyfour.workout

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibration cues. Uses alarm usage so the cues still fire with the screen off or in Do Not Disturb. */
class Haptics(context: Context) {
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** Single short tick for the 3-2-1 countdown. */
    fun countdownTick() = play(longArrayOf(0, 90), intArrayOf(0, 180))

    /** "Go hard": three long, full-strength pulses. */
    fun goHard() = play(
        longArrayOf(0, 450, 120, 450, 120, 700),
        intArrayOf(0, 255, 0, 255, 0, 255),
    )

    /** Ease off into recovery: two gentle medium pulses. */
    fun recovery() = play(
        longArrayOf(0, 250, 200, 250),
        intArrayOf(0, 140, 0, 140),
    )

    /** Warm-up / cool-down start: one medium pulse. */
    fun easyPhase() = play(longArrayOf(0, 400), intArrayOf(0, 160))

    /** Workout complete: long fading triple. */
    fun finished() = play(
        longArrayOf(0, 600, 150, 400, 150, 250),
        intArrayOf(0, 255, 0, 180, 0, 110),
    )

    fun forPhase(type: PhaseType) = when (type) {
        PhaseType.HARD -> goHard()
        PhaseType.RECOVERY -> recovery()
        PhaseType.WARM_UP, PhaseType.COOL_DOWN -> easyPhase()
    }

    private fun play(timings: LongArray, amplitudes: IntArray) {
        if (!vibrator.hasVibrator()) return
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
    }
}
