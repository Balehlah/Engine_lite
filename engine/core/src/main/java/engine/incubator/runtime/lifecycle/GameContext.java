package engine.incubator.runtime.lifecycle;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Mutable state that exists for exactly one game execution and is never global.
 */
public final class GameContext implements AutoCloseable {
    private final long executionId;
    private final OwnedResourceRegistry resources = new OwnedResourceRegistry();
    private final WorldState world = new WorldState(resources);
    private final RuntimeEventQueue events = new RuntimeEventQueue(resources);
    private final AssetStore assets = new AssetStore(resources);

    private Consumer<RuntimeScene> transitionRequester;
    private boolean closing;
    private boolean closed;

    GameContext(long executionId, Consumer<RuntimeScene> transitionRequester) {
        if (executionId < 1L) {
            throw new IllegalArgumentException("executionId must be positive");
        }
        this.executionId = executionId;
        this.transitionRequester = Objects.requireNonNull(
            transitionRequester,
            "transitionRequester"
        );
        resources.registerOwner(this, "execution:" + executionId);
    }

    public long executionId() {
        return executionId;
    }

    /** Returns the owner used for execution-wide resources and state. */
    public Object executionOwner() {
        return this;
    }

    public WorldState world() {
        return world;
    }

    public RuntimeEventQueue events() {
        return events;
    }

    public AssetStore assets() {
        return assets;
    }

    public OwnedResourceRegistry resources() {
        return resources;
    }

    /** Queues a scene change. A callback-triggered change is applied after that callback. */
    public void requestScene(RuntimeScene scene) {
        requireOpen();
        transitionRequester.accept(Objects.requireNonNull(scene, "scene"));
    }

    public boolean isClosed() {
        return closed;
    }

    public GameContextSnapshot snapshot() {
        return new GameContextSnapshot(
            executionId,
            closed,
            world.entityCount(),
            events.size(),
            assets.size(),
            resources.metrics()
        );
    }

    void registerSceneOwner(RuntimeScene scene) {
        requireOpen();
        resources.registerOwner(scene, "scene:" + scene.getClass().getName());
    }

    void releaseOwner(Object owner) {
        Throwable failure = null;
        world.releaseOwner(owner);
        events.releaseOwner(owner);
        assets.releaseOwner(owner);
        try {
            resources.disposeOwner(owner);
        } catch (Throwable cleanupFailure) {
            failure = LifecycleFailures.append(failure, cleanupFailure);
        }
        if (failure != null) {
            LifecycleFailures.rethrow(failure, "Runtime owner cleanup failed");
        }
    }

    @Override
    public void close() {
        if (closed || closing) {
            return;
        }
        closing = true;
        transitionRequester = null;
        world.clear();
        events.clear();
        assets.clear();

        Throwable failure = null;
        try {
            resources.close();
        } catch (Throwable cleanupFailure) {
            failure = LifecycleFailures.append(failure, cleanupFailure);
        } finally {
            closed = true;
            closing = false;
        }
        if (failure != null) {
            LifecycleFailures.rethrow(failure, "Game context cleanup failed");
        }
    }

    private void requireOpen() {
        if (closed || closing) {
            throw new IllegalStateException("GameContext is closed");
        }
    }
}
