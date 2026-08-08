package engine.incubator.gdx.spike;

/** Small toggle whose disabled hot path is one volatile read and one branch. */
public final class DebugOverlayState {
    private volatile boolean enabled;

    public DebugOverlayState(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
