package engine.input;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputCharacterizationTest {

    private final Canvas eventSource = new Canvas();

    @Test
    @Tag("characterization")
    void quickKeyboardPressAndReleaseDisappearAtTheDocumentedUpdateBoundary() {
        Keyboard keyboard = new Keyboard();
        keyboard.keyPressed(keyEvent(KeyEvent.KEY_PRESSED));
        keyboard.keyReleased(keyEvent(KeyEvent.KEY_RELEASED));

        keyboard.update();

        assertAll(
            () -> assertFalse(keyboard.isKeyDown(KeyEvent.VK_A)),
            () -> assertFalse(keyboard.isKeyPressed(KeyEvent.VK_A)),
            () -> assertFalse(keyboard.isKeyReleased(KeyEvent.VK_A))
        );
    }

    @Test
    @Disabled("Known INPUT-EDGE defect; the immutable tick snapshot belongs to issue #16")
    @Tag("specification")
    void quickKeyboardPressAndReleaseMustBothSurviveForOneTick() {
        Keyboard keyboard = new Keyboard();
        keyboard.keyPressed(keyEvent(KeyEvent.KEY_PRESSED));
        keyboard.keyReleased(keyEvent(KeyEvent.KEY_RELEASED));

        keyboard.update();

        assertAll(
            () -> assertTrue(keyboard.isKeyPressed(KeyEvent.VK_A)),
            () -> assertTrue(keyboard.isKeyReleased(KeyEvent.VK_A))
        );
    }

    @Test
    @Tag("characterization")
    void quickMousePressAndReleaseDisappearAtTheDocumentedUpdateBoundary() {
        Mouse mouse = new Mouse();
        mouse.mousePressed(mouseButtonEvent(MouseEvent.MOUSE_PRESSED));
        mouse.mouseReleased(mouseButtonEvent(MouseEvent.MOUSE_RELEASED));

        mouse.update();

        assertAll(
            () -> assertFalse(mouse.isButtonDown(MouseEvent.BUTTON1)),
            () -> assertFalse(mouse.isButtonPressed(MouseEvent.BUTTON1)),
            () -> assertFalse(mouse.isButtonReleased(MouseEvent.BUTTON1))
        );
    }

    @Test
    @Disabled("Known INPUT-EDGE defect; the immutable tick snapshot belongs to issue #16")
    @Tag("specification")
    void quickMousePressAndReleaseMustBothSurviveForOneTick() {
        Mouse mouse = new Mouse();
        mouse.mousePressed(mouseButtonEvent(MouseEvent.MOUSE_PRESSED));
        mouse.mouseReleased(mouseButtonEvent(MouseEvent.MOUSE_RELEASED));

        mouse.update();

        assertAll(
            () -> assertTrue(mouse.isButtonPressed(MouseEvent.BUTTON1)),
            () -> assertTrue(mouse.isButtonReleased(MouseEvent.BUTTON1))
        );
    }

    @Test
    @Tag("characterization")
    void mouseMovementBeforeUpdateIsCollapsedToZeroDelta() {
        Mouse mouse = new Mouse();
        mouse.mouseMoved(mouseMoveEvent(8, 5));

        mouse.update();

        assertAll(
            () -> assertEquals(0, mouse.getDeltaX()),
            () -> assertEquals(0, mouse.getDeltaY())
        );
    }

    @Test
    @Disabled("Known INPUT-DELTA defect; tick-relative mouse delta belongs to issue #16")
    @Tag("specification")
    void mouseMovementBeforeUpdateMustBeVisibleForOneTick() {
        Mouse mouse = new Mouse();
        mouse.mouseMoved(mouseMoveEvent(8, 5));

        mouse.update();

        assertAll(
            () -> assertEquals(8, mouse.getDeltaX()),
            () -> assertEquals(5, mouse.getDeltaY())
        );
    }

    @Test
    @Tag("specification")
    void scrollAccumulatesUntilUpdateAndLastsExactlyOneTick() {
        Mouse mouse = new Mouse();
        mouse.mouseWheelMoved(mouseWheelEvent(-1));
        mouse.mouseWheelMoved(mouseWheelEvent(-1));

        mouse.update();
        assertEquals(2, mouse.getScrollDelta());

        mouse.update();
        assertEquals(0, mouse.getScrollDelta());
    }

    private KeyEvent keyEvent(int eventId) {
        return new KeyEvent(eventSource, eventId, 0, 0, KeyEvent.VK_A, 'a');
    }

    private MouseEvent mouseButtonEvent(int eventId) {
        return new MouseEvent(
            eventSource,
            eventId,
            0,
            0,
            8,
            5,
            1,
            false,
            MouseEvent.BUTTON1
        );
    }

    private MouseEvent mouseMoveEvent(int x, int y) {
        return new MouseEvent(
            eventSource,
            MouseEvent.MOUSE_MOVED,
            0,
            0,
            x,
            y,
            0,
            false
        );
    }

    private MouseWheelEvent mouseWheelEvent(int rotation) {
        return new MouseWheelEvent(
            eventSource,
            MouseEvent.MOUSE_WHEEL,
            0,
            0,
            0,
            0,
            0,
            false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL,
            1,
            rotation
        );
    }
}
