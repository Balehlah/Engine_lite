package engine.incubator.events;

/** Receives an event while its phase queue is being drained synchronously. */
@FunctionalInterface
public interface EventHandler<T> {
    void handle(T event);
}
