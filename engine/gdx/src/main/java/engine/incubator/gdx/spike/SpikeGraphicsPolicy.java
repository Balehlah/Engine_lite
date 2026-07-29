package engine.incubator.gdx.spike;

import com.badlogic.gdx.graphics.Texture;

/**
 * GL-independent declaration of the pixel-perfect rendering contract.
 */
final class SpikeGraphicsPolicy {
    static final int VIRTUAL_WIDTH = 320;
    static final int VIRTUAL_HEIGHT = 180;
    static final Texture.TextureFilter MIN_FILTER =
        Texture.TextureFilter.Nearest;
    static final Texture.TextureFilter MAG_FILTER =
        Texture.TextureFilter.Nearest;

    private SpikeGraphicsPolicy() {
    }
}
