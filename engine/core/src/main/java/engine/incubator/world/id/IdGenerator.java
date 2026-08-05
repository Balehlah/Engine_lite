package engine.incubator.world.id;

/** Produces entity IDs for exactly one world. */
@FunctionalInterface
public interface IdGenerator {
    EntityId next();
}
