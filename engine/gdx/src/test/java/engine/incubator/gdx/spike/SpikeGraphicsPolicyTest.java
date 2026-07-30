package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.graphics.Texture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class SpikeGraphicsPolicyTest {
    @Test
    void fixesTheVirtualFramebufferAndNearestSamplingWithoutCreatingGl() {
        assertAll(
            () -> assertEquals(320, SpikeGraphicsPolicy.VIRTUAL_WIDTH),
            () -> assertEquals(180, SpikeGraphicsPolicy.VIRTUAL_HEIGHT),
            () -> assertEquals(
                Texture.TextureFilter.Nearest,
                SpikeGraphicsPolicy.MIN_FILTER
            ),
            () -> assertEquals(
                Texture.TextureFilter.Nearest,
                SpikeGraphicsPolicy.MAG_FILTER
            )
        );
    }
}
