# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-13
Summary: Prima Materia (sieve, vat and crucible skyblock mod for Forge and NeoForge 26.2 and 26.3) has its design written and a local git repository; no code, models or builds exist yet.
Spend: none

## Design
- [x] Game design: loop, progression, blocks, items, recipe families, drop tables, originality note (docs/DESIGN.md) (w:3)
- [x] Sifting experience: animation, rendering, sound, performance budget, fallback (docs/SIFTING.md) (w:2)
- [x] Art direction: palette, shading rules, model and texture lists, icon concept (docs/ART.md) (w:2)
- [x] Architecture: modules, data formats, ModJS, BEI, tests, work split (docs/ARCHITECTURE.md) (w:2)
- [x] Local git repository with first commit (w:1)
- [ ] Casey answers the open design questions (license, names, resolution, world type, tier count) (w:1)

## Foundation
- [ ] Four Gradle modules (forge and neoforge for 26.2 and 26.3) with shared common/ and version-26.x folders (w:4)
- [ ] ModContent with every block, item, recipe type, sound, particle and fluid id; placeholder models; lang file (w:3)
- [ ] Frozen interface files (view records, StateProps, Shapes, RecipeLookup, GameEvents) (w:2)
- [ ] scripts/sync_versions.py for 26.3 (w:2)
- [ ] Dev server ports added to the port registry (w:1)

## Logic and Data
- [ ] Config file (config/primamateria.properties) (w:2)
- [ ] Data readers and reload listeners: mesh tiers, heat sources, melts, compost values, synced to clients (w:3)
- [ ] Sieve: mesh slot, load of 1 to 4, hold-to-shake, drops rolled at load, residue, hopper in and out, comparator (w:5)
- [ ] Hammers and crushing recipes (5 tiers, bonus yield) (w:3)
- [ ] Vat: dew and rain, composting, soaking, mixing, steeping, growing; wooden and clay versions (w:5)
- [ ] Brine fluid and bucket (w:2)
- [ ] Crucible: heat, melting, melts as data, lava out (w:4)
- [ ] Casting tray: pour, cool, ingot out (w:3)
- [ ] Windup drive and furnace drive with output buffers (w:4)
- [ ] Shipped recipes and drop tables for the first release (w:3)
- [ ] Void Island world preset and start island structure (w:3)

## Blocks, Models and Textures
- [ ] Sieve model and textures in Blockbench; style approved by Casey (w:4)
- [ ] Shared material sheets (wood, wood end, stone, clay, brass, iron) (w:3)
- [ ] Wooden vat and clay vat models (w:3)
- [ ] Crucible and casting tray models (w:3)
- [ ] Windup drive and furnace drive models (w:4)
- [ ] Crystal model and four crystal textures (w:2)
- [ ] Block detail textures (meshes, compost, brine, melt, insides, dial, firebox, stone meal) (w:3)
- [ ] Item textures (23) (w:3)
- [ ] Particle textures (w:1)
- [ ] Selection shapes and display transforms for every block (w:2)

## Animation Renderer
- [ ] Animation loader and sampler in common/ (keyframes, four easing modes, blending) with unit tests (w:4)
- [ ] scripts/export_anims.py: .bbmodel to static model plus animation JSON (w:3)
- [ ] Bone contract document (docs/BONES.md) (w:1)
- [ ] Sieve animations authored in Blockbench (8 clips) (w:3)
- [ ] Sieve renderer: bones, domed material layer, finds on the mesh, clip blending, shake sync (w:5)
- [ ] Fines and dust particles, pour stream (w:2)
- [ ] Sounds made and wired (sieve, mesh, drives) (w:3)
- [ ] Vat, crucible, casting tray and drive renderers and animations (w:4)
- [ ] Distance levels, particle and sound caps, resting sieves at zero cost (w:3)
- [ ] Animation settings: full, reduced, off (block state fallback) (w:2)

## Integrations
- [ ] BEI categories: sifting, crushing, vat, crucible (w:3)
- [ ] ModJS bridge: PrimaMateria global (events, state, content, config) with tests (w:3)
- [ ] docs/scripting.md (w:1)

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

## Release and CI
- [ ] build.yml, release.yml, changed-builds-only plan_matrix.py, issue templates (w:3)
- [ ] GitHub repository, push, CI run (Project Manager) (w:2)
- [ ] CurseForge project and project id, secrets (Project Manager) (w:2)
- [ ] Release 2026.11.1 on GitHub and CurseForge (Project Manager) (w:2)
- [ ] Add Prima Materia to the Command Center (Project Manager) (w:1)
