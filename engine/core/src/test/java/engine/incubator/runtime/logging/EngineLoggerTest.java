package engine.incubator.runtime.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

final class EngineLoggerTest {
    @Test
    void contextualRecordsIncludeFrameTickAndWorldWithoutMutatingTheParent() {
        var records = new ArrayList<EngineLogRecord>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-08T20:00:00Z"), ZoneOffset.UTC);
        EngineLogger parent = new EngineLogger("runtime", LogLevel.INFO, clock, records::add);
        EngineLogger contextual = parent.withContext(LogContext.worldFrame(7L, 42L, 99L));

        contextual.debug("not emitted");
        contextual.info("frame completed");
        parent.warn("host warning");

        assertEquals(2, records.size());
        assertEquals(new LogContext(42L, 99L, 7L), records.get(0).context());
        assertEquals(LogContext.empty(), records.get(1).context());
        assertEquals(
            "timestamp=2026-08-08T20:00:00Z level=INFO category=runtime "
                + "frame=42 tick=99 world=7 message=\"frame completed\"",
            LogFormatter.format(records.get(0))
        );
        assertFalse(LogFormatter.format(records.get(1)).contains(" frame="));
        assertFalse(LogFormatter.format(records.get(1)).contains(" tick="));
        assertFalse(LogFormatter.format(records.get(1)).contains(" world="));
    }

    @Test
    void failureFormattingStaysOnOneLineAndDisabledLoggerDoesNoWork() {
        var records = new ArrayList<EngineLogRecord>();
        EngineLogger logger = new EngineLogger(
            "streaming assets",
            LogLevel.ERROR,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            records::add
        );
        logger.error("failed\ncleanly", new IllegalStateException("broken"));
        EngineLogger.disabled("disabled").error(
            "ignored",
            new IllegalStateException("ignored")
        );

        String formatted = LogFormatter.format(records.getFirst());
        assertEquals(1, records.size());
        assertTrue(formatted.contains("category=streaming\\ assets"));
        assertTrue(formatted.contains("message=\"failed\\ncleanly\""));
        assertTrue(formatted.endsWith("failure=java.lang.IllegalStateException"));
        assertFalse(formatted.contains(System.lineSeparator()));
    }
}
