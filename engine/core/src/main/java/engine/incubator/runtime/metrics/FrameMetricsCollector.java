package engine.incubator.runtime.metrics;

import engine.incubator.runtime.time.NanoClock;
import engine.incubator.runtime.time.SchedulerMetrics;
import java.time.Duration;
import java.util.Objects;

/** Windowed FPS/UPS collector driven by an injectable monotonic clock. */
public final class FrameMetricsCollector {
    private final NanoClock clock;
    private final long sampleWindowNanos;
    private long windowStartNanos;
    private long windowStartFrame;
    private long windowStartTick;
    private double framesPerSecond;
    private double updatesPerSecond;

    public FrameMetricsCollector(NanoClock clock, Duration sampleWindow) {
        this.clock = Objects.requireNonNull(clock, "clock");
        Objects.requireNonNull(sampleWindow, "sampleWindow");
        if (sampleWindow.isZero() || sampleWindow.isNegative()) {
            throw new IllegalArgumentException("sampleWindow must be positive");
        }
        try {
            sampleWindowNanos = sampleWindow.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("sampleWindow is too large", exception);
        }
        windowStartNanos = clock.nanoTime();
    }

    public FrameHealthMetrics recordFrame(
        SchedulerMetrics scheduler,
        AssetHealthMetrics assets,
        long drawCalls
    ) {
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(assets, "assets");
        if (drawCalls < 0L) {
            throw new IllegalArgumentException("drawCalls must be non-negative");
        }
        long now = clock.nanoTime();
        long elapsed = now - windowStartNanos;
        if (elapsed < 0L) {
            throw new IllegalStateException("NanoClock moved backwards");
        }
        if (elapsed >= sampleWindowNanos) {
            double elapsedSeconds = elapsed / 1_000_000_000.0;
            framesPerSecond = (scheduler.frameCount() - windowStartFrame) / elapsedSeconds;
            updatesPerSecond = (scheduler.updateCount() - windowStartTick) / elapsedSeconds;
            windowStartNanos = now;
            windowStartFrame = scheduler.frameCount();
            windowStartTick = scheduler.updateCount();
        }
        return new FrameHealthMetrics(
            scheduler.frameCount(),
            scheduler.updateCount(),
            framesPerSecond,
            updatesPerSecond,
            scheduler.lastFrameUpdateCount(),
            scheduler.catchUpLimitHitCount(),
            scheduler.catchUpDiscardedSimulationTimeNanos(),
            scheduler.clampedWallTimeNanos(),
            scheduler.interpolationAlpha(),
            scheduler.paused(),
            assets,
            drawCalls
        );
    }
}
