package engine.incubator.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Tag("specification")
final class GameRuntimeFailureTest {
    @ParameterizedTest(name = "cleanup survives injected {0} failure")
    @EnumSource(FailurePhase.class)
    void everyCallbackFailureStillDisposesTheSceneOwnerAndContext(FailurePhase phase) {
        AtomicInteger assetDisposals = new AtomicInteger();
        FailingScene scene = new FailingScene(phase, assetDisposals);
        GameRuntime runtime = new GameRuntime();

        assertThrows(InjectedFailure.class, () -> executeFailure(runtime, scene, phase));

        GameContextSnapshot snapshot = runtime.metrics().lastClosedContext();
        assertAll(
            () -> assertEquals(1, scene.disposeCalls),
            () -> assertEquals(1, assetDisposals.get()),
            () -> assertEquals(0, snapshot.entityCount()),
            () -> assertEquals(0, snapshot.eventCount()),
            () -> assertEquals(0, snapshot.assetCount()),
            () -> assertEquals(0L, snapshot.resources().liveOwners()),
            () -> assertEquals(0L, snapshot.resources().leakedResources()),
            () -> assertTrue(snapshot.isClean())
        );
    }

    private static void executeFailure(
        GameRuntime runtime,
        FailingScene scene,
        FailurePhase phase
    ) {
        runtime.start(scene);
        switch (phase) {
            case CREATE, ENTER -> throw new AssertionError("start should have failed");
            case FIXED_UPDATE -> runtime.fixedUpdate(1.0 / 60.0);
            case RENDER -> runtime.render(0.0);
            case EXIT, DISPOSE -> runtime.close();
        }
    }

    private enum FailurePhase {
        CREATE,
        ENTER,
        FIXED_UPDATE,
        RENDER,
        EXIT,
        DISPOSE,
    }

    private static final class InjectedFailure extends RuntimeException {
        private InjectedFailure(FailurePhase phase) {
            super("injected " + phase + " failure");
        }
    }

    private static final class FailingScene implements RuntimeScene {
        private final FailurePhase phase;
        private final AtomicInteger assetDisposals;
        private int disposeCalls;

        private FailingScene(FailurePhase phase, AtomicInteger assetDisposals) {
            this.phase = phase;
            this.assetDisposals = assetDisposals;
        }

        @Override
        public void create(GameContext context) {
            context.world().add(this, new Object());
            context.events().post(this, new Object());
            context.assets().put(this, "owned", new Object(), ignored -> {
                assetDisposals.incrementAndGet();
            });
            failAt(FailurePhase.CREATE);
        }

        @Override
        public void enter(GameContext context) {
            failAt(FailurePhase.ENTER);
        }

        @Override
        public void fixedUpdate(GameContext context, double fixedDeltaSeconds) {
            failAt(FailurePhase.FIXED_UPDATE);
        }

        @Override
        public void render(GameContext context, double interpolationAlpha) {
            failAt(FailurePhase.RENDER);
        }

        @Override
        public void exit(GameContext context) {
            failAt(FailurePhase.EXIT);
        }

        @Override
        public void dispose(GameContext context) {
            disposeCalls++;
            failAt(FailurePhase.DISPOSE);
        }

        private void failAt(FailurePhase callback) {
            if (phase == callback) {
                throw new InjectedFailure(callback);
            }
        }
    }
}
