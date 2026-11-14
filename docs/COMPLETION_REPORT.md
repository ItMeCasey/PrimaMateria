# Completion report, 2026.11.2 (code complete, not released)

Version 2026.11.2 on all four modules. Nothing pushed, tagged or released.

## Done (evidence)
- **Real fluids** (brine, molten copper, iron, gold) on both loaders and versions; barrels, crucible and casting tray expose the fluid handler; dispensers place buckets. Self test pipe checks pass on all four builds; real-client frames on NeoForge 26.2 and Forge 26.3: `docs/testing/fluids.md`, `docs/frames/fluid_*`.
- **Recipe sync to dedicated-server clients**: proven with a real dedicated server and joining client on NeoForge 26.2 for BEI, JEI and REI (`docs/testing/recipe_sync_*.log`, `docs/frames/sync_*`). Forge 26.3 BEI proof: rerun from main, result not read when this report was written (see `forge-26.3/runs`, `proof_f263b`); until read, Forge is covered by unit and self tests only.
- **Sounds and particle**: all 13 sounds and the pour particle are triggered (`scripts/check_sound_triggers.py`).
- **Crystal textures**: five drawn, amethyst placeholder removed, visible under the fluid (`docs/frames/crystal_review.png`).
- **Start island** is now a structure file a datapack can replace (`scripts/make_start_island.py`); code plan stays as fallback.
- **Missing recipe found and added**: 4 rock chips to 1 cobblestone.
- **Texture review sheet** `docs/frames/texture_review.png` refreshed; textures are NOT approved.
- Self tests on all four builds pass (NeoForge 26.2 443, Forge 26.2 446, NeoForge 26.3 444, Forge 26.3 447); `check_data.py` passes. Docs, wiki, CurseForge description and changelog updated.

## Not done, by Casey's instruction
- The scripted real-client progression playthrough and bug log were cancelled; Casey plays it himself. A tour was started and discarded.

## Open for Casey to decide
- Tray bucket casts 10 ingots; mixing uses the poured bucket as the second 1000 mB; breaking a barrel loses its contents; hammer works through the break event (not right click); powered drive accepts energy on all sides; invented recipe shapes (auto hammer, windup drive, hammers, sieve, meshes).
- Other mods' materials from `ore-matrix/PRIMA_COMPAT_LISTS.md` are not added as compat entries.
- 200-sieve performance scene not measured in full; SMWTI, Jade and ModJS lines not checked in a real client.
- BEI shows empty slot boxes for unused outputs; JEI, REI on Forge, EMI, KubeJS and Jade on Forge have no builds for 26.2 and 26.3.
