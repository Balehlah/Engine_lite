package engine.incubator.runtime.lifecycle;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Per-execution entity registry with explicit ownership.
 */
public final class WorldState {
    private final OwnedResourceRegistry ownership;
    private final IdentityHashMap<Object, List<Object>> entitiesByOwner = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Object> ownersByEntity = new IdentityHashMap<>();

    WorldState(OwnedResourceRegistry ownership) {
        this.ownership = ownership;
    }

    public <T> T add(Object owner, T entity) {
        ownership.requireActiveOwner(owner);
        Objects.requireNonNull(entity, "entity");
        if (ownersByEntity.containsKey(entity)) {
            throw new IllegalStateException("Entity already belongs to a runtime owner");
        }
        entitiesByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(entity);
        ownersByEntity.put(entity, owner);
        return entity;
    }

    public boolean remove(Object entity) {
        Objects.requireNonNull(entity, "entity");
        Object owner = ownersByEntity.remove(entity);
        if (owner == null) {
            return false;
        }
        List<Object> entities = entitiesByOwner.get(owner);
        removeByIdentity(entities, entity);
        if (entities.isEmpty()) {
            entitiesByOwner.remove(owner);
        }
        return true;
    }

    public int entityCount() {
        return ownersByEntity.size();
    }

    public int entityCount(Object owner) {
        List<Object> entities = entitiesByOwner.get(owner);
        return entities == null ? 0 : entities.size();
    }

    public List<Object> entities() {
        return List.copyOf(ownersByEntity.keySet());
    }

    void releaseOwner(Object owner) {
        List<Object> entities = entitiesByOwner.remove(owner);
        if (entities == null) {
            return;
        }
        entities.forEach(ownersByEntity::remove);
    }

    void clear() {
        entitiesByOwner.clear();
        ownersByEntity.clear();
    }

    private static void removeByIdentity(List<Object> entities, Object target) {
        for (int index = 0; index < entities.size(); index++) {
            if (entities.get(index) == target) {
                entities.remove(index);
                return;
            }
        }
        throw new IllegalStateException("Entity ownership indexes are inconsistent");
    }
}
