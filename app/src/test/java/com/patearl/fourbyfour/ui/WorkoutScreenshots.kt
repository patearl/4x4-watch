package com.patearl.fourbyfour.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.patearl.fourbyfour.workout.Status
import com.patearl.fourbyfour.workout.WorkoutPlan
import com.patearl.fourbyfour.workout.WorkoutState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the workout screen on a Pixel Watch 2-sized round display into ./screenshots. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w192dp-h192dp-round-xhdpi")
class WorkoutScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val maxHr = 190

    private fun render(name: String, state: WorkoutState) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FourByFourTheme {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.Black)) {
                    WorkoutScreen(state, maxHr, onPause = {}, onResume = {}, onStop = {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        val dir = File(System.getProperty("screenshotDir")!!).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun state(phaseIndex: Int, progress: Float, bpm: Int?, status: Status = Status.RUNNING): WorkoutState {
        val duration = WorkoutPlan.phases[phaseIndex].durationMs
        return WorkoutState(status, phaseIndex, (duration * (1 - progress)).toLong(), bpm)
    }

    @Test fun warmUpStartBigUp() = render("1-warmup-big-up", state(0, 0f, 95))
    @Test fun warmUpEndOnTarget() = render("2-warmup-heart", state(0, 0.9f, 135))
    @Test fun hardBigUp() = render("3-hard-big-up", state(3, 0.3f, 150))
    @Test fun hardSmallDown() = render("4-hard-small-down", state(1, 0.6f, 178))
    @Test fun hardBigDownPaused() = render("5-hard-big-down-paused", state(5, 0.5f, 185, Status.PAUSED))
    @Test fun recoveryOnTarget() = render("6-recovery-heart", state(2, 0.4f, 133))
    @Test fun coolDownBigDown() = render("7-cooldown-big-down", state(8, 0.2f, 140))
    @Test fun noHeartRate() = render("8-no-hr", state(4, 0.75f, null))
}
