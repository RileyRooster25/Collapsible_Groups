# ClientDevBridge tests for Minecraft 1.21.1

Run Collapsible Groups in isolated Prism instances and check its UI, saved groups, and behavior after restarting. ClientDevBridge supplies communication, world operations and screenshots; the companion probe supplies native input and observations after rendering. Neither is included in release JARs.

Start with the two NeoForge core profiles, neo-jei and neo-emi. [RESULTS.md](RESULTS.md) contains historical evidence; setup instructions do not imply a current beta5 game-test pass.

## Before you start

You need Windows, Node.js 22 or newer, JDK 25 for Gradle, Java 21 for Minecraft, and Prism Launcher with a signed-in account. No npm packages need to be installed.

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Collapsible Groups | The version in the root gradle.properties |
| NeoForge | 21.1.249 |
| ClientDevBridge | 1.0.0, build 171 |
| JEI / EMI | 19.69.0.456 / 1.1.24+1.21.1 |

Third-party JAR names and SHA-256 hashes are pinned in [dependencies.json](dependencies.json). Probe builds read the product version from gradle.properties and generate an exact dependency automatically.

## 1. Configure your local paths

Open PowerShell at the repository root:

```powershell
Set-Location test-tools/clientdevbridge
$env:JAVA_HOME = 'C:/Path/To/jdk-25'
```

Run all remaining commands from this directory and stop if a command fails. Copy [local.example.json](local.example.json) to local.json once; edit an existing local.json instead of replacing it.

| Field | What to enter |
| --- | --- |
| java | Java 21's bin/javaw.exe. |
| prism | Your configured Prism launch executable. Keep machine-specific helper paths in local.json. |
| prismData | The matching Prism data directory containing instances. |
| packTemplate | The full path to this checkout's pack-template.json. |
| instanceSuffix | A unique name suffix for a new set of instances. |
| dependencyFiles | Paths to the pinned third-party JARs. |

## 2. Build the mod and probes

```powershell
../../gradlew.bat -p ../.. :common:check :neoforge:build :fabric:build
node runner/stage.mjs neo-jei neo-emi
../../gradlew.bat -p companion build
```

Supply the pinned Bridge, JEI and EMI files through dependencyFiles, or place them in companion/libs. stage checks their hashes and copies the current product JAR for probe compilation. This checkout has no fetch.mjs; supply the files before staging.

The retained Fabric profiles use companion-fabric and its separate build command. They are outside the two-profile core run below.

## 3. Create the test instances

```powershell
node runner/prepare.mjs neo-jei neo-emi
```

| Profile | Viewer | Port |
| --- | --- | --- |
| neo-jei | JEI | 25831 |
| neo-emi | EMI | 25832 |

prepare creates owned CG Bridge Beta instances with your suffix and saves manifests under profiles/. It refuses existing names or occupied ports and does not copy personal worlds or settings. Use it once per instance.

## 4. Run the core tests

```powershell
node --test runner/*.test.mjs
node runner/run.mjs neo-jei neo-emi
node runner/audit-evidence.mjs neo-jei neo-emi
```

Profiles run sequentially and include a cold restart. Keep the active game focused and avoid mouse or keyboard input. The instances use 1280 x 800, GUI scale 2, a 60 FPS limit, and en_us for image comparisons.

Core cases cover editing, mixed members, native input, search, rules, ownership, batch operations, categories, invalid-rule previews and persistence. Image comparisons cover reviewed text and control regions, excluding animated icons, the cursor and world background. New baselines require manual review.

Each run saves reports, screenshots, logs and configuration backups under evidence/suite-<timestamp>/. audit uses each profile's latest non-negative-control run; an older pass cannot hide a newer failure. It verifies deployed hashes, normal exit and byte-for-byte configuration restoration.

### Focused JEI compatibility smoke check

```powershell
node runner/run.mjs neo-jei --jei-smoke
```

This mode runs 14 native click, manager, search and clear checks, saves screenshots, skips drag operations and cold restart, and restores fixtures after normal shutdown. It does not replace the full suite. Forge has a separate [JEI smoke probe](../jei-smoke/README.md).

## Rebuild and rerun

After product or probe code changes, close the test clients, repeat step 2, then update the stopped instances:

```powershell
node runner/deploy.mjs neo-jei neo-emi
node runner/run.mjs neo-jei neo-emi
node runner/audit-evidence.mjs neo-jei neo-emi
```

deploy replaces the recorded product and probe artifacts and hashes. For a product version, loader or dependency change, finish pending recovery and prepare replacement instances with a fresh instanceSuffix. Preserve prior evidence; deploy does not migrate versioned filenames or dependency pins.

## If something fails

Read result.json and logs in the failed profile's evidence folder first. A failed check or failed restoration returns a nonzero exit status.

If a client remains running after the runner stops, node runner/quit.mjs <profile> verifies its identity and requests normal shutdown. Do not stop shared Prism or unrelated Java processes. If restoration failed, run node runner/recover.mjs "evidence/<suite>/<profile>" only after verified shutdown. Keep backups if the process or port cannot be verified.

For Gradle's Unable to establish loopback connection error, the local JAVA_TOOL_OPTIONS workaround can select TCP fallback with -Djdk.net.unixdomain.tmpdir pointing to an intentionally nonexistent directory. Do not create that directory or commit machine-specific paths.

## Files and scope

Keep local.json, generated profiles, dependency JARs, evidence and build outputs local. Commit test source, example configuration, dependency pins and reviewed baseline images.

The two core profiles cover local single-player JEI and EMI testing on NeoForge. Retained Fabric, KubeJS, generic-provider, locale and coexistence profiles require their own current-version validation. These tests do not establish multiplayer, other-resolution or performance coverage. Historical results are not a beta5 certification.

Communication and world operations use [ClientDevBridge](https://github.com/CyclopsMC/ClientDevBridge); its source revision and artifact identities are recorded in dependencies.json.
