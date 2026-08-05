package engine.incubator.runtime.lifecycle;

import engine.incubator.events.EventPhase;
import engine.incubator.events.EventType;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class GameRuntimeLifecycleTest {
    private static final EventType<String> STATE_EVENT =
        EventType.of("runtime.state", String.class);

    @Test
    void hostLifecycleCommandsAreRejectedInsideCallbacksWithoutMutatingExecution() {
        GameRuntime runtime = new GameRuntime();
        GameContext initialContext = runtime.context();
        List<String> calls = new ArrayList<>();
        RecordingScene replacement = new RecordingScene("replacement", calls, ignored -> {
        });
        RecordingScene initial = new RecordingScene("initial", calls, ignored -> {
            assertThrows(IllegalStateException.class, () -> runtime.restart(replacement));
            assertThrows(IllegalStateException.class, runtime::close);
        });

        runtime.start(initial);
        runtime.fixedUpdate(1.0 / 60.0);
        runtime.render(0.0);

        assertAll(
            () -> assertSame(initialContext, runtime.context()),
            () -> assertFalse(initialContext.isClosed()),
            () -> assertFalse(runtime.isClosed()),
            () -> assertEquals(1L, runtime.metrics().executionsStarted()),
            () -> assertEquals(0L, runtime.metrics().restarts()),
            () -> assertEquals(
                List.of(
                    "initial.create",
                    "initial.enter",
                    "initial.update.begin",
                    "initial.update.end",
                    "initial.render"
                ),
                calls
            )
        );

        runtime.close();
        assertTrue(runtime.metrics().lastClosedContext().isClean());
    }

    @Test
    void resourceDisposersCannotReenterLifecycleOrMutateTheirDisposingOwner() {
        GameRuntime runtime = new GameRuntime();
        AtomicInteger disposedAssets = new AtomicInteger();
        AtomicInteger disposedLateAssets = new AtomicInteger();
        List<String> replacementCalls = new ArrayList<>();
        RuntimeScene replacement = new RecordingScene(
            "replacement",
            replacementCalls,
            ignored -> {
            }
        );
        RuntimeScene scene = new RuntimeScene() {
            @Override
            public void create(GameContext context) {
                context.assets().put(this, "survivor", new Object(), ignored -> {
                    disposedAssets.incrementAndGet();
                });
                context.assets().put(this, "reentrant", new Object(), ignored -> {
                    assertThrows(IllegalStateException.class, runtime::close);
                    assertThrows(
                        IllegalStateException.class,
                        () -> runtime.restart(replacement)
                    );
                    assertThrows(
                        IllegalStateException.class,
                        () -> context.requestScene(replacement)
                    );
                    assertThrows(
                        IllegalStateException.class,
                        () -> runtime.start(replacement)
                    );
                    assertThrows(
                        IllegalStateException.class,
                        () -> context.assets().put(
                            this,
                            "late",
                            new Object(),
                            late -> disposedLateAssets.incrementAndGet()
                        )
                    );
                    disposedAssets.incrementAndGet();
                });
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
        };

        runtime.start(scene);
        runtime.close();

        RuntimeMetrics metrics = runtime.metrics();
        assertAll(
            () -> assertTrue(runtime.isClosed()),
            () -> assertEquals(2, disposedAssets.get()),
            () -> assertEquals(0, disposedLateAssets.get()),
            () -> assertTrue(replacementCalls.isEmpty()),
            () -> assertEquals(1L, metrics.executionsStarted()),
            () -> assertEquals(0L, metrics.restarts()),
            () -> assertEquals(1L, metrics.sceneTransitions()),
            () -> assertEquals(0L, metrics.failedExecutions()),
            () -> assertEquals(
                2L,
                metrics.lastClosedContext().resources().resourcesRegistered()
            ),
            () -> assertTrue(metrics.lastClosedContext().isClean())
        );
    }

    @Test
    void callbackRequestedTransitionRunsOnlyAtTheSafeUpdateBoundary() {
        List<String> calls = new ArrayList<>();
        GameRuntime runtime = new GameRuntime();
        RecordingScene second = new RecordingScene("second", calls, ignored -> {
        });
        RecordingScene first = new RecordingScene(
            "first",
            calls,
            context -> context.requestScene(second)
        );

        runtime.start(first);
        runtime.fixedUpdate(1.0 / 60.0);
        runtime.render(0.25);
        runtime.close();
        runtime.close();

        assertEquals(
            List.of(
                "first.create",
                "first.enter",
                "first.update.begin",
                "first.update.end",
                "first.exit",
                "first.dispose",
                "first.asset.dispose",
                "second.create",
                "second.enter",
                "second.render",
                "second.exit",
                "second.dispose",
                "second.asset.dispose"
            ),
            calls
        );
        assertAll(
            () -> assertEquals(2L, runtime.metrics().sceneTransitions()),
            () -> assertTrue(runtime.metrics().lastClosedContext().isClean())
        );
    }

    @Test
    void multipleCallbackRequestsAreAppliedFifoAtTheSameSafeBoundary() {
        List<String> calls = new ArrayList<>();
        GameRuntime runtime = new GameRuntime();
        RecordingScene third = new RecordingScene("third", calls, ignored -> {
        });
        RecordingScene second = new RecordingScene("second", calls, ignored -> {
        });
        RecordingScene first = new RecordingScene("first", calls, context -> {
            context.requestScene(second);
            context.requestScene(third);
        });

        runtime.start(first);
        runtime.fixedUpdate(1.0 / 60.0);
        runtime.render(0.0);
        runtime.close();

        assertEquals(
            List.of(
                "first.create",
                "first.enter",
                "first.update.begin",
                "first.update.end",
                "first.exit",
                "first.dispose",
                "first.asset.dispose",
                "second.create",
                "second.enter",
                "second.exit",
                "second.dispose",
                "second.asset.dispose",
                "third.create",
                "third.enter",
                "third.render",
                "third.exit",
                "third.dispose",
                "third.asset.dispose"
            ),
            calls
        );
    }

    @Test
    void oneHundredQueuedSceneChangesReleaseThePreviousOwnerEveryTime() {
        AtomicInteger disposedAssets = new AtomicInteger();
        GameRuntime runtime = new GameRuntime();
        ChainScene current = new ChainScene(0, disposedAssets);
        runtime.start(current);

        for (int index = 1; index <= 100; index++) {
            current.next = new ChainScene(index, disposedAssets);
            runtime.fixedUpdate(1.0 / 60.0);
            current = current.next;
            assertAll(
                () -> assertEquals(1, runtime.context().world().entityCount()),
                () -> assertEquals(1, runtime.context().events().pendingEventCount()),
                () -> assertEquals(1, runtime.context().assets().size())
            );
        }

        assertEquals(100, disposedAssets.get());
        runtime.close();
        assertAll(
            () -> assertEquals(101, disposedAssets.get()),
            () -> assertEquals(101L, runtime.metrics().sceneTransitions()),
            () -> assertTrue(runtime.metrics().lastClosedContext().isClean())
        );
    }

    @Test
    void oneHundredRestartsCreateFreshContextsWithoutStateOrAssetInheritance() {
        AtomicInteger disposedAssets = new AtomicInteger();
        GameRuntime runtime = new GameRuntime();
        runtime.start(new StatefulScene(0, disposedAssets));
        List<GameContext> contexts = new ArrayList<>();
        contexts.add(runtime.context());

        for (int restart = 1; restart <= 100; restart++) {
            GameContext previous = runtime.context();
            runtime.restart(new StatefulScene(restart, disposedAssets));
            GameContext current = runtime.context();
            contexts.add(current);

            assertAll(
                () -> assertTrue(previous.isClosed()),
                () -> assertTrue(runtime.metrics().lastClosedContext().isClean()),
                () -> assertFalse(previous == current),
                () -> assertEquals(1, current.world().entityCount()),
                () -> assertEquals(1, current.events().pendingEventCount()),
                () -> assertEquals(1, current.assets().size()),
                () -> assertEquals("asset-" + current.executionId(), current.assets().get("current"))
            );
        }

        assertEquals(101, contexts.stream().distinct().count());
        assertEquals(100, disposedAssets.get());
        runtime.close();

        RuntimeMetrics metrics = runtime.metrics();
        assertAll(
            () -> assertEquals(101, disposedAssets.get()),
            () -> assertEquals(101L, metrics.executionsStarted()),
            () -> assertEquals(100L, metrics.restarts()),
            () -> assertEquals(101L, metrics.sceneTransitions()),
            () -> assertEquals(0L, metrics.failedExecutions()),
            () -> assertTrue(metrics.lastClosedContext().isClean())
        );
    }

    private static final class RecordingScene implements RuntimeScene {
        private final String name;
        private final List<String> calls;
        private final Consumer<GameContext> update;

        private RecordingScene(
            String name,
            List<String> calls,
            Consumer<GameContext> update
        ) {
            this.name = name;
            this.calls = calls;
            this.update = update;
        }

        @Override
        public void create(GameContext context) {
            calls.add(name + ".create");
            context.assets().put(this, name, new Object(), ignored -> {
                calls.add(name + ".asset.dispose");
            });
        }

        @Override
        public void enter(GameContext context) {
            calls.add(name + ".enter");
        }

        @Override
        public void fixedUpdate(GameContext context, double fixedDeltaSeconds) {
            calls.add(name + ".update.begin");
            update.accept(context);
            calls.add(name + ".update.end");
        }

        @Override
        public void render(GameContext context, double interpolationAlpha) {
            calls.add(name + ".render");
        }

        @Override
        public void exit(GameContext context) {
            calls.add(name + ".exit");
        }

        @Override
        public void dispose(GameContext context) {
            calls.add(name + ".dispose");
        }
    }

    private static final class ChainScene implements RuntimeScene {
        private final int id;
        private final AtomicInteger disposedAssets;
        private ChainScene next;

        private ChainScene(int id, AtomicInteger disposedAssets) {
            this.id = id;
            this.disposedAssets = disposedAssets;
        }

        @Override
        public void create(GameContext context) {
            context.world().add(this, "entity-" + id);
            context.events().post(
                this,
                EventPhase.AFTER_FIXED_UPDATE,
                STATE_EVENT,
                "event-" + id
            );
            context.assets().put(this, "asset-" + id, new Object(), ignored -> {
                disposedAssets.incrementAndGet();
            });
        }

        @Override
        public void enter(GameContext context) {
        }

        @Override
        public void fixedUpdate(GameContext context, double fixedDeltaSeconds) {
            context.requestScene(next);
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

    private static final class StatefulScene implements RuntimeScene {
        private final int restart;
        private final AtomicInteger disposedAssets;

        private StatefulScene(int restart, AtomicInteger disposedAssets) {
            this.restart = restart;
            this.disposedAssets = disposedAssets;
        }

        @Override
        public void create(GameContext context) {
            assertEquals(restart + 1L, context.executionId());
            assertAll(
                () -> assertEquals(0, context.world().entityCount()),
                () -> assertEquals(0, context.events().pendingEventCount()),
                () -> assertEquals(0, context.assets().size())
            );
            context.world().add(this, "entity-" + context.executionId());
            context.events().post(
                this,
                EventPhase.AFTER_FIXED_UPDATE,
                STATE_EVENT,
                "event-" + context.executionId()
            );
            context.assets().put(
                this,
                "current",
                "asset-" + context.executionId(),
                ignored -> disposedAssets.incrementAndGet()
            );
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
