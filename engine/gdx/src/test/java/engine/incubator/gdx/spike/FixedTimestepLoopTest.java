package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import engine.incubator.runtime.time.FakeNanoClock;
import engine.incubator.runtime.time.FixedTimestepConfig;
import engine.incubator.runtime.time.FixedTimestepScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class FixedTimestepLoopTest {
    @Test
    void rendersEvenWhenNoLogicalUpdateIsDue() {
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepLoop loop = loop(clock);
        AtomicInteger updates = new AtomicInteger();
        AtomicInteger renders = new AtomicInteger();

        var frame = loop.runFrame(
            ignored -> updates.incrementAndGet(),
            (alpha, metrics) -> renders.incrementAndGet()
        );

        assertAll(
            () -> assertEquals(0, frame.updateCount()),
            () -> assertEquals(0, updates.get()),
            () -> assertEquals(1, renders.get())
        );
    }

    @Test
    void everyCatchUpUpdateReceivesTheSameConfiguredDeltaThenRendersOnce() {
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        FixedTimestepLoop loop = loop(clock);
        List<Double> updateDeltas = new ArrayList<>();
        List<String> order = new ArrayList<>();

        clock.advanceNanos(configuration.fixedStepNanos() * 3L);
        var frame = loop.runFrame(
            delta -> {
                updateDeltas.add(delta);
                order.add("update");
            },
            (alpha, metrics) -> order.add("render")
        );

        assertAll(
            () -> assertEquals(3, frame.updateCount()),
            () -> assertEquals(
                List.of(
                    configuration.fixedDeltaSeconds(),
                    configuration.fixedDeltaSeconds(),
                    configuration.fixedDeltaSeconds()
                ),
                updateDeltas
            ),
            () -> assertEquals(List.of("update", "update", "update", "render"), order)
        );
    }

    @Test
    void pausedSingleStepRunsOneUpdateAndStillRenders() {
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepLoop loop = loop(clock);
        AtomicInteger updates = new AtomicInteger();
        AtomicInteger renders = new AtomicInteger();

        loop.pause();
        loop.requestStep();
        clock.advanceNanos(9_000_000_000L);
        var frame = loop.runFrame(
            ignored -> updates.incrementAndGet(),
            (alpha, metrics) -> renders.incrementAndGet()
        );

        assertAll(
            () -> assertEquals(1, frame.updateCount()),
            () -> assertEquals(1, updates.get()),
            () -> assertEquals(1, renders.get()),
            () -> assertEquals(9_000_000_000L, frame.inactiveWallTimeNanos())
        );
    }

    private static FixedTimestepLoop loop(FakeNanoClock clock) {
        return new FixedTimestepLoop(
            new FixedTimestepScheduler(clock, FixedTimestepConfig.default60Hz())
        );
    }
}
