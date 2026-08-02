package engine.incubator.runtime.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deterministic, entirely in-memory input script for tests and simulations. */
public final class FakeInput {
    private final List<List<InputEvent>> eventsByTick;
    private final TickInput input;
    private int cursor;

    public FakeInput(List<? extends List<? extends InputEvent>> eventsByTick) {
        this(eventsByTick, InputEventQueue.DEFAULT_CAPACITY);
    }

    public FakeInput(
        List<? extends List<? extends InputEvent>> eventsByTick,
        int queueCapacity
    ) {
        Objects.requireNonNull(eventsByTick, "eventsByTick");
        List<List<InputEvent>> copy = new ArrayList<>(eventsByTick.size());
        for (List<? extends InputEvent> tickEvents : eventsByTick) {
            copy.add(List.copyOf(Objects.requireNonNull(tickEvents, "tickEvents")));
        }
        this.eventsByTick = List.copyOf(copy);
        input = new TickInput(queueCapacity);
    }

    public boolean hasNext() {
        return cursor < eventsByTick.size();
    }

    public InputSnapshot nextSnapshot(ScreenToVirtual mapping) {
        if (!hasNext()) {
            throw new IllegalStateException("Fake input script is exhausted");
        }
        for (InputEvent event : eventsByTick.get(cursor)) {
            input.enqueue(event);
        }
        cursor++;
        return input.nextSnapshot(mapping);
    }

    public List<InputSnapshot> replay(List<ScreenToVirtual> mappings) {
        Objects.requireNonNull(mappings, "mappings");
        if (mappings.size() != eventsByTick.size()) {
            throw new IllegalArgumentException(
                "Expected "
                    + eventsByTick.size()
                    + " mappings but received "
                    + mappings.size()
            );
        }
        List<InputSnapshot> snapshots = new ArrayList<>(mappings.size());
        for (ScreenToVirtual mapping : mappings) {
            snapshots.add(nextSnapshot(Objects.requireNonNull(mapping, "mapping")));
        }
        return List.copyOf(snapshots);
    }

    public InputEventQueue.Metrics queueMetrics() {
        return input.queueMetrics();
    }
}
