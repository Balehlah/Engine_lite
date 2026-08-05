package engine.incubator.events;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Synchronous typed event bus owned by one world.
 *
 * <p>Events are FIFO inside each phase. For each event, active handlers run in subscription
 * order. A handler removed before its turn is skipped; a handler added during delivery starts
 * with the next event. Events posted during delivery are appended to their phase queue.
 * Recursive calls to {@link #dispatch(EventPhase)} are rejected so handler stacks cannot
 * silently reorder work.</p>
 *
 * <p>This type is intentionally single-threaded and contains no global state.</p>
 */
public final class WorldEventBus implements AutoCloseable {
    private final EnumMap<EventPhase, ArrayDeque<QueuedEvent>> queues =
        new EnumMap<>(EventPhase.class);
    private final Map<EventType<?>, List<Registration<?>>> handlersByType =
        new LinkedHashMap<>();
    private final IdentityHashMap<Object, List<Registration<?>>> handlersByOwner =
        new IdentityHashMap<>();
    private final List<WeakReference<Object>> unloadedOwners = new ArrayList<>();
    private final Predicate<Object> ownerValidator;
    private boolean dispatching;
    private boolean closed;

    public WorldEventBus() {
        this(ignored -> true);
    }

    /**
     * Creates a bus whose owners must pass the supplied world-ownership check.
     * The validator may throw a more specific lifecycle exception.
     */
    public WorldEventBus(Predicate<Object> ownerValidator) {
        this.ownerValidator = Objects.requireNonNull(ownerValidator, "ownerValidator");
        for (EventPhase phase : EventPhase.values()) {
            queues.put(phase, new ArrayDeque<>());
        }
    }

    public <T> EventSubscription subscribe(
        Object owner,
        EventType<T> type,
        EventHandler<? super T> handler
    ) {
        requireOwnerActive(owner);
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");

        Registration<T> registration = new Registration<>(this, owner, type, handler);
        handlersByType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(registration);
        handlersByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(registration);
        return registration;
    }

    public <T> void post(
        Object owner,
        EventPhase phase,
        EventType<T> type,
        T event
    ) {
        requireOwnerActive(owner);
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(type, "type");
        queues.get(phase).addLast(new QueuedEvent(
            owner,
            type,
            type.cast(Objects.requireNonNull(event, "event"))
        ));
    }

    /** Drains one phase synchronously; other phase queues remain untouched. */
    public void dispatch(EventPhase phase) {
        requireOpen();
        Objects.requireNonNull(phase, "phase");
        if (dispatching) {
            throw new IllegalStateException("Reentrant event dispatch is not allowed");
        }

        dispatching = true;
        try {
            ArrayDeque<QueuedEvent> queue = queues.get(phase);
            while (!queue.isEmpty()) {
                QueuedEvent event = queue.removeFirst();
                List<Registration<?>> registrations = handlersByType.get(event.type);
                if (registrations == null || registrations.isEmpty()) {
                    continue;
                }
                for (Registration<?> registration : List.copyOf(registrations)) {
                    if (registration.active) {
                        deliver(registration, event);
                    }
                }
            }
        } finally {
            dispatching = false;
        }
    }

    /** Removes every queued event and subscription owned by {@code owner}. */
    public void unload(Object owner) {
        Objects.requireNonNull(owner, "owner");
        if (closed || wasUnloaded(owner)) {
            return;
        }
        unloadedOwners.add(new WeakReference<>(owner));

        List<Registration<?>> registrations = handlersByOwner.remove(owner);
        if (registrations != null) {
            for (Registration<?> registration : List.copyOf(registrations)) {
                remove(registration);
            }
        }
        for (ArrayDeque<QueuedEvent> queue : queues.values()) {
            Iterator<QueuedEvent> iterator = queue.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().owner == owner) {
                    iterator.remove();
                }
            }
        }
    }

    public int pendingEventCount() {
        return queues.values().stream().mapToInt(ArrayDeque::size).sum();
    }

    public int subscriptionCount() {
        return handlersByType.values().stream().mapToInt(List::size).sum();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        handlersByType.values().forEach(registrations ->
            registrations.forEach(Registration::deactivate)
        );
        handlersByType.clear();
        handlersByOwner.clear();
        unloadedOwners.clear();
        queues.values().forEach(ArrayDeque::clear);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("World event bus is closed");
        }
    }

    private void requireOwnerActive(Object owner) {
        requireOpen();
        Objects.requireNonNull(owner, "owner");
        if (wasUnloaded(owner)) {
            throw new IllegalStateException("Event owner is unloaded");
        }
        if (!ownerValidator.test(owner)) {
            throw new IllegalArgumentException("Event owner does not belong to this world");
        }
    }

    private boolean wasUnloaded(Object owner) {
        boolean found = false;
        for (int index = unloadedOwners.size() - 1; index >= 0; index--) {
            Object unloadedOwner = unloadedOwners.get(index).get();
            if (unloadedOwner == null) {
                unloadedOwners.remove(index);
            } else if (unloadedOwner == owner) {
                found = true;
            }
        }
        return found;
    }

    private void remove(Registration<?> registration) {
        if (!registration.active) {
            return;
        }
        EventType<?> type = registration.type;
        Object owner = registration.owner;
        registration.deactivate();
        removeFromIndex(handlersByType, type, registration);
        removeFromIdentityIndex(handlersByOwner, owner, registration);
    }

    private static <K> void removeFromIndex(
        Map<K, List<Registration<?>>> index,
        K key,
        Registration<?> registration
    ) {
        List<Registration<?>> registrations = index.get(key);
        if (registrations == null) {
            return;
        }
        registrations.remove(registration);
        if (registrations.isEmpty()) {
            index.remove(key);
        }
    }

    private static void removeFromIdentityIndex(
        IdentityHashMap<Object, List<Registration<?>>> index,
        Object key,
        Registration<?> registration
    ) {
        List<Registration<?>> registrations = index.get(key);
        if (registrations == null) {
            return;
        }
        registrations.remove(registration);
        if (registrations.isEmpty()) {
            index.remove(key);
        }
    }

    private static <T> void deliver(Registration<T> registration, QueuedEvent event) {
        registration.handler.handle(registration.type.cast(event.payload));
    }

    private static final class Registration<T> implements EventSubscription {
        private WorldEventBus bus;
        private Object owner;
        private EventType<T> type;
        private EventHandler<? super T> handler;
        private boolean active = true;

        private Registration(
            WorldEventBus bus,
            Object owner,
            EventType<T> type,
            EventHandler<? super T> handler
        ) {
            this.bus = bus;
            this.owner = owner;
            this.type = type;
            this.handler = handler;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void unsubscribe() {
            WorldEventBus currentBus = bus;
            if (currentBus != null) {
                currentBus.remove(this);
            }
        }

        private void deactivate() {
            active = false;
            bus = null;
            owner = null;
            type = null;
            handler = null;
        }
    }

    private record QueuedEvent(Object owner, EventType<?> type, Object payload) {
    }
}
