package engine.incubator.runtime.logging;

import java.util.Objects;

/** Stable single-line formatter for local files and process streams. */
public final class LogFormatter {
    private LogFormatter() {
    }

    public static String format(EngineLogRecord record) {
        Objects.requireNonNull(record, "record");
        StringBuilder output = new StringBuilder(128)
            .append("timestamp=").append(record.timestamp())
            .append(" level=").append(record.level())
            .append(" category=").append(escapeToken(record.category()));
        LogContext context = record.context();
        if (context.frame() != null) {
            output.append(" frame=").append(context.frame());
        }
        if (context.tick() != null) {
            output.append(" tick=").append(context.tick());
        }
        if (context.world() != null) {
            output.append(" world=").append(context.world());
        }
        output.append(" message=\"").append(escapeQuoted(record.message())).append('"');
        if (record.failure() != null) {
            output.append(" failure=")
                .append(escapeToken(record.failure().getClass().getName()));
        }
        return output.toString();
    }

    private static String escapeToken(String value) {
        return value.replace("\\", "\\\\").replace(" ", "\\ ");
    }

    private static String escapeQuoted(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }
}
