package engine.incubator.gdx.runtime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import engine.incubator.gdx.spike.FixedTimestepLoop;
import engine.incubator.runtime.lifecycle.GameContext;
import engine.incubator.runtime.lifecycle.GameRuntime;
import engine.incubator.runtime.lifecycle.RuntimeScene;
import engine.incubator.runtime.logging.EngineLogRecord;
import engine.incubator.runtime.logging.EngineLogger;
import engine.incubator.runtime.logging.LogLevel;
import engine.incubator.runtime.time.FakeNanoClock;
import engine.incubator.runtime.time.FixedTimestepConfig;
import engine.incubator.runtime.time.FixedTimestepScheduler;
import java.util.ArrayList;
import java.util.List;
import java.time.Clock;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class GdxGameRuntimeLoopTest {
    @Test
    void fixedUpdatesAndRenderAreDelegatedBeforeIdempotentDispose() {
        List<String> calls = new ArrayList<>();
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        GameRuntime runtime = new GameRuntime();
        runtime.start(new RecordingScene(calls));
        GdxGameRuntimeLoop loop = new GdxGameRuntimeLoop(
            runtime,
            new FixedTimestepLoop(new FixedTimestepScheduler(clock, configuration))
        );

        clock.advanceNanos(configuration.fixedStepNanos());
        var frame = loop.renderFrame();
        loop.dispose();
        loop.dispose();

        assertAll(
            () -> assertEquals(1, frame.updateCount()),
            () -> assertEquals(
                List.of("create", "enter", "update", "render", "exit", "dispose"),
                calls
            ),
            () -> assertThrows(IllegalStateException.class, loop::renderFrame)
        );
    }

    @Test
    void contextualFrameLogCarriesTheActualExecutionWorldFrameAndTick() {
        List<EngineLogRecord> records = new ArrayList<>();
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        GameRuntime runtime = new GameRuntime();
        runtime.start(new RecordingScene(new ArrayList<>()));
        GdxGameRuntimeLoop loop = new GdxGameRuntimeLoop(
            runtime,
            new FixedTimestepLoop(new FixedTimestepScheduler(clock, configuration)),
            new EngineLogger(
                "runtime.loop",
                LogLevel.DEBUG,
                Clock.systemUTC(),
                records::add
            )
        );

        clock.advanceNanos(configuration.fixedStepNanos());
        var frame = loop.renderFrame();
        loop.dispose();

        assertEquals(1, records.size());
        assertEquals(frame.metrics().frameCount(), records.getFirst().context().frame());
        assertEquals(frame.metrics().updateCount(), records.getFirst().context().tick());
        assertEquals(1L, records.getFirst().context().world());
        assertEquals("frame completed", records.getFirst().message());
    }

    private static final class RecordingScene implements RuntimeScene {
        private final List<String> calls;

        private RecordingScene(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public void create(GameContext context) {
            calls.add("create");
        }

        @Override
        public void enter(GameContext context) {
            calls.add("enter");
        }

        @Override
        public void fixedUpdate(GameContext context, double fixedDeltaSeconds) {
            calls.add("update");
        }

        @Override
        public void render(GameContext context, double interpolationAlpha) {
            calls.add("render");
        }

        @Override
        public void exit(GameContext context) {
            calls.add("exit");
        }

        @Override
        public void dispose(GameContext context) {
            calls.add("dispose");
        }
    }
}
