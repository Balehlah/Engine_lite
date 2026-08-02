package engine.incubator.runtime.input;

import java.util.Objects;

/**
 * Pointer coordinates mapped for one logical tick.
 *
 * <p>Screen coordinates use the backend's logical, top-left origin. Backbuffer
 * and virtual coordinates use a bottom-left origin to match the rendering
 * viewport.</p>
 */
public record PointerPosition(
    int screenX,
    int screenY,
    int backbufferX,
    int backbufferY,
    int virtualX,
    int virtualY,
    Region region
) {
    public PointerPosition {
        Objects.requireNonNull(region, "region");
    }

    public static PointerPosition unavailable() {
        return new PointerPosition(-1, -1, -1, -1, -1, -1, Region.UNAVAILABLE);
    }

    public boolean isInViewport() {
        return region == Region.VIEWPORT;
    }

    public boolean isInBars() {
        return region == Region.BARS;
    }

    public enum Region {
        VIEWPORT,
        BARS,
        OUTSIDE_SURFACE,
        UNAVAILABLE,
    }
}
