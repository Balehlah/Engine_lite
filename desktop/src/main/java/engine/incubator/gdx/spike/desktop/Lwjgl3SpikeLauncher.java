package engine.incubator.gdx.spike.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import engine.incubator.gdx.spike.LibGdxSpikeApplication;
import engine.incubator.gdx.spike.SpikeRunConfiguration;
import java.nio.file.Path;
import java.util.Objects;

/**
 * LWJGL3 process boundary for the libGDX acceptance spike.
 */
public final class Lwjgl3SpikeLauncher {
    private static final int INITIAL_WIDTH = 640;
    private static final int INITIAL_HEIGHT = 360;
    private static final int MAX_SMOKE_WIDTH = 1280;
    private static final int MAX_SMOKE_HEIGHT = 720;

    private Lwjgl3SpikeLauncher() {
    }

    public static void main(String[] args) {
        var runConfiguration = parseRunConfiguration(args);
        var windowConfiguration = new Lwjgl3ApplicationConfiguration();
        windowConfiguration.setTitle("Engine Lite - libGDX/LWJGL3 spike");
        windowConfiguration.setWindowedMode(INITIAL_WIDTH, INITIAL_HEIGHT);
        windowConfiguration.setResizable(true);
        windowConfiguration.setHdpiMode(HdpiMode.Logical);
        windowConfiguration.setPauseWhenLostFocus(true);
        windowConfiguration.useVsync(true);
        windowConfiguration.setForegroundFPS(60);
        windowConfiguration.setIdleFPS(60);
        if (runConfiguration.smoke()) {
            windowConfiguration.setWindowSizeLimits(
                INITIAL_WIDTH,
                INITIAL_HEIGHT,
                MAX_SMOKE_WIDTH,
                MAX_SMOKE_HEIGHT
            );
        }

        new Lwjgl3Application(
            new LibGdxSpikeApplication(runConfiguration),
            windowConfiguration
        );
    }

    static SpikeRunConfiguration parseRunConfiguration(String[] args) {
        Objects.requireNonNull(args, "args");
        boolean smoke = false;
        Path evidenceDirectory = Path.of(
            System.getProperty("java.io.tmpdir"),
            "engine-lite-spike-evidence"
        ).toAbsolutePath().normalize();
        boolean evidenceDirectorySupplied = false;

        for (String argument : args) {
            Objects.requireNonNull(argument, "argument");
            if ("--smoke".equals(argument)) {
                if (smoke) {
                    throw new IllegalArgumentException("--smoke may be supplied only once.");
                }
                smoke = true;
            } else if (argument.startsWith("--evidence-dir=")) {
                if (evidenceDirectorySupplied) {
                    throw new IllegalArgumentException(
                        "--evidence-dir may be supplied only once."
                    );
                }
                String value = argument.substring("--evidence-dir=".length());
                if (value.isBlank()) {
                    throw new IllegalArgumentException(
                        "--evidence-dir requires a non-empty absolute path."
                    );
                }
                Path candidate = Path.of(value);
                if (!candidate.isAbsolute()) {
                    throw new IllegalArgumentException(
                        "--evidence-dir must be absolute: " + value
                    );
                }
                evidenceDirectory = candidate.normalize();
                evidenceDirectorySupplied = true;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
        }

        return new SpikeRunConfiguration(smoke, evidenceDirectory);
    }
}
