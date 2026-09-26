# NeoForge 1.21.1 validation

Retain the existing module, shared tree algorithms and maintained fork behavior. Preserve `fallingtree`, version `1.21.1.11` and release filename `FallingTree-1.21.1-1.21.1.11.jar`.

1. Add a standalone pinned ModDevGradle build inside the existing NeoForge module. Keep the original root/Fabric build intact. Compile shared platform-neutral source into the NeoForge artifact.
2. Run the existing tree/scan/durability regression tests and native server startup against NeoForge 21.1.248. Correct target API or client-side linkage failures revealed by those checks.
3. Add NeoForge-specific configuration loader replacements without changing the Fabric/common loader. Test recursive defaults, owner lists/maps/nulls/unknowns, invalid values, repeated loads, concurrent edits and real write failures. Preserve decoded settings when persistence fails and previous settings when reload input is invalid.
4. Exercise native tree breaking, sneak bypass, tool damage and cancellation/protection behavior on isolated worlds; check configuration packets and resource packaging. Pixelmon 9.4.1 co-installation must load successfully. Client configuration-screen rendering remains deferred.
5. Review, rebuild original filename, deliver, commit and push to the existing current user-owned branch.
