package engine.incubator.assets;

import java.util.Objects;
import java.util.Optional;

/**
 * One typed manifest entry with a portable classpath-relative source and optional fallback.
 *
 * @param <T> shared asset data type
 */
public record AssetEntry<T extends SharedAssetData>(
    AssetId<T> id,
    String sourcePath,
    Optional<String> fallbackPath
) {
    public AssetEntry {
        Objects.requireNonNull(id, "id");
        sourcePath = AssetPaths.requirePortable(sourcePath, "source path");
        fallbackPath = Objects.requireNonNull(fallbackPath, "fallbackPath")
            .map(path -> AssetPaths.requirePortable(path, "fallback path"));
        if (fallbackPath.filter(sourcePath::equals).isPresent()) {
            throw new IllegalArgumentException(
                "Fallback path must differ from the primary source for " + id.value()
            );
        }
    }

    public static <T extends SharedAssetData> AssetEntry<T> required(
        AssetId<T> id,
        String sourcePath
    ) {
        return new AssetEntry<>(id, sourcePath, Optional.empty());
    }

    public static <T extends SharedAssetData> AssetEntry<T> withFallback(
        AssetId<T> id,
        String sourcePath,
        String fallbackPath
    ) {
        return new AssetEntry<>(id, sourcePath, Optional.of(fallbackPath));
    }
}
