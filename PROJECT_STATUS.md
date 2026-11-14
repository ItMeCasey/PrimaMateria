# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-13
Summary: Prima Materia (sieve, barrel and crucible skyblock mod for Forge and NeoForge 26.2 and 26.3) has its design and a scaffold that builds and passes its self test on all four targets with every block and item as a stand-in; no gameplay yet, nothing pushed.
Spend: none

## Design
- [x] Game design: loop, progression, blocks, items, recipe families, drop tables, originality note (docs/DESIGN.md) (w:3)
- [x] Sifting experience: animation, rendering, sound, performance budget, fallback (docs/SIFTING.md) (w:2)
- [x] Art direction: palette, shading rules, model and texture lists, icon concept (docs/ART.md) (w:2)
- [x] Architecture: modules, data formats, ModJS, BEI, tests, work split (docs/ARCHITECTURE.md) (w:2)
- [x] Local git repository with first commit (w:1)
- [ ] Casey answers the open design questions (all answered 2026-11-13); decisions: MIT license, Barrel name, vanilla textures for blocks and 16x16 items, Auto Hammer and Powered Drive names, mesh tiers cloth to netherite, and the Nether and End chain, auto hammer, powered drive, mob drops and all four recipe viewers in the first release (w:1)

## Foundation
- [x] Four Gradle modules (forge and neoforge for 26.2 and 26.3) with shared common/ and version-26.x folders (w:4)
- [x] Ids and ModContent with every block, item and sound registered as placeholders; placeholder models, textures, loot tables; lang file (w:3)
- [x] Frozen interface files (view records, StateProps, ModShapes, RecipeLookup, GameEvents) (w:2)
- [x] scripts/sync_versions.py for 26.3 (w:2)
- [x] Dev server ports 8734 to 8737 in the port registry (w:1)
- [x] Scaffold unit tests and headless server self test (every id registered, every block placed) on all four builds (w:2)

## Logic and Data
- [x] Config file (config/primamateria.properties) with every key and default (w:2)
- [~] Data readers and reload listeners: mesh tiers done (server side); heat sources and compost values not written; client sync not written (w:3)
- [x] Sieve: mesh slot, one block at a time, hold-to-shake, drops rolled when the block goes in, residue, hopper in, comparator, drops to the container below or hopping to the player; unit tests and server self test on four builds (pipes and other mods' item handlers not yet; real-client feel not yet checked) (w:5)
- [x] Linked sieves (sieve.link, sieve.link_radius): shake and refill together, proven in the server self test (w:3)
- [ ] Hammers and crushing recipes (5 tiers, bonus yield) (w:3)
- [ ] Barrel: dew and rain, composting, soaking, mixing, steeping, growing; wooden and clay versions (w:5)
- [ ] Brine fluid and bucket (w:2)
- [ ] Crucible: heat, melting into lava or molten metal fluids, fluid in and out (w:4)
- [ ] Molten copper, iron and gold as real fluids with buckets (w:3)
- [ ] Casting tray: pour, cool, ingot out (w:3)
- [ ] Windup drive and furnace drive with output buffers (w:4)
- [ ] Powered drive (FE) (w:3)
- [ ] Auto hammer (w:3)
- [ ] Nether and End chain: scoria, pale sand, soul sand sifting, netherite hammer and mesh (w:3)
- [~] Shipped recipes and drop tables: all sifting tables, residue, five meshes and the sieve and mesh crafting recipes written by scripts/make_sifting_data.py; hammer, barrel, crucible and the rest not yet (w:3)
- [ ] Void Island world preset and start island structure (w:3)

## Blocks, Models and Textures
- [ ] Sieve model and textures in Blockbench; style approved by Casey (w:4)
- [ ] Wooden barrel and clay barrel models (w:3)
- [ ] Crucible and casting tray models (w:3)
- [ ] Windup drive and furnace drive models (w:4)
- [ ] Auto hammer and powered drive models (w:4)
- [ ] Crystal model and five crystal textures (w:2)
- [x] Stand-in block models built from vanilla textures by reference (w:2)
- [~] Block textures, 16x16: meshes, stone meal, scoria and pale sand drawn (first pass, waiting for review); brine, molten metals and crystals not drawn (w:3)
- [~] Item textures (26), 16x16: first pass drawn, waiting for Casey's review next to vanilla (w:3)
- [x] Particle textures (w:1)
- [ ] Selection shapes and display transforms for every block (w:2)

## Animation Renderer
- [ ] Animation loader and sampler in common/ (keyframes, four easing modes, blending) with unit tests (w:4)
- [ ] scripts/export_anims.py: .bbmodel to static model plus animation JSON (w:3)
- [ ] Bone contract document (docs/BONES.md) (w:1)
- [ ] Sieve animations authored in Blockbench (8 clips) (w:3)
- [ ] Sieve renderer: bones, domed material layer, finds on the mesh, clip blending, shake sync (w:5)
- [ ] Fines and dust particles, pour stream (w:2)
- [ ] Sounds made and wired (sieve, mesh, drives) (w:3)
- [ ] Barrel, crucible, casting tray and drive renderers and animations (w:4)
- [ ] Distance levels, particle and sound caps, resting sieves at zero cost (w:3)
- [ ] Animation settings: full, reduced, off (block state fallback) (w:2)

## Integrations
- [x] Shared card layout and RecipeLookup wiring for all viewers (viewer/Cards, unit tested) (w:2)
- [~] BEI: six categories plus drops page; compiled on all four builds, frames on NeoForge 26.2 and 26.3 with demo recipes (w:3)
- [~] JEI plugin, same categories; compiled on both NeoForge builds, frames on 26.2 and 26.3 with demo recipes (w:3)
- [~] REI plugin, same categories; compiled on both NeoForge builds, frames on 26.2 and 26.3 with demo recipes (w:3)
- [x] EMI plugin written (src/pending/emi, no EMI build for 26.2 or 26.3: not compiled or tested) (w:2)
- [x] Drop table information pages per siftable block (drops category in BEI, JEI, REI; seen in JEI and BEI frames) (w:2)
- [~] SMWTI lines written and compiled on all four builds; need ViewProvider block entities from workstream A, no client check (w:2)
- [~] Jade lines written and compiled on both NeoForge builds; need ViewProvider block entities, no client check (w:2)
- [x] ModJS bridge: PrimaMateria global (events, state, content, data, config) with tests; self test with the ModJS jar passes on all four layers (w:3)
- [ ] KubeJS: every recipe, drop table, mesh tier and heat source changeable as datapack JSON, with a remove form, proven by a test pack (w:2)
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
- [ ] Unit tests of the pure logic in all four modules (w:3)
- [ ] scripts/check_data.py (recipes parse, ids exist, lang holds names only) (w:2)
- [ ] Headless server self test on all four builds (w:4)
- [ ] Real-client frames on one Forge and one NeoForge build (isolated launcher) (w:4)
- [ ] 200-sieve performance scene measured in full, reduced and off (w:2)
- [ ] Casey plays a fresh Void Island; balance pass on the data files (w:3)

## Branding and Docs
- [ ] Icon (svg and png sizes) (w:2)
- [ ] Banners and social image (w:2)
- [ ] CurseForge description (mod only: features, settings, compatibility, FAQ) (w:2)
- [ ] Wiki pages on itmecasey.com (w:3)
- [ ] README and CHANGELOG for 2026.11.1 (w:1)

## After 2026.11.1
- [ ] Real placeable fluids: brine and molten copper, iron and gold (fluid type, source, flowing fluid, block, bucket, client fluid model on both loaders and both versions) and fluid handlers on the barrels, the crucible and the casting tray, so other mods' tanks and pipes can move them (cut from the first release, w:5)
- [ ] Our own recorded or synthesized sounds (the first release uses the vanilla block sounds) (w:2)

## Release and CI
- [ ] build.yml, release.yml, changed-builds-only plan_matrix.py, issue templates (w:3)
- [ ] GitHub repository, push, CI run (Project Manager) (w:2)
- [ ] CurseForge project and project id, secrets (Project Manager) (w:2)
- [ ] Release 2026.11.1 on GitHub and CurseForge (Project Manager) (w:2)
- [ ] Add Prima Materia to the Command Center (Project Manager) (w:1)
