package engine.graphics;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnimationCharacterizationTest {

    @Test
    @Tag("characterization")
    void oneFramePingPongMovesToNegativeFrameAndBreaksSpriteAccess() {
        Animation animation = oneFramePingPong();

        animation.update(0.1f);

        assertEquals(-1, animation.getCurrentFrame());
        assertThrows(ArrayIndexOutOfBoundsException.class, animation::getCurrentSprite);
    }

    @Test
    @Disabled("Known ANIM-ONE defect; one-frame ping-pong is specified by issue #41")
    @Tag("specification")
    void oneFramePingPongMustAlwaysRemainOnFrameZero() {
        Animation animation = oneFramePingPong();

        animation.update(0.1f);

        assertEquals(0, animation.getCurrentFrame());
        assertDoesNotThrow(animation::getCurrentSprite);
    }

    private Animation oneFramePingPong() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Animation animation = new Animation(new Sprite[]{new Sprite(image)}, 0.1f);
        animation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        return animation;
    }
}
