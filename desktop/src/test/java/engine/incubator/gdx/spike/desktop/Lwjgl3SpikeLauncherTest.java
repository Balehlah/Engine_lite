package engine.incubator.gdx.spike.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Lwjgl3SpikeLauncherTest {
    @Test
    void parsesSmokeAndAbsoluteEvidenceDirectory() {
        Path evidenceDirectory = Path.of(
            System.getProperty("java.io.tmpdir"),
            "engine-lite-launcher-test"
        ).toAbsolutePath().normalize();

        var configuration = Lwjgl3SpikeLauncher.parseRunConfiguration(
            new String[] {
                "--smoke",
                "--evidence-dir=" + evidenceDirectory
            }
        );

        assertTrue(configuration.smoke());
        assertEquals(evidenceDirectory, configuration.evidenceDirectory());
    }

    @Test
    void rejectsRelativeEvidenceDirectory() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Lwjgl3SpikeLauncher.parseRunConfiguration(
                new String[] {"--evidence-dir=relative"}
            )
        );
    }

    @Test
    void rejectsUnknownAndDuplicateArguments() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Lwjgl3SpikeLauncher.parseRunConfiguration(
                new String[] {"--unknown"}
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> Lwjgl3SpikeLauncher.parseRunConfiguration(
                new String[] {"--smoke", "--smoke"}
            )
        );
    }
}
