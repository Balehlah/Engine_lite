package engine.incubator.runtime.lifecycle;

/**
 * Backend-neutral disposal operation for a resource owned by one runtime owner.
 *
 * @param <T> resource type
 */
@FunctionalInterface
public interface ResourceDisposer<T> {
    void dispose(T resource) throws Exception;
}
