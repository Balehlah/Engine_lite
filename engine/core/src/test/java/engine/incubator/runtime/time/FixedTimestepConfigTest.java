package engine.incubator.runtime.time;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

final class FixedTimestepConfigTest {
    @Test
    void defaultPolicyIsConfiguredAtSixtyHertzDuringInitialization() {
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();

        assertAll(
            () -> assertEquals(60.0, configuration.updatesPerSecond()),
            () -> assertEquals(1.0 / 60.0, configuration.fixedDeltaSeconds()),
            () -> assertEquals(16_666_667L, configuration.fixedStepNanos()),
            () -> assertEquals(Duration.ofMillis(250L).toNanos(), configuration.maximumFrameTimeNanos()),
            () -> assertEquals(5, configuration.maximumCatchUpSteps())
        );
    }

    @Test
    void rejectsInvalidInitializationPolicy() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new FixedTimestepConfig(0.0, 1L, 1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new FixedTimestepConfig(60.0, 0L, 1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new FixedTimestepConfig(60.0, 1L, 0)
            )
        );
    }
}
