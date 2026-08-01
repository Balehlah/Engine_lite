package engine.incubator.runtime.time;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

final class FakeNanoClockTest {
    @Test
    void advancesOnlyWhenExplicitlyRequested() {
        FakeNanoClock clock = new FakeNanoClock(10L);

        assertAll(
            () -> assertEquals(10L, clock.nanoTime()),
            () -> assertEquals(15L, clock.advanceNanos(5L)),
            () -> assertEquals(25L, clock.advance(Duration.ofNanos(10L))),
            () -> assertEquals(25L, clock.nanoTime())
        );
    }

    @Test
    void rejectsRewindAndOverflow() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new FakeNanoClock(-1L)),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new FakeNanoClock().advanceNanos(-1L)
            ),
            () -> assertThrows(
                ArithmeticException.class,
                () -> new FakeNanoClock(Long.MAX_VALUE).advanceNanos(1L)
            )
        );
    }
}
