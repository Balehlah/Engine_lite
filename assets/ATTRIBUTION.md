# Asset attribution

Only assets with a known author, origin, license and attribution requirement may
be committed or distributed. Accepted-by-default licenses are original work,
CC0 and CC BY 4.0; any other license requires compatibility review under
ADR-004.

The spike assets are original, deliberately tiny textual fixtures. They are
stored as source text so their origin and the renderer/Tiled expectations are
reviewable without binary provenance.

## Inventory

Paths must be relative to the repository root and enclosed in backticks so
`verifyAssetAttribution` can validate the inventory.

| Repository path | Author/owner | Origin | License | Required attribution |
|---|---|---|---|---|
| `assets/spike/sprite.rgba` | Engine Lite contributors | Original textual RGBA pixel fixture created for Issue #14 | Apache-2.0 | Preserve the project `LICENSE` and this inventory |
| `assets/spike/probe.tmx` | Engine Lite contributors | Original textual Tiled fixture created for Issue #14 | Apache-2.0 | Preserve the project `LICENSE` and this inventory |
