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
| 1 | **Real, placeable molten metals and brine, and fluid handlers for pipes and tanks of other mods.** In 2026.11.1 these fluids are internal (an id and an amount) held by the barrels, the crucible and the casting tray and moved by bucket. Casey asked for real fluids ("storable as real fluids"); this is not delivered in the first release. | Each fluid needs a fluid type, a source and a flowing fluid, a block, a bucket and client fluid models on both loaders and both versions, plus fluid handler capabilities on every machine. That is a large change that could not be tested safely overnight. | First step of the next release (`PROJECT_STATUS.md`, `docs/known-gaps.md`) |
| 2 | **Recipe viewers on a dedicated server.** The recipe lookup that BEI, JEI, REI, SMWTI, Jade and scripts read is built on the server after every reload. In single player and on a LAN host the client shares it; players joining a dedicated server get no lookup because the client sync (a network payload on each loader) is not written. | Needs a payload registered on both loaders and both versions, untestable without a real client and server pair tonight | Step in `PROJECT_STATUS.md` (After 2026.11.1) |
| 3 | **Nothing was played by hand in a real game client.** The server self tests, the unit tests and the animation and integration sessions' client tours are the evidence. | Time and the rule of one game at a time | Casey plays a fresh Void Island |
| 4 | The Void Island start island is placed from code, not from a structure file | A saved flag and a code plan were quicker to test headlessly | `world/StartIslandPlacer` |
| 5 | A bucket of molten metal used on a casting tray casts as many ingots as 1000 mB makes (10) | A bucket cannot be partly used and nothing should be lost | `CastingTrayBlockEntity` |
| 6 | Mixing: the poured bucket's fluid counts as the second 1000 mB (obsidian is lava in the barrel with water poured in, stone is the reverse, both in the clay barrel) | A barrel holds 1000 mB | Recipes `primamateria:mixing` |
| 7 | Breaking a barrel drops waiting results only; the contents are lost | Fluids are internal ids | `BarrelBlockEntity` |
| 8 | Invented recipes: auto hammer `PHP` / `SIS` / `PPP` (planks, iron hammer, sticks, iron ingot); windup drive `PCP` / `CIC` / `PSP`; hammers `HH` / ` S` / ` S`; sieve and mesh shapes | The design gave only ingredient lists | The recipe files |
| 9 | Hammers break blocks through a block-break event on each loader (not right click); creative and spectator players are skipped | Works the same on both loaders | `HammerLogic` |
| 10 | The powered drive accepts energy on every side and never gives any back | Simplest safe behavior | `DriveBlockEntity` |
