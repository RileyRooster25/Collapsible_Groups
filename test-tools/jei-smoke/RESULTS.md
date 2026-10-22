# JEI 15.74.0.225 compatibility — 2026-10-20

The local CG 2.0.0-beta4 build now targets JEI's `createInputHandler(IInternalKeyMappings)` method. Forge also adds intrinsic search-field bridges: the new JEI interface keeps the names `setValue` and `setFocused`, while Forge remaps the corresponding Minecraft overrides. The bridges call JEI's existing overrides, preserving its text colors, focus restoration, and search history. They leave an existing interface implementation intact.

At the time of this run, these were unreleased local builds; they had not been committed, pushed, or published.

| Loader | Official JEI | In-game checks | Result |
| --- | --- | ---: | --- |
| Fabric 0.19.5 | 15.74.0.225 | 8 | Passed |
| Forge 47.4.23 | 15.74.0.225 | 8 | Passed |

Both runs verify visible search and manager controls, group expansion/collapse, manager entry, empty search, manager entry from empty results, a positive search, and clearing search to restore the collapsed group. Inputs use Minecraft's keyboard/mouse handlers, and each assertion requires three distinct rendered frames and client ticks. Screenshots were inspected. Both clients exited normally; the test jars remain outside the product builds.

The initial Forge run reproduced JEI's `AbstractMethodError` before its overlay appeared. The final run uses the unchanged official JEI jar and the CG search-field fix. The earlier failure remains in a separate evidence folder.

## Validation

- All product builds and checks passed; 1,071 JUnit tests, zero failures, errors, or skips.
- The packaged-JAR contract test checks both loader injection targets and verifies that Forge's missing search interface methods are bridged to the actual remapped JEI overrides.
- Fabric evidence: `evidence/fabric-2026-10-20T01-54-40-044Z/result.json`.
- Forge evidence: `evidence/forge-2026-10-20T02-02-48-946Z/result.json`.
- Initial Forge failure: `evidence/forge-2026-10-20T01-56-01-047Z/result.json`.

Evidence and generated artifacts are local and Git-ignored.

## Product SHA-256

| Artifact | SHA-256 |
| --- | --- |
| `fabric/build/libs/collapsible_groups-fabric-1.20.1-2.0.0-beta4.jar` | `c2f3e6d3988cd6007e337d3511afa46020d44be6174fc9262848046601c8102f` |
| `forge/build/libs/collapsible_groups-forge-1.20.1-2.0.0-beta4.jar` | `c442cb18c3fb05f9608c6d59d2aff0a859964f82ac2128f0b2cd86d02719be5a` |

## Historical scope

These dated results apply only to the versions and artifact hashes recorded above. They do not certify beta5 or later builds. Run the current suite to produce evidence for a new artifact.
