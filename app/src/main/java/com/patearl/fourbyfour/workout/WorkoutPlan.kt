package com.patearl.fourbyfour.workout

enum class PhaseType(val label: String) {
    WARM_UP("Warm-up"),
    HARD("Go hard"),
    RECOVERY("Recovery"),
    COOL_DOWN("Cool-down"),
}

/** One segment of the workout. [round] is 1-based for intervals and 0 for warm-up/cool-down. */
data class Phase(val type: PhaseType, val durationMs: Long, val round: Int = 0)

/** Where the workout is at a given elapsed time. */
data class Position(val index: Int, val phase: Phase, val remainingMs: Long)

/**
 * The Norwegian 4×4: 10 min warm-up, 4 × 4 min hard with 3 min recovery between them,
 * then straight into a 5 min cool-down after the last hard interval.
 */
object WorkoutPlan {
    const val ROUNDS = 4
    private const val MINUTE = 60_000L

    val phases: List<Phase> = buildList {
        add(Phase(PhaseType.WARM_UP, 10 * MINUTE))
        for (round in 1..ROUNDS) {
            add(Phase(PhaseType.HARD, 4 * MINUTE, round))
            if (round < ROUNDS) add(Phase(PhaseType.RECOVERY, 3 * MINUTE, round))
        }
        add(Phase(PhaseType.COOL_DOWN, 5 * MINUTE))
    }

    val totalMs: Long = phases.sumOf { it.durationMs }

    /** Returns the position at [elapsedMs], or null once the workout is complete. */
    fun positionAt(elapsedMs: Long): Position? {
        var start = 0L
        phases.forEachIndexed { index, phase ->
            val end = start + phase.durationMs
            if (elapsedMs < end) return Position(index, phase, end - elapsedMs)
            start = end
        }
        return null
    }
}
