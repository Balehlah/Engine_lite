package engine.incubator.gdx.spike;

/**
 * Converts a requested backbuffer size into the logical GLFW window size.
 */
record LogicalWindowSize(int width, int height) {
    static LogicalWindowSize forBackbuffer(
        int targetBackbufferWidth,
        int targetBackbufferHeight,
        int currentLogicalWidth,
        int currentLogicalHeight,
        int currentBackbufferWidth,
        int currentBackbufferHeight
    ) {
        requirePositive(targetBackbufferWidth, "targetBackbufferWidth");
        requirePositive(targetBackbufferHeight, "targetBackbufferHeight");
        requirePositive(currentLogicalWidth, "currentLogicalWidth");
        requirePositive(currentLogicalHeight, "currentLogicalHeight");
        requirePositive(currentBackbufferWidth, "currentBackbufferWidth");
        requirePositive(currentBackbufferHeight, "currentBackbufferHeight");

        return new LogicalWindowSize(
            scale(
                targetBackbufferWidth,
                currentLogicalWidth,
                currentBackbufferWidth
            ),
            scale(
                targetBackbufferHeight,
                currentLogicalHeight,
                currentBackbufferHeight
            )
        );
    }

    private static int scale(
        int targetBackbufferSize,
        int currentLogicalSize,
        int currentBackbufferSize
    ) {
        long numerator = Math.multiplyExact(
            (long) targetBackbufferSize,
            currentLogicalSize
        );
        return Math.max(
            1,
            Math.toIntExact(
                Math.round((double) numerator / currentBackbufferSize)
            )
        );
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }
}
