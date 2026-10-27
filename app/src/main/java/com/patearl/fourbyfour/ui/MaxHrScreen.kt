package com.patearl.fourbyfour.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.StepperDefaults
import androidx.wear.compose.material.Text
import com.patearl.fourbyfour.settings.Settings

/** Max-HR picker: tap +/- or turn the crown. */
@Composable
fun MaxHrScreen(maxHr: Int, onChange: (Int) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val scroll = remember { floatArrayOf(0f) }
    Stepper(
        value = maxHr,
        onValueChange = onChange,
        valueProgression = Settings.MAX_HR_RANGE,
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        modifier = Modifier
            .onRotaryScrollEvent { event ->
                scroll[0] += event.verticalScrollPixels
                val steps = (scroll[0] / ROTARY_PX_PER_BPM).toInt()
                if (steps != 0) {
                    scroll[0] -= steps * ROTARY_PX_PER_BPM
                    onChange((maxHr + steps).coerceIn(Settings.MAX_HR_RANGE))
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Max HR", style = MaterialTheme.typography.caption2)
            Text("$maxHr", style = MaterialTheme.typography.display2)
            val low = maxHr * 85 / 100
            val high = maxHr * 95 / 100
            Text("Hard: $low–$high", style = MaterialTheme.typography.caption3, color = HrZone.TARGET.color)
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

private const val ROTARY_PX_PER_BPM = 48f

