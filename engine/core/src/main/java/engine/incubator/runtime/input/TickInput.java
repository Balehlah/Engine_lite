package engine.incubator.runtime.input;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Stateful reducer that turns an ordered callback queue into immutable tick snapshots.
 */
public final class TickInput {
    private final InputEventQueue eventQueue;
    private final Set<Integer> keysDown = new HashSet<>();
    private final Set<Integer> mouseButtonsDown = new HashSet<>();

    private long nextTickIndex;
    private boolean focused = true;
    private boolean pointerAvailable;
    private int pointerScreenX;
    private int pointerScreenY;
    private PointerPosition lastSnapshotPointerPosition =
        PointerPosition.unavailable();

    public TickInput() {
        this(new InputEventQueue());
    }

    public TickInput(int queueCapacity) {
        this(new InputEventQueue(queueCapacity));
    }

    public TickInput(InputEventQueue eventQueue) {
        this.eventQueue = Objects.requireNonNull(eventQueue, "eventQueue");
    }

    public InputEventQueue.EnqueueResult enqueue(InputEvent event) {
        return eventQueue.enqueue(event);
    }

    public InputSnapshot nextSnapshot(ScreenToVirtual mapping) {
        Objects.requireNonNull(mapping, "mapping");
        List<InputEvent> events = eventQueue.drain();
        Set<Integer> keysPressed = new HashSet<>();
        Set<Integer> keysReleased = new HashSet<>();
        Set<Integer> buttonsPressed = new HashSet<>();
        Set<Integer> buttonsReleased = new HashSet<>();
        boolean movedThisTick = false;
        boolean pointerWasAvailable = pointerAvailable;
        int previousScreenX = pointerScreenX;
        int previousScreenY = pointerScreenY;
        double scrollX = 0.0;
        double scrollY = 0.0;

        for (InputEvent event : events) {
            if (event instanceof InputEvent.KeyChanged key) {
                if (focused || !key.down()) {
                    applyEdge(
                        key.keyCode(),
                        key.down(),
                        keysDown,
                        keysPressed,
                        keysReleased
                    );
                }
            } else if (event instanceof InputEvent.MouseButtonChanged button) {
                if (focused || !button.down()) {
                    applyEdge(
                        button.button(),
                        button.down(),
                        mouseButtonsDown,
                        buttonsPressed,
                        buttonsReleased
                    );
                }
            } else if (event instanceof InputEvent.PointerMoved pointer) {
                pointerAvailable = true;
                pointerScreenX = pointer.screenX();
                pointerScreenY = pointer.screenY();
                movedThisTick = true;
            } else if (event instanceof InputEvent.Scrolled scroll) {
                scrollX = addFinite(scrollX, scroll.amountX(), "scrollX");
                scrollY = addFinite(scrollY, scroll.amountY(), "scrollY");
            } else if (event instanceof InputEvent.FocusChanged focus) {
                focused = focus.focused();
                if (!focused) {
                    keysReleased.addAll(keysDown);
                    buttonsReleased.addAll(mouseButtonsDown);
                    keysDown.clear();
                    mouseButtonsDown.clear();
                }
            }
        }

        InputSnapshot.Pointer pointer = pointerSnapshot(
            mapping,
            pointerWasAvailable,
            previousScreenX,
            previousScreenY,
            movedThisTick
        );
        InputSnapshot snapshot = new InputSnapshot(
            nextTickIndex,
            keysDown,
            keysPressed,
            keysReleased,
            mouseButtonsDown,
            buttonsPressed,
            buttonsReleased,
            pointer,
            scrollX,
            scrollY,
            focused,
            events.size()
        );
        nextTickIndex = Math.incrementExact(nextTickIndex);
        return snapshot;
    }

    public long nextTickIndex() {
        return nextTickIndex;
    }

    public InputEventQueue.Metrics queueMetrics() {
        return eventQueue.metrics();
    }

    private InputSnapshot.Pointer pointerSnapshot(
        ScreenToVirtual mapping,
        boolean pointerWasAvailable,
        int previousScreenX,
        int previousScreenY,
        boolean movedThisTick
    ) {
        if (!pointerAvailable) {
            return InputSnapshot.Pointer.unavailable();
        }

        PointerPosition current = mapping.map(pointerScreenX, pointerScreenY);
        int screenDeltaX = 0;
        int screenDeltaY = 0;
        int virtualDeltaX = 0;
        int virtualDeltaY = 0;
        if (movedThisTick && pointerWasAvailable) {
            screenDeltaX = Math.subtractExact(pointerScreenX, previousScreenX);
            screenDeltaY = Math.subtractExact(pointerScreenY, previousScreenY);
            if (
                current.isInViewport()
                    && lastSnapshotPointerPosition.isInViewport()
            ) {
                virtualDeltaX = Math.subtractExact(
                    current.virtualX(),
                    lastSnapshotPointerPosition.virtualX()
                );
                virtualDeltaY = Math.subtractExact(
                    current.virtualY(),
                    lastSnapshotPointerPosition.virtualY()
                );
            }
        }
        lastSnapshotPointerPosition = current;
        return new InputSnapshot.Pointer(
            current,
            movedThisTick,
            screenDeltaX,
            screenDeltaY,
            virtualDeltaX,
            virtualDeltaY
        );
    }

    private static void applyEdge(
        int code,
        boolean down,
        Set<Integer> held,
        Set<Integer> pressed,
        Set<Integer> released
    ) {
        if (down) {
            if (held.add(code)) {
                pressed.add(code);
            }
        } else if (held.remove(code)) {
            released.add(code);
        }
    }

    private static double addFinite(double left, double right, String name) {
        double result = left + right;
        if (!Double.isFinite(result)) {
            throw new ArithmeticException(name + " overflowed while accumulating input");
        }
        return result;
    }
}
