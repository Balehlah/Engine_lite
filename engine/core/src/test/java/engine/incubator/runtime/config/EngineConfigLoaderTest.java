package engine.incubator.runtime.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.incubator.runtime.logging.LogLevel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class EngineConfigLoaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void precedenceIsDefaultsThenFileThenCliWithPerFieldProvenance() throws Exception {
        Path home = temporaryDirectory.resolve("package").toAbsolutePath();
        Files.createDirectories(home.resolve("config"));
        Files.writeString(
            home.resolve("config/engine.properties"),
            String.join(
                "\n",
                "runtime.updates-per-second=30",
                "graphics.window-width=800",
                "debug.overlay-enabled=false",
                "logging.level=DEBUG",
                "paths.evidence-directory=reports/from-file"
            )
        );

        LoadedEngineConfig loaded = EngineConfigLoader.load(
            home,
            new String[] {
                "--smoke",
                "--set=runtime.updates-per-second=120",
                "--overlay"
            }
        );

        EngineConfig config = loaded.configuration();
        assertEquals(120.0, config.updatesPerSecond());
        assertEquals(800, config.windowWidth());
        assertTrue(config.overlayEnabled());
        assertEquals(LogLevel.DEBUG, config.logLevel());
        assertEquals(180, config.virtualHeight());
        assertEquals(
            home.resolve("reports/from-file").normalize(),
            config.evidenceDirectory()
        );
        assertEquals(ConfigurationSource.CLI, loaded.sources().get(
            EngineConfigLoader.UPDATES_PER_SECOND
        ));
        assertEquals(ConfigurationSource.FILE, loaded.sources().get(
            EngineConfigLoader.WINDOW_WIDTH
        ));
        assertEquals(ConfigurationSource.DEFAULTS, loaded.sources().get(
            EngineConfigLoader.VIRTUAL_HEIGHT
        ));
        assertEquals(ConfigurationSource.CLI, loaded.sources().get(
            EngineConfigLoader.OVERLAY_ENABLED
        ));
        assertEquals(java.util.List.of("--smoke"), loaded.remainingArguments());
    }

    @ParameterizedTest
    @MethodSource("invalidBoundaries")
    void invalidBoundariesAlwaysReportFieldAndRawValue(String field, String value) {
        Path home = temporaryDirectory.toAbsolutePath();

        EngineConfigException failure = assertThrows(
            EngineConfigException.class,
            () -> EngineConfigLoader.load(
                home,
                new String[] {"--set=" + field + "=" + value}
            )
        );

        assertEquals(field, failure.field());
        assertEquals(value, failure.rejectedValue());
        assertTrue(failure.getMessage().contains(field));
        assertTrue(failure.getMessage().contains("'" + value + "'"));
    }

    @Test
    void explicitConfigMustBeAbsoluteAndUnknownFieldsFailClosed() throws Exception {
        Path home = temporaryDirectory.toAbsolutePath();
        EngineConfigException relative = assertThrows(
            EngineConfigException.class,
            () -> EngineConfigLoader.load(home, new String[] {"--config=relative.properties"})
        );
        assertEquals("config.file", relative.field());
        assertEquals("relative.properties", relative.rejectedValue());

        Path config = temporaryDirectory.resolve("unknown.properties").toAbsolutePath();
        Files.writeString(config, "remote.telemetry=true\n");
        EngineConfigException unknown = assertThrows(
            EngineConfigException.class,
            () -> EngineConfigLoader.load(
                home,
                new String[] {"--config=" + config}
            )
        );
        assertEquals("remote.telemetry", unknown.field());
        assertEquals("true", unknown.rejectedValue());
    }

    @Test
    void missingDefaultFileUsesImmutableDefaultsWithoutDependingOnCwd() {
        Path home = temporaryDirectory.resolve("application-home").toAbsolutePath();
        LoadedEngineConfig loaded = EngineConfigLoader.load(home, new String[0]);

        assertFalse(loaded.configurationFile().isPresent());
        assertEquals(60.0, loaded.configuration().updatesPerSecond());
        assertEquals(320, loaded.configuration().virtualWidth());
        assertTrue(loaded.configuration().evidenceDirectory().isAbsolute());
        assertTrue(loaded.sources().values().stream().allMatch(
            source -> source == ConfigurationSource.DEFAULTS
        ));
    }

    @Test
    void duplicateCliFieldsAreRejectedInsteadOfSilentlyReordered() {
        Path home = temporaryDirectory.toAbsolutePath();
        EngineConfigException failure = assertThrows(
            EngineConfigException.class,
            () -> EngineConfigLoader.load(
                home,
                new String[] {
                    "--overlay",
                    "--set=debug.overlay-enabled=false"
                }
            )
        );
        assertEquals(EngineConfigLoader.OVERLAY_ENABLED, failure.field());
        assertEquals("false", failure.rejectedValue());
    }

    private static Stream<Arguments> invalidBoundaries() {
        return Stream.of(
            Arguments.of(EngineConfigLoader.UPDATES_PER_SECOND, "0"),
            Arguments.of(EngineConfigLoader.UPDATES_PER_SECOND, "Infinity"),
            Arguments.of(EngineConfigLoader.MAXIMUM_FRAME_TIME_MS, "0"),
            Arguments.of(EngineConfigLoader.MAXIMUM_FRAME_TIME_MS, "60001"),
            Arguments.of(EngineConfigLoader.MAXIMUM_CATCH_UP_STEPS, "0"),
            Arguments.of(EngineConfigLoader.MAXIMUM_CATCH_UP_STEPS, "10001"),
            Arguments.of(EngineConfigLoader.VIRTUAL_WIDTH, "0"),
            Arguments.of(EngineConfigLoader.VIRTUAL_HEIGHT, "32769"),
            Arguments.of(EngineConfigLoader.WINDOW_WIDTH, "-1"),
            Arguments.of(EngineConfigLoader.WINDOW_HEIGHT, "32769"),
            Arguments.of(EngineConfigLoader.VSYNC, "yes"),
            Arguments.of(EngineConfigLoader.FOREGROUND_FPS, "0"),
            Arguments.of(EngineConfigLoader.IDLE_FPS, "1001"),
            Arguments.of(EngineConfigLoader.OVERLAY_ENABLED, "1"),
            Arguments.of(EngineConfigLoader.LOG_LEVEL, "VERBOSE"),
            Arguments.of(EngineConfigLoader.METRICS_SAMPLE_WINDOW_MS, "0"),
            Arguments.of(EngineConfigLoader.EVIDENCE_DIRECTORY, "")
        );
    }
}
