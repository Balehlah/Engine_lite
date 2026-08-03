package engine.incubator.assets;

/** Loaded group ownership handle; closing it unloads each manifest entry once. */
public interface AssetGroupHandle extends AutoCloseable {
    String groupId();

    long generation();

    <T extends SharedAssetData> AssetHandle<T> handle(AssetId<T> id);

    boolean isStale();

    @Override
    void close();
}
