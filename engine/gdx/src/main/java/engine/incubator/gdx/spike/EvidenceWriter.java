package engine.incubator.gdx.spike;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;

final class EvidenceWriter {
    private static final String[] RUN_OUTPUTS = {
        "lifecycle.log",
        "probe.log",
        "dispose.log",
        "viewport.log",
        "timing.log",
        "input.log",
        "summary.properties",
        "failure.log",
        "probe-tone.wav",
        "viewport-640x360.png",
        "viewport-800x600.png",
        "viewport-1280x720.png",
    };

    private final Path directory;

    EvidenceWriter(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to create evidence directory " + directory,
                exception
            );
        }
    }

    Path directory() {
        return directory;
    }

    Path resolve(String fileName) {
        return directory.resolve(fileName);
    }

    synchronized void beginRun() {
        for (String fileName : RUN_OUTPUTS) {
            try {
                Files.deleteIfExists(resolve(fileName));
            } catch (IOException exception) {
                throw new IllegalStateException(
                    "Unable to reset spike evidence " + fileName,
                    exception
                );
            }
        }
    }

    synchronized void append(String fileName, String message) {
        String line = Instant.now() + " " + message + System.lineSeparator();
        try {
            Files.writeString(
                resolve(fileName),
                line,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to append spike evidence " + fileName,
                exception
            );
        }
    }

    synchronized void write(String fileName, String contents) {
        try {
            Files.writeString(
                resolve(fileName),
                contents,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to write spike evidence " + fileName,
                exception
            );
        }
    }
}
