package engine.incubator.runtime.time;

/**
 * Production clock backed by {@link System#nanoTime()}.
 */
public enum SystemNanoClock implements NanoClock {
    INSTANCE;

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }
}
