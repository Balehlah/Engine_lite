package engine.incubator.runtime.logging;

/** Bounded execution coordinates attached only when they apply to a log event. */
public record LogContext(Long frame, Long tick, Long world) {
    public LogContext {
        if (frame != null && frame < 0L) {
            throw new IllegalArgumentException("frame must be non-negative");
        }
        if (tick != null && tick < 0L) {
            throw new IllegalArgumentException("tick must be non-negative");
        }
        if (world != null && world < 1L) {
            throw new IllegalArgumentException("world must be positive");
        }
    }

    public static LogContext empty() {
        return new LogContext(null, null, null);
    }

    public static LogContext frame(long frame, long tick) {
        return new LogContext(frame, tick, null);
    }

    public static LogContext worldFrame(long world, long frame, long tick) {
        return new LogContext(frame, tick, world);
    }

    public LogContext withWorld(long world) {
        return new LogContext(frame, tick, world);
    }
}
