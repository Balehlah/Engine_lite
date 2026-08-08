package engine.incubator.gdx.spike.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import engine.incubator.gdx.spike.LibGdxSpikeApplication;
import engine.incubator.gdx.spike.SpikeRunConfiguration;
import engine.incubator.runtime.config.EngineConfig;
import engine.incubator.runtime.config.EngineConfigLoader;
import java.nio.file.Path;
import java.util.Objects;

/**
 * LWJGL3 process boundary for the libGDX acceptance spike.
 */
public final class Lwjgl3SpikeLauncher {
    private static final int INITIAL_WIDTH = EngineConfig.DEFAULT_WINDOW_WIDTH;
    private static final int INITIAL_HEIGHT = EngineConfig.DEFAULT_WINDOW_HEIGHT;
    private static final int MAX_SMOKE_WIDTH = 1280;
    private static final int MAX_SMOKE_HEIGHT = 720;

    private Lwjgl3SpikeLauncher() {
    }

    public static void main(String[] args) {
        var runConfiguration = parseRunConfiguration(args);
        var engineConfiguration = runConfiguration.engineConfig();
        var windowConfiguration = new Lwjgl3ApplicationConfiguration();
        windowConfiguration.setTitle("Engine Lite - libGDX/LWJGL3 spike");
        windowConfiguration.setWindowedMode(
            engineConfiguration.windowWidth(),
            engineConfiguration.windowHeight()
        );
        windowConfiguration.setResizable(true);
        windowConfiguration.setHdpiMode(HdpiMode.Logical);
        windowConfiguration.setPauseWhenLostFocus(true);
        windowConfiguration.useVsync(engineConfiguration.vsync());
        windowConfiguration.setForegroundFPS(engineConfiguration.foregroundFps());
        windowConfiguration.setIdleFPS(engineConfiguration.idleFps());
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
        return parseRunConfiguration(ApplicationHome.resolve(Lwjgl3SpikeLauncher.class), args);
    }

    static SpikeRunConfiguration parseRunConfiguration(Path applicationHome, String[] args) {
        Objects.requireNonNull(args, "args");
        var loadedConfig = EngineConfigLoader.load(applicationHome, args);
        boolean smoke = false;

        for (String argument : loadedConfig.remainingArguments()) {
            Objects.requireNonNull(argument, "argument");
            if ("--smoke".equals(argument)) {
                if (smoke) {
                    throw new IllegalArgumentException("--smoke may be supplied only once.");
                }
                smoke = true;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
        }

        return new SpikeRunConfiguration(smoke, loadedConfig);
    }
}
