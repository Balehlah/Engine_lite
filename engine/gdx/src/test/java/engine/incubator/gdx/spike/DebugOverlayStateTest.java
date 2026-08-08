package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class DebugOverlayStateTest {
    @Test
    void toggleIsExplicitAndTheDisabledBenchmarkNeverEntersRenderPath() {
        DebugOverlayState state = new DebugOverlayState(false);
        assertFalse(state.isEnabled());
        assertTrue(state.toggle());
        assertFalse(state.toggle());

        DebugOverlayDisabledBenchmark.Result result =
            DebugOverlayDisabledBenchmark.run(100_000);
        assertEquals(100_000, result.iterations());
        assertEquals(0L, result.renders());
        assertTrue(result.elapsedNanos() >= 0L);
    }
}
