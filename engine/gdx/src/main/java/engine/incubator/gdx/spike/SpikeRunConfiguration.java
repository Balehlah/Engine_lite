package engine.incubator.gdx.spike;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Process-independent inputs for the removable Issue #14 spike.
 */
public record SpikeRunConfiguration(boolean smoke, Path evidenceDirectory) {
    public SpikeRunConfiguration {
        Objects.requireNonNull(evidenceDirectory, "evidenceDirectory");
        if (!evidenceDirectory.isAbsolute()) {
            throw new IllegalArgumentException(
                "evidenceDirectory must be absolute: " + evidenceDirectory
            );
        }
        evidenceDirectory = evidenceDirectory.normalize();
    }
}
