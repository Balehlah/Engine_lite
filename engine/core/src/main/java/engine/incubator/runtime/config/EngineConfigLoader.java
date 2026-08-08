package engine.incubator.runtime.config;

import engine.incubator.runtime.logging.LogLevel;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/** Loads defaults, an optional UTF-8 properties file and CLI overrides in that order. */
public final class EngineConfigLoader {
    public static final String UPDATES_PER_SECOND = "runtime.updates-per-second";
    public static final String MAXIMUM_FRAME_TIME_MS = "runtime.maximum-frame-time-ms";
    public static final String MAXIMUM_CATCH_UP_STEPS = "runtime.maximum-catch-up-steps";
    public static final String VIRTUAL_WIDTH = "graphics.virtual-width";
    public static final String VIRTUAL_HEIGHT = "graphics.virtual-height";
    public static final String WINDOW_WIDTH = "graphics.window-width";
    public static final String WINDOW_HEIGHT = "graphics.window-height";
    public static final String VSYNC = "graphics.vsync";
    public static final String FOREGROUND_FPS = "graphics.foreground-fps";
    public static final String IDLE_FPS = "graphics.idle-fps";
    public static final String OVERLAY_ENABLED = "debug.overlay-enabled";
    public static final String LOG_LEVEL = "logging.level";
    public static final String METRICS_SAMPLE_WINDOW_MS = "metrics.sample-window-ms";
    public static final String EVIDENCE_DIRECTORY = "paths.evidence-directory";

    private static final Set<String> KEYS = Set.of(
        UPDATES_PER_SECOND,
        MAXIMUM_FRAME_TIME_MS,
        MAXIMUM_CATCH_UP_STEPS,
        VIRTUAL_WIDTH,
        VIRTUAL_HEIGHT,
        WINDOW_WIDTH,
        WINDOW_HEIGHT,
        VSYNC,
        FOREGROUND_FPS,
        IDLE_FPS,
        OVERLAY_ENABLED,
        LOG_LEVEL,
        METRICS_SAMPLE_WINDOW_MS,
        EVIDENCE_DIRECTORY
    );

    private EngineConfigLoader() {
    }

    public static LoadedEngineConfig load(Path applicationHome, String[] arguments) {
        Path home = requireAbsoluteHome(applicationHome);
        if (arguments == null) {
            throw new NullPointerException("arguments");
        }

        ParsedArguments parsed = parseArguments(arguments);
        Path defaultFile = home.resolve("config/engine.properties").normalize();
        Optional<Path> file = parsed.configurationFile() == null
            ? Files.isRegularFile(defaultFile) ? Optional.of(defaultFile) : Optional.empty()
            : Optional.of(resolveExplicitConfigurationFile(parsed.configurationFile()));

        LinkedHashMap<String, String> values = defaultValues(home);
        LinkedHashMap<String, ConfigurationSource> sources = new LinkedHashMap<>();
        KEYS.forEach(key -> sources.put(key, ConfigurationSource.DEFAULTS));

        file.ifPresent(path -> applyFile(path, values, sources));
        parsed.overrides().forEach((key, value) -> {
            requireKnownKey(key, value);
            values.put(key, value);
            sources.put(key, ConfigurationSource.CLI);
        });

        return new LoadedEngineConfig(
            toConfiguration(home, values),
            home,
            file,
            sources,
            parsed.remainingArguments()
        );
    }

    public static Set<String> keys() {
        return KEYS;
    }

    private static ParsedArguments parseArguments(String[] arguments) {
        String configurationFile = null;
        LinkedHashMap<String, String> overrides = new LinkedHashMap<>();
        List<String> remaining = new ArrayList<>();
        for (String argument : arguments) {
            if (argument == null) {
                throw new NullPointerException("argument");
            }
            if (argument.startsWith("--config=")) {
                if (configurationFile != null) {
                    throw invalid("config.file", argument, "may be supplied only once");
                }
                configurationFile = argument.substring("--config=".length());
                if (configurationFile.isBlank()) {
                    throw invalid("config.file", configurationFile, "must not be blank");
                }
            } else if (argument.startsWith("--set=")) {
                String assignment = argument.substring("--set=".length());
                int separator = assignment.indexOf('=');
                if (separator <= 0) {
                    throw invalid("cli.override", assignment, "must use --set=field=value");
                }
                putUniqueOverride(
                    overrides,
                    assignment.substring(0, separator),
                    assignment.substring(separator + 1)
                );
            } else if (argument.startsWith("--evidence-dir=")) {
                putUniqueOverride(
                    overrides,
                    EVIDENCE_DIRECTORY,
                    argument.substring("--evidence-dir=".length())
                );
            } else if ("--overlay".equals(argument)) {
                putUniqueOverride(overrides, OVERLAY_ENABLED, "true");
            } else if ("--no-overlay".equals(argument)) {
                putUniqueOverride(overrides, OVERLAY_ENABLED, "false");
            } else {
                remaining.add(argument);
            }
        }
        return new ParsedArguments(configurationFile, overrides, remaining);
    }

    private static void putUniqueOverride(
        Map<String, String> overrides,
        String key,
        String value
    ) {
        requireKnownKey(key, value);
        if (overrides.putIfAbsent(key, value) != null) {
            throw invalid(key, value, "CLI field may be supplied only once");
        }
    }

    private static void applyFile(
        Path file,
        Map<String, String> values,
        Map<String, ConfigurationSource> sources
    ) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw invalid("config.file", file.toString(), "cannot be read", exception);
        }
        for (String key : new LinkedHashSet<>(properties.stringPropertyNames())) {
            String value = properties.getProperty(key);
            requireKnownKey(key, value);
            values.put(key, value);
            sources.put(key, ConfigurationSource.FILE);
        }
    }

    private static EngineConfig toConfiguration(Path home, Map<String, String> values) {
        return new EngineConfig(
            finitePositiveDouble(values, UPDATES_PER_SECOND, 1_000_000_000.0),
            boundedLong(values, MAXIMUM_FRAME_TIME_MS, 1L, 60_000L),
            boundedInt(values, MAXIMUM_CATCH_UP_STEPS, 1, 10_000),
            boundedInt(values, VIRTUAL_WIDTH, 1, 32_768),
            boundedInt(values, VIRTUAL_HEIGHT, 1, 32_768),
            boundedInt(values, WINDOW_WIDTH, 1, 32_768),
            boundedInt(values, WINDOW_HEIGHT, 1, 32_768),
            strictBoolean(values, VSYNC),
            boundedInt(values, FOREGROUND_FPS, 1, 1_000),
            boundedInt(values, IDLE_FPS, 1, 1_000),
            strictBoolean(values, OVERLAY_ENABLED),
            logLevel(values),
            boundedLong(values, METRICS_SAMPLE_WINDOW_MS, 1L, 60_000L),
            portablePath(home, values.get(EVIDENCE_DIRECTORY))
        );
    }

    private static LinkedHashMap<String, String> defaultValues(Path home) {
        Path temporaryDirectory = Path.of(System.getProperty("java.io.tmpdir"));
        Path evidenceBase = temporaryDirectory.isAbsolute()
            ? temporaryDirectory
            : home.resolve(temporaryDirectory);
        LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
        defaults.put(
            UPDATES_PER_SECOND,
            Double.toString(EngineConfig.DEFAULT_UPDATES_PER_SECOND)
        );
        defaults.put(
            MAXIMUM_FRAME_TIME_MS,
            Long.toString(EngineConfig.DEFAULT_MAXIMUM_FRAME_TIME_MILLIS)
        );
        defaults.put(
            MAXIMUM_CATCH_UP_STEPS,
            Integer.toString(EngineConfig.DEFAULT_MAXIMUM_CATCH_UP_STEPS)
        );
        defaults.put(VIRTUAL_WIDTH, Integer.toString(EngineConfig.DEFAULT_VIRTUAL_WIDTH));
        defaults.put(VIRTUAL_HEIGHT, Integer.toString(EngineConfig.DEFAULT_VIRTUAL_HEIGHT));
        defaults.put(WINDOW_WIDTH, Integer.toString(EngineConfig.DEFAULT_WINDOW_WIDTH));
        defaults.put(WINDOW_HEIGHT, Integer.toString(EngineConfig.DEFAULT_WINDOW_HEIGHT));
        defaults.put(VSYNC, Boolean.toString(EngineConfig.DEFAULT_VSYNC));
        defaults.put(
            FOREGROUND_FPS,
            Integer.toString(EngineConfig.DEFAULT_FOREGROUND_FPS)
        );
        defaults.put(IDLE_FPS, Integer.toString(EngineConfig.DEFAULT_IDLE_FPS));
        defaults.put(
            OVERLAY_ENABLED,
            Boolean.toString(EngineConfig.DEFAULT_OVERLAY_ENABLED)
        );
        defaults.put(LOG_LEVEL, EngineConfig.DEFAULT_LOG_LEVEL.name());
        defaults.put(
            METRICS_SAMPLE_WINDOW_MS,
            Long.toString(EngineConfig.DEFAULT_METRICS_SAMPLE_WINDOW_MILLIS)
        );
        defaults.put(
            EVIDENCE_DIRECTORY,
            evidenceBase.resolve("engine-lite-spike-evidence").normalize().toString()
        );
        return defaults;
    }

    private static double finitePositiveDouble(
        Map<String, String> values,
        String key,
        double maximum
    ) {
        String raw = values.get(key);
        try {
            double parsed = Double.parseDouble(raw);
            if (!Double.isFinite(parsed) || parsed <= 0.0 || parsed > maximum) {
                throw invalid(key, raw, "must be finite and in (0, " + maximum + "]");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid(key, raw, "must be a decimal number", exception);
        }
    }

    private static int boundedInt(
        Map<String, String> values,
        String key,
        int minimum,
        int maximum
    ) {
        String raw = values.get(key);
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < minimum || parsed > maximum) {
                throw invalid(key, raw, "must be in [" + minimum + ", " + maximum + "]");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid(key, raw, "must be an integer", exception);
        }
    }

    private static long boundedLong(
        Map<String, String> values,
        String key,
        long minimum,
        long maximum
    ) {
        String raw = values.get(key);
        try {
            long parsed = Long.parseLong(raw);
            if (parsed < minimum || parsed > maximum) {
                throw invalid(key, raw, "must be in [" + minimum + ", " + maximum + "]");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw invalid(key, raw, "must be an integer", exception);
        }
    }

    private static boolean strictBoolean(Map<String, String> values, String key) {
        String raw = values.get(key);
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        throw invalid(key, raw, "must be true or false");
    }

    private static LogLevel logLevel(Map<String, String> values) {
        String raw = values.get(LOG_LEVEL);
        try {
            return LogLevel.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid(LOG_LEVEL, raw, "must be TRACE, DEBUG, INFO, WARN, ERROR or OFF");
        }
    }

    private static Path portablePath(Path home, String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid(EVIDENCE_DIRECTORY, String.valueOf(raw), "must not be blank");
        }
        try {
            Path candidate = Path.of(raw);
            return (candidate.isAbsolute() ? candidate : home.resolve(candidate))
                .normalize()
                .toAbsolutePath();
        } catch (RuntimeException exception) {
            throw invalid(EVIDENCE_DIRECTORY, raw, "must be a valid path", exception);
        }
    }

    private static Path requireAbsoluteHome(Path applicationHome) {
        if (applicationHome == null) {
            throw new NullPointerException("applicationHome");
        }
        if (!applicationHome.isAbsolute()) {
            throw invalid("application.home", applicationHome.toString(), "must be absolute");
        }
        return applicationHome.normalize();
    }

    private static Path resolveExplicitConfigurationFile(String raw) {
        Path path;
        try {
            path = Path.of(raw);
        } catch (RuntimeException exception) {
            throw invalid("config.file", raw, "must be a valid absolute path", exception);
        }
        if (!path.isAbsolute()) {
            throw invalid("config.file", raw, "must be absolute and independent of CWD");
        }
        path = path.normalize();
        if (!Files.isRegularFile(path)) {
            throw invalid("config.file", raw, "does not identify a readable file");
        }
        return path;
    }

    private static void requireKnownKey(String key, String value) {
        if (!KEYS.contains(key)) {
            throw invalid(key, String.valueOf(value), "unknown field");
        }
    }

    private static EngineConfigException invalid(
        String field,
        String value,
        String reason
    ) {
        return new EngineConfigException(field, value, reason);
    }

    private static EngineConfigException invalid(
        String field,
        String value,
        String reason,
        Throwable cause
    ) {
        return new EngineConfigException(field, value, reason, cause);
    }

    private record ParsedArguments(
        String configurationFile,
        Map<String, String> overrides,
        List<String> remainingArguments
    ) {
    }
}
