package engine.incubator.assets;

/** Machine-readable failure category for asset lifecycle operations. */
public enum AssetFailure {
    DUPLICATE_GROUP,
    TYPE_MISMATCH,
    CWD_DEPENDENT_SOURCE,
    SOURCE_NOT_FOUND,
    LOADER_NOT_FOUND,
    LOAD_FAILED,
    STALE_HANDLE,
    UNKNOWN_ASSET,
    SERVICE_CLOSED
}
