package engine.incubator.runtime.lifecycle;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Per-execution asset store whose entries participate in owner disposal.
 */
public final class AssetStore {
    private final OwnedResourceRegistry ownership;
    private final Map<String, AssetEntry> assets = new LinkedHashMap<>();

    AssetStore(OwnedResourceRegistry ownership) {
        this.ownership = ownership;
    }

    public <T> T put(Object owner, String id, T asset) {
        return put(owner, id, asset, ignored -> {
        });
    }

    public <T> T put(
        Object owner,
        String id,
        T asset,
        ResourceDisposer<? super T> disposer
    ) {
        requireId(id);
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(disposer, "disposer");
        ownership.requireActiveOwner(owner);
        if (assets.containsKey(id)) {
            throw new IllegalStateException("Asset id is already registered: " + id);
        }
        ownership.register(owner, "asset:" + id, asset, disposer);
        assets.put(id, new AssetEntry(owner, asset));
        return asset;
    }

    public Object get(String id) {
        requireId(id);
        AssetEntry entry = assets.get(id);
        if (entry == null) {
            throw new NoSuchElementException("Unknown asset: " + id);
        }
        return entry.asset;
    }

    public <T> T get(String id, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return type.cast(get(id));
    }

    public boolean contains(String id) {
        requireId(id);
        return assets.containsKey(id);
    }

    public int size() {
        return assets.size();
    }

    void releaseOwner(Object owner) {
        Iterator<AssetEntry> iterator = assets.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().owner == owner) {
                iterator.remove();
            }
        }
    }

    void clear() {
        assets.clear();
    }

    private static void requireId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Asset id must not be blank");
        }
    }

    private record AssetEntry(Object owner, Object asset) {
    }
}
