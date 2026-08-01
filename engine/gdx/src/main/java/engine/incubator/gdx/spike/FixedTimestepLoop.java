package engine.incubator.gdx.spike;

import engine.incubator.runtime.time.FixedTimestepConfig;
import engine.incubator.runtime.time.FixedTimestepScheduler;
import engine.incubator.runtime.time.FrameSchedule;
import engine.incubator.runtime.time.SchedulerMetrics;
import java.util.Objects;

/**
 * Adapts the backend-neutral scheduler to libGDX's render-driven lifecycle.
 */
public final class FixedTimestepLoop {
    private final FixedTimestepScheduler scheduler;

    public FixedTimestepLoop(FixedTimestepScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public static FixedTimestepLoop createDefault() {
        return new FixedTimestepLoop(FixedTimestepScheduler.createDefault());
    }

    public FrameSchedule runFrame(FixedUpdate update, IndependentRender render) {
        Objects.requireNonNull(update, "update");
        Objects.requireNonNull(render, "render");

        FrameSchedule frame = scheduler.nextFrame();
        for (int index = 0; index < frame.updateCount(); index++) {
            update.update(frame.fixedDeltaSeconds());
        }
        render.render(frame.interpolationAlpha(), frame.metrics());
        return frame;
    }

    public FixedTimestepConfig configuration() {
        return scheduler.configuration();
    }

    public SchedulerMetrics metrics() {
        return scheduler.metrics();
    }

    public void pause() {
        scheduler.pause();
    }

    public void resume() {
        scheduler.resume();
    }

    public boolean isPaused() {
        return scheduler.isPaused();
    }

    public void requestStep() {
        scheduler.requestStep();
    }

    public double timeScale() {
        return scheduler.timeScale();
    }

    public void setTimeScale(double timeScale) {
        scheduler.setTimeScale(timeScale);
    }

    @FunctionalInterface
    public interface FixedUpdate {
        void update(double fixedDeltaSeconds);
    }

    @FunctionalInterface
    public interface IndependentRender {
        void render(double interpolationAlpha, SchedulerMetrics metrics);
    }
}
