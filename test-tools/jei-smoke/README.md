# Forge 1.21.1 JEI smoke test

This isolated probe covers the Forge loader, where the ClientDevBridge suite is unavailable. It verifies real MouseHandler/KeyboardHandler input for search, clearing search, group expansion/collapse, and the manager with both populated and empty results. Each assertion requires three distinct frames and ticks; evidence includes screenshots, exact artifact hashes, session identity, and normal shutdown.

Build the product first. With JDK 25, run `../../gradlew.bat -p . :forge:build`, then `node prepare.mjs forge` and `node run.mjs forge`. Configure the launcher, Java 21, and latest JEI paths in ignored `local.json`, following `local.example.json`. Preparation requires a fresh instance suffix and refuses an occupied port. The probe is excluded from product builds. Fabric and NeoForge continue to use `../clientdevbridge`.

Run `node --test *.test.mjs` to check port detection, launcher failure handling and shutdown ownership without starting Minecraft. The configured launcher receives both `prismData` and the instance name. Keep machine-specific launcher helper paths in ignored `local.json`.

The probe only starts with matching instance ownership and an explicit loopback port. RPC requires its random session token and exposes bounded test operations. It never loads a personal world. A failed test remains failed, and only its verified owned process receives normal shutdown.
