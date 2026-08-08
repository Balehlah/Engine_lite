package engine.incubator.runtime.metrics;

import engine.incubator.assets.AssetMetrics;
import java.util.Objects;

/** Comparable asset health fields consumed by logs and overlays. */
public record AssetHealthMetrics(
    int pendingGroups,
    int liveGroups,
    int liveReferences,
    int backendAssets
) {
    public AssetHealthMetrics {
        if (
            pendingGroups < 0
                || liveGroups < 0
                || liveReferences < 0
                || backendAssets < 0
        ) {
            throw new IllegalArgumentException("asset health counters must be non-negative");
        }
    }

    public static AssetHealthMetrics none() {
        return new AssetHealthMetrics(0, 0, 0, 0);
    }

    public static AssetHealthMetrics from(AssetMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics");
        return new AssetHealthMetrics(
            metrics.pendingGroups(),
            metrics.liveGroups(),
            metrics.liveAssetReferences(),
            metrics.backendAssets()
        );
    }
}
