package engine.incubator.world.id;

/**
 * Deterministic monotonically increasing entity-ID generator.
 *
 * <p>The maximum positive {@code long} is returned once. The next call fails explicitly and
 * never wraps into a colliding or invalid value.</p>
 */
public final class SequentialIdGenerator implements IdGenerator {
    private long nextValue;
    private boolean exhausted;

    public SequentialIdGenerator() {
        this(1L);
    }

    public SequentialIdGenerator(long firstValue) {
        if (firstValue < 1L) {
            throw new IllegalArgumentException("firstValue must be positive");
        }
        nextValue = firstValue;
    }

    @Override
    public EntityId next() {
        if (exhausted) {
            throw new EntityIdExhaustedException();
        }
        EntityId result = new EntityId(nextValue);
        if (nextValue == Long.MAX_VALUE) {
            exhausted = true;
        } else {
            nextValue++;
        }
        return result;
    }
}
