package engine.incubator.world.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class SequentialIdGeneratorTest {
    @Test
    void equalSeedsProduceTheSameCollisionFreeSequence() {
        SequentialIdGenerator first = new SequentialIdGenerator(37L);
        SequentialIdGenerator second = new SequentialIdGenerator(37L);
        List<EntityId> firstRun = new ArrayList<>();
        List<EntityId> secondRun = new ArrayList<>();

        for (int index = 0; index < 10_000; index++) {
            firstRun.add(first.next());
            secondRun.add(second.next());
        }

        Set<EntityId> uniqueIds = new HashSet<>(firstRun);
        assertEquals(firstRun, secondRun);
        assertEquals(firstRun.size(), uniqueIds.size());
        assertEquals(new EntityId(37L), firstRun.get(0));
        assertEquals(new EntityId(10_036L), firstRun.get(firstRun.size() - 1));
    }

    @Test
    void maximumValueIsReturnedOnceAndOverflowNeverWraps() {
        SequentialIdGenerator generator = new SequentialIdGenerator(Long.MAX_VALUE);

        assertEquals(new EntityId(Long.MAX_VALUE), generator.next());
        assertThrows(EntityIdExhaustedException.class, generator::next);
        assertThrows(EntityIdExhaustedException.class, generator::next);
    }

    @Test
    void idsAndSeedsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new EntityId(0L));
        assertThrows(IllegalArgumentException.class, () -> new EntityId(-1L));
        assertThrows(IllegalArgumentException.class, () -> new SequentialIdGenerator(0L));
    }
}
