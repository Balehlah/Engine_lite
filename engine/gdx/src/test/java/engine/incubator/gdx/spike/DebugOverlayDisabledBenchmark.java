package engine.incubator.gdx.spike;

import java.util.Locale;

/** Repeatable micro-benchmark for the disabled overlay branch; no GL context is required. */
public final class DebugOverlayDisabledBenchmark {
    private static final int DEFAULT_ITERATIONS = 20_000_000;

    private DebugOverlayDisabledBenchmark() {
    }

    public static void main(String[] arguments) {
        int iterations = arguments.length == 0
            ? DEFAULT_ITERATIONS
            : Integer.parseInt(arguments[0]);
        Result result = run(iterations);
        System.out.printf(
            Locale.ROOT,
            "overlay.disabled.iterations=%d;elapsed-nanos=%d;ns-per-check=%.3f;renders=%d%n",
            result.iterations(),
            result.elapsedNanos(),
            result.nanosecondsPerCheck(),
            result.renders()
        );
    }

    static Result run(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        DebugOverlayState state = new DebugOverlayState(false);
        long renders = 0L;
        long start = System.nanoTime();
        for (int index = 0; index < iterations; index++) {
            if (state.isEnabled()) {
                renders++;
            }
        }
        long elapsed = System.nanoTime() - start;
        if (renders != 0L) {
            throw new IllegalStateException("Disabled overlay entered its render path");
        }
        return new Result(iterations, elapsed, elapsed / (double) iterations, renders);
    }

    record Result(
        int iterations,
        long elapsedNanos,
        double nanosecondsPerCheck,
        long renders
    ) {
    }
}
