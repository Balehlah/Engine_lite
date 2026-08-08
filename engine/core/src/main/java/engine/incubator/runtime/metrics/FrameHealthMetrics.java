package engine.incubator.runtime.metrics;

import java.util.Objects;

/** Immutable frame-health snapshot shared by logs, tests and backend overlays. */
public record FrameHealthMetrics(
    long frame,
    long tick,
    double framesPerSecond,
    double updatesPerSecond,
    int updatesThisFrame,
    long catchUpLimitHits,
    long catchUpDiscardedNanos,
    long clampedWallNanos,
    double interpolationAlpha,
    boolean paused,
    AssetHealthMetrics assets,
    long drawCalls
) {
    public FrameHealthMetrics {
        Objects.requireNonNull(assets, "assets");
        if (
            frame < 0L
                || tick < 0L
                || framesPerSecond < 0.0
                || updatesPerSecond < 0.0
                || updatesThisFrame < 0
                || catchUpLimitHits < 0L
                || catchUpDiscardedNanos < 0L
                || clampedWallNanos < 0L
                || drawCalls < 0L
        ) {
            throw new IllegalArgumentException("frame health counters must be non-negative");
        }
        if (
            !Double.isFinite(framesPerSecond)
                || !Double.isFinite(updatesPerSecond)
                || !Double.isFinite(interpolationAlpha)
                || interpolationAlpha < 0.0
                || interpolationAlpha >= 1.0
        ) {
            throw new IllegalArgumentException("frame health rates or alpha are invalid");
        }
    }
}
