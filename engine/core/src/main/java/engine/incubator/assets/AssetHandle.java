package engine.incubator.assets;

/**
 * Generation-bound view of shared asset data.
 *
 * <p>The returned value is shared read-only data. Consumers do not own it and must never
 * dispose or mutate it. A handle fails with {@link AssetFailure#STALE_HANDLE} after its group
 * unloads.</p>
 */
public interface AssetHandle<T extends SharedAssetData> {
    AssetId<T> id();

    T value();

    boolean isStale();
}
