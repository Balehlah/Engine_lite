package engine.incubator.gdx.runtime;

import com.badlogic.gdx.utils.Disposable;
import engine.incubator.gdx.spike.FixedTimestepLoop;
import engine.incubator.runtime.lifecycle.GameRuntime;
import engine.incubator.runtime.logging.EngineLogger;
import engine.incubator.runtime.logging.LogContext;
import engine.incubator.runtime.time.FrameSchedule;
import java.util.Objects;

/**
 * Connects a backend-neutral {@link GameRuntime} to libGDX's render-driven loop.
 */
public final class GdxGameRuntimeLoop implements Disposable {
    private final GameRuntime runtime;
    private final FixedTimestepLoop fixedTimestep;
    private final EngineLogger logger;
    private boolean disposed;

    public GdxGameRuntimeLoop(GameRuntime runtime, FixedTimestepLoop fixedTimestep) {
        this(runtime, fixedTimestep, EngineLogger.disabled("runtime.loop"));
    }

    public GdxGameRuntimeLoop(
        GameRuntime runtime,
        FixedTimestepLoop fixedTimestep,
        EngineLogger logger
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.fixedTimestep = Objects.requireNonNull(fixedTimestep, "fixedTimestep");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public FrameSchedule renderFrame() {
        requireOpen();
        long world = runtime.context().executionId();
        FrameSchedule frame = fixedTimestep.runFrame(runtime::fixedUpdate, (alpha, ignored) -> {
            runtime.render(alpha);
        });
        EngineLogger contextual = logger.withContext(
            LogContext.worldFrame(
                world,
                frame.metrics().frameCount(),
                frame.metrics().updateCount()
            )
        );
        if (frame.catchUpDiscardedSimulationTimeNanos() > 0L) {
            contextual.warn(
                "catch-up discarded "
                    + frame.catchUpDiscardedSimulationTimeNanos()
                    + " simulation nanoseconds"
            );
        } else {
            contextual.debug("frame completed");
        }
        return frame;
    }

    public void pause() {
        requireOpen();
        fixedTimestep.pause();
    }

    public void resume() {
        requireOpen();
        fixedTimestep.resume();
    }

    public boolean isPaused() {
        return fixedTimestep.isPaused();
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        runtime.close();
    }

    private void requireOpen() {
        if (disposed) {
            throw new IllegalStateException("GdxGameRuntimeLoop is disposed");
        }
    }
}
