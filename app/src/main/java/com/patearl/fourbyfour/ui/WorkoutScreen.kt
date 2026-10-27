package com.patearl.fourbyfour.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.patearl.fourbyfour.workout.PhaseType
import com.patearl.fourbyfour.workout.Status
import com.patearl.fourbyfour.workout.WorkoutPlan
import com.patearl.fourbyfour.workout.WorkoutState

@Composable
fun WorkoutScreen(
    state: WorkoutState,
    maxHr: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val phase = state.phase
    val accent = phase.type.color
    val paused = state.status == Status.PAUSED
    var confirmStop by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = state.phaseProgress,
            modifier = Modifier.fillMaxSize().padding(2.dp),
            indicatorColor = if (paused) Color.Gray else accent,
            trackColor = accent.copy(alpha = 0.2f),
            strokeWidth = 6.dp,
        )
        // Sizes are fixed so the content stays inside the ring whatever the phase or HR cue.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = phase.type.label.uppercase(),
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = if (phase.type == PhaseType.HARD) 18.sp else 16.sp,
                lineHeight = 20.sp,
            )
            Text(
                text = if (phase.round > 0) "Round ${phase.round} of ${WorkoutPlan.ROUNDS}" else " ",
                fontSize = 12.sp,
                lineHeight = 14.sp,
            )
            Text(
                text = formatCountdown(state.remainingMs),
                fontSize = 42.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Medium,
                // Tabular digits so the countdown doesn't shift sideways as it ticks.
                style = TextStyle(fontFeatureSettings = "tnum"),
                color = if (paused) Color.Gray else Color.White,
            )
            HeartRateRow(state.heartRate, maxHr, phase.type, state.phaseProgress)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = if (paused) onResume else onPause,
                    modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) {
                    if (paused) {
                        Icon(Icons.Filled.PlayArrow, "Resume", Modifier.size(18.dp))
                    } else {
                        Text("❚❚", fontSize = 10.sp)
                    }
                }
                Button(
                    onClick = { confirmStop = true },
                    modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) {
                    Icon(Icons.Filled.Close, "Stop", Modifier.size(18.dp))
                }
            }
        }
    }

    Dialog(showDialog = confirmStop, onDismissRequest = { confirmStop = false }) {
        Alert(
            title = { Text("End workout?") },
            negativeButton = {
                Button(onClick = { confirmStop = false }, colors = ButtonDefaults.secondaryButtonColors()) {
                    Icon(Icons.Filled.PlayArrow, "Keep going")
                }
            },
            positiveButton = {
                Button(onClick = { confirmStop = false; onStop() }) {
                    Icon(Icons.Filled.Close, "End")
                }
            },
        )
    }
}

@Composable
private fun HeartRateRow(bpm: Int?, maxHr: Int, phase: PhaseType, phaseProgress: Float) {
    val zone = bpm?.let { HrZone.of(it, maxHr) }
    val color = zone?.color ?: Color.Gray
    val cue = if (bpm != null) HrTargets.cue(phase, bpm, maxHr, phaseProgress) else HrCue.ON_TARGET
    Row(Modifier.height(26.dp), verticalAlignment = Alignment.CenterVertically) {
        // Fixed-size slot so swapping heart/arrows never moves the layout.
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            when (cue) {
                HrCue.ON_TARGET -> Icon(Icons.Filled.Favorite, "Heart rate", tint = color, modifier = Modifier.size(16.dp))
                HrCue.SMALL_UP -> Icon(ArrowUp, "Slightly low", tint = color, modifier = Modifier.size(14.dp))
                HrCue.BIG_UP -> Icon(ArrowUp, "Too low", tint = color, modifier = Modifier.size(26.dp))
                HrCue.SMALL_DOWN -> Icon(ArrowDown, "Slightly high", tint = color, modifier = Modifier.size(14.dp))
                HrCue.BIG_DOWN -> Icon(ArrowDown, "Too high", tint = color, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = bpm?.toString() ?: "--",
            color = color,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        if (bpm != null) {
            Spacer(Modifier.width(4.dp))
            Text("${bpm * 100 / maxHr}%", color = color, style = MaterialTheme.typography.caption2)
        }
    }
}

@Composable
fun FinishedScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Done!", style = MaterialTheme.typography.display3, color = PhaseType.RECOVERY.color)
        Text("4 × 4 complete", style = MaterialTheme.typography.caption1)
        Spacer(Modifier.height(10.dp))
        Button(onClick = onDone) { Icon(Icons.Filled.Close, "Close") }
    }
}

private fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private val ArrowUp: ImageVector = materialIcon(name = "ArrowUp") {
    materialPath {
        moveTo(4f, 12f); lineToRelative(1.41f, 1.41f); lineTo(11f, 7.83f); verticalLineTo(20f)
        horizontalLineToRelative(2f); verticalLineTo(7.83f); lineToRelative(5.58f, 5.59f)
        lineTo(20f, 12f); lineToRelative(-8f, -8f); close()
    }
}

private val ArrowDown: ImageVector = materialIcon(name = "ArrowDown") {
    materialPath {
        moveTo(20f, 12f); lineToRelative(-1.41f, -1.41f); lineTo(13f, 16.17f); verticalLineTo(4f)
        horizontalLineToRelative(-2f); verticalLineToRelative(12.17f); lineToRelative(-5.58f, -5.59f)
        lineTo(4f, 12f); lineToRelative(8f, 8f); close()
    }
}
