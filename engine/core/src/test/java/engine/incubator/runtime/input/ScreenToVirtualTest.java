package engine.incubator.runtime.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class ScreenToVirtualTest {
    @Test
    void mapsLogicalCoordinatesThroughDpiToBottomLeftVirtualCoordinates() {
        ScreenToVirtual mapping = new ScreenToVirtual(
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

        assertEquals(
            new PointerPosition(
                40,
                60,
                80,
                479,
                0,
                179,
                PointerPosition.Region.VIEWPORT
            ),
            mapping.map(40, 60)
        );
        assertEquals(
            new PointerPosition(
                200,
                150,
                400,
                299,
                160,
                89,
                PointerPosition.Region.VIEWPORT
            ),
            mapping.map(200, 150)
        );
    }

    @Test
    void identifiesLetterboxAndPillarboxPixelsAsBars() {
        ScreenToVirtual mapping = new ScreenToVirtual(
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

        assertEquals(PointerPosition.Region.BARS, mapping.map(79, 300).region());
        assertEquals(PointerPosition.Region.BARS, mapping.map(400, 119).region());
        assertEquals(PointerPosition.Region.BARS, mapping.map(720, 300).region());
        assertEquals(PointerPosition.Region.BARS, mapping.map(400, 480).region());
        assertEquals(PointerPosition.Region.VIEWPORT, mapping.map(80, 120).region());
        assertEquals(PointerPosition.Region.OUTSIDE_SURFACE, mapping.map(800, 0).region());
    }

    @Test
    void rejectsInvalidSurfaceOrViewportDimensions() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ScreenToVirtual(0, 600, 800, 600, 0, 0, 800, 600, 320, 180)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ScreenToVirtual(800, 600, 800, 600, 0, 0, 0, 600, 320, 180)
        );
    }
}
