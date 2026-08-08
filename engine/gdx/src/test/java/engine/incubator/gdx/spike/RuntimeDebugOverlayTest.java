package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;

import engine.incubator.runtime.metrics.AssetHealthMetrics;
import engine.incubator.runtime.metrics.FrameHealthMetrics;
import engine.incubator.runtime.time.FakeNanoClock;
import engine.incubator.runtime.time.FixedTimestepConfig;
import engine.incubator.runtime.time.FixedTimestepScheduler;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimeDebugOverlayTest {
    @Test
    void formatsEveryRequiredMetricFromAFakeSnapshotWithoutLocaleDrift() {
        FrameHealthMetrics metrics = metrics(120L, 100L, 4L);

        assertEquals(
            "FPS 59.5 | UPS 60.0 | frame 120 | tick 100" + System.lineSeparator()
                + "updates 2 | catch-up 3 / 2.250 ms | alpha 0.125"
                + System.lineSeparator()
                + "assets pending/live/refs/backend 1/2/4/3 | draw calls 4 | PAUSED",
            RuntimeDebugOverlay.format(metrics)
        );
    }

    @Test
    void togglingAndFormattingTheOverlayDoesNotChangeFixedSimulation() {
        List<Long> hidden = runSimulation(false);
        List<Long> visible = runSimulation(true);

        assertEquals(hidden, visible);
        assertEquals(List.of(1L, 2L, 3L, 4L), visible);
    }

    private static List<Long> runSimulation(boolean overlayEnabled) {
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepConfig config = FixedTimestepConfig.default60Hz();
        FixedTimestepLoop loop = new FixedTimestepLoop(
            new FixedTimestepScheduler(clock, config)
        );
        DebugOverlayState state = new DebugOverlayState(overlayEnabled);
        List<Long> simulation = new ArrayList<>();
        for (long expected = 1L; expected <= 4L; expected++) {
            clock.advanceNanos(config.fixedStepNanos());
            loop.runFrame(
                ignored -> simulation.add((long) simulation.size() + 1L),
                (alpha, scheduler) -> {
                    if (state.isEnabled()) {
                        RuntimeDebugOverlay.format(
                            metrics(scheduler.frameCount(), scheduler.updateCount(), 2L)
                        );
                    }
                }
            );
        }
        return simulation;
    }

    private static FrameHealthMetrics metrics(long frame, long tick, long drawCalls) {
        return new FrameHealthMetrics(
            frame,
            tick,
            59.5,
            60.0,
            2,
            3L,
            2_250_000L,
            1_500_000L,
            0.125,
            true,
            new AssetHealthMetrics(1, 2, 4, 3),
            drawCalls
        );
    }
}
