package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class SpriteSpecTest {
    @Test
    void parsesPaletteAndRectangularPixels() {
        SpriteSpec spec = SpriteSpec.parse(
            """
            # test sprite
            palette . 00000000
            palette X ff0000ff
            pixels
            .X
            X.
            """
        );

        assertEquals(2, spec.width());
        assertEquals(2, spec.height());
        assertEquals(0x00000000, spec.rgbaAt(0, 0));
        assertEquals(0xff0000ff, spec.rgbaAt(1, 0));
    }

    @Test
    void rejectsUndefinedPaletteKeysAndRaggedRows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SpriteSpec.parse(
                """
                palette . 00000000
                pixels
                .X
                """
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> SpriteSpec.parse(
                """
                palette . 00000000
                pixels
                ..
                .
                """
            )
        );
    }
}
