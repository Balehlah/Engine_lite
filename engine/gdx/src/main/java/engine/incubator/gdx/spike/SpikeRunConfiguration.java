package engine.incubator.gdx.spike;

import engine.incubator.runtime.config.EngineConfig;
import engine.incubator.runtime.config.LoadedEngineConfig;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Process-independent inputs for the removable Issue #14 spike.
 */
public record SpikeRunConfiguration(boolean smoke, LoadedEngineConfig loadedConfig) {
    public SpikeRunConfiguration {
        Objects.requireNonNull(loadedConfig, "loadedConfig");
    }

    public EngineConfig engineConfig() {
        return loadedConfig.configuration();
    }

    public Path evidenceDirectory() {
        return engineConfig().evidenceDirectory();
    }
}
