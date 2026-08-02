package engine.incubator.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class WorldStateIdentityTest {
    @Test
    void removingAnEqualEntityRemovesTheExactRegisteredInstance() {
        OwnedResourceRegistry ownership = new OwnedResourceRegistry();
        Object owner = new Object();
        ownership.registerOwner(owner, "scene");
        WorldState world = new WorldState(ownership);
        EqualEntity first = new EqualEntity();
        EqualEntity second = new EqualEntity();
        world.add(owner, first);
        world.add(owner, second);

        assertTrue(world.remove(second));

        assertAll(
            () -> assertEquals(1, world.entityCount()),
            () -> assertEquals(1, world.entityCount(owner)),
            () -> assertSame(first, world.entities().get(0))
        );
        world.releaseOwner(owner);
        ownership.close();
    }

    private static final class EqualEntity {
        @Override
        public boolean equals(Object ignored) {
            return true;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
