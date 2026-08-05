package engine.incubator.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import engine.incubator.world.id.EntityId;
import engine.incubator.world.id.SequentialIdGenerator;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class WorldStateIdentityTest {
    @Test
    void generatedIdsAreStableAndEntitiesRetainInsertionOrder() {
        OwnedResourceRegistry ownership = new OwnedResourceRegistry();
        Object owner = new Object();
        ownership.registerOwner(owner, "scene");
        WorldState world = new WorldState(ownership, new SequentialIdGenerator(100L));
        Object first = new Object();
        Object second = new Object();

        EntityId firstId = world.register(owner, first);
        EntityId secondId = world.register(owner, second);

        assertAll(
            () -> assertEquals(new EntityId(100L), firstId),
            () -> assertEquals(new EntityId(101L), secondId),
            () -> assertEquals(firstId, world.idOf(first).orElseThrow()),
            () -> assertSame(first, world.entity(firstId).orElseThrow()),
            () -> assertEquals(List.of(first, second), world.entities())
        );

        assertTrue(world.remove(firstId));
        assertFalse(world.idOf(first).isPresent());
        assertFalse(world.entity(firstId).isPresent());
        world.releaseOwner(owner);
        ownership.close();
    }

    @Test
    void aBrokenInjectedGeneratorCannotCreateAnIdCollision() {
        OwnedResourceRegistry ownership = new OwnedResourceRegistry();
        Object owner = new Object();
        ownership.registerOwner(owner, "scene");
        EntityId duplicate = new EntityId(7L);
        WorldState world = new WorldState(ownership, () -> duplicate);
        Object first = new Object();
        Object second = new Object();

        assertEquals(duplicate, world.register(owner, first));
        assertThrows(IllegalStateException.class, () -> world.register(owner, second));
        assertAll(
            () -> assertEquals(1, world.entityCount()),
            () -> assertSame(first, world.entity(duplicate).orElseThrow()),
            () -> assertFalse(world.idOf(second).isPresent())
        );
        world.releaseOwner(owner);
        ownership.close();
    }

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
