package engine.physics;

import engine.math.Vector2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AabbCollisionBaselineTest {

    @Test
    @Tag("specification")
    void touchingEdgesDoNotCountAsIntersection() {
        AABB left = new AABB(0, 0, 1, 1);
        AABB right = new AABB(1, 0, 1, 1);

        assertFalse(left.intersects(right));
    }

    @Test
    @Tag("characterization")
    void nonIntersectingMtvContainsNegativeZeroAndIsNotVectorZero() {
        AABB left = new AABB(0, 0, 1, 1);
        AABB right = new AABB(1, 0, 1, 1);

        Vector2 mtv = left.getMTV(right);

        assertEquals(0.0f, mtv.x);
        assertEquals(
            Float.floatToRawIntBits(-0.0f),
            Float.floatToRawIntBits(mtv.y)
        );
        assertFalse(Vector2.ZERO.equals(mtv));
    }

    @Test
    @Disabled("Known AABB-ZERO defect; no-overlap results are normalized by issue #28")
    @Tag("specification")
    void nonIntersectingMtvMustEqualVectorZero() {
        AABB left = new AABB(0, 0, 1, 1);
        AABB right = new AABB(1, 0, 1, 1);

        assertEquals(Vector2.ZERO, left.getMTV(right));
    }

    @Test
    @Tag("specification")
    void overlapReturnsTheMinimumTranslationVector() {
        AABB left = new AABB(0, 0, 2, 2);
        AABB right = new AABB(1.5f, 0, 2, 2);

        assertTrue(Collision.rectVsRect(left, right));
        assertEquals(new Vector2(-0.5f, 0), Collision.getSeparation(left, right));
    }

    @Test
    @Tag("characterization")
    void constructorAcceptsNaNAndNegativeDimensions() {
        AABB invalid = assertDoesNotThrow(() -> new AABB(Float.NaN, 0, -1, 1));

        assertTrue(Float.isNaN(invalid.x));
        assertEquals(-1, invalid.width);
    }

    @Test
    @Disabled("Known AABB-FINITE defect; finite immutable shapes belong to issue #28")
    @Tag("specification")
    void constructorMustRejectNaNAndNegativeDimensions() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AABB(Float.NaN, 0, -1, 1)
        );
    }

    @Test
    @Tag("characterization")
    void zeroVelocitySweepOfOverlappingBoxesReturnsNaN() {
        float collisionTime = Collision.sweepAABB(
            new AABB(0, 0, 1, 1),
            Vector2.ZERO,
            new AABB(0, 0, 1, 1)
        );

        assertTrue(Float.isNaN(collisionTime));
    }

    @Test
    @Disabled("Known SWEEP-ZERO defect; move zero becomes overlap in issues #28 and #31")
    @Tag("specification")
    void zeroVelocitySweepMustReturnAFiniteResult() {
        float collisionTime = Collision.sweepAABB(
            new AABB(0, 0, 1, 1),
            Vector2.ZERO,
            new AABB(0, 0, 1, 1)
        );

        assertTrue(Float.isFinite(collisionTime));
    }
}
