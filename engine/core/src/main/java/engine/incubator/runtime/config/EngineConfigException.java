package engine.incubator.runtime.config;

/** Failure that always identifies the rejected field and raw value. */
public final class EngineConfigException extends IllegalArgumentException {
    private final String field;
    private final String rejectedValue;

    public EngineConfigException(String field, String rejectedValue, String reason) {
        super(
            "Invalid configuration field '"
                + field
                + "' with value '"
                + rejectedValue
                + "': "
                + reason
        );
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public EngineConfigException(
        String field,
        String rejectedValue,
        String reason,
        Throwable cause
    ) {
        this(field, rejectedValue, reason);
        initCause(cause);
    }

    public String field() {
        return field;
    }

    public String rejectedValue() {
        return rejectedValue;
    }
}
