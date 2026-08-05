package engine.incubator.runtime.lifecycle;

/**
 * Immutable leak and isolation evidence captured from one execution context.
 */
public record GameContextSnapshot(
    long executionId,
    boolean closed,
    int entityCount,
    int eventCount,
    int eventSubscriptionCount,
    int assetCount,
    ResourceMetrics resources
) {
    public boolean isClean() {
        return closed
            && entityCount == 0
            && eventCount == 0
            && eventSubscriptionCount == 0
            && assetCount == 0
            && !resources.hasLeaks();
    }
}
