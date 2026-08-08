package engine.incubator.gdx.spike;

import com.badlogic.gdx.graphics.Texture;
import engine.incubator.runtime.config.EngineConfig;

/**
 * GL-independent declaration of the pixel-perfect rendering contract.
 */
final class SpikeGraphicsPolicy {
    static final int VIRTUAL_WIDTH = EngineConfig.DEFAULT_VIRTUAL_WIDTH;
    static final int VIRTUAL_HEIGHT = EngineConfig.DEFAULT_VIRTUAL_HEIGHT;
    static final Texture.TextureFilter MIN_FILTER =
        Texture.TextureFilter.Nearest;
    static final Texture.TextureFilter MAG_FILTER =
        Texture.TextureFilter.Nearest;

    private SpikeGraphicsPolicy() {
    }
}
