package engine.incubator.runtime.input;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class TickInputTest {
    private static final ScreenToVirtual FULL_VIEWPORT = new ScreenToVirtual(
        640,
        360,
        640,
        360,
        0,
        0,
        640,
        360,
        320,
        180
    );

    @Test
    void pressAndReleaseBetweenTicksPreserveBothEdgesForExactlyOneTick() {
        TickInput input = new TickInput();
        input.enqueue(new InputEvent.KeyChanged(10, true));
        input.enqueue(new InputEvent.KeyChanged(10, false));
        input.enqueue(new InputEvent.MouseButtonChanged(1, true));
        input.enqueue(new InputEvent.MouseButtonChanged(1, false));

        InputSnapshot edgeTick = input.nextSnapshot(FULL_VIEWPORT);
        InputSnapshot followingTick = input.nextSnapshot(FULL_VIEWPORT);

        assertAll(
            () -> assertTrue(edgeTick.isKeyPressed(10)),
            () -> assertTrue(edgeTick.isKeyReleased(10)),
            () -> assertFalse(edgeTick.isKeyDown(10)),
            () -> assertTrue(edgeTick.isMouseButtonPressed(1)),
            () -> assertTrue(edgeTick.isMouseButtonReleased(1)),
            () -> assertFalse(edgeTick.isMouseButtonDown(1)),
            () -> assertFalse(followingTick.isKeyPressed(10)),
            () -> assertFalse(followingTick.isKeyReleased(10)),
            () -> assertFalse(followingTick.isMouseButtonPressed(1)),
            () -> assertFalse(followingTick.isMouseButtonReleased(1))
        );
    }

    @Test
    void repeatedDownCallbacksDoNotRepeatThePressedEdge() {
        TickInput input = new TickInput();
        input.enqueue(new InputEvent.KeyChanged(4, true));
        input.enqueue(new InputEvent.KeyChanged(4, true));
        InputSnapshot first = input.nextSnapshot(FULL_VIEWPORT);

        input.enqueue(new InputEvent.KeyChanged(4, true));
        InputSnapshot repeated = input.nextSnapshot(FULL_VIEWPORT);

        assertAll(
            () -> assertTrue(first.isKeyDown(4)),
            () -> assertTrue(first.isKeyPressed(4)),
            () -> assertTrue(repeated.isKeyDown(4)),
            () -> assertFalse(repeated.isKeyPressed(4)),
            () -> assertFalse(repeated.isKeyReleased(4))
        );
    }

    @Test
    void focusLostReleasesHeldControlsAndIgnoresLateDownCallbacks() {
        TickInput input = new TickInput();
        input.enqueue(new InputEvent.KeyChanged(8, true));
        input.enqueue(new InputEvent.MouseButtonChanged(2, true));
        input.nextSnapshot(FULL_VIEWPORT);

        input.enqueue(new InputEvent.FocusChanged(false));
        input.enqueue(new InputEvent.KeyChanged(9, true));
        input.enqueue(new InputEvent.MouseButtonChanged(3, true));
        InputSnapshot lost = input.nextSnapshot(FULL_VIEWPORT);
        InputSnapshot following = input.nextSnapshot(FULL_VIEWPORT);

        assertAll(
            () -> assertFalse(lost.focused()),
            () -> assertTrue(lost.isKeyReleased(8)),
            () -> assertTrue(lost.isMouseButtonReleased(2)),
            () -> assertFalse(lost.isKeyDown(8)),
            () -> assertFalse(lost.isKeyDown(9)),
            () -> assertFalse(lost.isMouseButtonDown(2)),
            () -> assertFalse(lost.isMouseButtonDown(3)),
            () -> assertFalse(following.isKeyReleased(8)),
            () -> assertFalse(following.isMouseButtonReleased(2))
        );

        input.enqueue(new InputEvent.FocusChanged(true));
        input.enqueue(new InputEvent.KeyChanged(9, true));
        assertTrue(input.nextSnapshot(FULL_VIEWPORT).isKeyPressed(9));
    }

    @Test
    void movementDeltaScrollAndResizeMappingAreTickRelative() {
        TickInput input = new TickInput();
        input.enqueue(new InputEvent.PointerMoved(160, 90));
        InputSnapshot initial = input.nextSnapshot(FULL_VIEWPORT);

        input.enqueue(new InputEvent.PointerMoved(170, 95));
        input.enqueue(new InputEvent.PointerMoved(180, 100));
        input.enqueue(new InputEvent.Scrolled(1.5, -2.0));
        input.enqueue(new InputEvent.Scrolled(-0.5, 3.0));
        InputSnapshot moved = input.nextSnapshot(FULL_VIEWPORT);

        ScreenToVirtual resizedAtTwoX = new ScreenToVirtual(
            320,
            180,
            640,
            360,
            0,
            0,
            640,
            360,
            320,
            180
        );
        InputSnapshot remapped = input.nextSnapshot(resizedAtTwoX);
        InputSnapshot idle = input.nextSnapshot(resizedAtTwoX);

        assertAll(
            () -> assertTrue(initial.pointer().movedThisTick()),
            () -> assertEquals(0, initial.pointer().screenDeltaX()),
            () -> assertEquals(80, initial.pointer().position().virtualX()),
            () -> assertEquals(10, moved.pointer().virtualDeltaX()),
            () -> assertEquals(-5, moved.pointer().virtualDeltaY()),
            () -> assertEquals(20, moved.pointer().screenDeltaX()),
            () -> assertEquals(10, moved.pointer().screenDeltaY()),
            () -> assertEquals(1.0, moved.scrollX()),
            () -> assertEquals(1.0, moved.scrollY()),
            () -> assertFalse(remapped.pointer().movedThisTick()),
            () -> assertEquals(180, remapped.pointer().position().virtualX()),
            () -> assertEquals(79, remapped.pointer().position().virtualY()),
            () -> assertEquals(0, remapped.pointer().virtualDeltaX()),
            () -> assertEquals(0.0, idle.scrollX()),
            () -> assertEquals(0.0, idle.scrollY())
        );
    }

    @Test
    void movementDuringResizeUsesThePreviousTickVirtualPositionForDelta() {
        TickInput input = new TickInput();
        input.enqueue(new InputEvent.PointerMoved(120, 152));
        InputSnapshot beforeResize = input.nextSnapshot(FULL_VIEWPORT);

        ScreenToVirtual resizedWithBars = new ScreenToVirtual(
            800,
            600,
            800,
            600,
            80,
            120,
            640,
            360,
            320,
            180
        );
        input.enqueue(new InputEvent.PointerMoved(200, 272));
        InputSnapshot afterResize = input.nextSnapshot(resizedWithBars);

        assertAll(
            () -> assertEquals(60, beforeResize.pointer().position().virtualX()),
            () -> assertEquals(103, beforeResize.pointer().position().virtualY()),
            () -> assertEquals(60, afterResize.pointer().position().virtualX()),
            () -> assertEquals(103, afterResize.pointer().position().virtualY()),
            () -> assertEquals(0, afterResize.pointer().virtualDeltaX()),
            () -> assertEquals(0, afterResize.pointer().virtualDeltaY()),
            () -> assertEquals(80, afterResize.pointer().screenDeltaX()),
            () -> assertEquals(120, afterResize.pointer().screenDeltaY())
        );
    }

    @Test
    void snapshotsAreImmutableCopies() {
        TickInput input = new TickInput();
        input.enqueue(new InputEvent.KeyChanged(1, true));
        InputSnapshot snapshot = input.nextSnapshot(FULL_VIEWPORT);

        assertThrows(UnsupportedOperationException.class, () -> snapshot.keysDown().add(2));
        input.enqueue(new InputEvent.KeyChanged(1, false));
        input.nextSnapshot(FULL_VIEWPORT);
        assertEquals(java.util.Set.of(1), snapshot.keysDown());
    }
}
