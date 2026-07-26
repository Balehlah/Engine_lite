package engine.math;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("specification")
class Vector2SpecificationTest {

    private static final float EPSILON = 0.00001f;

    @Test
    void arithmeticReturnsNewValuesWithoutMutatingTheOperands() {
        Vector2 left = new Vector2(3, 4);
        Vector2 right = new Vector2(-1, 2);

        assertAll(
            () -> assertEquals(new Vector2(2, 6), left.add(right)),
            () -> assertEquals(new Vector2(4, 2), left.sub(right)),
            () -> assertEquals(new Vector2(6, 8), left.mul(2)),
            () -> assertEquals(new Vector2(3, 4), left),
            () -> assertEquals(new Vector2(-1, 2), right)
        );
    }

    @Test
    void normalizationPreservesDirectionAndProducesUnitLength() {
        Vector2 normalized = new Vector2(3, 4).normalize();

        assertAll(
            () -> assertEquals(0.6f, normalized.x, EPSILON),
            () -> assertEquals(0.8f, normalized.y, EPSILON),
            () -> assertEquals(1.0f, normalized.length(), EPSILON),
            () -> assertEquals(Vector2.ZERO, Vector2.ZERO.normalize())
        );
    }

    @Test
    void divisionByZeroIsRejected() {
        Vector2 vector = new Vector2(2, 4);

        assertThrows(ArithmeticException.class, () -> vector.div(0));
    }
}
