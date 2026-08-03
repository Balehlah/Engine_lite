package engine.incubator.assets;

/** Immutable counters used to audit ownership and return-to-baseline behavior. */
public record AssetMetrics(
    long loadRequests,
    long groupsLoaded,
    long groupsUnloaded,
    long assetReferencesLoaded,
    long assetReferencesUnloaded,
    long backendLoads,
    long backendUnloads,
    long loadFailures,
    long staleHandleAccesses,
    int pendingGroups,
    int liveGroups,
    int liveAssetReferences,
    int backendAssets
) {
    public boolean isAtResourceBaseline() {
        return pendingGroups == 0
            && liveGroups == 0
            && liveAssetReferences == 0
            && backendAssets == 0
            && backendLoads == backendUnloads;
    }
}
