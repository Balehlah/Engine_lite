package engine.incubator.runtime.logging;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Immutable contextual logger with an injected sink and clock.
 *
 * <p>Creating a child context never mutates the parent and no global logger is installed.</p>
 */
public final class EngineLogger {
    private final String category;
    private final LogLevel minimumLevel;
    private final Clock clock;
    private final Consumer<EngineLogRecord> sink;
    private final LogContext context;

    public EngineLogger(
        String category,
        LogLevel minimumLevel,
        Clock clock,
        Consumer<EngineLogRecord> sink
    ) {
        this(category, minimumLevel, clock, sink, LogContext.empty());
    }

    private EngineLogger(
        String category,
        LogLevel minimumLevel,
        Clock clock,
        Consumer<EngineLogRecord> sink,
        LogContext context
    ) {
        this.category = Objects.requireNonNull(category, "category");
        this.minimumLevel = Objects.requireNonNull(minimumLevel, "minimumLevel");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.context = Objects.requireNonNull(context, "context");
        if (category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
    }

    public static EngineLogger disabled(String category) {
        return new EngineLogger(category, LogLevel.OFF, Clock.systemUTC(), ignored -> { });
    }

    public EngineLogger withContext(LogContext context) {
        return new EngineLogger(category, minimumLevel, clock, sink, context);
    }

    public boolean isEnabled(LogLevel level) {
        return minimumLevel.accepts(Objects.requireNonNull(level, "level"));
    }

    public void trace(String message) {
        log(LogLevel.TRACE, message, null);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public void error(String message, Throwable failure) {
        log(LogLevel.ERROR, message, Objects.requireNonNull(failure, "failure"));
    }

    public void log(LogLevel level, String message, Throwable failure) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(message, "message");
        if (!isEnabled(level)) {
            return;
        }
        sink.accept(
            new EngineLogRecord(
                clock.instant(),
                level,
                category,
                context,
                message,
                failure
            )
        );
    }
}
