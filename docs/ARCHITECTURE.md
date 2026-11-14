# Prima Materia: architecture

Forge and NeoForge on Minecraft 26.2 and 26.3 only. Four jars' worth of code, combined into one jar per Minecraft version the way Barely Crafting does it. Java 25. Mod id `primamateria`, package `com.itmecasey.primamateria`, version `2026.11.1`.

The layout is a copy of `C:\Users\casey\Desktop\BarelyCrafting` (four modules, shared `common/`, `version-26.2` as the source of truth, `version-26.3` generated) with ideas from `C:\Users\casey\Desktop\BarelyStorage` (an `art/` style model pipeline). Read those two before scaffolding.

## 1. Folders

```
PrimaMateria/
  common/src/main/java/com/itmecasey/primamateria/
      PrimaMateria.java            mod id, init
      Ids.java                     every id as a string (frozen)
      view/                        SieveView, BarrelView, CrucibleView, TrayView, DriveView, AutoHammerView (frozen records)
      event/                       GameEvents (frozen)
      Settings.java                config/primamateria.properties (pure Java)
      sift/                        SiftMath, SiftLoad, Finds, MeshTier (pure)
      barrel/                         BarrelState, CompostMath, GrowSlots (pure)
      crucible/                    CrucibleMath, Melt, Heat (pure)
      drive/                       DriveMath (pure)
      data/                        RecipeLookup (frozen), JSON readers for mesh, heat, melt, compost files (pure, Gson)
      anim/                        AnimModel, AnimClip, Keyframe, Easing, AnimSampler, AnimJson (pure)
      script/                      ModJsBridge, PrimaMateriaApi, ScriptEvents, ScriptHooks, info records
  common/src/main/resources/
      assets/primamateria/         blockstates, models, items, textures, lang/en_us.json, sounds.json, sounds/, primamateria_anim/
      data/primamateria/           recipe/, primamateria/{mesh,heat,melt,compost}/, structure/, worldgen/world_preset/, loot_table/, tags
      primamateria_logo.png
  common/src/test/java/...         JUnit tests of everything pure, plus the ModJS stub classes
  version-26.2/src/main/java/com/itmecasey/primamateria/
      ModContent.java              every registry entry (blocks, items, sounds; A and C add block entities, recipe types, fluid, particles)
      StateProps.java, ModShapes.java   block state properties and voxel shapes (frozen names)
      block/                       SieveBlock, BarrelBlock, CrucibleBlock, CastingTrayBlock, DriveBlock, AutoHammerBlock, falling blocks
      blockentity/                 SieveBlockEntity, BarrelBlockEntity, CrucibleBlockEntity, CastingTrayBlockEntity, DriveBlockEntity
      item/                        HammerItem, MeshItem
      recipe/                      one class per recipe type, serializers, RecipeLookup
      datapack/                    reload listeners for mesh, heat, melt, compost; sync to clients
      net/                         SieveShakePacket, state sync payloads
      world/                       void preset wiring, start island placement
      client/                      render/ (SieveRenderer, BarrelRenderer, ...), particle/, ClientSetup, block color handlers
      compat/                      Cards, PrimaBeiPlugin, PrimaJeiPlugin, PrimaEmiPlugin, PrimaReiPlugin
  version-26.2/src/dev/java/.../dev/   SelfTest, ShotTour
  version-26.3/                    generated from version-26.2 by scripts/sync_versions.py, plus the 26.3 differences
  forge-26.2/  neoforge-26.2/  forge-26.3/  neoforge-26.3/
      build.gradle, settings.gradle, gradle.properties, gradlew, libs/ (BEI api jar)
      src/main/java/.../<loader>/  loader entry point, event wiring, capabilities, client entry
      src/dev/java/.../<loader>/dev/  dev entry (self test, tour)
      src/main/templates/META-INF/ mods.toml or neoforge.mods.toml
  branding/                        icon/, banners/, models/*.bbmodel, source/, LISTING.md
  curseforge/DESCRIPTION.md
  docs/                            DESIGN.md, SIFTING.md, ART.md, ARCHITECTURE.md, scripting.md, testing/, frames/
  scripts/                         make_placeholders.py, sync_versions.py, server_check.py (exist); export_anims.py, check_data.py, make_branding.py, client_tour.py, combine_jars.py (to write)
  .github/                         workflows/build.yml, release.yml, scripts/plan_matrix.py, ISSUE_TEMPLATE/
  licenses/                        fonts, our own sound sources
  CHANGELOG.md  LICENSE  PROJECT_STATUS.md  README.md
```

Rules that keep it in order:

- **Frozen interfaces change on paper first.** A change to `Ids`, `ModContent`, a view record, `StateProps`, `GameEvents`, `RecipeLookup` or a data format is written in this document first and gets one line in `CHANGELOG.md` under "Interfaces", so the other workstreams can follow.

- **`common/` has no game classes.** It compiles and tests without Minecraft. Everything a number depends on lives there.
- **`version-26.2` is edited by hand; `version-26.3` is never edited by hand.** `scripts/sync_versions.py` copies and patches it. Each known 26.3 difference is one function in that script.
- **Each loader folder is its own Gradle build** that includes `common/` and its `version-*` folder as source sets. Loader code is only: entry point, event bus wiring, item and fluid handler capabilities, network registration, client entry.
- **Every Gradle command goes through the slot wrapper:** `bash /c/Users/casey/.claude/tools/gradle-slot.sh ./gradlew <tasks> --no-daemon`.
- **Dev test server ports** (in the port registry): 8734 NeoForge 26.2, 8735 Forge 26.2, 8736 NeoForge 26.3, 8737 Forge 26.3.

## 2. Data formats

All data loads from datapacks and reloads with `/reload`. Recipes are real recipe types, so the recipe manager, datapacks, KubeJS and ModJS handle them without help from us. The four small non-recipe folders are read by our own reload listeners and synced to clients (BEI and the renderer need them).

### Recipes (`data/<namespace>/recipe/<path>.json`)

```json
{ "type": "primamateria:crushing",
  "input": "#minecraft:leaves",
  "hammer_tier": 1,
  "results": [
    { "item": "primamateria:plant_fiber" },
    { "item": "primamateria:plant_fiber", "chance": 0.35 }
  ] }
```

```json
{ "type": "primamateria:sifting",
  "input": "minecraft:gravel",
  "result": "primamateria:iron_dust",
  "count": [1, 2],
  "chance": 0.16,
  "mesh_min": 3 }
```
`count` may be a number. `mesh_max` is optional. `input` is an item ingredient (item, tag or list).

```json
{ "type": "primamateria:sifting_residue",
  "input": "minecraft:gravel", "result": "minecraft:sand", "chance": 0.15 }
```

```json
{ "type": "primamateria:composting", "result": "minecraft:dirt", "time": 400 }
```
One composting recipe is active (the highest-priority datapack's). `time` is in ticks.

```json
{ "type": "primamateria:soaking",
  "fluid": "minecraft:water", "amount": 250,
  "input": "primamateria:stone_meal", "result": "minecraft:clay", "time": 100 }
```

```json
{ "type": "primamateria:mixing",
  "base_fluid": "minecraft:lava", "added_fluid": "minecraft:water", "amount": 1000,
  "result": "minecraft:obsidian", "needs_heatproof": true }
```

```json
{ "type": "primamateria:steeping",
  "fluid": "minecraft:water", "input": "primamateria:salt_dust", "count": 4,
  "result_fluid": "primamateria:brine", "time": 600 }
```

```json
{ "type": "primamateria:growing",
  "fluid": "primamateria:brine", "amount": 250,
  "seed": "primamateria:diamond_seed", "result": "minecraft:diamond", "count": 1,
  "time": 6000, "crystal": "primamateria:block/crystal_diamond" }
```

```json
{ "type": "primamateria:melting",
  "input": "primamateria:iron_dust",
  "result": { "melt": "primamateria:molten_iron" }, "amount": 20 }
```
`result` is `{ "melt": id }` or `{ "fluid": id }`.

```json
{ "type": "primamateria:casting",
  "melt": "primamateria:molten_iron", "amount": 100,
  "result": "minecraft:iron_ingot", "time": 60 }
```

### Mesh tiers (`data/<namespace>/primamateria/mesh/<id>.json`)

```json
{ "item": "primamateria:iron_mesh", "tier": 3, "speed": 1.25,
  "texture": "primamateria:block/mesh_iron", "slot": 3 }
```
Any item can be made a mesh. `slot` (1 to 8) is the block state value used when the sieve is at rest; two meshes may share a slot if they share a texture.

### Heat sources (`data/<namespace>/primamateria/heat/<id>.json`)

```json
{ "block": "minecraft:campfire", "state": { "lit": "true" }, "heat": 2 }
```
`block` or `tag`. The highest matching value wins.

### Melts (`data/<namespace>/primamateria/melt/<id>.json`)

```json
{ "color": "#d8742a", "glow": 12, "name": "melt.primamateria.molten_copper" }
```

### Compost values (`data/<namespace>/primamateria/compost/<id>.json`)

```json
{ "tag": "minecraft:saplings", "units": 100 }
```
Optional `mature_boost` (0 to 1) shortens the maturing time once.

### World

- `data/primamateria/worldgen/world_preset/void_island.json` and its noise settings: the Void Island preset.
- `data/primamateria/structure/start_island.nbt`: the start island.
- Loot tables for our blocks (26.2 and 26.3 use different layouts; kept as separate resource files per version, as Barely Crafting does).

## 3. Config

`config/primamateria.properties` through `Settings.java` in `common/`, the same pattern as Barely Crafting: defaults written on first start, comments in the file, unknown keys kept, bad values replaced with the default and logged once. Keys and defaults are in `DESIGN.md` section 13. Server values are sent to clients on join so BEI shows the real numbers. `client.*` keys are read on the client only.

## 4. Scripting: ModJS and KubeJS (both optional)

The studio standard (Mod-Playbook, "Compatibility standard", 2026-11-13) requires both in the first release. The mod loads and works with neither installed, and a test proves it.

### Everything is datapack JSON

Every recipe (crushing, sifting, residue, composting, soaking, mixing, steeping, growing, melting, casting) is a real recipe type under `data/<namespace>/recipe/`, and every mesh tier, heat source, melt and compost value is a JSON file under `data/<namespace>/primamateria/`. So a datapack, KubeJS or ModJS can add, change and remove any of them with no Java:

- **Datapack:** add a file to add, use the same path to replace, and replace with `{ "type": "primamateria:none" }` (recipes) or `{ "remove": true }` (data files) to remove.
- **KubeJS:** `ServerEvents.recipes` handles our recipe types like any other (`event.custom({ type: 'primamateria:sifting', ... })`, `event.remove({ type: 'primamateria:sifting', input: 'minecraft:gravel' })`); the four data folders are written through KubeJS's generated-data event.
- **ModJS:** the content calls of the `PrimaMateria` global, below.

### ModJS bridge

Same rules as the other mods (`Mod-Playbook/ROUND_MODJS_AND_HINTS.md`, Part A). Templates: `BarelyCrafting/common/.../script/` and `BarelyQuesting/common/.../core/script/ModJsBridge.java`.

- Installed only when ModJS is present, through reflection on `ExtraBindings.register(BindingProvider)`. `PrimaMateria.init` already looks for `script.ModJsBridge.install()` by name.
- A global named `PrimaMateria`, server scripts only in the first release.
- A script error never crashes anything: one plain log line, then carry on.

| Group | Members | Notes |
|---|---|---|
| Events | `PrimaMateria.on('sifted', e => ...)`, `'crushed'`, `'barrelFinished'`, `'melted'`, `'cast'`, `'driveWound'` | The `GameEvents` channels. `e` has `level`, `pos`, `player` (may be null), the input, and a mutable `drops` list for `sifted` and `crushed`. `e.cancel()` on `sifted` returns the load unprocessed. |
| State (read-only) | `PrimaMateria.sieveAt(level, pos)`, `barrelAt`, `crucibleAt`, `trayAt`, `driveAt`, `autoHammerAt` | The view records. Null when the block is something else. |
| Content | `PrimaMateria.sifting.add({...})`, `.remove(filter)`, `.replace(filter, {...})`; the same for `crushing`, `soaking`, `mixing`, `steeping`, `growing`, `melting`, `casting` | Same field names as the JSON. Applied after each datapack reload, in script order. |
| Data | `PrimaMateria.meshes.add/remove`, `PrimaMateria.heat.add/remove`, `PrimaMateria.melts.add/remove`, `PrimaMateria.compost.add/remove` | Mirrors the four data folders. |
| Config | `PrimaMateria.config.get('sieve.max_load')` | Read-only. |

### KubeJS bridge

For events and reads, the way Barely Questing does it (`BarelyQuesting/docs/integrations.md`): no native KubeJS plugin and no compile dependency. The mod writes a small helper script into `kubejs/startup_scripts/` when KubeJS is present; the helper loads our `script.PrimaMateriaApi` class and exposes the same `PrimaMateria` object with the same events, reads and config as the ModJS global. Listeners are cleared on script reload (`clearListeners()`), so `/reload` does not stack them. The content and data calls are not needed there because KubeJS edits the JSON directly.

**Gap on our targets:** KubeJS has no build for Minecraft 26.2 or 26.3 on Forge or NeoForge (checked on CurseForge 2026-11-13). The data side works today because it is plain datapack JSON. The helper is written against the shared `PrimaMateriaApi` and unit tested with Rhino, and gets its real-client check when a KubeJS build for 26.2 or 26.3 exists. Recorded in `docs/known-gaps.md`.

Docs: `docs/scripting.md` and the wiki page `scripting.md`, covering ModJS, KubeJS and plain datapacks with one example each. Tests: unit tests against the stub ModJS classes in `common/src/test`, one headless run with the real ModJS jar on one build, and one headless run with no script mod at all.

## 5. Recipe viewers and in-world info

All optional: each plugin is its own class in `compat/`, compiled against that mod's API only (compile-only, from the Maven mirror or `libs/`) and loaded only when the mod is present. All plugins read the same `RecipeLookup` snapshot and share `compat/Cards.java`, which lays out slots and values, so a card looks the same in every viewer.

### Which builds exist (CurseForge, 2026-11-13)

| Integration | NeoForge 26.2 | Forge 26.2 | NeoForge 26.3 | Forge 26.3 |
|---|---|---|---|---|
| BEI (ours, `bei-api`) | yes | yes | yes | yes |
| JEI | yes (30.48.0.244, beta) | no build | yes (31.17.0.113, beta) | no build |
| REI | yes (26.2.820, beta) | no build | yes (26.3.824, beta) | no build |
| EMI | no build | no build | no build | no build |
| SMWTI (ours, `smwti-api`) | yes | yes | yes | yes |
| Jade | yes (26.2.10) | no build | yes (26.3.5) | no build |
| ModJS (ours) | yes | yes | yes | yes |
| KubeJS | no build | no build | no build | no build |

So the first release ships BEI, SMWTI and ModJS on all four builds, JEI, REI and Jade on the two NeoForge builds, and the EMI plugin and KubeJS helper written but untested. Every "no build" cell is a row in `docs/known-gaps.md` and a step in `PROJECT_STATUS.md`, so it is added and checked when that mod ports. Recheck the table with the `curseforge-lookup` skill before each release.

### Recipe categories

| Category id | Icon | One card shows |
|---|---|---|
| `primamateria:sifting` | Sieve | One input block and one mesh, with every drop it can give as a result slot, each with its count range and chance as a value under the slot. One card per input and mesh tier pair that differs. Residue in a separate slot. Meshes and drives are catalysts. |
| `primamateria:crushing` | Wooden hammer | Input block, lowest hammer, results with chances. Hammers and the auto hammer are catalysts. |
| `primamateria:composting` | Wooden barrel with compost | Every compostable with its units as a value, the result block and the maturing time. |
| `primamateria:barrel` | Wooden barrel | Soaking, mixing, steeping and growing: fluid slots, an item slot, a result and a time value. A clay barrel icon marks recipes that need the clay barrel. |
| `primamateria:melting` | Crucible | Input, result fluid or melt with its amount. Heat sources are catalysts with their heat value. |
| `primamateria:casting` | Casting tray | Melt amount, result, time. |

Cards carry names and values only: counts, chances, amounts, times. No sentences. Melts are shown with a tinted melt icon (a `CardPainter` in BEI, the matching drawable in the others). Clicking any slot looks up its recipes and uses as usual.

### Drop table information pages

Each siftable block (dirt, gravel, sand, stone meal, scoria, soul sand, pale sand, and any a pack adds) gets an information page in every viewer that supports them (JEI ingredient info, REI and EMI info displays, a BEI info card): a table generated from `RecipeLookup` with one row per drop and one column per mesh tier holding the chance, and the residue on the last row. Values and names only. The same generator writes the wiki's drop table page, so the two never differ.

### In-world info

SMWTI (preferred, all four builds) and Jade (NeoForge builds) show the state of our blocks when looked at, as names and values: sieve (mesh, load, progress), barrel (contents, amount, stage, time left), crucible (contents, amount, heat), casting tray (stage), drives (charge), auto hammer (hammer, queue). The lines come from the view records.

### Proof

Each supported integration gets a real-client check per target build (open the viewer, find a Prima Materia item, open a sifting card and an info page; look at a sieve with SMWTI or Jade) with frames in `docs/frames/`. Results go in `docs/testing` and `PROJECT_STATUS.md`.

## 6. Tests

| Layer | What | Where |
|---|---|---|
| Unit | Sift math (shake counts, load factor, mesh speed), drop rolling with a fixed seed, mesh tier filtering, residue, compost math, grow slots, crucible rates, drive math, config parsing, every data reader with good and bad JSON | `common/src/test`, JUnit, run in all four modules |
| Unit | Animation: JSON loading, each easing mode against known values, clip blending, looping, the contract-bone check | `common/src/test` |
| Unit | Script bridge and the KubeJS helper with the stub ModJS classes and Rhino; a run with no script mod present | `common/src/test` |
| Data | A script check that every shipped recipe and data file parses, every id in `DESIGN.md` exists in `ModContent`, every lang key exists, and no lang value contains more than a name | `scripts/check_data.py` |
| Headless | `SelfTest` on a dev server per build: place each block, run each recipe family end to end, hopper in and out, drives, world preset creates the island, datapack reload, a pack that overrides a drop | `version-*/src/dev`, `scripts/server_check.py` |
| Client | `ShotTour` through `launch-isolated.ps1`: each block placed next to vanilla, the sift sequence in 8 frames, barrel stages, crucible and tray, drives, one check per supported integration (section 5), animations `full`, `reduced`, `off`, and the 200-sieve hall timing | `scripts/client_tour.py`, frames in `docs/frames` |

Test results go in `docs/testing`, `PROJECT_STATUS.md` and the changelog, never in the CurseForge description.

## 7. Work split

One short scaffold step first (done), then five workstreams in parallel: A logic and data, B models and textures, C the animation renderer, D integrations, E docs, branding and CI. Each workstream owns its files; nobody edits another stream's files. Requests across streams go through the interfaces below or through the PM.

### Step 0: scaffold (done 2026-11-13)

The four Gradle modules, `common/`, `version-26.2`, the generated `version-26.3` and the scripts exist, all four modules build, and a dev server on each starts with every id registered (`python scripts/server_check.py all`). These shared files are **frozen** (changes only through the PM):

- `common/.../Ids.java`: every id as a string, in lists: blocks, block entities, hammers, meshes, materials, recipe types, data folders, the brine fluid, melts, sounds, particles. A new id is added here first.
- `version-26.2/.../ModContent.java`: registers every block (a plain placeholder `Block`), every item (a plain `Item`) and every sound event from the `Ids` lists, plus the creative tab. Workstream A swaps the constructor calls in `makeBlock` and `makeItem` for the real classes and adds its own register methods for block entity types, recipe types and the brine fluid; workstream C adds one for particles. Ids and maps stay.
- The interface files listed under "Interfaces" below.
- `lang/en_us.json` with every name, and a placeholder cube model, blockstate, item model and loot table for every block and item, written by `scripts/make_placeholders.py` (it never overwrites an existing file, so real art is safe).
- `PrimaMateria.init` finds `script.ModJsBridge.install()` by name, so workstream D adds the bridge without touching a frozen file.

Loader entry classes (`NeoForgePrimaMateria`, `ForgePrimaMateria`) belong to workstream A after the scaffold; the client classes (`NeoForgeClient`, `ForgeClient`) to workstream C; the dev classes to A.

### Workstream A: logic and data

Owns: `common/.../{sift,barrel,crucible,drive,data}/`, `Settings.java`, `version-26.2/.../{block,blockentity,item,recipe,datapack,net,world}/`, everything under `data/primamateria/`, the loader entry classes (capabilities for items, fluids and energy), `SelfTest`, `scripts/check_data.py`, `scripts/server_check.py`.

Delivers: all gameplay working with placeholder cubes and no animation. Block entities fill the render-state records and fire the gameplay events.

### Workstream B: blocks, models and textures

Owns: `branding/models/*.bbmodel` (model geometry and textures, not the animations), `assets/primamateria/{blockstates,models,items,textures}/`, selection shapes (`ModShapes.java`, voxel shape constants that A's blocks read), `branding/icon/`.

Delivers: every model and texture in `ART.md`, in order: sieve first (for Casey's approval of the style), then barrels, crucible and tray, drives, items, particles' textures, icon.

### Workstream C: animation renderer

Owns: `common/.../anim/`, `scripts/export_anims.py`, `assets/primamateria/primamateria_anim/`, the animations inside the `.bbmodel` files, `version-26.2/.../client/` (renderers, particles, client setup, block color handlers), `sounds.json` and `sounds/`, the client part of `ShotTour`.

Delivers: the loader and sampler with tests, then the sieve renderer against a gray-box sieve model of their own (so C does not wait for B), then the other renderers, particles and sounds, then the performance scene.

### Workstream D: integrations

Owns: `common/.../script/` (ModJS bridge, `PrimaMateriaApi`, KubeJS helper), `version-26.2/.../compat/` (BEI, JEI, REI, EMI, SMWTI and Jade plugins, `Cards`, the drop table info pages), the compile-only dependency lines in the four `build.gradle` files, `docs/scripting.md`, `docs/known-gaps.md`, the integration part of `ShotTour`.

Delivers, in order: BEI categories and info pages (all four builds), the ModJS bridge, JEI and REI plugins (NeoForge), SMWTI lines (all four) and Jade lines (NeoForge), the EMI plugin and the KubeJS helper (written and unit tested; client checks wait for builds), then the client check per integration.

### Workstream E: docs, branding and CI

Owns: the wiki pages, `curseforge/DESCRIPTION.md`, `branding/LISTING.md`, `branding/banners/` and `branding/source/`, `README.md`, `CHANGELOG.md`, `.github/` (workflows written for the PM to enable), issue templates. Small enough to be done by whoever finishes first, or by the PM.

Delivers: wiki, listing text, CI files. The CurseForge description describes the mod only: what it does, features, settings, compatibility, a short FAQ. No install steps, no test notes.

### Interfaces (frozen in step 0)

| Interface | Between | Content |
|---|---|---|
| `Ids` (common) and `ModContent` | all | Final registry ids. Nobody renames one. |
| `view/SieveView` (record) | A fills, C reads | `meshId`, `meshTexture`, `loadBlock`, `load` (0 to 4), `progress` (0 to 1), `shakeCount`, `lastShakeTick`, `drive` (`DriveKind`: none, hand, windup, furnace, powered), `blocked`, `seed`, `finds` (item, count, revealAt, spot)[] |
| `view/BarrelView`, `CrucibleView`, `TrayView`, `DriveView`, `AutoHammerView` (records) | A fills, C reads | Contents id and kind, amount, color, stage and stage progress, grow slots (crystal texture, progress, slot)[], heat, pouring sides, cooling progress, charge, running, fitted hammer, queued blocks |
| `StateProps` (version) | A sets, B models, C reads | `MOVING` (sieve, auto hammer), `MESH` 0 to 8 and `FILL` 0 to 4 (sieve), `LIT` (furnace drive), `CHARGE` 0 to 5 (powered drive), `FACING` (drives, casting tray) |
| Bone contract | B builds, C animates | Per model, the bone names in `SIFTING.md` section 2 (sieve) and one table per other model kept in `docs/BONES.md`, written by C in its first day and followed by B. The exporter fails when a contract bone is missing. |
| `ModShapes` (version) | B writes, A reads | One voxel shape constant per model block (full cubes until B replaces them). |
| `data/RecipeLookup` (common) | A writes, D reads | `RecipeLookup.get()` returns a snapshot of plain records for each recipe type and the four data maps, on both sides, for the recipe viewers and the script bridge. |
| `event/GameEvents` (common) | A fires, D listens | Channels `SIFTED`, `CRUSHED`, `BARREL_FINISHED`, `MELTED`, `CAST`, `DRIVE_WOUND`, each with a small event class. A listener that throws is logged and skipped. |
| Sounds and particles | C supplies assets, A triggers | Sound events are registered from `Ids.SOUNDS`; A plays them by id and sends shake packets; C supplies the files, `sounds.json` and the particle types. |

### Order

1. Step 0 scaffold (done).
2. A, B, C and D start together.
   - A: sieve logic first, then hammer, barrel, crucible and tray, drives and auto hammer, Nether and End chain, world preset.
   - B: sieve model first, approval frame, then the rest.
   - C: loader and sampler, sieve renderer on a gray box, then the bone contract doc, then the rest.
   - D: BEI and the script bridge against `RecipeLookup` and `GameEvents` (usable as soon as A's recipe types exist), then the other viewers and in-world info.
   - E: CI files and listing text any time; wiki when the mechanics are final.
3. Integration (one builder): B's real models into C's renderers, `sync_versions.py` to 26.3 with its patches, full self test on four builds, client tour on one Forge and one NeoForge build.
4. Casey plays; balance pass on the data files only.
5. The PM pushes, runs CI, creates the repository and the CurseForge project, tags `2026.11.1` and releases. Builders commit locally and never push.

## 8. Requests from workstream D (integrations)

Written 2026-11-13 by workstream D. None of these change a frozen file; they are what the other workstreams need to do so the integrations show real data.

1. **Block entities implement `view.ViewProvider`** (new file in `common/.../view/`, one method `Object view()` returning the block entity's view record). SMWTI, Jade and the script reads (`sieveAt` and the others) find the state through it (`compat/GameAccess`). The record must also exist on the client (synced), because Jade reads the client side.
2. **Apply script content after each reload.** After the datapack reload on the server, workstream A builds its `RecipeLookup.Snapshot`, calls `script.ScriptContent.apply(snapshot)` and uses the result both for gameplay and for `RecipeLookup.set` (and the client sync). With no script changes `apply` returns the same snapshot, so the call is free. Script changes are kept in script order; a reload that re-runs scripts starts with `PrimaMateria.reset()`.
3. **Fire the `GameEvents` channels** (already frozen). The script bridge listens to all six.
4. **`PrimaMateria.init` calls `script.ModJsBridge.install()` by name** (already so). It now also installs the game state lookup (`compat.GameAccess`), writes the KubeJS helper script when KubeJS is present and, in dev runs, installs `dev.DemoRecipes`.

What workstream D added outside its own folders (one line each, listed so they survive a merge):

- `common/.../view/ViewProvider.java`, `view/ViewAccess.java`: new files.
- `common/.../viewer/` (new): `Cards` (the shared card model and layout, moved here from the planned `compat/Cards` so it can be unit tested without Minecraft), `Fmt`, `DropTable`, `InfoLines`.
- `lang/en_us.json`: keys `viewer.primamateria.category.*` and `viewer.primamateria.info.*`.
- Each `build.gradle`: the "Optional integrations" block at the end (compile-only SMWTI, JEI, REI, Architectury, Cloth Config, Jade; `-Pviewers=` for dev runs).
- `version-26.2/src/dev/.../SelfTest.java`: one line, `failures += IntegrationCheck.run(server);`.
- `scripts/server_check.py`: the `PM_GRADLE_ARGS` environment variable (extra Gradle arguments, `-Pviewers=modjs` also copies the ModJS test script) and `PRIMAMATERIA_DEMO=1`.
- New dev tools: `scripts/integration_tour.py`, `scripts/fixtures/`, `IntegrationTour`, `IntegrationCheck` and `DemoRecipes` (version dev), `IntegrationDev` (NeoForge dev).

Loader layout of the integrations: BEI, SMWTI and the shared helpers (`Stacks`, `GameAccess`) are in `version-26.x/.../compat/` and ship in all four jars. JEI, REI and Jade only exist for NeoForge, so their plugins live in `neoforge-26.x/src/main/java/.../neoforge/compat/` (copied by hand between 26.2 and 26.3; they are not generated).
