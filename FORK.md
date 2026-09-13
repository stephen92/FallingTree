# Fabric 1.21.1 server fork

Based on RakambdaOrg/FallingTree tag `1.21.1.11` (commit `15173b7e2d54cff5218061c59ac2ab0bdec5224e`).
The mod ID, name, version and release filename remain unchanged. Original authorship and LGPL-3.0 license are retained.

## Changes

- Pending scan positions have a hash index instead of repeatedly searching the priority queue. The original priority queue traversal order is retained.
- The before-break hook only checks player/tool eligibility. Its previous tree scan discarded the result; the after-break hook still scans the current world and applies the original cutting rules.
- Empty adjacency restrictions skip the six neighboring block reads. Configured restrictions retain their checks.
- Scan-limit console messages are limited to one per 30 seconds per tree builder, across positions. Scans still abort at the original limit and players still receive the existing failure notification.
- Fabric is the default build target; Forge and NeoForge sources remain available but are not built by default.

No scan-result cache is used, so changes to connected trees cannot leave stale tree data. There is no claim that connected trees are automatically separated: trees inside the configured search area can still join and hit the safety limit.

## Server configuration

`config/server/fallingtree.json` is based on the supplied production config. Its only changed setting is `trees.searchAreaRadius`, from `-1` (unlimited) to `8` (eight blocks in each horizontal direction from the cut). The scan limit remains 500, maximum break size remains 100, and oversized trees still abort. Large branches outside the radius may remain standing. Dense connections within the radius may still exceed either limit.

This is a starting setting to test in the affected biomes. Normal logs obey this radius; upstream special handling for mangrove roots, nether wart blocks and leaf edges is retained, so it is not a universal bound for every scanned block type. The supplied config uses `INSTANTANEOUS`; it does not enable a falling animation.

## Build and install

Requires Java 21. Run `./gradlew :common:test :fabric:build buildJar` (Windows: `gradlew.bat`).
The combined installable artifact is `build/libs/FallingTree-1.21.1-1.21.1.11.jar`; intermediate common/fabric JARs are not separate installable mods.

Stop the server, replace the existing FallingTree JAR with that file, and optionally replace `config/fallingtree.json` with the supplied tuned config. Keep only one FallingTree JAR installed. Start the server and test ordinary trees, interconnected trees, tool durability, sneak bypass and any claim protections. Capture another Spark profile during comparable chopping activity to measure actual improvement.

## Validation

Automated tests cover traversal order, indexed lookup work, deduplication, log throttling, original scan-limit aborts, a bounded connected tree, dense connected logs, empty adjacency rules and pre-break eligibility. These are synthetic tests, not a measurement of production tick savings.
