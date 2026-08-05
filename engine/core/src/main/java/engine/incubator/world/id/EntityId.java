package engine.incubator.world.id;

/**
 * Stable identity of an entity inside one world.
 *
 * <p>Values are positive. Equality and hashing depend only on the numeric value, so an ID can
 * safely outlive the mutable entity object that it identifies.</p>
 */
public record EntityId(long value) implements Comparable<EntityId> {
    public EntityId {
        if (value < 1L) {
            throw new IllegalArgumentException("EntityId value must be positive");
        }
    }

    @Override
    public int compareTo(EntityId other) {
        return Long.compare(value, other.value);
    }
}
