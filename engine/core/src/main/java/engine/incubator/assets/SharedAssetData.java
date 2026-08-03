package engine.incubator.assets;

/**
 * Marker for transitively immutable data shared by asset handles.
 *
 * <p>Implementations expose values only and must not expose backend disposal or mutable render
 * state. A backend may keep a private implementation that also owns disposable resources while
 * consumers receive this read-only type.</p>
 */
public interface SharedAssetData {
}
