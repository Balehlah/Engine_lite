package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class SpikeRunConfigurationTest {
    @Test
    void normalizesAnAbsoluteEvidenceDirectory() {
        Path absolute = Path.of(
            System.getProperty("java.io.tmpdir"),
            "issue-14",
            "..",
            "evidence"
        ).toAbsolutePath();

        var configuration = new SpikeRunConfiguration(true, absolute);

        assertEquals(absolute.normalize(), configuration.evidenceDirectory());
    }

    @Test
    void rejectsRelativeEvidenceDirectory() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SpikeRunConfiguration(true, Path.of("relative"))
        );
    }
}
