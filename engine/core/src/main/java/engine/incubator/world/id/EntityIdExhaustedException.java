package engine.incubator.world.id;

/** Raised instead of wrapping when a sequential entity-ID range is exhausted. */
public final class EntityIdExhaustedException extends IllegalStateException {
    public EntityIdExhaustedException() {
        super("EntityId sequence is exhausted");
    }
}
