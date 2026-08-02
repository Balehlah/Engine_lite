package engine.incubator.runtime.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class InputEventQueueTest {
    @Test
    void coalescesOnlyAdjacentMovementWithoutReorderingOtherEvents() {
        InputEventQueue queue = new InputEventQueue(4);

        assertEquals(
            InputEventQueue.EnqueueResult.ENQUEUED,
            queue.enqueue(new InputEvent.PointerMoved(1, 2))
        );
        assertEquals(
            InputEventQueue.EnqueueResult.MOVEMENT_COALESCED,
            queue.enqueue(new InputEvent.PointerMoved(3, 4))
        );
        queue.enqueue(new InputEvent.KeyChanged(7, true));
        queue.enqueue(new InputEvent.PointerMoved(5, 6));
        queue.enqueue(new InputEvent.Scrolled(0.0, 1.0));

        assertEquals(
            List.of(
                new InputEvent.PointerMoved(3, 4),
                new InputEvent.KeyChanged(7, true),
                new InputEvent.PointerMoved(5, 6),
                new InputEvent.Scrolled(0.0, 1.0)
            ),
            queue.drain()
        );
        assertEquals(
            new InputEventQueue.Metrics(4, 0, 5L, 1L, 0L),
            queue.metrics()
        );
    }

    @Test
    void aMovementCanBeCoalescedAtCapacityButEdgesNeverAre() {
        InputEventQueue movementQueue = new InputEventQueue(1);
        movementQueue.enqueue(new InputEvent.PointerMoved(1, 1));
        movementQueue.enqueue(new InputEvent.PointerMoved(2, 2));

        assertEquals(List.of(new InputEvent.PointerMoved(2, 2)), movementQueue.drain());
        assertEquals(1L, movementQueue.metrics().coalescedMovementCount());
        assertEquals(0L, movementQueue.metrics().overflowCount());

        InputEventQueue edgeQueue = new InputEventQueue(1);
        edgeQueue.enqueue(new InputEvent.KeyChanged(4, true));
        InputEventQueue.OverflowException failure = assertThrows(
            InputEventQueue.OverflowException.class,
            () -> edgeQueue.enqueue(new InputEvent.KeyChanged(4, false))
        );

        assertEquals(1, failure.capacity());
        assertInstanceOf(InputEvent.KeyChanged.class, failure.rejectedEvent());
        assertEquals(1L, edgeQueue.metrics().overflowCount());
        assertEquals(
            List.of(new InputEvent.KeyChanged(4, true)),
            edgeQueue.drain()
        );
    }
}
