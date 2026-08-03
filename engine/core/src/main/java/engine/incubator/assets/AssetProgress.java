package engine.incubator.assets;

/** Immutable progress snapshot for one queued or active group load. */
public record AssetProgress(String groupId, int loadedAssets, int totalAssets) {
    public AssetProgress {
        AssetId.requireName(groupId, "asset group id");
        if (loadedAssets < 0 || totalAssets < 1 || loadedAssets > totalAssets) {
            throw new IllegalArgumentException("Invalid asset progress counters");
        }
    }

    public double fraction() {
        return (double) loadedAssets / (double) totalAssets;
    }
}
