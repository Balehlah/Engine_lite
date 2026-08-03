package engine.incubator.runtime.lifecycle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("specification")
final class OwnedResourceRegistryTest {
    @Test
    void ownerDisposalIsReverseOrderedAndIdempotent() {
        OwnedResourceRegistry registry = new OwnedResourceRegistry();
        Object owner = new Object();
        List<String> calls = new ArrayList<>();
        registry.registerOwner(owner, "scene");
        registry.register(owner, "first", new Object(), ignored -> calls.add("first"));
        registry.register(owner, "second", new Object(), ignored -> calls.add("second"));

        registry.disposeOwner(owner);
        registry.disposeOwner(owner);
        registry.close();

        ResourceMetrics metrics = registry.metrics();
        assertAll(
            () -> assertEquals(List.of("second", "first"), calls),
            () -> assertEquals(1L, metrics.ownersRegistered()),
            () -> assertEquals(1L, metrics.ownersDisposed()),
            () -> assertEquals(2L, metrics.resourcesRegistered()),
            () -> assertEquals(2L, metrics.disposalAttempts()),
            () -> assertEquals(2L, metrics.resourcesDisposed()),
            () -> assertEquals(0L, metrics.disposalFailures()),
            () -> assertFalse(metrics.hasLeaks())
        );
    }

    @Test
    void oneResourceCannotBeRegisteredByTwoOwners() {
        OwnedResourceRegistry registry = new OwnedResourceRegistry();
        Object firstOwner = new Object();
        Object secondOwner = new Object();
        Object sharedResource = new Object();
        registry.registerOwner(firstOwner, "first");
        registry.registerOwner(secondOwner, "second");
        registry.register(firstOwner, "shared", sharedResource, ignored -> {
        });

        assertThrows(
            IllegalStateException.class,
            () -> registry.register(secondOwner, "alias", sharedResource, ignored -> {
            })
        );
        registry.close();
    }

    @Test
    void aFailingDisposerDoesNotSkipOtherResourcesOrOwners() {
        OwnedResourceRegistry registry = new OwnedResourceRegistry();
        Object firstOwner = new Object();
        Object secondOwner = new Object();
        List<String> calls = new ArrayList<>();
        registry.registerOwner(firstOwner, "first");
        registry.registerOwner(secondOwner, "second");
        registry.register(firstOwner, "survivor", new Object(), ignored -> calls.add("survivor"));
        registry.register(firstOwner, "failure", new Object(), ignored -> {
            calls.add("failure");
            throw new Exception("injected resource failure");
        });
        registry.register(secondOwner, "last-owner", new Object(), ignored -> {
            calls.add("last-owner");
        });

        assertThrows(LifecycleException.class, registry::close);

        ResourceMetrics metrics = registry.metrics();
        assertAll(
            () -> assertEquals(List.of("last-owner", "failure", "survivor"), calls),
            () -> assertEquals(2L, metrics.ownersDisposed()),
            () -> assertEquals(3L, metrics.disposalAttempts()),
            () -> assertEquals(2L, metrics.resourcesDisposed()),
            () -> assertEquals(1L, metrics.disposalFailures()),
            () -> assertEquals(1L, metrics.leakedResources()),
            () -> assertTrue(metrics.hasLeaks())
        );
    }
}
