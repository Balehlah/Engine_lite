package engine.incubator.runtime.input;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bounded FIFO between backend callbacks and logical input ticks.
 *
 * <p>Only adjacent absolute pointer movements may be coalesced. Key/button
 * edges, scroll and focus events are never merged or silently discarded. If a
 * non-coalescible event reaches a full queue, enqueue fails immediately and
 * the overflow remains observable through {@link #metrics()}.</p>
 */
public final class InputEventQueue {
    public static final int DEFAULT_CAPACITY = 4_096;

    private final int capacity;
    private final ArrayDeque<InputEvent> events;
    private long acceptedEventCount;
    private long coalescedMovementCount;
    private long overflowCount;

    public InputEventQueue() {
        this(DEFAULT_CAPACITY);
    }

    public InputEventQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        events = new ArrayDeque<>(capacity);
    }

    public synchronized EnqueueResult enqueue(InputEvent event) {
        Objects.requireNonNull(event, "event");

        if (
            event instanceof InputEvent.PointerMoved
                && events.peekLast() instanceof InputEvent.PointerMoved
        ) {
            events.removeLast();
            events.addLast(event);
            acceptedEventCount = Math.incrementExact(acceptedEventCount);
            coalescedMovementCount = Math.incrementExact(coalescedMovementCount);
            return EnqueueResult.MOVEMENT_COALESCED;
        }

        if (events.size() == capacity) {
            overflowCount = Math.incrementExact(overflowCount);
            throw new OverflowException(capacity, event);
        }

        events.addLast(event);
        acceptedEventCount = Math.incrementExact(acceptedEventCount);
        return EnqueueResult.ENQUEUED;
    }

    public synchronized List<InputEvent> drain() {
        List<InputEvent> drained = new ArrayList<>(events);
        events.clear();
        return List.copyOf(drained);
    }

    public synchronized Metrics metrics() {
        return new Metrics(
            capacity,
            events.size(),
            acceptedEventCount,
            coalescedMovementCount,
            overflowCount
        );
    }

    public enum EnqueueResult {
        ENQUEUED,
        MOVEMENT_COALESCED,
    }

    public record Metrics(
        int capacity,
        int pendingEventCount,
        long acceptedEventCount,
        long coalescedMovementCount,
        long overflowCount
    ) {
    }

    public static final class OverflowException extends IllegalStateException {
        private final int capacity;
        private final InputEvent rejectedEvent;

        private OverflowException(int capacity, InputEvent rejectedEvent) {
            super(
                "Input event queue capacity "
                    + capacity
                    + " was exhausted before "
                    + rejectedEvent.getClass().getSimpleName()
            );
            this.capacity = capacity;
            this.rejectedEvent = rejectedEvent;
        }

        public int capacity() {
            return capacity;
        }

        public InputEvent rejectedEvent() {
            return rejectedEvent;
        }
    }
}
