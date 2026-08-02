package engine.incubator.runtime.lifecycle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Registers exactly one owner for every resource and releases owners idempotently.
 *
 * <p>The registry is intentionally single-threaded. Owners and resources are compared by
 * identity, avoiding accidental ownership aliases through {@link Object#equals(Object)}.</p>
 */
public final class OwnedResourceRegistry implements AutoCloseable {
    private final IdentityHashMap<Object, OwnerRegistration> owners = new IdentityHashMap<>();
    private final IdentityHashMap<Object, ResourceRegistration<?>> resources =
        new IdentityHashMap<>();
    private final List<OwnerRegistration> ownershipOrder = new ArrayList<>();
    private final List<WeakReference<Object>> disposedOwners = new ArrayList<>();

    private long ownersRegistered;
    private long disposalAttempts;
    private long resourcesRegistered;
    private long resourcesDisposed;
    private long disposalFailures;
    private long ownersDisposed;
    private boolean closed;

    public void registerOwner(Object owner, String name) {
        Objects.requireNonNull(owner, "owner");
        requireName(name, "owner name");
        requireOpen();
        if (owners.containsKey(owner) || wasDisposed(owner)) {
            throw new IllegalStateException("Owner is already registered: " + name);
        }
        OwnerRegistration registration = new OwnerRegistration(owner, name);
        owners.put(owner, registration);
        ownershipOrder.add(registration);
        ownersRegistered++;
    }

    public <T> T register(
        Object owner,
        String name,
        T resource,
        ResourceDisposer<? super T> disposer
    ) {
        requireName(name, "resource name");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(disposer, "disposer");
        requireOpen();

        OwnerRegistration ownerRegistration = requireActiveOwner(owner);
        if (!ownerRegistration.resourceNames.add(name)) {
            throw new IllegalStateException(
                "Owner already has a resource named '" + name + "': " + ownerRegistration.name
            );
        }
        if (resources.containsKey(resource)) {
            ownerRegistration.resourceNames.remove(name);
            throw new IllegalStateException("Resource already has an owner: " + name);
        }

        ResourceRegistration<T> registration = new ResourceRegistration<>(
            name,
            resource,
            disposer
        );
        ownerRegistration.resources.add(registration);
        resources.put(resource, registration);
        resourcesRegistered++;
        return resource;
    }

    public void disposeOwner(Object owner) {
        Objects.requireNonNull(owner, "owner");
        OwnerRegistration registration = owners.get(owner);
        if (registration == null) {
            if (wasDisposed(owner)) {
                return;
            }
            throw new IllegalArgumentException("Owner is not registered");
        }

        ownersDisposed++;
        Throwable failure = null;
        for (int index = registration.resources.size() - 1; index >= 0; index--) {
            ResourceRegistration<?> resource = registration.resources.get(index);
            try {
                disposeResource(resource);
            } catch (Throwable resourceFailure) {
                failure = LifecycleFailures.append(failure, resourceFailure);
            }
        }
        registration.resources.clear();
        registration.resourceNames.clear();
        owners.remove(owner);
        ownershipOrder.remove(registration);
        disposedOwners.add(new WeakReference<>(owner));
        if (failure != null) {
            LifecycleFailures.rethrow(
                failure,
                "One or more resources failed to dispose for owner " + registration.name
            );
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Throwable failure = null;
        for (int index = ownershipOrder.size() - 1; index >= 0; index--) {
            try {
                disposeOwner(ownershipOrder.get(index).owner);
            } catch (Throwable ownerFailure) {
                failure = LifecycleFailures.append(failure, ownerFailure);
            }
        }
        if (failure != null) {
            LifecycleFailures.rethrow(failure, "One or more resource owners failed to dispose");
        }
    }

    public ResourceMetrics metrics() {
        return new ResourceMetrics(
            ownersRegistered,
            ownersDisposed,
            resourcesRegistered,
            disposalAttempts,
            resourcesDisposed,
            disposalFailures
        );
    }

    OwnerRegistration requireActiveOwner(Object owner) {
        Objects.requireNonNull(owner, "owner");
        OwnerRegistration registration = owners.get(owner);
        if (registration == null) {
            if (wasDisposed(owner)) {
                throw new IllegalStateException("Owner is already disposed");
            }
            throw new IllegalArgumentException("Owner is not registered");
        }
        return registration;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Resource registry is closed");
        }
    }

    private boolean wasDisposed(Object owner) {
        boolean found = false;
        for (int index = disposedOwners.size() - 1; index >= 0; index--) {
            Object disposedOwner = disposedOwners.get(index).get();
            if (disposedOwner == null) {
                disposedOwners.remove(index);
            } else if (disposedOwner == owner) {
                found = true;
            }
        }
        return found;
    }

    private void disposeResource(ResourceRegistration<?> registration) throws Exception {
        if (registration.disposalAttempted) {
            return;
        }
        registration.disposalAttempted = true;
        disposalAttempts++;
        resources.remove(registration.resource);
        try {
            registration.dispose();
            resourcesDisposed++;
        } catch (Exception | Error failure) {
            disposalFailures++;
            throw failure;
        }
    }

    private static String requireName(String name, String role) {
        Objects.requireNonNull(name, role);
        if (name.isBlank()) {
            throw new IllegalArgumentException(role + " must not be blank");
        }
        return name;
    }

    static final class OwnerRegistration {
        private final Object owner;
        private final String name;
        private final Set<String> resourceNames = new HashSet<>();
        private final List<ResourceRegistration<?>> resources = new ArrayList<>();
        private OwnerRegistration(Object owner, String name) {
            this.owner = owner;
            this.name = name;
        }
    }

    private static final class ResourceRegistration<T> {
        private final String name;
        private final T resource;
        private final ResourceDisposer<? super T> disposer;
        private boolean disposalAttempted;

        private ResourceRegistration(
            String name,
            T resource,
            ResourceDisposer<? super T> disposer
        ) {
            this.name = name;
            this.resource = resource;
            this.disposer = disposer;
        }

        private void dispose() throws Exception {
            disposer.dispose(resource);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
