package engine.incubator.runtime.time;

import java.util.Objects;

/**
 * Clock-driven scheduler with a fixed logical step and independent host frames.
 */
public final class FixedTimestepScheduler {
    private final NanoClock clock;
    private final FixedTimestepConfig configuration;
    private final FixedStepAccumulator accumulator;

    private long previousClockNanos;
    private boolean paused;
    private int pendingSteps;
    private double timeScale = 1.0;

    private long frameCount;
    private long updateCount;
    private long clampedFrameCount;
    private long catchUpLimitHitCount;
    private long clampedWallTimeNanos;
    private long catchUpDiscardedSimulationTimeNanos;
    private long inactiveWallTimeNanos;
    private int lastFrameUpdateCount;
    private double interpolationAlpha;

    public FixedTimestepScheduler(NanoClock clock, FixedTimestepConfig configuration) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        accumulator = new FixedStepAccumulator(configuration);
        previousClockNanos = clock.nanoTime();
    }

    public static FixedTimestepScheduler createDefault() {
        return new FixedTimestepScheduler(
            SystemNanoClock.INSTANCE,
            FixedTimestepConfig.default60Hz()
        );
    }

    public FrameSchedule nextFrame() {
        long now = clock.nanoTime();
        long wallElapsedNanos = now - previousClockNanos;
        if (wallElapsedNanos < 0L) {
            throw new IllegalStateException("NanoClock moved backwards");
        }
        previousClockNanos = now;

        int scheduledUpdates;
        long clampDiscarded = 0L;
        long catchUpDiscarded = 0L;
        long inactiveDiscarded = 0L;

        if (paused || timeScale == 0.0) {
            inactiveDiscarded = wallElapsedNanos;
            scheduledUpdates = paused && pendingSteps > 0 ? 1 : 0;
            if (scheduledUpdates == 1) {
                pendingSteps--;
            }
            interpolationAlpha = accumulator.interpolationAlpha();
        } else {
            AccumulatorFrame frame = accumulator.advance(wallElapsedNanos, timeScale);
            scheduledUpdates = frame.updateCount();
            clampDiscarded = frame.clampDiscardedWallTimeNanos();
            catchUpDiscarded = frame.catchUpDiscardedSimulationTimeNanos();
            interpolationAlpha = frame.interpolationAlpha();
            if (frame.wasClamped()) {
                clampedFrameCount++;
            }
            if (frame.reachedCatchUpLimit()) {
                catchUpLimitHitCount++;
            }
        }

        frameCount++;
        updateCount = saturatingAdd(updateCount, scheduledUpdates);
        clampedWallTimeNanos = saturatingAdd(clampedWallTimeNanos, clampDiscarded);
        catchUpDiscardedSimulationTimeNanos = saturatingAdd(
            catchUpDiscardedSimulationTimeNanos,
            catchUpDiscarded
        );
        inactiveWallTimeNanos = saturatingAdd(inactiveWallTimeNanos, inactiveDiscarded);
        lastFrameUpdateCount = scheduledUpdates;

        SchedulerMetrics metrics = metrics();
        return new FrameSchedule(
            scheduledUpdates,
            configuration.fixedDeltaSeconds(),
            interpolationAlpha,
            wallElapsedNanos,
            clampDiscarded,
            catchUpDiscarded,
            inactiveDiscarded,
            metrics
        );
    }

    public SchedulerMetrics metrics() {
        return new SchedulerMetrics(
            frameCount,
            updateCount,
            clampedFrameCount,
            catchUpLimitHitCount,
            clampedWallTimeNanos,
            catchUpDiscardedSimulationTimeNanos,
            inactiveWallTimeNanos,
            lastFrameUpdateCount,
            interpolationAlpha,
            paused,
            timeScale
        );
    }

    public FixedTimestepConfig configuration() {
        return configuration;
    }

    public void pause() {
        if (!paused) {
            discardClockElapsedAsInactive();
            paused = true;
        }
    }

    public void resume() {
        if (paused) {
            discardClockElapsedAsInactive();
            paused = false;
            pendingSteps = 0;
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void requestStep() {
        if (!paused) {
            throw new IllegalStateException("Single-step is available only while paused");
        }
        pendingSteps = Math.addExact(pendingSteps, 1);
    }

    public double timeScale() {
        return timeScale;
    }

    public void setTimeScale(double timeScale) {
        if (!Double.isFinite(timeScale) || timeScale < 0.0) {
            throw new IllegalArgumentException("timeScale must be finite and non-negative");
        }
        if ((this.timeScale == 0.0) != (timeScale == 0.0)) {
            discardClockElapsedAsInactive();
        }
        this.timeScale = timeScale;
    }

    public void reset() {
        accumulator.reset();
        pendingSteps = 0;
        frameCount = 0L;
        updateCount = 0L;
        clampedFrameCount = 0L;
        catchUpLimitHitCount = 0L;
        clampedWallTimeNanos = 0L;
        catchUpDiscardedSimulationTimeNanos = 0L;
        inactiveWallTimeNanos = 0L;
        lastFrameUpdateCount = 0;
        interpolationAlpha = 0.0;
        rebaseClock();
    }

    private void rebaseClock() {
        previousClockNanos = clock.nanoTime();
    }

    private void discardClockElapsedAsInactive() {
        long now = clock.nanoTime();
        long elapsed = now - previousClockNanos;
        if (elapsed < 0L) {
            throw new IllegalStateException("NanoClock moved backwards");
        }
        previousClockNanos = now;
        inactiveWallTimeNanos = saturatingAdd(inactiveWallTimeNanos, elapsed);
    }

    private static long saturatingAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
