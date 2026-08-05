package engine.incubator.runtime.lifecycle;

import engine.incubator.events.WorldEventBus;
import engine.incubator.world.id.EntityId;
import engine.incubator.world.id.IdGenerator;
import engine.incubator.world.id.SequentialIdGenerator;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-execution entity registry with explicit ownership.
 */
public final class WorldState {
    private final OwnedResourceRegistry ownership;
    private final IdGenerator idGenerator;
    private final WorldEventBus events;
    private final IdentityHashMap<Object, List<EntityId>> entitiesByOwner =
        new IdentityHashMap<>();
    private final IdentityHashMap<Object, EntityId> idsByEntity = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Object> ownersByEntity = new IdentityHashMap<>();
    private final LinkedHashMap<EntityId, Object> entitiesById = new LinkedHashMap<>();

    WorldState(OwnedResourceRegistry ownership) {
        this(ownership, new SequentialIdGenerator());
    }

    WorldState(OwnedResourceRegistry ownership, IdGenerator idGenerator) {
        this.ownership = Objects.requireNonNull(ownership, "ownership");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        events = new WorldEventBus(owner -> {
            ownership.requireActiveOwner(owner);
            return true;
        });
    }

    /** Preserves the existing fluent add contract; use {@link #register} to receive the ID. */
    public <T> T add(Object owner, T entity) {
        register(owner, entity);
        return entity;
    }

    public EntityId register(Object owner, Object entity) {
        ownership.requireActiveOwner(owner);
        Objects.requireNonNull(entity, "entity");
        if (idsByEntity.containsKey(entity)) {
            throw new IllegalStateException("Entity already belongs to a runtime owner");
        }

        EntityId id = Objects.requireNonNull(idGenerator.next(), "generated EntityId");
        if (entitiesById.containsKey(id)) {
            throw new IllegalStateException("IdGenerator produced a duplicate EntityId: " + id);
        }
        entitiesByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(id);
        idsByEntity.put(entity, id);
        ownersByEntity.put(entity, owner);
        entitiesById.put(id, entity);
        return id;
    }

    public boolean remove(Object entity) {
        Objects.requireNonNull(entity, "entity");
        EntityId id = idsByEntity.get(entity);
        return id != null && remove(id);
    }

    public boolean remove(EntityId id) {
        Objects.requireNonNull(id, "id");
        Object entity = entitiesById.remove(id);
        if (entity == null) {
            return false;
        }
        idsByEntity.remove(entity);
        Object owner = ownersByEntity.remove(entity);
        List<EntityId> ownerEntities = entitiesByOwner.get(owner);
        if (owner == null || ownerEntities == null || !ownerEntities.remove(id)) {
            throw new IllegalStateException("Entity ownership indexes are inconsistent");
        }
        if (ownerEntities.isEmpty()) {
            entitiesByOwner.remove(owner);
        }
        return true;
    }

    public int entityCount() {
        return entitiesById.size();
    }

    public int entityCount(Object owner) {
        List<EntityId> entities = entitiesByOwner.get(owner);
        return entities == null ? 0 : entities.size();
    }

    public List<Object> entities() {
        return List.copyOf(entitiesById.values());
    }

    public Optional<EntityId> idOf(Object entity) {
        return Optional.ofNullable(idsByEntity.get(Objects.requireNonNull(entity, "entity")));
    }

    public Optional<Object> entity(EntityId id) {
        return Optional.ofNullable(entitiesById.get(Objects.requireNonNull(id, "id")));
    }

    public WorldEventBus events() {
        return events;
    }

    void releaseOwner(Object owner) {
        events.unload(owner);
        List<EntityId> entities = entitiesByOwner.remove(owner);
        if (entities == null) {
            return;
        }
        for (EntityId id : entities) {
            Object entity = entitiesById.remove(id);
            idsByEntity.remove(entity);
            ownersByEntity.remove(entity);
        }
    }

    void clear() {
        entitiesByOwner.clear();
        idsByEntity.clear();
        ownersByEntity.clear();
        entitiesById.clear();
        events.close();
    }
}
