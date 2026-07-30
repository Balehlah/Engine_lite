package engine.incubator.gdx.spike;

/**
 * Integer-scaled, centered presentation rectangle for a virtual framebuffer.
 *
 * <p>When the backbuffer is smaller than the virtual resolution, the result is
 * explicitly marked degraded and uses a cropped 1x presentation. It never
 * reports a fractional or downscaled pixel-perfect mode.</p>
 */
public record IntegerViewport(
    int x,
    int y,
    int width,
    int height,
    int scale,
    boolean degraded
) {
    public static IntegerViewport calculate(
        int backbufferWidth,
        int backbufferHeight,
        int virtualWidth,
        int virtualHeight
    ) {
        requirePositive(backbufferWidth, "backbufferWidth");
        requirePositive(backbufferHeight, "backbufferHeight");
        requirePositive(virtualWidth, "virtualWidth");
        requirePositive(virtualHeight, "virtualHeight");

        int fittingScale = Math.min(
            backbufferWidth / virtualWidth,
            backbufferHeight / virtualHeight
        );
        boolean degraded = fittingScale < 1;
        int integerScale = Math.max(1, fittingScale);
        int presentedWidth = Math.multiplyExact(virtualWidth, integerScale);
        int presentedHeight = Math.multiplyExact(virtualHeight, integerScale);

        return new IntegerViewport(
            Math.floorDiv(backbufferWidth - presentedWidth, 2),
            Math.floorDiv(backbufferHeight - presentedHeight, 2),
            presentedWidth,
            presentedHeight,
            integerScale,
            degraded
        );
    }

    public int leftBar() {
        return Math.max(0, x);
    }

    public int bottomBar() {
        return Math.max(0, y);
    }

    public boolean hasBars() {
        return x > 0 || y > 0;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
