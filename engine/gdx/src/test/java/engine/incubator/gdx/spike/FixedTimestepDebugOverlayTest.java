package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;

import engine.incubator.runtime.time.SchedulerMetrics;
import org.junit.jupiter.api.Test;

final class FixedTimestepDebugOverlayTest {
    @Test
    void formatsBoundariesAndDiscardTelemetryWithoutLocaleDrift() {
        SchedulerMetrics metrics = new SchedulerMetrics(
            120L,
            100L,
            2L,
            1L,
            1_500_000L,
            2_250_000L,
            0L,
            0,
            0.125,
            true,
            0.5
        );

        assertEquals(
            "ticks=100 frames=120 alpha=0.125 clamp=1.500ms catchup=2.250ms scale=0.50x PAUSED",
            FixedTimestepDebugOverlay.format(metrics)
        );
    }
}
