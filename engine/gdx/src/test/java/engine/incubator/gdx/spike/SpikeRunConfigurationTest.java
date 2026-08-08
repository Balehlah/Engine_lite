package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;

import engine.incubator.runtime.config.EngineConfigLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SpikeRunConfigurationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void exposesTheImmutableLoadedConfigurationAndNormalizedEvidenceDirectory() {
        Path absolute = Path.of(
            System.getProperty("java.io.tmpdir"),
            "issue-14",
            "..",
            "evidence"
        ).toAbsolutePath();

        var loaded = EngineConfigLoader.load(
            temporaryDirectory.toAbsolutePath(),
            new String[] {"--evidence-dir=" + absolute}
        );
        var configuration = new SpikeRunConfiguration(true, loaded);

        assertEquals(absolute.normalize(), configuration.evidenceDirectory());
        assertEquals(loaded.configuration(), configuration.engineConfig());
        assertEquals(loaded, configuration.loadedConfig());
    }
}
