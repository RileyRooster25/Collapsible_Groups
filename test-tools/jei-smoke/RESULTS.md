# JEI 19.69.0.456 compatibility — 2026-10-20

All three CG loaders now target JEI's `createInputHandler(IInternalKeyMappings)` method. These results describe local CG 2.0.0-beta4 builds. At the time of this run they had not been committed, pushed, or published.

| Loader | JEI artifact | In-game checks | Result |
| --- | --- | ---: | --- |
| Fabric 0.16.14 | Official 19.69.0.456 | 14 | Passed |
| NeoForge 21.1.249 | Official 19.69.0.456 | 14 | Passed |
| Forge 52.0.28 and 52.1.20 | Official 19.69.0.456 | 0 | Blocked during JEI discovery |
| Forge 52.1.20 | Diagnostic copy of 19.69.0.456 | 8 | Passed; not an official-artifact compatibility pass |

All successful runs cover search, group expansion/collapse, the manager, and opening the manager with empty search results. Fabric and NeoForge also cover mixed item/fluid groups and editor operations. Assertions require distinct frames and ticks, screenshots accompany the results, and successful clients exited normally. Fabric/NeoForge configuration files were restored to their original contents. The test probes are excluded from product builds.

## Forge upstream blocker

The official JEI jar fails before CG loads:

```text
Invalid module-info: Package org.apache.lucene.search.comparators not found in module
```

JEI relocates the bundled Lucene classes but includes an unrelocated root `module-info.class`. Updating Forge from 52.0.28 to 52.1.20 does not resolve the error.

For diagnosis, a separate JEI copy removes only `module-info.class`. All 3,974 remaining ZIP entries were verified byte-for-byte identical to the official download. That copy passed eight in-game operations with the unchanged CG Forge product jar. The diagnostic file was not published, and the test instance was restored to the official JEI file afterward. An upstream JEI packaging fix is still required before the official Forge combination can be declared supported.

| JEI artifact | SHA-256 |
| --- | --- |
| Official Forge | `151869348e8c26cdb46cbf22bbf23f33a643ae7498e205274e9155e4c4725a25` |
| Diagnostic Forge | `ee6103fa3f31e171fa6d6e8057e2169b0e40d72e324560141e3835d129dd1bb2` |

## Validation and evidence

- All five CG product jars across 1.20.1/1.21.1 build successfully.
- This 1.21.1 checkout: 1,152 JUnit tests and 13 bridge-runner tests passed, zero failures or skips.
- Packaged-JAR contract tests verify the required injection selector against each loader's actual JEI artifact.
- Fabric/NeoForge: `../clientdevbridge/evidence/suite-2026-10-20T01-58-24-718Z/`.
- Official Forge 52.1.20 failure: `evidence/forge-2026-10-20T01-59-05-594Z/`.
- Diagnostic Forge: `evidence/forge-2026-10-20T02-04-17-849Z/`.

Evidence and generated artifacts are local and Git-ignored. The diagnostic result embeds both JEI hashes and the exact change. No release testing for EMI, KubeJS, or unrelated Minecraft versions is claimed by this JEI-focused smoke run.

## Product SHA-256

| Artifact | SHA-256 |
| --- | --- |
| `fabric/build/libs/collapsible_groups-fabric-1.21.1-2.0.0-beta4.jar` | `66861d3feb3bdce34f5289a11061b97bca2bed33ab5da7737635a901fc4ae73b` |
| `forge/build/libs/collapsible_groups-forge-1.21.1-2.0.0-beta4.jar` | `3ea4080f07762620af30744827dbe43c91dbe303a6cf865cf60858a45a2bfe3e` |
| `neoforge/build/libs/collapsible_groups-neoforge-1.21.1-2.0.0-beta4.jar` | `6f21273e375b1b40b1a026c03f92d840b8ed6aaa717e27eafd6fd355bf767d88` |

## Historical scope

These dated results apply only to the versions and artifact hashes recorded above. They do not certify beta5 or later builds. Run the current suite to produce evidence for a new artifact.
