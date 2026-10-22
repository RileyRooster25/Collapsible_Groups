# JEI smoke tests for Minecraft 1.20.1

Run Collapsible Groups and JEI in isolated Prism instances. The probe checks native mouse and keyboard input, group expansion and collapse, search and clearing, and manager entry with populated and empty results. It is excluded from product builds.

## Before you start

Use Windows, Node.js 22 or newer, JDK 25 for Gradle, Java 17 for Minecraft, and a signed-in Prism Launcher account. No npm installation is needed. Dependency versions come from the repository's gradle.properties and your configured artifact paths.

## 1. Configure local paths

Run the remaining commands from test-tools/jei-smoke. Copy local.example.json to local.json once; edit an existing local.json instead of replacing it. Set java to Java 17's javaw.exe, prism to your configured Prism launch executable, prismData to the matching data directory, the dependency JAR paths, and a unique instanceSuffix. Machine-specific launcher helpers belong in local.json.

## 2. Build the mod and probes

Set JAVA_HOME to JDK 25 in PowerShell, then run:

```powershell
../../gradlew.bat -p ../.. :common:check :forge:build :fabric:build
../../gradlew.bat -p . :forge:build :fabric:build
node --test *.test.mjs
```

The probes target Java 17. Use the exact JEI and Fabric API versions expected by the current checkout.

## 3. Prepare and run

```powershell
node prepare.mjs forge fabric
node run.mjs forge
node run.mjs fabric
```

Preparation refuses existing instance names and occupied or indeterminate ports. It creates owned instances on ports 25741 (Forge) and 25742 (Fabric), with separate superflat worlds and fixed screen settings. The launcher receives the configured Prism data directory. Stop if a command fails.

Assertions require three distinct rendered frames and client ticks. Keep the game focused and avoid mouse or keyboard input during a run. A failed check stays failed, and each run keeps separate evidence.

## Rebuild and rerun

After rebuilding the product or probe, use a fresh instanceSuffix and prepare replacement instances after the previous clients have exited. If launcher confirmation left the already prepared client running, node run.mjs <loader> --attach can attach to it; ownership, session and artifact verification still apply.

## Troubleshooting and scope

Inspect result.json, screenshots and logs under evidence/ first. Only a verified owned client receives normal shutdown. Preserve backups and inspect the process and port if shutdown cannot be verified; never stop a shared Prism process to recover this test.

The HTTP probe binds only to loopback, requires its random session and ownership marker, and exposes bounded test operations. It has no arbitrary evaluation endpoint. Keep the probe out of normal modpacks.

Keep local.json, profiles, evidence and build outputs local. Commit test source, example configuration and dated result summaries. [RESULTS.md](RESULTS.md) records historical runs, not certification of the current beta5 artifacts. This suite does not cover drag input, EMI, or performance measurements.
