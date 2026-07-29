package engine.incubator.gdx.spike;

import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Records explicit ownership and disposes resources once in reverse order.
 */
final class DisposableRegistry {
    private final List<Entry> ownershipOrder = new ArrayList<>();
    private final IdentityHashMap<Disposable, Entry> entriesByIdentity =
        new IdentityHashMap<>();
    private boolean disposalStarted;

    <T extends Disposable> T own(String name, T resource) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(resource, "resource");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Disposable name must not be blank.");
        }
        if (disposalStarted) {
            throw new IllegalStateException("Cannot register after disposal has started.");
        }
        if (ownershipOrder.stream().anyMatch(entry -> entry.name.equals(name))) {
            throw new IllegalArgumentException("Duplicate disposable name: " + name);
        }
        if (entriesByIdentity.containsKey(resource)) {
            throw new IllegalArgumentException(
                "Disposable identity already owned as "
                    + entriesByIdentity.get(resource).name
            );
        }

        var entry = new Entry(name, resource);
        ownershipOrder.add(entry);
        entriesByIdentity.put(resource, entry);
        return resource;
    }

    void disposeAll(Consumer<String> logger) {
        Objects.requireNonNull(logger, "logger");
        if (disposalStarted) {
            throw new IllegalStateException("disposeAll may be called only once.");
        }
        disposalStarted = true;

        RuntimeException aggregate = null;
        var reversed = new ArrayList<>(ownershipOrder);
        Collections.reverse(reversed);
        for (Entry entry : reversed) {
            try {
                entry.resource.dispose();
                entry.disposeCount++;
                logger.accept(entry.name + "=1");
            } catch (RuntimeException exception) {
                logger.accept(entry.name + "=FAILED:" + exception.getClass().getName());
                if (aggregate == null) {
                    aggregate = new IllegalStateException(
                        "One or more spike resources failed to dispose."
                    );
                }
                aggregate.addSuppressed(exception);
            }
        }

        if (aggregate != null) {
            throw aggregate;
        }
        assertDisposedExactlyOnce();
    }

    void assertDisposedExactlyOnce() {
        var invalid = ownershipOrder.stream()
            .filter(entry -> entry.disposeCount != 1)
            .map(entry -> entry.name + "=" + entry.disposeCount)
            .toList();
        if (!invalid.isEmpty()) {
            throw new IllegalStateException(
                "Every owned Disposable must be released exactly once: " + invalid
            );
        }
    }

    Map<String, Integer> disposalCounts() {
        var counts = new LinkedHashMap<String, Integer>();
        ownershipOrder.forEach(entry -> counts.put(entry.name, entry.disposeCount));
        return Collections.unmodifiableMap(counts);
    }

    int size() {
        return ownershipOrder.size();
    }

    private static final class Entry {
        private final String name;
        private final Disposable resource;
        private int disposeCount;

        private Entry(String name, Disposable resource) {
            this.name = name;
            this.resource = resource;
        }
    }
}
