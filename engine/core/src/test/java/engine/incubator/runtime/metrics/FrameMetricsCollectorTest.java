package engine.incubator.runtime.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import engine.incubator.assets.AssetMetrics;
import engine.incubator.runtime.time.FakeNanoClock;
import engine.incubator.runtime.time.SchedulerMetrics;
import java.time.Duration;
import org.junit.jupiter.api.Test;

final class FrameMetricsCollectorTest {
    @Test
    void fakeClockProducesComparableFpsUpsAssetsCatchUpAndDrawCalls() {
        FakeNanoClock clock = new FakeNanoClock();
        FrameMetricsCollector collector = new FrameMetricsCollector(
            clock,
            Duration.ofSeconds(1L)
        );
        AssetHealthMetrics assets = AssetHealthMetrics.from(
            new AssetMetrics(3L, 2L, 0L, 5L, 1L, 4L, 1L, 0L, 0L, 1, 2, 4, 3)
        );

        clock.advance(Duration.ofMillis(500L));
        FrameHealthMetrics first = collector.recordFrame(
            scheduler(30L, 55L, 2, 1L, 10_000_000L),
            assets,
            4L
        );
        assertEquals(0.0, first.framesPerSecond());
        assertEquals(0.0, first.updatesPerSecond());

        clock.advance(Duration.ofMillis(500L));
        FrameHealthMetrics sampled = collector.recordFrame(
            scheduler(60L, 120L, 3, 2L, 20_000_000L),
            assets,
            5L
        );

        assertEquals(60.0, sampled.framesPerSecond());
        assertEquals(120.0, sampled.updatesPerSecond());
        assertEquals(60L, sampled.frame());
        assertEquals(120L, sampled.tick());
        assertEquals(3, sampled.updatesThisFrame());
        assertEquals(2L, sampled.catchUpLimitHits());
        assertEquals(20_000_000L, sampled.catchUpDiscardedNanos());
        assertEquals(new AssetHealthMetrics(1, 2, 4, 3), sampled.assets());
        assertEquals(5L, sampled.drawCalls());
    }

    @Test
    void invalidSampleWindowAndDrawCallsFailAtTheBoundary() {
        FakeNanoClock clock = new FakeNanoClock();
        assertThrows(
            IllegalArgumentException.class,
            () -> new FrameMetricsCollector(clock, Duration.ZERO)
        );
        FrameMetricsCollector collector = new FrameMetricsCollector(
            clock,
            Duration.ofMillis(1L)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> collector.recordFrame(
                scheduler(0L, 0L, 0, 0L, 0L),
                AssetHealthMetrics.none(),
                -1L
            )
        );
    }

    private static SchedulerMetrics scheduler(
        long frames,
        long updates,
        int lastFrameUpdates,
        long catchUpHits,
        long catchUpDiscarded
    ) {
        return new SchedulerMetrics(
            frames,
            updates,
            1L,
            catchUpHits,
            2_000_000L,
            catchUpDiscarded,
            0L,
            lastFrameUpdates,
            0.25,
            false,
            1.0
        );
    }
}
