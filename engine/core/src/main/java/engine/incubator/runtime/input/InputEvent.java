package engine.incubator.runtime.input;

/**
 * Backend-neutral input event captured between logical ticks.
 *
 * <p>Events are immutable and their insertion order is preserved by
 * {@link InputEventQueue}. Backend callbacks must only translate and enqueue
 * events; logical state changes when {@link TickInput#nextSnapshot} consumes
 * them.</p>
 */
public sealed interface InputEvent permits
    InputEvent.KeyChanged,
    InputEvent.MouseButtonChanged,
    InputEvent.PointerMoved,
    InputEvent.Scrolled,
    InputEvent.FocusChanged {

    record KeyChanged(int keyCode, boolean down) implements InputEvent {
        public KeyChanged {
            requireNonNegative(keyCode, "keyCode");
        }
    }

    record MouseButtonChanged(int button, boolean down) implements InputEvent {
        public MouseButtonChanged {
            requireNonNegative(button, "button");
        }
    }

    record PointerMoved(int screenX, int screenY) implements InputEvent {
    }

    record Scrolled(double amountX, double amountY) implements InputEvent {
        public Scrolled {
            requireFinite(amountX, "amountX");
            requireFinite(amountY, "amountY");
        }
    }

    record FocusChanged(boolean focused) implements InputEvent {
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative: " + value);
        }
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
