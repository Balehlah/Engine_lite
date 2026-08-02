package engine.incubator.runtime.time;

import java.time.Duration;

/**
 * Immutable fixed-timestep and overload policy selected during initialization.
 */
public record FixedTimestepConfig(
    double updatesPerSecond,
    long maximumFrameTimeNanos,
    int maximumCatchUpSteps
) {
    public static final double DEFAULT_UPDATES_PER_SECOND = 60.0;
    public static final Duration DEFAULT_MAXIMUM_FRAME_TIME = Duration.ofMillis(250L);
    public static final int DEFAULT_MAXIMUM_CATCH_UP_STEPS = 5;

    public FixedTimestepConfig {
        if (!Double.isFinite(updatesPerSecond) || updatesPerSecond <= 0.0) {
            throw new IllegalArgumentException("updatesPerSecond must be finite and positive");
        }
        if (maximumFrameTimeNanos <= 0L) {
            throw new IllegalArgumentException("maximumFrameTimeNanos must be positive");
        }
        if (maximumCatchUpSteps <= 0) {
            throw new IllegalArgumentException("maximumCatchUpSteps must be positive");
        }
        if (Math.round(1_000_000_000.0 / updatesPerSecond) <= 0L) {
            throw new IllegalArgumentException("updatesPerSecond is too large for nanosecond timing");
        }
    }

    public static FixedTimestepConfig default60Hz() {
        return of(
            DEFAULT_UPDATES_PER_SECOND,
            DEFAULT_MAXIMUM_FRAME_TIME,
            DEFAULT_MAXIMUM_CATCH_UP_STEPS
        );
    }

    public static FixedTimestepConfig of(
        double updatesPerSecond,
        Duration maximumFrameTime,
        int maximumCatchUpSteps
    ) {
        if (maximumFrameTime == null) {
            throw new NullPointerException("maximumFrameTime");
        }
        return new FixedTimestepConfig(
            updatesPerSecond,
            maximumFrameTime.toNanos(),
            maximumCatchUpSteps
        );
    }

    public long fixedStepNanos() {
        return Math.round(1_000_000_000.0 / updatesPerSecond);
    }

    public double fixedDeltaSeconds() {
        return 1.0 / updatesPerSecond;
    }
}
