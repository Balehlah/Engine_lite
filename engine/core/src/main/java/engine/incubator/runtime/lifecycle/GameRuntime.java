package engine.incubator.runtime.lifecycle;

import java.util.ArrayDeque;
import java.util.Objects;

/**
 * Owns deterministic scene transitions and isolated {@link GameContext} executions.
 *
 * <p>Scene requests are FIFO. Requests made by any lifecycle callback are applied only after
 * that callback returns, so an update or render never observes a half-applied transition.
 * Host lifecycle commands such as {@link #restart(RuntimeScene)} and {@link #close()} are
 * accepted only outside lifecycle callbacks.</p>
 */
public final class GameRuntime implements AutoCloseable {
    private final ArrayDeque<RuntimeScene> pendingScenes = new ArrayDeque<>();
    private GameContext context;
    private SceneState activeScene;
    private GameContextSnapshot lastClosedContext;
    private long executionsStarted;
    private long restarts;
    private long sceneTransitions;
    private long failedExecutions;
    private boolean inCallback;
    private boolean applyingTransitions;
    private boolean closed;

    public GameRuntime() {
        createExecution();
    }

    public GameContext context() {
        requireExecution();
        return context;
    }

    public void start(RuntimeScene initialScene) {
        requireExecution();
        if (activeScene != null || !pendingScenes.isEmpty()) {
            throw new IllegalStateException("Execution is already started");
        }
        requestScene(initialScene);
    }

    public void requestScene(RuntimeScene scene) {
        requireExecution();
        pendingScenes.addLast(Objects.requireNonNull(scene, "scene"));
        if (!inCallback && !applyingTransitions) {
            applyTransitionsOrFail();
        }
    }

    public void fixedUpdate(double fixedDeltaSeconds) {
        if (!Double.isFinite(fixedDeltaSeconds) || fixedDeltaSeconds <= 0.0) {
            throw new IllegalArgumentException(
                "fixedDeltaSeconds must be finite and positive"
            );
        }
        requireExecution();
        applyTransitionsOrFail();
        SceneState scene = activeScene;
        if (scene == null) {
            return;
        }
        invokeActiveCallback(
            () -> scene.scene.fixedUpdate(context, fixedDeltaSeconds),
            "fixedUpdate"
        );
    }

    public void render(double interpolationAlpha) {
        if (!Double.isFinite(interpolationAlpha)
            || interpolationAlpha < 0.0
            || interpolationAlpha >= 1.0) {
            throw new IllegalArgumentException("interpolationAlpha must be in [0, 1)");
        }
        requireExecution();
        applyTransitionsOrFail();
        SceneState scene = activeScene;
        if (scene == null) {
            return;
        }
        invokeActiveCallback(
            () -> scene.scene.render(context, interpolationAlpha),
            "render"
        );
    }

    public void restart(RuntimeScene initialScene) {
        requireRuntimeOpen();
        requireHostBoundary("restart");
        Objects.requireNonNull(initialScene, "initialScene");
        Throwable cleanupFailure = closeExecutionCapture();
        if (cleanupFailure != null) {
            failedExecutions++;
            LifecycleFailures.rethrow(cleanupFailure, "Execution restart cleanup failed");
        }
        restarts++;
        createExecution();
        start(initialScene);
    }

    public RuntimeMetrics metrics() {
        return new RuntimeMetrics(
            executionsStarted,
            restarts,
            sceneTransitions,
            failedExecutions,
            lastClosedContext
        );
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        requireHostBoundary("close");
        Throwable failure = closeExecutionCapture();
        closed = true;
        if (failure != null) {
            LifecycleFailures.rethrow(failure, "Game runtime cleanup failed");
        }
    }

    private void createExecution() {
        executionsStarted++;
        context = new GameContext(executionsStarted, this::requestScene);
    }

    private void invokeActiveCallback(Runnable callback, String phase) {
        Throwable callbackFailure = null;
        inCallback = true;
        try {
            callback.run();
        } catch (Throwable failure) {
            callbackFailure = failure;
        } finally {
            inCallback = false;
        }
        if (callbackFailure != null) {
            failExecution(callbackFailure, "Scene " + phase + " failed");
        }
        applyTransitionsOrFail();
    }

    private void applyTransitionsOrFail() {
        if (pendingScenes.isEmpty() || applyingTransitions) {
            return;
        }
        Throwable transitionFailure = null;
        applyingTransitions = true;
        try {
            while (!pendingScenes.isEmpty()) {
                transitionTo(pendingScenes.removeFirst());
            }
        } catch (Throwable failure) {
            transitionFailure = failure;
        } finally {
            applyingTransitions = false;
        }
        if (transitionFailure != null) {
            failExecution(transitionFailure, "Scene transition failed");
        }
    }

    private void transitionTo(RuntimeScene nextScene) {
        SceneState previous = activeScene;
        activeScene = null;
        if (previous != null) {
            Throwable previousFailure = disposeScene(previous, context);
            if (previousFailure != null) {
                LifecycleFailures.rethrow(previousFailure, "Previous scene cleanup failed");
            }
        }

        SceneState next = new SceneState(nextScene);
        context.registerSceneOwner(nextScene);

        Throwable failure = null;
        next.createInvoked = true;
        try {
            invokeLifecycleCallback(() -> nextScene.create(context));
            next.created = true;
            invokeLifecycleCallback(() -> nextScene.enter(context));
            next.entered = true;
            activeScene = next;
            sceneTransitions++;
        } catch (Throwable callbackFailure) {
            failure = LifecycleFailures.append(failure, callbackFailure);
        }

        if (failure != null) {
            Throwable cleanupFailure = disposeScene(next, context);
            failure = LifecycleFailures.append(failure, cleanupFailure);
            LifecycleFailures.rethrow(failure, "Next scene initialization failed");
        }
    }

    private Throwable disposeScene(SceneState state, GameContext executionContext) {
        Throwable failure = null;
        if (state.entered && !state.exited) {
            state.exited = true;
            try {
                invokeLifecycleCallback(() -> state.scene.exit(executionContext));
            } catch (Throwable exitFailure) {
                failure = LifecycleFailures.append(failure, exitFailure);
            }
        }
        if (state.createInvoked && !state.disposed) {
            state.disposed = true;
            try {
                invokeLifecycleCallback(() -> state.scene.dispose(executionContext));
            } catch (Throwable disposeFailure) {
                failure = LifecycleFailures.append(failure, disposeFailure);
            }
        }
        try {
            executionContext.releaseOwner(state.scene);
        } catch (Throwable ownerFailure) {
            failure = LifecycleFailures.append(failure, ownerFailure);
        }
        return failure;
    }

    private void invokeLifecycleCallback(Runnable callback) {
        boolean previousInCallback = inCallback;
        inCallback = true;
        try {
            callback.run();
        } finally {
            inCallback = previousInCallback;
        }
    }

    private void failExecution(Throwable primaryFailure, String message) {
        failedExecutions++;
        Throwable cleanupFailure = closeExecutionCapture();
        Throwable combined = LifecycleFailures.append(primaryFailure, cleanupFailure);
        LifecycleFailures.rethrow(combined, message);
    }

    private Throwable closeExecutionCapture() {
        GameContext executionContext = context;
        if (executionContext == null) {
            pendingScenes.clear();
            return null;
        }

        Throwable failure = null;
        SceneState scene = activeScene;
        activeScene = null;
        pendingScenes.clear();
        if (scene != null) {
            failure = LifecycleFailures.append(
                failure,
                disposeScene(scene, executionContext)
            );
        }
        try {
            executionContext.close();
        } catch (Throwable contextFailure) {
            failure = LifecycleFailures.append(failure, contextFailure);
        } finally {
            pendingScenes.clear();
            lastClosedContext = executionContext.snapshot();
            context = null;
        }
        return failure;
    }

    private void requireExecution() {
        requireRuntimeOpen();
        if (context == null) {
            throw new IllegalStateException("Execution failed; restart is required");
        }
    }

    private void requireRuntimeOpen() {
        if (closed) {
            throw new IllegalStateException("GameRuntime is closed");
        }
    }

    private void requireHostBoundary(String operation) {
        if (inCallback || applyingTransitions) {
            throw new IllegalStateException(
                operation + " is allowed only outside lifecycle callbacks"
            );
        }
    }

    private static final class SceneState {
        private final RuntimeScene scene;
        private boolean createInvoked;
        private boolean created;
        private boolean entered;
        private boolean exited;
        private boolean disposed;

        private SceneState(RuntimeScene scene) {
            this.scene = scene;
        }
    }
}
