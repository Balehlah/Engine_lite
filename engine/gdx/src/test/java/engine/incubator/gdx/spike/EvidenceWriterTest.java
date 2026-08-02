package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class EvidenceWriterTest {
    @TempDir
    Path evidenceDirectory;

    @Test
    void beginRunRemovesTickTelemetryFromAPreviousExecution() throws Exception {
        EvidenceWriter writer = new EvidenceWriter(evidenceDirectory);
        Path timingLog = evidenceDirectory.resolve("timing.log");
        Path inputLog = evidenceDirectory.resolve("input.log");
        Path unrelatedFile = evidenceDirectory.resolve("runner-owned.log");
        Files.writeString(timingLog, "stale timing");
        Files.writeString(inputLog, "stale input");
        Files.writeString(unrelatedFile, "owned by the package runner");

        writer.beginRun();

        assertFalse(Files.exists(timingLog));
        assertFalse(Files.exists(inputLog));
        assertTrue(Files.exists(unrelatedFile));
    }
}
