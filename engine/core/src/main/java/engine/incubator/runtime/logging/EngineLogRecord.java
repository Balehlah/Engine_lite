package engine.incubator.runtime.logging;

import java.time.Instant;
import java.util.Objects;

/** Immutable local log event before formatting. */
public record EngineLogRecord(
    Instant timestamp,
    LogLevel level,
    String category,
    LogContext context,
    String message,
    Throwable failure
) {
    public EngineLogRecord {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(message, "message");
        if (category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
