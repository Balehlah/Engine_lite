package engine.incubator.assets;

import java.util.List;

/** Backend-neutral typed facade and sole lifecycle authority for loaded assets. */
public interface AssetService extends AutoCloseable {
    AssetLoad load(AssetManifest manifest);

    /** Pumps queued backend work once and returns whether no group load remains pending. */
    boolean update();

    AssetMetrics metrics();

    List<AssetDiagnostic> diagnostics();

    boolean isClosed();

    @Override
    void close();
}
