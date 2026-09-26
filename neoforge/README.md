# FallingTree for NeoForge 1.21.1

Standalone build of the maintained fork for Java 21, Minecraft 1.21.1 and NeoForge 21.1.248. Install `build/libs/FallingTree-1.21.1-1.21.1.11.jar`. Name **FallingTree**, ID `fallingtree` and version `1.21.1.11` are unchanged.

This module reuses the original common tree detection, breaking, loot and tool-damage algorithms. The root/Fabric build and common sources are unchanged. Pixelmon 9.4.1 can be installed alongside it; FallingTree has no Pokemon or economy dependency.

## Standalone build

From this directory, use the included Gradle 8.12.1 wrapper:

```powershell
./gradlew.bat test packagedTest build --max-workers=2 --no-daemon
./gradlew.bat runPackagedGametest --max-workers=2 --no-daemon
```

`settings.gradle` selects `build-neoforge.gradle`, pinned to ModDevGradle 2.0.80. The older root-managed build file is retained. The NeoForge-specific `src/pixelmon` source directory replaces only the common JSON loader/updater in this standalone artifact. Despite the directory name, these classes do not depend on Pixelmon.

For the isolated Pixelmon check, put the operator-provided Pixelmon 9.4.1 release JAR in `build/packaged-gametest/mods/`. The test run stages the exact FallingTree release JAR there automatically. Configure the isolated server for `server-ip=127.0.0.1` and `server-port=29562`. No additional EULA was accepted for these GameTest runs.

## Configuration and protection fixes

`config/fallingtree.json` fills missing fixed fields recursively on startup and supported reloads. Operator values, explicit nulls, unknown keys, list membership/order and biome selections remain intact. Existing empty lists/maps remain empty. JSON does not support comments. There are no auxiliary editable language or GUI files in this mod.

The loader validates before writing, rejects malformed/duplicate JSON and invalid typed values, uses guarded temporary-file replacement, and keeps decoded operator settings when default persistence fails. Failed reloads retain the prior configuration. Explicit nulls remain in the file and use safe runtime fallbacks. Client configuration saves update changed known fields while retaining unrelated operator fields.

Two regression fixes were added:

- Float values such as `trees.trunkLootPercentage: 1e100` are rejected before persistence. Previously this passed double validation, overflowed the Java float and threw during baseline serialization. The regression first failed, then passed while preserving both file bytes and prior settings.
- Origin block handling runs at `LOWEST` event priority, after ordinary protection listeners. A `LOW` priority cancellation regression first showed extra logs changing despite the canceled origin; it now leaves the whole tree intact. Additional log protection is also tested. This does not establish compatibility with every protection plugin or with a listener that cancels later at the same priority.

## Verification — September 26, 2026

- 24 source tests and 24 tests against the packaged release passed, with no failures or skipped tests. They cover shared scan/size/durability behavior and configuration preservation, including the maintained `config/server/fallingtree.json` operator-file copy, invalid values, explicit nulls, custom lists/unknown fields, repeat loads and an actual Windows file-lock persistence failure.
- Exact packaged release plus Pixelmon 9.4.1 passed 3 native GameTests. The final rebuild passed again and stopped cleanly; its optional Cloth Config dependency ID was checked against Cloth Config 15.0.130 and corrected to `cloth_config`.
- Native checks cover instantaneous tree breaking, exactly three log drops, the existing fork's two durability points for that three-log test, crouch bypass, the player toggle, and origin/additional-log protection.
- `FALL_ITEM`, `FALL_BLOCK` and `FALL_ALL_BLOCK` created native falling log entities with the expected block states and item-drop flags. `FALL_ALL_BLOCK` also created a leaf entity. These exercise the packaged access transformer for `FallingBlockEntity.blockState`. `SHIFT_DOWN` removed the upper log while retaining the origin.
- The native configuration packet codec round-tripped all five break modes and its remaining fields without leftover bytes.
- Release inspection found one replacement `ConfigLoader`, the required access-transformer resource, and no Fabric metadata, Cobblemon, Impactor, TheMatrix or test classes. Test hooks exist only in a separate test mod. No additional libraries are bundled by this mod.
- Final command: `./gradlew.bat test packagedTest build runPackagedGametest --max-workers=2 --no-daemon --console=plain`.

SHA-256: `763504EDD26C8CB6981095ACB918D553C4FC55BAA6141AE901F863982C25C6EA`

The native runs use the development launcher with packaged production mods and an isolated GameTest world. They verify server behavior and entity/packet data. Connected-client animation, final falling-entity landing, optional Cloth Config screens, live network negotiation and real protection-plugin integrations remain unverified. Connected-client checks were explicitly deferred. Pixelmon's known missing drowned-biome tag warning appeared during its startup.
