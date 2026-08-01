package engine.incubator.runtime.time;

/**
 * Immutable scheduler telemetry suitable for logs and debug overlays.
 */
public record SchedulerMetrics(
    long frameCount,
    long updateCount,
    long clampedFrameCount,
    long catchUpLimitHitCount,
    long clampedWallTimeNanos,
    long catchUpDiscardedSimulationTimeNanos,
    long inactiveWallTimeNanos,
    int lastFrameUpdateCount,
    double interpolationAlpha,
    boolean paused,
    double timeScale
) {
}
