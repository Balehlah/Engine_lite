package engine.incubator.gdx.runtime;

import com.badlogic.gdx.utils.Disposable;
import engine.incubator.gdx.spike.FixedTimestepLoop;
import engine.incubator.runtime.lifecycle.GameRuntime;
import engine.incubator.runtime.time.FrameSchedule;
import java.util.Objects;

/**
 * Connects a backend-neutral {@link GameRuntime} to libGDX's render-driven loop.
 */
public final class GdxGameRuntimeLoop implements Disposable {
    private final GameRuntime runtime;
    private final FixedTimestepLoop fixedTimestep;
    private boolean disposed;

    public GdxGameRuntimeLoop(GameRuntime runtime, FixedTimestepLoop fixedTimestep) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.fixedTimestep = Objects.requireNonNull(fixedTimestep, "fixedTimestep");
    }

    public FrameSchedule renderFrame() {
        requireOpen();
        return fixedTimestep.runFrame(runtime::fixedUpdate, (alpha, ignored) -> {
            runtime.render(alpha);
        });
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
