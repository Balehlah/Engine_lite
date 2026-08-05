package engine.incubator.events;

/** Idempotent handle for one world-event handler registration. */
public interface EventSubscription extends AutoCloseable {
    boolean isActive();

    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
