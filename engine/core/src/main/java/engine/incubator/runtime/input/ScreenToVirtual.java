package engine.incubator.runtime.input;

/**
 * Immutable mapping from logical screen coordinates to a virtual framebuffer.
 *
 * <p>The presentation viewport is expressed in physical backbuffer pixels and
 * uses a bottom-left origin. This lets a backend supply its exact integer
 * viewport while this backend-neutral type handles DPI conversion and bar
 * detection at the logical tick boundary.</p>
 */
public record ScreenToVirtual(
    int logicalWidth,
    int logicalHeight,
    int backbufferWidth,
    int backbufferHeight,
    int viewportX,
    int viewportY,
    int viewportWidth,
    int viewportHeight,
    int virtualWidth,
    int virtualHeight
) {
    public ScreenToVirtual {
        requirePositive(logicalWidth, "logicalWidth");
        requirePositive(logicalHeight, "logicalHeight");
        requirePositive(backbufferWidth, "backbufferWidth");
        requirePositive(backbufferHeight, "backbufferHeight");
        requirePositive(viewportWidth, "viewportWidth");
        requirePositive(viewportHeight, "viewportHeight");
        requirePositive(virtualWidth, "virtualWidth");
        requirePositive(virtualHeight, "virtualHeight");
        Math.addExact(viewportX, viewportWidth);
        Math.addExact(viewportY, viewportHeight);
    }

    public PointerPosition map(int screenX, int screenY) {
        if (
            screenX < 0
                || screenY < 0
                || screenX >= logicalWidth
                || screenY >= logicalHeight
        ) {
            return new PointerPosition(
                screenX,
                screenY,
                -1,
                -1,
                -1,
                -1,
                PointerPosition.Region.OUTSIDE_SURFACE
            );
        }

        int backbufferX = scaleFloor(screenX, backbufferWidth, logicalWidth);
        int backbufferYFromTop = scaleFloor(
            screenY,
            backbufferHeight,
            logicalHeight
        );
        int backbufferY = backbufferHeight - 1 - backbufferYFromTop;
        boolean insideViewport =
            backbufferX >= viewportX
                && backbufferX < viewportX + viewportWidth
                && backbufferY >= viewportY
                && backbufferY < viewportY + viewportHeight;

        if (!insideViewport) {
            return new PointerPosition(
                screenX,
                screenY,
                backbufferX,
                backbufferY,
                -1,
                -1,
                PointerPosition.Region.BARS
            );
        }

        int virtualX = scaleFloor(
            backbufferX - viewportX,
            virtualWidth,
            viewportWidth
        );
        int virtualY = scaleFloor(
            backbufferY - viewportY,
            virtualHeight,
            viewportHeight
        );
        return new PointerPosition(
            screenX,
            screenY,
            backbufferX,
            backbufferY,
            virtualX,
            virtualY,
            PointerPosition.Region.VIEWPORT
        );
    }

    private static int scaleFloor(int coordinate, int targetSize, int sourceSize) {
        return Math.toIntExact(
            Math.floorDiv(Math.multiplyExact((long) coordinate, targetSize), sourceSize)
        );
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
