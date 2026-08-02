package engine.incubator.runtime.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class FakeInputTest {
    private static final ScreenToVirtual FULL = new ScreenToVirtual(
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
    private static final ScreenToVirtual DPI_WITH_BARS = new ScreenToVirtual(
        400,
        300,
        800,
        600,
        80,
        120,
        640,
        360,
        320,
        180
    );

    @Test
    void oneThousandInMemoryReplaysProduceTheIdenticalSnapshotSequence() {
        List<List<InputEvent>> script = List.of(
            List.of(
                new InputEvent.KeyChanged(7, true),
                new InputEvent.KeyChanged(7, true),
                new InputEvent.PointerMoved(100, 80)
            ),
            List.of(
                new InputEvent.PointerMoved(120, 90),
                new InputEvent.PointerMoved(130, 95),
                new InputEvent.Scrolled(0.0, 1.0),
                new InputEvent.Scrolled(0.0, 2.0)
            ),
            List.of(
                new InputEvent.MouseButtonChanged(1, true),
                new InputEvent.FocusChanged(false)
            ),
            List.of(new InputEvent.FocusChanged(true)),
            List.of(
                new InputEvent.KeyChanged(9, true),
                new InputEvent.KeyChanged(9, false)
            )
        );
        List<ScreenToVirtual> mappings = List.of(
            FULL,
            FULL,
            DPI_WITH_BARS,
            DPI_WITH_BARS,
            FULL
        );
        List<InputSnapshot> expected = new FakeInput(script).replay(mappings);

        for (int replay = 0; replay < 1_000; replay++) {
            assertEquals(expected, new FakeInput(script).replay(mappings));
        }

        assertTrue(expected.get(0).isKeyPressed(7));
        assertEquals(3.0, expected.get(1).scrollY());
        assertTrue(expected.get(2).isKeyReleased(7));
        assertTrue(expected.get(2).isMouseButtonReleased(1));
        assertFalse(expected.get(2).focused());
        assertTrue(expected.get(4).isKeyPressed(9));
        assertTrue(expected.get(4).isKeyReleased(9));
    }
}
