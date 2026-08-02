package engine.incubator.runtime.time;

/**
 * Pure result of adding one wall-clock delta to a fixed-step accumulator.
 */
public record AccumulatorFrame(
    int updateCount,
    long wallElapsedNanos,
    long acceptedWallTimeNanos,
    long scaledSimulationTimeNanos,
    long clampDiscardedWallTimeNanos,
    long catchUpDiscardedSimulationTimeNanos,
    double interpolationAlpha
) {
    public AccumulatorFrame {
        if (updateCount < 0) {
            throw new IllegalArgumentException("updateCount must be non-negative");
        }
        if (
            wallElapsedNanos < 0L
                || acceptedWallTimeNanos < 0L
                || scaledSimulationTimeNanos < 0L
                || clampDiscardedWallTimeNanos < 0L
                || catchUpDiscardedSimulationTimeNanos < 0L
        ) {
            throw new IllegalArgumentException("time values must be non-negative");
        }
        if (
            !Double.isFinite(interpolationAlpha)
                || interpolationAlpha < 0.0
                || interpolationAlpha >= 1.0
        ) {
            throw new IllegalArgumentException("interpolationAlpha must be in [0, 1)");
        }
    }

    public boolean reachedCatchUpLimit() {
        return catchUpDiscardedSimulationTimeNanos > 0L;
    }

    public boolean wasClamped() {
        return clampDiscardedWallTimeNanos > 0L;
    }
}
