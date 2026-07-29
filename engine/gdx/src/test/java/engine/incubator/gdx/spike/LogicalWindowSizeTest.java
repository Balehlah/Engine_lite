package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class LogicalWindowSizeTest {
    @Test
    void preservesFixturePixelsAtOneTimesScaling() {
        assertEquals(
            new LogicalWindowSize(800, 600),
            LogicalWindowSize.forBackbuffer(800, 600, 640, 360, 640, 360)
        );
    }

    @Test
    void convertsFixturePixelsAtRetinaTwoTimesScaling() {
        assertEquals(
            new LogicalWindowSize(400, 300),
            LogicalWindowSize.forBackbuffer(800, 600, 640, 360, 1280, 720)
        );
    }

    @Test
    void convertsFixturePixelsAtFractionalScaling() {
        assertEquals(
            new LogicalWindowSize(640, 480),
            LogicalWindowSize.forBackbuffer(800, 600, 800, 600, 1000, 750)
        );
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThrows(
            IllegalArgumentException.class,
            () -> LogicalWindowSize.forBackbuffer(800, 600, 0, 360, 640, 360)
        );
    }
}
