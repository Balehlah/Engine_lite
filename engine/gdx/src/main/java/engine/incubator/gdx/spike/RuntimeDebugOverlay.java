package engine.incubator.gdx.spike;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import engine.incubator.runtime.metrics.FrameHealthMetrics;
import java.util.Locale;
import java.util.Objects;

/** Toggleable local overlay for comparable runtime health metrics. */
public final class RuntimeDebugOverlay implements Disposable {
    private static final float LEFT_MARGIN = 8f;
    private static final float TOP_MARGIN = 8f;

    private final DebugOverlayState state;
    private final BitmapFont font = new BitmapFont();

    public RuntimeDebugOverlay(DebugOverlayState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    /** Returns false without formatting or issuing a draw when the overlay is disabled. */
    public boolean render(
        SpriteBatch batch,
        Matrix4 projection,
        int backbufferHeight,
        FrameHealthMetrics metrics
    ) {
        if (!state.isEnabled()) {
            return false;
        }
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
        return true;
    }

    public static String format(FrameHealthMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics");
        return String.format(
            Locale.ROOT,
            "FPS %.1f | UPS %.1f | frame %d | tick %d%n"
                + "updates %d | catch-up %d / %.3f ms | alpha %.3f%n"
                + "assets pending/live/refs/backend %d/%d/%d/%d | draw calls %d | %s",
            metrics.framesPerSecond(),
            metrics.updatesPerSecond(),
            metrics.frame(),
            metrics.tick(),
            metrics.updatesThisFrame(),
            metrics.catchUpLimitHits(),
            metrics.catchUpDiscardedNanos() / 1_000_000.0,
            metrics.interpolationAlpha(),
            metrics.assets().pendingGroups(),
            metrics.assets().liveGroups(),
            metrics.assets().liveReferences(),
            metrics.assets().backendAssets(),
            metrics.drawCalls(),
            metrics.paused() ? "PAUSED" : "RUNNING"
        );
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
