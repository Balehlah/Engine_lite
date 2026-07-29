package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class IntegerViewportTest {
    @Test
    void matchesEveryAdr002AcceptanceFixture() {
        assertEquals(
            new IntegerViewport(0, 0, 640, 360, 2, false),
            IntegerViewport.calculate(640, 360, 320, 180)
        );
        assertEquals(
            new IntegerViewport(80, 120, 640, 360, 2, false),
            IntegerViewport.calculate(800, 600, 320, 180)
        );
        assertEquals(
            new IntegerViewport(0, 0, 1280, 720, 4, false),
            IntegerViewport.calculate(1280, 720, 320, 180)
        );
    }

    @Test
    void reportsBarsAndNeverUsesFractionalScale() {
        IntegerViewport viewport = IntegerViewport.calculate(1000, 700, 320, 180);

        assertEquals(3, viewport.scale());
        assertEquals(960, viewport.width());
        assertEquals(540, viewport.height());
        assertEquals(20, viewport.leftBar());
        assertEquals(80, viewport.bottomBar());
        assertTrue(viewport.hasBars());
        assertFalse(viewport.degraded());
    }

    @Test
    void marksSmallBackbufferAsExplicitCroppedDegradedMode() {
        IntegerViewport viewport = IntegerViewport.calculate(160, 90, 320, 180);

        assertEquals(1, viewport.scale());
        assertEquals(-80, viewport.x());
        assertEquals(-45, viewport.y());
        assertTrue(viewport.degraded());
    }

    @Test
    void rejectsNonPositiveDimensions() {
        assertThrows(
            IllegalArgumentException.class,
            () -> IntegerViewport.calculate(0, 360, 320, 180)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> IntegerViewport.calculate(640, 360, -1, 180)
        );
    }
}
