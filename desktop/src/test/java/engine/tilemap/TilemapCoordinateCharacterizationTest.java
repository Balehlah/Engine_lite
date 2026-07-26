package engine.tilemap;

import engine.math.Vector2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TilemapCoordinateCharacterizationTest {

    @Test
    @Tag("specification")
    void positiveWorldCoordinatesRespectTileBoundaries() {
        Tilemap tilemap = tilemap();

        assertEquals(new Vector2(0, 0), tilemap.worldToTile(15.999f, 15.999f));
        assertEquals(new Vector2(1, 1), tilemap.worldToTile(16, 16));
    }

    @Test
    @Tag("characterization")
    void negativeWorldCoordinateIsTruncatedIntoTileZero() {
        Tilemap tilemap = tilemap();

        assertEquals(new Vector2(0, 0), tilemap.worldToTile(-1, -1));
    }

    @Test
    @Disabled("Known TILE-NEG defect; negative world coordinates require mathematical floor")
    @Tag("specification")
    void negativeWorldCoordinateMustMapOutsideTheTilemap() {
        Tilemap tilemap = tilemap();

        assertEquals(new Vector2(-1, -1), tilemap.worldToTile(-1, -1));
    }

    private Tilemap tilemap() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Tileset tileset = new Tileset("test", image, 16, 16);
        return new Tilemap(2, 2, tileset);
    }
}
