package engine.incubator.assets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable declaration of every asset owned by one lifecycle group. */
public final class AssetManifest {
    private final String groupId;
    private final List<AssetEntry<?>> entries;

    private AssetManifest(String groupId, List<AssetEntry<?>> entries) {
        this.groupId = AssetId.requireName(groupId, "asset group id");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Asset manifest must contain at least one entry");
        }
        this.entries = List.copyOf(entries);
    }

    public static Builder builder(String groupId) {
        return new Builder(groupId);
    }

    public String groupId() {
        return groupId;
    }

    public List<AssetEntry<?>> entries() {
        return entries;
    }

    /** Builder that rejects ambiguous logical IDs and source aliases within a group. */
    public static final class Builder {
        private final String groupId;
        private final Map<String, AssetEntry<?>> entriesById = new LinkedHashMap<>();
        private final Set<String> candidatePaths = new HashSet<>();

        private Builder(String groupId) {
            this.groupId = AssetId.requireName(groupId, "asset group id");
        }

        public <T extends SharedAssetData> Builder add(AssetId<T> id, String sourcePath) {
            return addEntry(AssetEntry.required(id, sourcePath));
        }

        public <T extends SharedAssetData> Builder add(
            AssetId<T> id,
            String sourcePath,
            String fallbackPath
        ) {
            return addEntry(AssetEntry.withFallback(id, sourcePath, fallbackPath));
        }

        public Builder addEntry(AssetEntry<?> entry) {
            Objects.requireNonNull(entry, "entry");
            String id = entry.id().value();
            if (entriesById.containsKey(id)) {
                throw new IllegalArgumentException(
                    "Duplicate asset id in group '" + groupId + "': " + id
                );
            }

            List<String> paths = new ArrayList<>();
            paths.add(entry.sourcePath());
            entry.fallbackPath().ifPresent(paths::add);
            for (String path : paths) {
                if (candidatePaths.contains(path)) {
                    throw new IllegalArgumentException(
                        "Duplicate asset source in group '" + groupId + "': " + path
                    );
                }
            }
            entriesById.put(id, entry);
            candidatePaths.addAll(paths);
            return this;
        }

        public AssetManifest build() {
            return new AssetManifest(groupId, List.copyOf(entriesById.values()));
        }
    }
}
