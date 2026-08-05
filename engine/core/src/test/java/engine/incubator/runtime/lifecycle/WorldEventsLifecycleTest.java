package engine.incubator.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.incubator.events.EventPhase;
import engine.incubator.events.EventSubscription;
import engine.incubator.events.EventType;
import engine.incubator.events.WorldEventBus;
import engine.incubator.world.id.EntityId;
import engine.incubator.world.id.SequentialIdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class WorldEventsLifecycleTest {
    private static final EventType<String> MESSAGE = EventType.of("message", String.class);

    @Test
    void sceneUnloadRemovesItsSubscriptionsAndQueuedEvents() {
        GameRuntime runtime = new GameRuntime();
        List<String> calls = new ArrayList<>();
        AtomicReference<EventSubscription> firstHandle = new AtomicReference<>();
        RuntimeScene second = new EmptyScene() {
            @Override
            public void create(GameContext context) {
                context.events().subscribe(this, MESSAGE, calls::add);
            }
        };
        RuntimeScene first = new EmptyScene() {
            @Override
            public void create(GameContext context) {
                firstHandle.set(context.events().subscribe(this, MESSAGE, value ->
                    calls.add("stale:" + value)
                ));
                context.events().post(
                    this,
                    EventPhase.AFTER_FIXED_UPDATE,
                    MESSAGE,
                    "owned-by-first"
                );
            }

            @Override
            public void fixedUpdate(GameContext context, double fixedDeltaSeconds) {
                context.requestScene(second);
            }
        };

        runtime.start(first);
        runtime.fixedUpdate(1.0 / 60.0);

        WorldEventBus bus = runtime.context().events();
        assertAll(
            () -> assertFalse(firstHandle.get().isActive()),
            () -> assertEquals(1, bus.subscriptionCount()),
            () -> assertEquals(0, bus.pendingEventCount())
        );
        bus.post(
            runtime.context().executionOwner(),
            EventPhase.AFTER_FIXED_UPDATE,
            MESSAGE,
            "current"
        );
        bus.dispatch(EventPhase.AFTER_FIXED_UPDATE);
        assertEquals(List.of("current"), calls);

        runtime.close();
        assertTrue(runtime.metrics().lastClosedContext().isClean());
    }

    @Test
    void oneHundredWorldRestartsResetIdsAndCloseEveryOldBus() {
        GameRuntime runtime = new GameRuntime(() -> new SequentialIdGenerator(500L));
        List<EntityId> firstIds = new ArrayList<>();
        List<WorldEventBus> oldBuses = new ArrayList<>();
        List<EventSubscription> oldHandles = new ArrayList<>();
        AtomicInteger delivered = new AtomicInteger();
        runtime.start(new IdentityScene(firstIds, oldHandles, delivered));

        for (int restart = 1; restart <= 100; restart++) {
            GameContext previous = runtime.context();
            oldBuses.add(previous.events());
            runtime.restart(new IdentityScene(firstIds, oldHandles, delivered));
            assertAll(
                () -> assertTrue(previous.isClosed()),
                () -> assertTrue(previous.snapshot().isClean()),
                () -> assertNotSame(previous.events(), runtime.context().events())
            );
        }

        assertAll(
            () -> assertEquals(101, firstIds.size()),
            () -> assertTrue(firstIds.stream().allMatch(new EntityId(500L)::equals)),
            () -> assertEquals(101, oldHandles.size()),
            () -> assertTrue(oldBuses.stream().allMatch(WorldEventBus::isClosed)),
            () -> assertTrue(oldHandles.subList(0, 100).stream().noneMatch(
                EventSubscription::isActive
            )),
            () -> assertEquals(0, delivered.get())
        );

        WorldEventBus currentBus = runtime.context().events();
        currentBus.post(
            runtime.context().executionOwner(),
            EventPhase.AFTER_FIXED_UPDATE,
            MESSAGE,
            "current"
        );
        currentBus.dispatch(EventPhase.AFTER_FIXED_UPDATE);
        assertEquals(1, delivered.get());

        runtime.close();
        assertAll(
            () -> assertTrue(currentBus.isClosed()),
            () -> assertFalse(oldHandles.get(oldHandles.size() - 1).isActive()),
            () -> assertEquals(101L, runtime.metrics().executionsStarted()),
            () -> assertEquals(100L, runtime.metrics().restarts()),
            () -> assertTrue(runtime.metrics().lastClosedContext().isClean())
        );
    }

    @Test
    void contextBusRejectsOwnersFromOutsideItsWorld() {
        GameRuntime runtime = new GameRuntime();
        Object outsider = new Object();

        assertThrows(
            IllegalArgumentException.class,
            () -> runtime.context().events().subscribe(outsider, MESSAGE, ignored -> {
            })
        );
        runtime.close();
    }

    private static final class IdentityScene extends EmptyScene {
        private final List<EntityId> ids;
        private final List<EventSubscription> handles;
        private final AtomicInteger delivered;

        private IdentityScene(
            List<EntityId> ids,
            List<EventSubscription> handles,
            AtomicInteger delivered
        ) {
            this.ids = ids;
            this.handles = handles;
            this.delivered = delivered;
        }

        @Override
        public void create(GameContext context) {
            ids.add(context.world().register(this, new Object()));
            handles.add(context.events().subscribe(this, MESSAGE, ignored ->
                delivered.incrementAndGet()
            ));
        }
    }

    private static class EmptyScene implements RuntimeScene {
        @Override
        public void create(GameContext context) {
        }

        @Override
        public void enter(GameContext context) {
        }

        @Override
        public void fixedUpdate(GameContext context, double fixedDeltaSeconds) {
        }

        @Override
        public void render(GameContext context, double interpolationAlpha) {
        }

        @Override
        public void exit(GameContext context) {
        }

        @Override
        public void dispose(GameContext context) {
        }
    }
}
