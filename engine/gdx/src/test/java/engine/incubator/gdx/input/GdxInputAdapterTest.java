package engine.incubator.gdx.input;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.incubator.gdx.spike.FixedTimestepLoop;
import engine.incubator.runtime.input.InputSnapshot;
import engine.incubator.runtime.input.PointerPosition;
import engine.incubator.runtime.input.ScreenToVirtual;
import engine.incubator.runtime.input.TickInput;
import engine.incubator.runtime.time.FakeNanoClock;
import engine.incubator.runtime.time.FixedTimestepConfig;
import engine.incubator.runtime.time.FixedTimestepScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class GdxInputAdapterTest {
    private static final ScreenToVirtual FULL = new ScreenToVirtual(
        640,
        360,
        640,
        360,
        0,
        0,
        640,
        360,
        320,
        180
    );

    @Test
    void callbacksOnlyBecomeLogicalStateWhenTheTickConsumesThem() {
        GdxInputAdapter adapter = new GdxInputAdapter(new TickInput(), () -> FULL);

        adapter.keyDown(12);
        adapter.keyUp(12);
        adapter.mouseMoved(100, 80);
        adapter.mouseMoved(120, 90);
        adapter.touchDown(120, 90, 0, 1);
        adapter.touchUp(120, 90, 0, 1);
        adapter.scrolled(1.0f, -2.0f);
        InputSnapshot snapshot = adapter.nextSnapshot();
        InputSnapshot next = adapter.nextSnapshot();

        assertAll(
            () -> assertTrue(snapshot.isKeyPressed(12)),
            () -> assertTrue(snapshot.isKeyReleased(12)),
            () -> assertTrue(snapshot.isMouseButtonPressed(1)),
            () -> assertTrue(snapshot.isMouseButtonReleased(1)),
            () -> assertFalse(snapshot.isMouseButtonDown(1)),
            () -> assertEquals(120, snapshot.pointer().position().screenX()),
            () -> assertEquals(1.0, snapshot.scrollX()),
            () -> assertEquals(-2.0, snapshot.scrollY()),
            () -> assertFalse(next.isKeyPressed(12)),
            () -> assertFalse(next.isKeyReleased(12)),
            () -> assertEquals(0.0, next.scrollY()),
            () -> assertEquals(2L, adapter.queueMetrics().coalescedMovementCount()),
            () -> assertEquals(0L, adapter.queueMetrics().overflowCount())
        );
    }

    @Test
    void mappingIsReadAtEachTickForResizeAndDpiChanges() {
        AtomicReference<ScreenToVirtual> mapping = new AtomicReference<>(FULL);
        GdxInputAdapter adapter = new GdxInputAdapter(
            new TickInput(),
            mapping::get
        );
        adapter.mouseMoved(39, 60);
        InputSnapshot beforeResize = adapter.nextSnapshot();

        mapping.set(
            new ScreenToVirtual(
                400,
                300,
                800,
                600,
                80,
                120,
                640,
                360,
                320,
                180
            )
        );
        InputSnapshot afterResize = adapter.nextSnapshot();

        assertAll(
            () -> assertEquals(19, beforeResize.pointer().position().virtualX()),
            () -> assertEquals(
                PointerPosition.Region.BARS,
                afterResize.pointer().position().region()
            ),
            () -> assertFalse(afterResize.pointer().movedThisTick())
        );
    }

    @Test
    void focusLostIsQueuedAndReleasesControlsAtTheNextLogicalTick() {
        GdxInputAdapter adapter = new GdxInputAdapter(new TickInput(), () -> FULL);
        adapter.keyDown(3);
        adapter.nextSnapshot();

        adapter.focusLost();
        InputSnapshot lost = adapter.nextSnapshot();
        adapter.focusGained();
        InputSnapshot gained = adapter.nextSnapshot();

        assertAll(
            () -> assertFalse(lost.focused()),
            () -> assertTrue(lost.isKeyReleased(3)),
            () -> assertFalse(lost.isKeyDown(3)),
            () -> assertTrue(gained.focused()),
            () -> assertFalse(gained.isKeyReleased(3))
        );
    }

    @Test
    void catchUpConsumesOneImmutableSnapshotPerLogicalTick() {
        FixedTimestepConfig configuration = FixedTimestepConfig.default60Hz();
        FakeNanoClock clock = new FakeNanoClock();
        FixedTimestepLoop loop = new FixedTimestepLoop(
            new FixedTimestepScheduler(clock, configuration)
        );
        GdxInputAdapter adapter = new GdxInputAdapter(new TickInput(), () -> FULL);
        List<InputSnapshot> snapshots = new ArrayList<>();

        adapter.keyDown(5);
        adapter.keyUp(5);
        clock.advanceNanos(Math.multiplyExact(configuration.fixedStepNanos(), 2L));
        loop.runFrame(delta -> snapshots.add(adapter.nextSnapshot()), (alpha, metrics) -> {
        });

        assertEquals(2, snapshots.size());
        assertAll(
            () -> assertTrue(snapshots.get(0).isKeyPressed(5)),
            () -> assertTrue(snapshots.get(0).isKeyReleased(5)),
            () -> assertFalse(snapshots.get(1).isKeyPressed(5)),
            () -> assertFalse(snapshots.get(1).isKeyReleased(5)),
            () -> assertEquals(0L, snapshots.get(0).tickIndex()),
            () -> assertEquals(1L, snapshots.get(1).tickIndex())
        );
    }
}
