package engine.incubator.runtime.config;

import engine.incubator.runtime.logging.LogLevel;
import engine.incubator.runtime.time.FixedTimestepConfig;
import java.nio.file.Path;
import java.time.Duration;

/** Immutable effective configuration selected before backend initialization. */
public record EngineConfig(
    double updatesPerSecond,
    long maximumFrameTimeMillis,
    int maximumCatchUpSteps,
    int virtualWidth,
    int virtualHeight,
    int windowWidth,
    int windowHeight,
    boolean vsync,
    int foregroundFps,
    int idleFps,
    boolean overlayEnabled,
    LogLevel logLevel,
    long metricsSampleWindowMillis,
    Path evidenceDirectory
) {
    public static final double DEFAULT_UPDATES_PER_SECOND =
        FixedTimestepConfig.DEFAULT_UPDATES_PER_SECOND;
    public static final long DEFAULT_MAXIMUM_FRAME_TIME_MILLIS =
        FixedTimestepConfig.DEFAULT_MAXIMUM_FRAME_TIME.toMillis();
    public static final int DEFAULT_MAXIMUM_CATCH_UP_STEPS =
        FixedTimestepConfig.DEFAULT_MAXIMUM_CATCH_UP_STEPS;
    public static final int DEFAULT_VIRTUAL_WIDTH = 320;
    public static final int DEFAULT_VIRTUAL_HEIGHT = 180;
    public static final int DEFAULT_WINDOW_WIDTH = 640;
    public static final int DEFAULT_WINDOW_HEIGHT = 360;
    public static final boolean DEFAULT_VSYNC = true;
    public static final int DEFAULT_FOREGROUND_FPS = 60;
    public static final int DEFAULT_IDLE_FPS = 60;
    public static final boolean DEFAULT_OVERLAY_ENABLED = false;
    public static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.INFO;
    public static final long DEFAULT_METRICS_SAMPLE_WINDOW_MILLIS = 1_000L;

    public EngineConfig {
        if (logLevel == null) {
            throw new EngineConfigException(
                EngineConfigLoader.LOG_LEVEL,
                "null",
                "must not be null"
            );
        }
        if (evidenceDirectory == null) {
            throw new EngineConfigException(
                EngineConfigLoader.EVIDENCE_DIRECTORY,
                "null",
                "must not be null"
            );
        }
        require(
            Double.isFinite(updatesPerSecond)
                && updatesPerSecond > 0.0
                && updatesPerSecond <= 1_000_000_000.0,
            EngineConfigLoader.UPDATES_PER_SECOND,
            updatesPerSecond,
            "must be finite and in (0, 1.0E9]"
        );
        requireRange(
            maximumFrameTimeMillis,
            1L,
            60_000L,
            EngineConfigLoader.MAXIMUM_FRAME_TIME_MS
        );
        requireRange(
            maximumCatchUpSteps,
            1L,
            10_000L,
            EngineConfigLoader.MAXIMUM_CATCH_UP_STEPS
        );
        requireRange(virtualWidth, 1L, 32_768L, EngineConfigLoader.VIRTUAL_WIDTH);
        requireRange(virtualHeight, 1L, 32_768L, EngineConfigLoader.VIRTUAL_HEIGHT);
        requireRange(windowWidth, 1L, 32_768L, EngineConfigLoader.WINDOW_WIDTH);
        requireRange(windowHeight, 1L, 32_768L, EngineConfigLoader.WINDOW_HEIGHT);
        requireRange(foregroundFps, 1L, 1_000L, EngineConfigLoader.FOREGROUND_FPS);
        requireRange(idleFps, 1L, 1_000L, EngineConfigLoader.IDLE_FPS);
        requireRange(
            metricsSampleWindowMillis,
            1L,
            60_000L,
            EngineConfigLoader.METRICS_SAMPLE_WINDOW_MS
        );
        if (!evidenceDirectory.isAbsolute()) {
            throw new EngineConfigException(
                EngineConfigLoader.EVIDENCE_DIRECTORY,
                evidenceDirectory.toString(),
                "must be absolute"
            );
        }
        evidenceDirectory = evidenceDirectory.normalize();
    }

    public FixedTimestepConfig fixedTimestepConfig() {
        return FixedTimestepConfig.of(
            updatesPerSecond,
            Duration.ofMillis(maximumFrameTimeMillis),
            maximumCatchUpSteps
        );
    }

    public Duration metricsSampleWindow() {
        return Duration.ofMillis(metricsSampleWindowMillis);
    }

    private static void requireRange(
        long value,
        long minimum,
        long maximum,
        String field
    ) {
        require(
            value >= minimum && value <= maximum,
            field,
            value,
            "must be in [" + minimum + ", " + maximum + "]"
        );
    }

    private static void require(
        boolean condition,
        String field,
        Object value,
        String reason
    ) {
        if (!condition) {
            throw new EngineConfigException(field, String.valueOf(value), reason);
        }
    }
}
