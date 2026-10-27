package com.patearl.fourbyfour.workout

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status as OngoingStatus
import com.patearl.fourbyfour.MainActivity
import com.patearl.fourbyfour.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs the workout clock in the foreground so it keeps ticking (and vibrating) with the screen off.
 * Time is measured with [SystemClock.elapsedRealtime] so it stays correct across deep sleep.
 */
class WorkoutService : LifecycleService() {

    private lateinit var haptics: Haptics
    private lateinit var heartRate: HeartRateMonitor
    private lateinit var wakeLock: PowerManager.WakeLock

    private var tickJob: Job? = null
    private var accumulatedMs = 0L
    private var segmentStartedAt = 0L
    private var lastPhaseIndex = -1
    private var lastCountdownSecond = -1L

    override fun onCreate() {
        super.onCreate()
        haptics = Haptics(this)
        heartRate = HeartRateMonitor(this) { bpm -> WorkoutRepository.update { it.copy(heartRate = bpm) } }
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FourByFour:workout")
            .apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stop(finished = false)
            else -> if (WorkoutRepository.state.value.status == Status.IDLE) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start() {
        if (WorkoutRepository.state.value.status.let { it == Status.RUNNING || it == Status.PAUSED }) return
        startInForeground()
        accumulatedMs = 0L
        lastPhaseIndex = -1
        lastCountdownSecond = -1L
        WorkoutRepository.update { WorkoutState(status = Status.RUNNING, heartRate = it.heartRate) }
        heartRate.start()
        resumeClock()
    }

    private fun pause() {
        if (WorkoutRepository.state.value.status != Status.RUNNING) return
        tickJob?.cancel()
        accumulatedMs += SystemClock.elapsedRealtime() - segmentStartedAt
        releaseWakeLock()
        WorkoutRepository.update { it.copy(status = Status.PAUSED) }
        updateNotification()
    }

    private fun resume() {
        if (WorkoutRepository.state.value.status != Status.PAUSED) return
        WorkoutRepository.update { it.copy(status = Status.RUNNING) }
        resumeClock()
    }

    private fun resumeClock() {
        segmentStartedAt = SystemClock.elapsedRealtime()
        val remainingWorkout = WorkoutPlan.totalMs - accumulatedMs
        wakeLock.acquire(remainingWorkout + WAKE_LOCK_SLACK_MS)
        tickJob?.cancel()
        tickJob = lifecycleScope.launch {
            while (isActive) {
                val elapsed = accumulatedMs + SystemClock.elapsedRealtime() - segmentStartedAt
                val position = WorkoutPlan.positionAt(elapsed)
                if (position == null) {
                    haptics.finished()
                    stop(finished = true)
                    return@launch
                }
                onTick(position)
                // Wake up right as the displayed second changes.
                val untilNextSecond = (position.remainingMs - 1) % 1000 + 1
                delay(untilNextSecond)
            }
        }
    }

    private fun onTick(position: Position) {
        WorkoutRepository.update { it.copy(phaseIndex = position.index, remainingMs = position.remainingMs) }
        val secondsLeft = (position.remainingMs + 999) / 1000
        if (position.index != lastPhaseIndex) {
            haptics.forPhase(position.phase.type)
            lastPhaseIndex = position.index
            lastCountdownSecond = -1L
            updateNotification()
        } else if (secondsLeft in 1..3 && secondsLeft != lastCountdownSecond) {
            haptics.countdownTick()
            lastCountdownSecond = secondsLeft
        }
    }

    private fun stop(finished: Boolean) {
        tickJob?.cancel()
        heartRate.stop()
        releaseWakeLock()
        WorkoutRepository.update {
            if (finished) it.copy(status = Status.FINISHED, remainingMs = 0) else WorkoutState()
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        heartRate.stop()
        releaseWakeLock()
        val status = WorkoutRepository.state.value.status
        if (status == Status.RUNNING || status == Status.PAUSED) WorkoutRepository.update { WorkoutState() }
        super.onDestroy()
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    // --- Notification / Ongoing Activity ---------------------------------------------------------

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW),
        )
        // The "health" type requires the body sensors permission; fall back to specialUse without it.
        val type = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            0
        } else if (checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun updateNotification() {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    /** Builds the notification with an Ongoing Activity attached, so the watch face shows a shortcut back. */
    private fun buildNotification(): Notification {
        val builder = notificationBuilder()
        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_stat_workout)
            .setTouchIntent(openAppIntent())
            .setStatus(ongoingStatus())
            .build()
            .apply(applicationContext)
        return builder.build()
    }

    private fun statusText(): String {
        val state = WorkoutRepository.state.value
        val phase = state.phase
        val round = if (phase.round > 0) " ${phase.round}/${WorkoutPlan.ROUNDS}" else ""
        val paused = if (state.status == Status.PAUSED) " (paused)" else ""
        return "${phase.type.label}$round$paused"
    }

    private fun ongoingStatus(): OngoingStatus = OngoingStatus.Builder()
        .addTemplate("#phase#")
        .addPart("phase", OngoingStatus.TextPart(statusText()))
        .build()

    private fun notificationBuilder(): NotificationCompat.Builder {
        val state = WorkoutRepository.state.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_workout)
            .setContentTitle("Norwegian 4×4")
            .setContentText(statusText())
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .apply {
                if (state.status == Status.RUNNING) {
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                    setWhen(System.currentTimeMillis() + state.remainingMs)
                    setShowWhen(true)
                }
            }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val CHANNEL_ID = "workout"
        private const val NOTIFICATION_ID = 44
        private const val WAKE_LOCK_SLACK_MS = 60_000L

        private const val ACTION_START = "com.patearl.fourbyfour.START"
        private const val ACTION_PAUSE = "com.patearl.fourbyfour.PAUSE"
        private const val ACTION_RESUME = "com.patearl.fourbyfour.RESUME"
        private const val ACTION_STOP = "com.patearl.fourbyfour.STOP"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, WorkoutService::class.java).setAction(ACTION_START))

        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun resume(context: Context) = send(context, ACTION_RESUME)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String) {
            context.startService(Intent(context, WorkoutService::class.java).setAction(action))
        }
    }
}
