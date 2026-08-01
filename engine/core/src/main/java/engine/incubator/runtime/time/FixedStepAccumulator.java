package engine.incubator.runtime.time;

import java.util.Objects;

/**
 * Backend-neutral accumulator. Its output depends only on configuration, prior state and input delta.
 */
public final class FixedStepAccumulator {
    private final FixedTimestepConfig configuration;
    private double accumulatedSimulationNanos;

    public FixedStepAccumulator(FixedTimestepConfig configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public AccumulatorFrame advance(long wallElapsedNanos, double timeScale) {
        if (wallElapsedNanos < 0L) {
            throw new IllegalArgumentException("wallElapsedNanos must be non-negative");
        }
        if (!Double.isFinite(timeScale) || timeScale < 0.0) {
            throw new IllegalArgumentException("timeScale must be finite and non-negative");
        }

        long acceptedWallTimeNanos = Math.min(
            wallElapsedNanos,
            configuration.maximumFrameTimeNanos()
        );
        long clampDiscardedWallTimeNanos = wallElapsedNanos - acceptedWallTimeNanos;
        double scaledSimulationTimeNanos = acceptedWallTimeNanos * timeScale;
        if (!Double.isFinite(scaledSimulationTimeNanos)) {
            throw new IllegalArgumentException("timeScale produces an unrepresentable delta");
        }
        accumulatedSimulationNanos += scaledSimulationTimeNanos;

        long fixedStepNanos = configuration.fixedStepNanos();
        double availableSteps = Math.floor(accumulatedSimulationNanos / fixedStepNanos);
        int updateCount = (int) Math.min(
            availableSteps,
            configuration.maximumCatchUpSteps()
        );
        accumulatedSimulationNanos -= updateCount * (double) fixedStepNanos;

        long catchUpDiscardedSimulationTimeNanos = 0L;
        if (availableSteps > configuration.maximumCatchUpSteps()) {
            double remainder = accumulatedSimulationNanos % fixedStepNanos;
            double discarded = accumulatedSimulationNanos - remainder;
            catchUpDiscardedSimulationTimeNanos = saturatingRound(discarded);
            accumulatedSimulationNanos = remainder;
        }

        double interpolationAlpha = accumulatedSimulationNanos / fixedStepNanos;
        if (interpolationAlpha >= 1.0) {
            interpolationAlpha = Math.nextDown(1.0);
        }

        return new AccumulatorFrame(
            updateCount,
            wallElapsedNanos,
            acceptedWallTimeNanos,
            saturatingRound(scaledSimulationTimeNanos),
            clampDiscardedWallTimeNanos,
            catchUpDiscardedSimulationTimeNanos,
            interpolationAlpha
        );
    }

    public double interpolationAlpha() {
        return accumulatedSimulationNanos / configuration.fixedStepNanos();
    }

    public void reset() {
        accumulatedSimulationNanos = 0.0;
    }

    private static long saturatingRound(double value) {
        if (value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.round(value);
    }
}
