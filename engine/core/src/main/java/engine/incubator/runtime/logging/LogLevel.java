package engine.incubator.runtime.logging;

/** Ordered local log levels; no remote telemetry is implied. */
public enum LogLevel {
    TRACE(0),
    DEBUG(10),
    INFO(20),
    WARN(30),
    ERROR(40),
    OFF(Integer.MAX_VALUE);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public boolean accepts(LogLevel eventLevel) {
        return this != OFF && eventLevel != OFF && eventLevel.severity >= severity;
    }
}
