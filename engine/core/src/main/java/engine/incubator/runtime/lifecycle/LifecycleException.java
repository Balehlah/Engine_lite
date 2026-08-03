package engine.incubator.runtime.lifecycle;

/**
 * Reports a checked lifecycle or resource cleanup failure without hiding earlier failures.
 */
public final class LifecycleException extends RuntimeException {
    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}

final class LifecycleFailures {
    private LifecycleFailures() {
    }

    static Throwable append(Throwable primary, Throwable next) {
        if (next == null) {
            return primary;
        }
        if (primary == null) {
            return next;
        }
        if (primary != next) {
            primary.addSuppressed(next);
        }
        return primary;
    }

    static void rethrow(Throwable failure, String message) {
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new LifecycleException(message, failure);
    }
}
