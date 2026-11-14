# Decisions and cuts made without Casey (overnight, 2026-11-13)

Casey was asleep and asked for Prima Materia to be completed and released as 2026.11.1. Every decision and every cut below was made by the lead session with the recommended default, and each can be reversed. The newest entries are at the bottom of each section.

## Decisions

| # | Decision | Why | How to change it |
|---|---|---|---|
| 1 | The Overworld of a Void Island world is void; the Nether and End are normal; one shared island at spawn with one oak tree; no starter chest | Recommended default, matches the design | Config `start.island`; the island is code and one world preset file |
| 2 | A sieve holds one block at a time, no storage; sieves within `sieve.link_radius` (default 1) work together | Casey's answer on 2026-11-13 | Config `sieve.link`, `sieve.link_radius` |
| 3 | Hand shake rate is one shake per 4 ticks | A held use button repeats at that rate in the game | `SiftMath.HAND_MIN_TICKS` |
| 4 | Pacing targets (iron about 45 minutes, a Nether portal about 2 hours, diamonds about 3 hours) are kept as designed | No play data yet | Edit the drop tables (data files) and the config |
| 5 | Sieve sounds are our own, played by the client renderer from the sieve state; the server plays none for the sieve (no doubled sounds). Drive sounds (crank, tick) have files but no trigger yet. | The animation branch ships them | `client/SieveFx` |
| 6 | The 16x16 textures and the stand-in or Blockbench models shipped in 2026.11.1 as a first pass. They are NOT approved: Casey reviews them (docs/frames/texture_review.png) and asks for redraws. | Casey's rule: DONE means played to his standard | Redraw any file; nothing in code depends on a texture |
| 7 | Adding a sifting material is only a recipe file: no id, texture or tier is written per material anywhere in code | Casey's requirement | Self test `pack_added_material_sifts_and_drops_with_no_code_change` proves it |
| 8 | Recipe results are kept as item ids (text), so a recipe for another mod's item loads without that mod and is skipped when the item is missing | Optional compat files | `SiftingRecipe` |
| 9 | Recipe types are real recipe types with codecs (not a private JSON folder) | So datapacks, KubeJS and ModJS work on them like any recipe | `PrimaRecipes` |

## Cuts

| # | Cut | Why | Follow-up |
|---|---|---|---|
| 1 | ~~Real, placeable molten metals and brine, and fluid handlers~~ **Delivered in 2026.11.2:** four real fluids (type, source, flowing, block, bucket, client model) on both loaders and versions; barrels, crucible and casting tray expose the loader fluid handler; dispensers place the buckets. | | See `docs/testing/fluids.md` |
| 2 | ~~Recipe viewers on a dedicated server~~ **Delivered in 2026.11.2:** the server sends the recipe lookup on join and after every reload (payload on each loader); proven with a dedicated server and a joining client. | | `docs/testing/recipe_sync_*.log` |
| 3 | ~~Nothing was played in a real game client~~ **Addressed in 2026.11.2** by a scripted progression playthrough in a real client (`docs/testing/PLAYTHROUGH_LOG.md`). Casey has still not played it himself. | | Casey plays a fresh Void Island |
| 4 | The Void Island start island is placed from code, not from a structure file | A saved flag and a code plan were quicker to test headlessly | `world/StartIslandPlacer` |
| 5 | A bucket of molten metal used on a casting tray casts as many ingots as 1000 mB makes (10) | A bucket cannot be partly used and nothing should be lost | `CastingTrayBlockEntity` |
| 6 | Mixing: the poured bucket's fluid counts as the second 1000 mB (obsidian is lava in the barrel with water poured in, stone is the reverse, both in the clay barrel) | A barrel holds 1000 mB | Recipes `primamateria:mixing` |
| 7 | Breaking a barrel drops waiting results only; the contents are lost | Fluids are internal ids | `BarrelBlockEntity` |
| 8 | Invented recipes: auto hammer `PHP` / `SIS` / `PPP` (planks, iron hammer, sticks, iron ingot); windup drive `PCP` / `CIC` / `PSP`; hammers `HH` / ` S` / ` S`; sieve and mesh shapes | The design gave only ingredient lists | The recipe files |
| 9 | Hammers break blocks through a block-break event on each loader (not right click); creative and spectator players are skipped | Works the same on both loaders | `HammerLogic` |
| 10 | The powered drive accepts energy on every side and never gives any back | Simplest safe behavior | `DriveBlockEntity` |
