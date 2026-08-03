package engine.incubator.runtime.lifecycle;

/**
 * Immutable ownership and disposal counters for one game execution.
 */
public record ResourceMetrics(
    long ownersRegistered,
    long ownersDisposed,
    long resourcesRegistered,
    long disposalAttempts,
    long resourcesDisposed,
    long disposalFailures
) {
    public long liveOwners() {
        return ownersRegistered - ownersDisposed;
    }

    public long undisposedResources() {
        return resourcesRegistered - disposalAttempts;
    }

    public long leakedResources() {
        return undisposedResources() + disposalFailures;
    }

    public boolean hasLeaks() {
        return liveOwners() != 0L || leakedResources() != 0L;
    }
}
