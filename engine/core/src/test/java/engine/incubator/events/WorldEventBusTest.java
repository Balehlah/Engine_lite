package engine.incubator.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class WorldEventBusTest {
    private static final EventType<Integer> NUMBER = EventType.of("number", Integer.class);

    @Test
    void eventsAreFifoPerPhaseAndHandlersFollowSubscriptionOrder() {
        WorldEventBus bus = new WorldEventBus();
        Object firstOwner = new Object();
        Object secondOwner = new Object();
        Object producer = new Object();
        List<String> calls = new ArrayList<>();
        bus.subscribe(firstOwner, NUMBER, value -> calls.add("first:" + value));
        bus.subscribe(secondOwner, NUMBER, value -> calls.add("second:" + value));

        bus.post(producer, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 10);
        bus.post(producer, EventPhase.BEFORE_RENDER, NUMBER, 30);
        bus.post(producer, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 20);

        bus.dispatch(EventPhase.AFTER_FIXED_UPDATE);
        assertEquals(
            List.of("first:10", "second:10", "first:20", "second:20"),
            calls
        );
        assertEquals(1, bus.pendingEventCount());

        bus.dispatch(EventPhase.BEFORE_RENDER);
        assertEquals(
            List.of(
                "first:10",
                "second:10",
                "first:20",
                "second:20",
                "first:30",
                "second:30"
            ),
            calls
        );
    }

    @Test
    void unsubscribeDuringDispatchSkipsAHandlerWhoseTurnHasNotStarted() {
        WorldEventBus bus = new WorldEventBus();
        Object owner = new Object();
        List<String> calls = new ArrayList<>();
        AtomicReference<EventSubscription> second = new AtomicReference<>();
        bus.subscribe(owner, NUMBER, value -> {
            calls.add("first:" + value);
            second.get().unsubscribe();
        });
        second.set(bus.subscribe(owner, NUMBER, value -> calls.add("second:" + value)));

        bus.post(owner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 1);
        bus.post(owner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 2);
        bus.dispatch(EventPhase.AFTER_FIXED_UPDATE);

        assertEquals(List.of("first:1", "first:2"), calls);
        assertFalse(second.get().isActive());
        assertEquals(1, bus.subscriptionCount());
    }

    @Test
    void recursiveDispatchIsRejectedButPostedWorkRemainsFifo() {
        WorldEventBus bus = new WorldEventBus();
        Object owner = new Object();
        List<Integer> calls = new ArrayList<>();
        AtomicBoolean nestedDispatchRejected = new AtomicBoolean();
        bus.subscribe(owner, NUMBER, value -> {
            calls.add(value);
            if (value == 1) {
                assertThrows(
                    IllegalStateException.class,
                    () -> bus.dispatch(EventPhase.AFTER_FIXED_UPDATE)
                );
                nestedDispatchRejected.set(true);
                bus.post(owner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 2);
            }
        });

        bus.post(owner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 1);
        bus.dispatch(EventPhase.AFTER_FIXED_UPDATE);

        assertTrue(nestedDispatchRejected.get());
        assertEquals(List.of(1, 2), calls);
        assertEquals(0, bus.pendingEventCount());
    }

    @Test
    void handlersAddedDuringDeliveryStartWithTheNextEvent() {
        WorldEventBus bus = new WorldEventBus();
        Object owner = new Object();
        List<String> calls = new ArrayList<>();
        AtomicBoolean added = new AtomicBoolean();
        bus.subscribe(owner, NUMBER, value -> {
            calls.add("original:" + value);
            if (added.compareAndSet(false, true)) {
                bus.subscribe(owner, NUMBER, later -> calls.add("late:" + later));
            }
        });
        bus.post(owner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 1);
        bus.post(owner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 2);

        bus.dispatch(EventPhase.AFTER_FIXED_UPDATE);

        assertEquals(List.of("original:1", "original:2", "late:2"), calls);
    }

    @Test
    void unloadRemovesOwnedSubscriptionsAndQueuedEvents() {
        WorldEventBus bus = new WorldEventBus();
        Object removedOwner = new Object();
        Object activeOwner = new Object();
        List<Integer> calls = new ArrayList<>();
        EventSubscription removed = bus.subscribe(
            removedOwner,
            NUMBER,
            value -> calls.add(-value)
        );
        bus.subscribe(activeOwner, NUMBER, calls::add);
        bus.post(removedOwner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 1);
        bus.post(activeOwner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 2);

        bus.unload(removedOwner);
        bus.dispatch(EventPhase.AFTER_FIXED_UPDATE);

        assertFalse(removed.isActive());
        assertEquals(List.of(2), calls);
        assertEquals(1, bus.subscriptionCount());
        assertThrows(
            IllegalStateException.class,
            () -> bus.post(removedOwner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 3)
        );
    }

    @Test
    void closeIsIdempotentAndInvalidatesEveryHandle() {
        WorldEventBus bus = new WorldEventBus();
        Object owner = new Object();
        EventSubscription subscription = bus.subscribe(owner, NUMBER, ignored -> {
        });
        bus.post(owner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, 1);

        bus.close();
        bus.close();

        assertTrue(bus.isClosed());
        assertFalse(subscription.isActive());
        assertEquals(0, bus.pendingEventCount());
        assertEquals(0, bus.subscriptionCount());
        assertThrows(
            IllegalStateException.class,
            () -> bus.dispatch(EventPhase.AFTER_FIXED_UPDATE)
        );
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void invalidPayloadsFailBeforeTheyCanEnterAQueue() {
        WorldEventBus bus = new WorldEventBus();
        Object owner = new Object();
        EventType rawNumber = NUMBER;

        assertThrows(
            ClassCastException.class,
            () -> bus.post(
                owner,
                EventPhase.AFTER_FIXED_UPDATE,
                rawNumber,
                "not-a-number"
            )
        );
        assertThrows(
            NullPointerException.class,
            () -> bus.post(owner, EventPhase.AFTER_FIXED_UPDATE, NUMBER, null)
        );
        assertEquals(0, bus.pendingEventCount());
    }
}
