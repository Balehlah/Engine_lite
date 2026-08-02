package engine.incubator.gdx.spike;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import engine.incubator.runtime.time.SchedulerMetrics;
import java.util.Locale;
import java.util.Objects;

/**
 * Lightweight runtime overlay for fixed-timestep telemetry.
 */
public final class FixedTimestepDebugOverlay implements Disposable {
    private static final float LEFT_MARGIN = 8f;
    private static final float TOP_MARGIN = 8f;

    private final BitmapFont font = new BitmapFont();

    public void render(
        SpriteBatch batch,
        Matrix4 projection,
        int backbufferHeight,
        SchedulerMetrics metrics
    ) {
        Objects.requireNonNull(batch, "batch");
        Objects.requireNonNull(projection, "projection");
        Objects.requireNonNull(metrics, "metrics");
        if (backbufferHeight <= 0) {
            throw new IllegalArgumentException("backbufferHeight must be positive");
        }

        batch.setProjectionMatrix(projection);
        batch.begin();
        font.draw(batch, format(metrics), LEFT_MARGIN, backbufferHeight - TOP_MARGIN);
        batch.end();
    }

    public static String format(SchedulerMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics");
        return String.format(
            Locale.ROOT,
            "ticks=%d frames=%d alpha=%.3f clamp=%.3fms catchup=%.3fms scale=%.2fx %s",
            metrics.updateCount(),
            metrics.frameCount(),
            metrics.interpolationAlpha(),
            nanosToMillis(metrics.clampedWallTimeNanos()),
            nanosToMillis(metrics.catchUpDiscardedSimulationTimeNanos()),
            metrics.timeScale(),
            metrics.paused() ? "PAUSED" : "RUNNING"
        );
    }

    @Override
    public void dispose() {
        font.dispose();
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
