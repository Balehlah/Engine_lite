package engine.incubator.runtime.input;

import java.util.Objects;
import java.util.Set;

/** Immutable logical input state consumed by exactly one simulation tick. */
public record InputSnapshot(
    long tickIndex,
    Set<Integer> keysDown,
    Set<Integer> keysPressed,
    Set<Integer> keysReleased,
    Set<Integer> mouseButtonsDown,
    Set<Integer> mouseButtonsPressed,
    Set<Integer> mouseButtonsReleased,
    Pointer pointer,
    double scrollX,
    double scrollY,
    boolean focused,
    int processedEventCount
) {
    public InputSnapshot {
        if (tickIndex < 0L) {
            throw new IllegalArgumentException("tickIndex must not be negative: " + tickIndex);
        }
        keysDown = immutableSet(keysDown, "keysDown");
        keysPressed = immutableSet(keysPressed, "keysPressed");
        keysReleased = immutableSet(keysReleased, "keysReleased");
        mouseButtonsDown = immutableSet(mouseButtonsDown, "mouseButtonsDown");
        mouseButtonsPressed = immutableSet(
            mouseButtonsPressed,
            "mouseButtonsPressed"
        );
        mouseButtonsReleased = immutableSet(
            mouseButtonsReleased,
            "mouseButtonsReleased"
        );
        Objects.requireNonNull(pointer, "pointer");
        requireFinite(scrollX, "scrollX");
        requireFinite(scrollY, "scrollY");
        if (processedEventCount < 0) {
            throw new IllegalArgumentException(
                "processedEventCount must not be negative: " + processedEventCount
            );
        }
    }

    public boolean isKeyDown(int keyCode) {
        return keysDown.contains(keyCode);
    }

    public boolean isKeyPressed(int keyCode) {
        return keysPressed.contains(keyCode);
    }

    public boolean isKeyReleased(int keyCode) {
        return keysReleased.contains(keyCode);
    }

    public boolean isMouseButtonDown(int button) {
        return mouseButtonsDown.contains(button);
    }

    public boolean isMouseButtonPressed(int button) {
        return mouseButtonsPressed.contains(button);
    }

    public boolean isMouseButtonReleased(int button) {
        return mouseButtonsReleased.contains(button);
    }

    private static Set<Integer> immutableSet(Set<Integer> source, String name) {
        return Set.copyOf(Objects.requireNonNull(source, name));
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }

    public record Pointer(
        PointerPosition position,
        boolean movedThisTick,
        int screenDeltaX,
        int screenDeltaY,
        int virtualDeltaX,
        int virtualDeltaY
    ) {
        public Pointer {
            Objects.requireNonNull(position, "position");
        }

        public static Pointer unavailable() {
            return new Pointer(PointerPosition.unavailable(), false, 0, 0, 0, 0);
        }
    }
}
