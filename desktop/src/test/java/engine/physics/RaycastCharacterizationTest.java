package engine.physics;

import engine.math.Vector2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaycastCharacterizationTest {

    @Test
    @Tag("characterization")
    void missAfterHitRetainsEveryFieldFromThePreviousHit() {
        Raycast ray = new Raycast(Vector2.ZERO, Vector2.RIGHT, 10);
        AABB hitBox = new AABB(2, -1, 1, 2);

        assertTrue(ray.testAABB(hitBox));
        Vector2 previousPoint = ray.point;
        Vector2 previousNormal = ray.normal;
        float previousDistance = ray.distance;

        assertFalse(ray.testAABB(new AABB(2, 2, 1, 1)));

        assertAll(
            () -> assertTrue(ray.hit),
            () -> assertSame(previousPoint, ray.point),
            () -> assertSame(previousNormal, ray.normal),
            () -> assertTrue(previousDistance == ray.distance),
            () -> assertSame(hitBox, ray.hitObject)
        );
    }

    @Test
    @Disabled("Known RAY-STATE defect; stateless hit results belong to issues #28 and #31")
    @Tag("specification")
    void missAfterHitMustClearThePreviousResult() {
        Raycast ray = new Raycast(Vector2.ZERO, Vector2.RIGHT, 10);
        assertTrue(ray.testAABB(new AABB(2, -1, 1, 2)));

        assertFalse(ray.testAABB(new AABB(2, 2, 1, 1)));

        assertAll(
            () -> assertFalse(ray.hit),
            () -> assertNull(ray.point),
            () -> assertNull(ray.normal),
            () -> assertTrue(ray.distance == 0),
            () -> assertNull(ray.hitObject)
        );
    }

    @Test
    @Tag("characterization")
    void zeroDirectionCircleQueryReportsAHitContainingNaN() {
        Raycast ray = new Raycast(Vector2.ZERO, Vector2.ZERO, 10);

        assertTrue(ray.testCircle(new Vector2(2, 0), 1));

        assertAll(
            () -> assertTrue(ray.hit),
            () -> assertTrue(Float.isNaN(ray.distance)),
            () -> assertTrue(Float.isNaN(ray.point.x)),
            () -> assertTrue(Float.isNaN(ray.point.y))
        );
    }

    @Test
    @Disabled("Known RAY-ZERO defect; zero direction must fail in issues #28 and #31")
    @Tag("specification")
    void zeroDirectionMustFailWithoutProducingNaN() {
        Raycast ray = new Raycast(Vector2.ZERO, Vector2.ZERO, 10);

        assertFalse(ray.testCircle(new Vector2(2, 0), 1));
        assertFalse(ray.hit);
        assertNull(ray.point);
    }
}
