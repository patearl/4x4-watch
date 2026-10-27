package com.patearl.fourbyfour

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.patearl.fourbyfour.settings.Settings
import com.patearl.fourbyfour.ui.FinishedScreen
import com.patearl.fourbyfour.ui.FourByFourTheme
import com.patearl.fourbyfour.ui.HomeScreen
import com.patearl.fourbyfour.ui.MaxHrScreen
import com.patearl.fourbyfour.ui.WorkoutScreen
import com.patearl.fourbyfour.workout.Status
import com.patearl.fourbyfour.workout.WorkoutRepository
import com.patearl.fourbyfour.workout.WorkoutService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(applicationContext)

        // Keep the screen on for the whole workout.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WorkoutRepository.state.collect { state ->
                    if (state.status == Status.RUNNING || state.status == Status.PAUSED) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        setContent { FourByFourTheme { App() } }
    }

    @Composable
    private fun App() {
        val state by WorkoutRepository.state.collectAsStateWithLifecycle()
        val maxHr by settings.maxHr.collectAsStateWithLifecycle()

        // Ask for sensors + notifications, then start whatever the answer (HR is optional).
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { WorkoutService.start(this) }

        when (state.status) {
            Status.RUNNING, Status.PAUSED -> WorkoutScreen(
                state = state,
                maxHr = maxHr,
                onPause = { WorkoutService.pause(this) },
                onResume = { WorkoutService.resume(this) },
                onStop = { WorkoutService.stop(this) },
            )
            Status.FINISHED -> FinishedScreen(onDone = WorkoutRepository::reset)
            Status.IDLE -> {
                val navController = rememberSwipeDismissableNavController()
                SwipeDismissableNavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            maxHr = maxHr,
                            onStart = {
                                val permissions = buildList {
                                    add(Manifest.permission.BODY_SENSORS)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                permissionLauncher.launch(permissions.toTypedArray())
                            },
                            onEditMaxHr = { navController.navigate("max_hr") },
                        )
                    }
                    composable("max_hr") {
                        MaxHrScreen(maxHr = maxHr, onChange = settings::setMaxHr)
                    }
                }
            }
        }
    }
}
