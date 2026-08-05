package engine.incubator.events;

import java.util.Objects;

/** Explicit type token for a world event; no reflection scanning is performed. */
public record EventType<T>(String name, Class<T> payloadType) {
    public EventType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(payloadType, "payloadType");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Event type name must not be blank");
        }
    }

    public static <T> EventType<T> of(String name, Class<T> payloadType) {
        return new EventType<>(name, payloadType);
    }

    T cast(Object payload) {
        return payloadType.cast(payload);
    }
}
