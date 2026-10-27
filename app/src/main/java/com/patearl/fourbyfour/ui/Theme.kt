package com.patearl.fourbyfour.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import com.patearl.fourbyfour.workout.PhaseType

val PhaseType.color: Color
    get() = when (this) {
        PhaseType.WARM_UP -> Color(0xFF4FC3F7)
        PhaseType.HARD -> Color(0xFFFF5252)
        PhaseType.RECOVERY -> Color(0xFF69F0AE)
        PhaseType.COOL_DOWN -> Color(0xFF9575CD)
    }

@Composable
fun FourByFourTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(primary = Color(0xFFFF5252), onPrimary = Color.Black),
        content = content,
    )
}
