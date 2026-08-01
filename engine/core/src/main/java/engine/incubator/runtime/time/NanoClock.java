package engine.incubator.runtime.time;

/**
 * Monotonic nanosecond clock used by the runtime scheduler.
 */
@FunctionalInterface
public interface NanoClock {
    long nanoTime();
}
