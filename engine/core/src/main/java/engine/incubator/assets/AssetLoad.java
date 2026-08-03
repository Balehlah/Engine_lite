package engine.incubator.assets;

import java.util.concurrent.CompletionStage;

/** Non-blocking group load submitted to the backend thread through {@link AssetService#update()}. */
public interface AssetLoad {
    String groupId();

    AssetProgress progress();

    boolean isDone();

    CompletionStage<AssetGroupHandle> completion();
}
