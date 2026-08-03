package engine.incubator.assets;

import java.util.Objects;

/**
 * Stable logical asset identifier carrying the exact Java type expected by consumers.
 *
 * @param <T> shared asset data type
 */
public final class AssetId<T extends SharedAssetData> {
    private final String value;
    private final Class<T> type;

    private AssetId(String value, Class<T> type) {
        this.value = requireName(value, "asset id");
        this.type = Objects.requireNonNull(type, "type");
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Asset type must be a reference type: " + type);
        }
    }

    public static <T extends SharedAssetData> AssetId<T> of(String value, Class<T> type) {
        return new AssetId<>(value, type);
    }

    public String value() {
        return value;
    }

    public Class<T> type() {
        return type;
    }

    @Override
    public boolean equals(Object candidate) {
        return this == candidate
            || candidate instanceof AssetId<?> other
            && value.equals(other.value)
            && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public String toString() {
        return value + "<" + type.getSimpleName() + ">";
    }

    static String requireName(String value, String role) {
        Objects.requireNonNull(value, role);
        if (value.isBlank()) {
            throw new IllegalArgumentException(role + " must not be blank");
        }
        return value;
    }
}
