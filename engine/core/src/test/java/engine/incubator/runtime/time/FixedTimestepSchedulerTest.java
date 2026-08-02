package engine.incubator.runtime.time;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class FixedTimestepSchedulerTest {
    @Test
    void identicalFakeClockSequencesProduceIdenticalSnapshots() {
        long[] deltas = {
            0L,
            1_000_000L,
            15_666_667L,
            50_000_000L,
            4_000_000L,
            250_000_000L,
            8_333_333L,
        };

        assertEquals(runSequence(deltas), runSequence(deltas));
    }

    @Test
    void tenThousandTicksRemainDeterministicAndUseExactlyOneSixtieth() {
        DeterministicRun first = runTenThousandTicks();
        DeterministicRun second = runTenThousandTicks();

        assertAll(
            () -> assertEquals(first, second),
            () -> assertEquals(10_000L, first.metrics().updateCount()),
            () -> assertEquals(10_000L, first.metrics().frameCount()),
            () -> assertEquals(10_000.0 / 60.0, first.simulatedSeconds(), 0.000_000_001),
            () -> assertEquals(0.0, first.metrics().interpolationAlpha())
        );
    }

    @Test
    void pauseAndSingleStepDiscardHiddenWallTimeWithoutChangingPartialAlpha() {
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepScheduler scheduler = new FixedTimestepScheduler(clock, configuration);
        long step = configuration.fixedStepNanos();

        clock.advanceNanos(step - 1L);
        FrameSchedule partial = scheduler.nextFrame();
        scheduler.pause();

        long hiddenPause = Duration.ofHours(1L).toNanos();
        clock.advanceNanos(hiddenPause);
        FrameSchedule paused = scheduler.nextFrame();
        scheduler.requestStep();
        clock.advanceNanos(hiddenPause);
        FrameSchedule stepped = scheduler.nextFrame();

        scheduler.resume();
        clock.advanceNanos(1L);
        FrameSchedule resumed = scheduler.nextFrame();

        assertAll(
            () -> assertEquals(0, partial.updateCount()),
            () -> assertEquals(0, paused.updateCount()),
            () -> assertEquals(hiddenPause, paused.inactiveWallTimeNanos()),
            () -> assertEquals(partial.interpolationAlpha(), paused.interpolationAlpha()),
            () -> assertEquals(1, stepped.updateCount()),
            () -> assertEquals(hiddenPause, stepped.inactiveWallTimeNanos()),
            () -> assertEquals(partial.interpolationAlpha(), stepped.interpolationAlpha()),
            () -> assertEquals(1, resumed.updateCount()),
            () -> assertEquals(0L, resumed.inactiveWallTimeNanos()),
            () -> assertFalse(scheduler.isPaused())
        );
    }

    @Test
    void zeroTimeScaleDoesNotCreateBacklog() {
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepScheduler scheduler = new FixedTimestepScheduler(clock, configuration);

        scheduler.setTimeScale(0.0);
        clock.advance(Duration.ofMinutes(10L));
        FrameSchedule stopped = scheduler.nextFrame();
        scheduler.setTimeScale(1.0);
        clock.advanceNanos(configuration.fixedStepNanos());
        FrameSchedule running = scheduler.nextFrame();

        assertAll(
            () -> assertEquals(0, stopped.updateCount()),
            () -> assertEquals(Duration.ofMinutes(10L).toNanos(), stopped.inactiveWallTimeNanos()),
            () -> assertEquals(1, running.updateCount()),
            () -> assertEquals(1.0 / 60.0, running.fixedDeltaSeconds())
        );
    }

    @Test
    void pauseAndZeroScaleTransitionsDiscardTimeEvenWithoutHostFrames() {
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepScheduler scheduler = new FixedTimestepScheduler(clock, configuration);
        long pausedTime = Duration.ofHours(1L).toNanos();
        long zeroScaleTime = Duration.ofMinutes(10L).toNanos();

        scheduler.pause();
        clock.advanceNanos(pausedTime);
        scheduler.resume();
        scheduler.setTimeScale(0.0);
        clock.advanceNanos(zeroScaleTime);
        scheduler.setTimeScale(1.0);
        clock.advanceNanos(configuration.fixedStepNanos());
        FrameSchedule frame = scheduler.nextFrame();

        assertAll(
            () -> assertEquals(1, frame.updateCount()),
            () -> assertEquals(
                pausedTime + zeroScaleTime,
                frame.metrics().inactiveWallTimeNanos()
            ),
            () -> assertEquals(0L, frame.clampDiscardedWallTimeNanos()),
            () -> assertEquals(0L, frame.catchUpDiscardedSimulationTimeNanos())
        );
    }

    @Test
    void reportsClampAndCatchUpTotalsAcrossFrames() {
        FixedTimestepConfig configuration = FixedTimestepConfig.of(
            60.0,
            Duration.ofMillis(100L),
            2
        );
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepScheduler scheduler = new FixedTimestepScheduler(clock, configuration);

        clock.advance(Duration.ofSeconds(2L));
        FrameSchedule frame = scheduler.nextFrame();

        assertAll(
            () -> assertEquals(2, frame.updateCount()),
            () -> assertEquals(1, frame.metrics().clampedFrameCount()),
            () -> assertEquals(1, frame.metrics().catchUpLimitHitCount()),
            () -> assertEquals(Duration.ofMillis(1_900L).toNanos(), frame.metrics().clampedWallTimeNanos()),
            () -> assertTrue(frame.metrics().catchUpDiscardedSimulationTimeNanos() > 0L)
        );
    }

    @Test
    void rejectsStepWhileRunningAndAClockThatMovesBackwards() {
        AtomicLong now = new AtomicLong(10L);
        FixedTimestepScheduler scheduler = new FixedTimestepScheduler(
            now::get,
            FixedTimestepConfig.default60Hz()
        );

        assertThrows(IllegalStateException.class, scheduler::requestStep);
        now.set(9L);
        assertThrows(IllegalStateException.class, scheduler::nextFrame);
    }

    private static List<FrameSchedule> runSequence(long[] deltas) {
        FakeNanoClock clock = new FakeNanoClock(1_000L);
        FixedTimestepScheduler scheduler = new FixedTimestepScheduler(
            clock,
            FixedTimestepConfig.default60Hz()
        );
        List<FrameSchedule> frames = new ArrayList<>();
        for (long delta : deltas) {
            clock.advanceNanos(delta);
            FrameSchedule frame = scheduler.nextFrame();
            assertTrue(frame.interpolationAlpha() >= 0.0);
            assertTrue(frame.interpolationAlpha() < 1.0);
            frames.add(frame);
        }
        return frames;
    }

    private static DeterministicRun runTenThousandTicks() {
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepScheduler scheduler = new FixedTimestepScheduler(clock, configuration);
        double simulatedSeconds = 0.0;

        for (int tick = 0; tick < 10_000; tick++) {
            clock.advanceNanos(configuration.fixedStepNanos());
            FrameSchedule frame = scheduler.nextFrame();
            assertEquals(1, frame.updateCount());
            simulatedSeconds += frame.fixedDeltaSeconds();
        }
        return new DeterministicRun(simulatedSeconds, scheduler.metrics());
    }

    private record DeterministicRun(
        double simulatedSeconds,
        SchedulerMetrics metrics
    ) {
    }
}
