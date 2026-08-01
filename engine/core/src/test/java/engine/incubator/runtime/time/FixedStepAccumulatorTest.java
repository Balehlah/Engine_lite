package engine.incubator.runtime.time;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class FixedStepAccumulatorTest {
    @Test
    void longStallIsClampedAndCatchUpIsBoundedWithExplicitDiscardMetrics() {
        FixedTimestepConfig configuration = FixedTimestepConfig.of(
            60.0,
            Duration.ofMillis(100L),
            3
        );
        FixedStepAccumulator accumulator = new FixedStepAccumulator(configuration);

        AccumulatorFrame frame = accumulator.advance(Duration.ofSeconds(1L).toNanos(), 1.0);

        assertAll(
            () -> assertEquals(3, frame.updateCount()),
            () -> assertEquals(Duration.ofMillis(100L).toNanos(), frame.acceptedWallTimeNanos()),
            () -> assertEquals(Duration.ofMillis(900L).toNanos(), frame.clampDiscardedWallTimeNanos()),
            () -> assertEquals(configuration.fixedStepNanos() * 2L, frame.catchUpDiscardedSimulationTimeNanos()),
            () -> assertTrue(frame.wasClamped()),
            () -> assertTrue(frame.reachedCatchUpLimit()),
            () -> assertTrue(frame.interpolationAlpha() >= 0.0),
            () -> assertTrue(frame.interpolationAlpha() < 1.0)
        );
    }

    @Test
    void alphaStaysWithinHalfOpenBoundaryAroundAnUpdate() {
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        FixedStepAccumulator accumulator = new FixedStepAccumulator(configuration);
        long step = configuration.fixedStepNanos();

        AccumulatorFrame beforeBoundary = accumulator.advance(step - 1L, 1.0);
        AccumulatorFrame atBoundary = accumulator.advance(1L, 1.0);

        assertAll(
            () -> assertEquals(0, beforeBoundary.updateCount()),
            () -> assertTrue(beforeBoundary.interpolationAlpha() < 1.0),
            () -> assertTrue(beforeBoundary.interpolationAlpha() > 0.999),
            () -> assertEquals(1, atBoundary.updateCount()),
            () -> assertEquals(0.0, atBoundary.interpolationAlpha())
        );
    }

    @Test
    void timeScaleChangesUpdateFrequencyWithoutChangingTheLogicalStep() {
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        FixedStepAccumulator accumulator = new FixedStepAccumulator(configuration);

        AccumulatorFrame first = accumulator.advance(configuration.fixedStepNanos(), 0.5);
        AccumulatorFrame second = accumulator.advance(configuration.fixedStepNanos(), 0.5);

        assertAll(
            () -> assertEquals(0, first.updateCount()),
            () -> assertEquals(1, second.updateCount()),
            () -> assertTrue(
                Math.abs(
                    configuration.fixedStepNanos()
                        - second.scaledSimulationTimeNanos() * 2L
                ) <= 1L
            )
        );
    }

    @Test
    void invalidDeltasAndScalesAreRejected() {
        FixedStepAccumulator accumulator = new FixedStepAccumulator(
            FixedTimestepConfig.default60Hz()
        );

        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> accumulator.advance(-1L, 1.0)),
            () -> assertThrows(IllegalArgumentException.class, () -> accumulator.advance(1L, -1.0)),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> accumulator.advance(Long.MAX_VALUE, Double.MAX_VALUE)
            )
        );
    }
}
