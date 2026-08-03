package engine.incubator.assets;

import java.util.Objects;

/** Deterministic, immutable diagnostic emitted by an {@link AssetService}. */
public record AssetDiagnostic(
    long sequence,
    AssetDiagnosticSeverity severity,
    AssetDiagnosticCode code,
    String groupId,
    String assetId,
    String sourcePath,
    String message
) {
    public AssetDiagnostic {
        if (sequence < 1L) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        groupId = Objects.requireNonNullElse(groupId, "");
        assetId = Objects.requireNonNullElse(assetId, "");
        sourcePath = Objects.requireNonNullElse(sourcePath, "");
        message = Objects.requireNonNull(message, "message");
    }
}
