package com.patearl.fourbyfour.workout

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Status { IDLE, RUNNING, PAUSED, FINISHED }

data class WorkoutState(
    val status: Status = Status.IDLE,
    val phaseIndex: Int = 0,
    val remainingMs: Long = WorkoutPlan.phases.first().durationMs,
    /** Latest heart rate in bpm, or null when there's no reliable reading. */
    val heartRate: Int? = null,
) {
    val phase: Phase get() = WorkoutPlan.phases[phaseIndex]
    val phaseProgress: Float get() = 1f - remainingMs.toFloat() / phase.durationMs
}

/** Shared between [WorkoutService] (writer) and the UI (reader). */
object WorkoutRepository {
    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    internal fun update(transform: (WorkoutState) -> WorkoutState) {
        while (true) {
            val current = _state.value
            if (_state.compareAndSet(current, transform(current))) return
        }
    }

    /** Clears a finished workout so the home screen shows again. */
    fun reset() {
        if (_state.value.status == Status.FINISHED) _state.value = WorkoutState()
    }
}
