package engine.incubator.assets;

import java.util.Objects;

/** Detectable asset failure with stable category and lifecycle context. */
public final class AssetException extends IllegalStateException {
    private final AssetFailure failure;
    private final String groupId;
    private final String assetId;

    public AssetException(
        AssetFailure failure,
        String groupId,
        String assetId,
        String message
    ) {
        this(failure, groupId, assetId, message, null);
    }

    public AssetException(
        AssetFailure failure,
        String groupId,
        String assetId,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.failure = Objects.requireNonNull(failure, "failure");
        this.groupId = Objects.requireNonNullElse(groupId, "");
        this.assetId = Objects.requireNonNullElse(assetId, "");
    }

    public AssetFailure failure() {
        return failure;
    }

    public String groupId() {
        return groupId;
    }

    public String assetId() {
        return assetId;
    }
}
