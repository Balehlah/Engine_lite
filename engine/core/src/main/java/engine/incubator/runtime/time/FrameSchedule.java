package engine.incubator.runtime.time;

import java.util.Objects;

/**
 * Work scheduled for one host render frame.
 */
public record FrameSchedule(
    int updateCount,
    double fixedDeltaSeconds,
    double interpolationAlpha,
    long wallElapsedNanos,
    long clampDiscardedWallTimeNanos,
    long catchUpDiscardedSimulationTimeNanos,
    long inactiveWallTimeNanos,
    SchedulerMetrics metrics
) {
    public FrameSchedule {
        if (updateCount < 0) {
            throw new IllegalArgumentException("updateCount must be non-negative");
        }
        if (!Double.isFinite(fixedDeltaSeconds) || fixedDeltaSeconds <= 0.0) {
            throw new IllegalArgumentException("fixedDeltaSeconds must be finite and positive");
        }
        if (
            !Double.isFinite(interpolationAlpha)
                || interpolationAlpha < 0.0
                || interpolationAlpha >= 1.0
        ) {
            throw new IllegalArgumentException("interpolationAlpha must be in [0, 1)");
        }
        if (
            wallElapsedNanos < 0L
                || clampDiscardedWallTimeNanos < 0L
                || catchUpDiscardedSimulationTimeNanos < 0L
                || inactiveWallTimeNanos < 0L
        ) {
            throw new IllegalArgumentException("time values must be non-negative");
        }
        metrics = Objects.requireNonNull(metrics, "metrics");
    }
}
