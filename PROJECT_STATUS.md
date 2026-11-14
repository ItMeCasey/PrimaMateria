# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-14
Summary: Prima Materia 2026.11.2 is code complete and not released: real placeable fluids (brine and a molten fluid for every metal, vanilla and compat), ore compat for 95 materials of other mods by item tag, recipe sync to dedicated-server clients, all sounds and the pour particle triggered, drawn crystal textures, structure-file start island; Casey plays it next.
Spend: none

## Design
- [x] Game design: loop, progression, blocks, items, recipe families, drop tables, originality note (docs/DESIGN.md) (w:3)
- [x] Sifting experience: animation, rendering, sound, performance budget, fallback (docs/SIFTING.md) (w:2)
- [x] Art direction: palette, shading rules, model and texture lists, icon concept (docs/ART.md) (w:2)
- [x] Architecture: modules, data formats, ModJS, BEI, tests, work split (docs/ARCHITECTURE.md) (w:2)
- [x] Local git repository with first commit (w:1)
- [x] Open design questions decided with the recommended defaults while Casey slept (docs/DESIGN.md section 16, docs/DECISIONS_OVERNIGHT.md); Casey can reverse any of them (w:1)

## Foundation
- [x] Four Gradle modules (forge and neoforge for 26.2 and 26.3) with shared common/ and version-26.x folders (w:4)
- [x] Ids and ModContent with every block, item and sound registered as placeholders; placeholder models, textures, loot tables; lang file (w:3)
- [x] Frozen interface files (view records, StateProps, ModShapes, RecipeLookup, GameEvents) (w:2)
- [x] scripts/sync_versions.py for 26.3 (w:2)
- [x] Dev server ports 8734 to 8737 in the port registry (w:1)
- [x] Scaffold unit tests and headless server self test (every id registered, every block placed) on all four builds (w:2)

## Logic and Data
- [x] Config file (config/primamateria.properties) with every key and default (w:2)
- [x] Data readers and reload listeners: mesh tiers, heat sources, compost values (server side), plus the recipe lookup built after every reload; client sync of the lookup not written (see known gaps) (w:3)
- [x] Sieve: mesh slot, one block at a time, hold-to-shake, drops rolled when the block goes in, residue, hopper in, comparator, drops to the container below or hopping to the player; unit tests and server self test on four builds (pipes and other mods' item handlers not yet; real-client feel not yet checked) (w:5)
- [x] Linked sieves (sieve.link, sieve.link_radius): shake and refill together, proven in the server self test (w:3)
- [x] Hammers (6 tiers) and crushing recipes with bonus yield, break hook on both loaders; unit tests and server self test on four builds (w:3)
- [x] Barrels (wooden and clay): dew and rain, composting, soaking, mixing, steeping, growing, buckets, hoppers; unit tests and server self test on four builds (w:5)
- [x] Brine as an internal fluid (made by steeping, moved by the brine bucket); the real placeable fluid is in the next release list (w:2)
- [x] Crucible: heat sources as data, melting into lava or molten metal, buckets, hoppers; server self test on four builds (w:4)
- [x] Molten copper, iron and gold as internal fluids with real bucket items; the real placeable fluids are in the next release list (w:3)
- [x] Casting tray: pour from the crucible, cool, ingot out by hand or hopper; server self test on four builds (w:3)
- [x] Windup drive and furnace drive with output buffers; server self test on four builds (w:4)
- [x] Powered drive (FE capability on NeoForge and on Forge, checked in the loader dev hooks) (w:3)
- [x] Auto hammer: fits a hammer, queue of 16, six strokes per block, works on a drive (w:3)
- [x] Nether and End chain: scoria and pale sand from hammering, soul sand from soaking scoria in brine, mob drops, netherite scrap, netherite hammer and mesh (data and recipes; no real-game play-through) (w:3)
- [x] Shipped recipes and drop tables for every family (sifting, residue, crushing, composting, soaking, mixing, steeping, growing, melting, casting, meshes, heat, compost), written by the scripts/make_*_data.py scripts (w:3)
- [x] Void Island world preset and start island (placed from code, not from a structure file; once per world, config start.island); server self test and a void-world pass (w:3)

## Blocks, Models and Textures
- [x] Sieve model in Blockbench (35 elements, 8 clips) built and exported; treated as approved (docs/DECISIONS_OVERNIGHT.md) (w:4)
- [x] Wooden barrel and clay barrel models (w:3)
- [x] Crucible and casting tray models (w:3)
- [x] Windup drive and furnace drive models (w:4)
- [x] Auto hammer and powered drive models (static; stamp and cam animations not authored) (w:4)
- [~] Crystal model done; the five crystal textures are not drawn (placeholder: the vanilla amethyst cluster texture) (w:2)
- [x] Stand-in block models built from vanilla textures by reference (w:2)
- [~] Block textures, 16x16: meshes, stone meal, scoria and pale sand drawn (first pass, waiting for review); brine, molten metals and crystals not drawn (w:3)
- [~] Item textures (26), 16x16: first pass drawn, waiting for Casey's review next to vanilla (w:3)
- [x] Particle textures (w:1)
- [x] Selection shapes for every model block (boxes that follow the models) (w:2)

## Animation Renderer
- [x] Animation loader and sampler in common/ (keyframes, four easing modes, blending) with unit tests (w:4)
- [x] scripts/export_anims.py: .bbmodel to static model plus animation JSON (w:3)
- [x] Bone contract document (docs/BONES.md) (w:1)
- [x] Sieve animations authored in Blockbench (8 clips, generated by scripts/make_models.py, open in Blockbench) (w:3)
- [x] Sieve renderer: bones, domed layer, finds, blending, shake sync; real-client frames on neoforge-26.2 and forge-26.2 (docs/testing/sieve_animation.md) (w:5)
- [~] Fines and dust particles done; pour stream not used yet (w:2)
- [~] Sounds synthesized (9 events x 3) and wired for the sieve; drive sounds have no trigger yet (w:3)
- [x] Renderers for the barrels, crucible, casting tray, three drives and the auto hammer (fluid and compost level and color, melt and pour, cooling, drive animations, hammer stamps); client frames on NeoForge 26.2, Forge 26.2 and NeoForge 26.3 (w:4)
- [x] Distance levels, particle and sound caps, resting sieves at zero cost (w:3)
- [x] Animation settings: full, reduced, off (block state fallback) (w:2)

## Integrations
- [x] Shared card layout and RecipeLookup wiring for all viewers (viewer/Cards, unit tested) (w:2)
- [x] BEI: six categories plus the drops page; compiled on all four builds, frames on NeoForge 26.2 and 26.3 (w:3)
- [x] JEI plugin, same categories; compiled on both NeoForge builds, frames on 26.2 and 26.3 (w:3)
- [x] REI plugin, same categories; compiled on both NeoForge builds, frames on 26.2 and 26.3 (w:3)
- [x] EMI plugin written (src/pending/emi, no EMI build for 26.2 or 26.3: not compiled or tested) (w:2)
- [x] Drop table information pages per siftable block (drops category in BEI, JEI, REI; seen in JEI and BEI frames) (w:2)
- [~] SMWTI lines written and compiled on all four builds; every block entity now offers its view; no client check (w:2)
- [~] Jade lines written and compiled on both NeoForge builds; every block entity now offers its view; no client check (w:2)
- [x] ModJS bridge: PrimaMateria global (events, state, content, data, config) with tests; self test with the ModJS jar passes on all four layers (w:3)
- [x] KubeJS: every recipe, drop table, mesh tier and heat source is datapack JSON that can be added, replaced or removed; proven by the pack-added sifting material test (w:2)
- [~] KubeJS helper bridge for events and reads, unit tested with Rhino; no KubeJS build for 26.2 or 26.3, no real run (w:2)
- [x] Run with no optional mod installed passes the self test (all four layers) (w:1)
- [x] docs/scripting.md (ModJS, KubeJS, datapacks) and the wiki pages (w:1)
- [x] docs/known-gaps.md with the viewer and scripting builds per version (w:1)
- [~] Real-client check per supported integration: BEI, JEI, REI frames on NeoForge 26.2 and 26.3 done; SMWTI, Jade, ModJS in a client not done (w:4)
- [ ] Follow-up when a build appears (recheck with curseforge-lookup before every release): JEI, REI and Jade on Forge 26.2 and 26.3 (w:1)
- [ ] Follow-up when a build appears: EMI client check on any 26.2 or 26.3 build (w:1)
- [ ] Follow-up when a build appears: KubeJS bridge written against the KubeJS API and client-checked on any 26.2 or 26.3 build (w:1)

- [ ] Compat data for other mods' metals and gems from ore-matrix/PRIMA_COMPAT_LISTS.md (tag-based sifting, crushing, melting and casting entries for 26.2 and 26.3) (w:3)

## Testing
- [x] Unit tests of the pure logic in all four modules (141 tests per module, all pass) (w:3)
- [ ] scripts/check_data.py (recipes parse, ids exist, lang holds names only) (w:2)
- [x] Headless server self test on all four builds (327 to 379 checks each, one module at a time) (w:4)
- [~] Real-client frames: the animation and integration sessions took frames on NeoForge 26.2 and Forge 26.2 (docs/testing, docs/frames); the machines were not played by hand (w:4)
- [ ] 200-sieve performance scene measured in full, reduced and off (w:2)
- [x] Ore compat: sifting, crushing, melting and casting for the materials of the 26.2 and 26.3 lists by item tag, tag results, optional tag entries, inert without the other mods (docs/ORE_COMPAT.md, scripts/make_compat.py) (w:4)
- [x] A molten fluid, bucket and look for every metal, vanilla and compat (42 metals) (w:3)
- [x] Machines fill and empty held items through the loader's item fluid handler (no bucket table); furnace drive lit model shares the unlit geometry; BEI shows fluids as buckets with amounts (w:3)
- [ ] Casey plays a fresh Void Island; balance pass on the data files (w:3)

## Branding and Docs
- [x] Icon (svg and png sizes) (w:2)
- [x] Banners and social image (w:2)
- [x] CurseForge description (mod only: features, settings, compatibility, FAQ) (w:2)
- [x] Wiki pages (sieve, hammers, barrels, crucible, drives, start, progression, config, drop tables, recipes, scripting, compatibility, known gaps), deployed (w:3)
- [x] README and CHANGELOG for 2026.11.1 (w:1)

## Release 2026.11.2
- [x] Real placeable fluids (brine, molten copper, iron, gold) with fluid handlers on the barrels, crucible and casting tray, both loaders and versions (w:5)
- [x] Recipe lookup sync to dedicated-server clients; real-client proof on NeoForge 26.2 (BEI, JEI, REI) and Forge 26.3 (BEI) (w:3)
- [x] Triggers for all sounds and the pour particle (w:2)
- [x] Five drawn crystal textures, amethyst placeholder removed (w:2)
- [x] Start island as a replaceable structure file; missing rock chip to cobblestone recipe added (w:2)
- [ ] Casey plays a fresh Void Island and reviews the 16x16 textures next to vanilla (w:4)
- [ ] Client checks of SMWTI and Jade lines and of ModJS in a real client (w:3)
- [ ] EMI plugin and KubeJS bridge checked when a build exists for 26.2 or 26.3 (w:1)

## Release and CI
- [x] build.yml, release.yml, changed-builds-only plan_matrix.py, issue templates (written by the Project Manager) (w:3)
- [x] GitHub repository (github.com/ItMeCasey/PrimaMateria), push, CI run green on a17e7cb (w:2)
- [x] CurseForge project 1803927 and secrets (w:2)
- [x] Release 2026.11.1 on GitHub (tag v2026.11.1) and CurseForge (two combined jars, 26.2 and 26.3) (w:2)
- [ ] Add Prima Materia to the Command Center (Project Manager) (w:1)
