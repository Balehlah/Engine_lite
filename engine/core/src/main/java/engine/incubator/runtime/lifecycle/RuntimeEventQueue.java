package engine.incubator.runtime.lifecycle;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

/**
 * FIFO event queue scoped to one game execution.
 */
public final class RuntimeEventQueue {
    private final OwnedResourceRegistry ownership;
    private final ArrayDeque<OwnedEvent> events = new ArrayDeque<>();

    RuntimeEventQueue(OwnedResourceRegistry ownership) {
        this.ownership = ownership;
    }

    public void post(Object owner, Object event) {
        ownership.requireActiveOwner(owner);
        events.addLast(new OwnedEvent(owner, Objects.requireNonNull(event, "event")));
    }

    public Optional<Object> poll() {
        OwnedEvent event = events.pollFirst();
        return event == null ? Optional.empty() : Optional.of(event.payload);
    }

    public int size() {
        return events.size();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    void releaseOwner(Object owner) {
        Iterator<OwnedEvent> iterator = events.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().owner == owner) {
                iterator.remove();
            }
        }
    }

    void clear() {
        events.clear();
    }

    private record OwnedEvent(Object owner, Object payload) {
    }
}
