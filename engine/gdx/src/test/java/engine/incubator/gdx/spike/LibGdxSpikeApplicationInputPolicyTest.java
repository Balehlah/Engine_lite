package engine.incubator.gdx.spike;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.incubator.runtime.input.InputSnapshot;
import engine.incubator.runtime.input.PointerPosition;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class LibGdxSpikeApplicationInputPolicyTest {
    @Test
    void onlyMovedPointersInsideTheVirtualViewportCanAffectVirtualState() {
        assertTrue(
            LibGdxSpikeApplication.canPointerAffectVirtualState(
                snapshot(PointerPosition.Region.VIEWPORT, true)
            )
        );
        assertFalse(
            LibGdxSpikeApplication.canPointerAffectVirtualState(
                snapshot(PointerPosition.Region.BARS, true)
            )
        );
        assertFalse(
            LibGdxSpikeApplication.canPointerAffectVirtualState(
                snapshot(PointerPosition.Region.OUTSIDE_SURFACE, true)
            )
        );
        assertFalse(
            LibGdxSpikeApplication.canPointerAffectVirtualState(
                snapshot(PointerPosition.Region.VIEWPORT, false)
            )
        );
    }

    private static InputSnapshot snapshot(
        PointerPosition.Region region,
        boolean moved
    ) {
        PointerPosition position = new PointerPosition(0, 0, 0, 0, 0, 0, region);
        return new InputSnapshot(
            0L,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            new InputSnapshot.Pointer(position, moved, 0, 0, 0, 0),
            0.0,
            0.0,
            true,
            0
        );
    }
}
