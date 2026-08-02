package engine.incubator.runtime.time;

import java.time.Duration;

/**
 * Manually advanced monotonic clock for deterministic tests and replays.
 */
public final class FakeNanoClock implements NanoClock {
    private long currentNanos;

    public FakeNanoClock() {
        this(0L);
    }

    public FakeNanoClock(long initialNanos) {
        if (initialNanos < 0L) {
            throw new IllegalArgumentException("initialNanos must be non-negative");
        }
        currentNanos = initialNanos;
    }

    @Override
    public long nanoTime() {
        return currentNanos;
    }

    public long advanceNanos(long nanos) {
        if (nanos < 0L) {
            throw new IllegalArgumentException("nanos must be non-negative");
        }
        currentNanos = Math.addExact(currentNanos, nanos);
        return currentNanos;
    }

    public long advance(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must be non-negative");
        }
        return advanceNanos(duration.toNanos());
    }
}
