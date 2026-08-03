/**
 * Experimental typed asset contracts.
 *
 * <p>Manifests own lifecycle groups, handles expose transitively immutable
 * {@link engine.incubator.assets.SharedAssetData}, and only the service may unload or dispose
 * backend resources. The package is backend-neutral and intentionally outside the stable
 * {@code engine.api.*} surface.</p>
 */
package engine.incubator.assets;
