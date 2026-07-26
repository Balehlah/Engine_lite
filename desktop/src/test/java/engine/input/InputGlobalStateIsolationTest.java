package engine.input;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("specification")
@ResourceLock("engine.input.Input")
class InputGlobalStateIsolationTest {

    private Input input;
    private Canvas eventSource;

    @BeforeEach
    void resetGlobalInputBeforeTest() {
        input = Input.getInstance();
        eventSource = new Canvas();
        input.clear();
    }

    @AfterEach
    void resetGlobalInputAfterTest() {
        input.clear();
    }

    @Test
    void singletonStartsWithoutHeldOrTransientState() {
        assertAll(
            () -> assertFalse(input.isAnyKeyDown()),
            () -> assertFalse(input.isMouseButtonDown(MouseEvent.BUTTON1)),
            () -> assertFalse(input.isMouseButtonPressed(MouseEvent.BUTTON1)),
            () -> assertFalse(input.isMouseButtonReleased(MouseEvent.BUTTON1)),
            () -> assertEquals(0, input.getScrollDelta())
        );
    }

    @Test
    void clearRemovesHeldAndQueuedState() {
        input.getKeyboard().keyPressed(new KeyEvent(
            eventSource,
            KeyEvent.KEY_PRESSED,
            0,
            0,
            KeyEvent.VK_A,
            'a'
        ));
        input.getMouse().mousePressed(new MouseEvent(
            eventSource,
            MouseEvent.MOUSE_PRESSED,
            0,
            0,
            0,
            0,
            1,
            false,
            MouseEvent.BUTTON1
        ));
        input.getMouse().mouseWheelMoved(new MouseWheelEvent(
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
            -1
        ));

        assertAll(
            () -> assertTrue(input.isKeyDown(KeyEvent.VK_A)),
            () -> assertTrue(input.isMouseButtonDown(MouseEvent.BUTTON1))
        );

        input.clear();
        input.update();

        assertAll(
            () -> assertFalse(input.isAnyKeyDown()),
            () -> assertFalse(input.isMouseButtonDown(MouseEvent.BUTTON1)),
            () -> assertEquals(0, input.getScrollDelta())
        );
    }
}
