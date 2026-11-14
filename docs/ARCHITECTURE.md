# Prima Materia: architecture

Forge and NeoForge on Minecraft 26.2 and 26.3 only. Four jars' worth of code, combined into one jar per Minecraft version the way Barely Crafting does it. Java 25. Mod id `primamateria`, package `com.itmecasey.primamateria`, version `2026.11.1`.

The layout is a copy of `C:\Users\casey\Desktop\BarelyCrafting` (four modules, shared `common/`, `version-26.2` as the source of truth, `version-26.3` generated) with ideas from `C:\Users\casey\Desktop\BarelyStorage` (an `art/` style model pipeline). Read those two before scaffolding.

## 1. Folders

```
PrimaMateria/
  common/src/main/java/com/itmecasey/primamateria/
      PrimaMateria.java            mod id, version, logger
      Settings.java                config/primamateria.properties (pure Java)
      sift/                        SiftMath, SiftLoad, Finds, MeshTier (pure)
      vat/                         VatState, CompostMath, GrowSlots (pure)
      crucible/                    CrucibleMath, Melt, Heat (pure)
      drive/                       DriveMath (pure)
      data/                        JSON readers for mesh, heat, melt, compost files (pure, Gson)
      anim/                        AnimModel, AnimClip, Keyframe, Easing, AnimSampler, AnimJson (pure)
      script/                      ModJsBridge, PrimaMateriaApi, ScriptEvents, ScriptHooks, info records
  common/src/main/resources/
      assets/primamateria/         blockstates, models, items, textures, lang/en_us.json, sounds.json, sounds/, primamateria_anim/
      data/primamateria/           recipe/, primamateria/{mesh,heat,melt,compost}/, structure/, worldgen/world_preset/, loot_table/, tags
      primamateria_logo.png
  common/src/test/java/...         JUnit tests of everything pure, plus the ModJS stub classes
  version-26.2/src/main/java/com/itmecasey/primamateria/
      ModContent.java              every registry entry (blocks, items, block entities, recipe types, sounds, particles, fluid)
      block/                       SieveBlock, VatBlock, CrucibleBlock, CastingTrayBlock, DriveBlock, StoneMealBlock
      blockentity/                 SieveBlockEntity, VatBlockEntity, CrucibleBlockEntity, CastingTrayBlockEntity, DriveBlockEntity
      item/                        HammerItem, MeshItem
      recipe/                      one class per recipe type, serializers, RecipeLookup
      datapack/                    reload listeners for mesh, heat, melt, compost; sync to clients
      net/                         SieveShakePacket, state sync payloads
      world/                       void preset wiring, start island placement
      client/                      render/ (SieveRenderer, VatRenderer, ...), particle/, ClientSetup, block color handlers
      compat/                      PrimaBeiPlugin
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
  scripts/                         sync_versions.py, export_anims.py, make_branding.py, server_check.py, client_tour.py, combine_jars.py
  .github/                         workflows/build.yml, release.yml, scripts/plan_matrix.py, ISSUE_TEMPLATE/
  licenses/                        fonts, our own sound sources
  CHANGELOG.md  LICENSE  PROJECT_STATUS.md  README.md
```

Rules that keep it in order:

- **`common/` has no game classes.** It compiles and tests without Minecraft. Everything a number depends on lives there.
- **`version-26.2` is edited by hand; `version-26.3` is never edited by hand.** `scripts/sync_versions.py` copies and patches it. Each known 26.3 difference is one function in that script.
- **Each loader folder is its own Gradle build** that includes `common/` and its `version-*` folder as source sets. Loader code is only: entry point, event bus wiring, item and fluid handler capabilities, network registration, client entry.
- **Every Gradle command goes through the slot wrapper:** `bash /c/Users/casey/.claude/tools/gradle-slot.sh ./gradlew <tasks> --no-daemon`.
- **Dev test server ports** (to be added to the port registry by whoever scaffolds, before the first `runServer`): proposed 8734 NeoForge 26.2, 8735 Forge 26.2, 8736 NeoForge 26.3, 8737 Forge 26.3.

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
  "result": "primamateria:iron_grains",
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
  "fluid": "minecraft:water", "input": "primamateria:mineral_salt", "count": 4,
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
  "input": "primamateria:iron_grains",
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

## 4. ModJS bridge (optional)

Same rules as the other mods (`Mod-Playbook/ROUND_MODJS_AND_HINTS.md`, Part A). Templates: `BarelyCrafting/common/.../script/` and `BarelyQuesting/common/.../core/script/ModJsBridge.java`.

- Installed only when ModJS is present, through reflection on `ExtraBindings.register(BindingProvider)`. Without ModJS nothing loads and nothing fails.
- A global named `PrimaMateria`, server scripts only in the first release.
- A script error never crashes anything: one plain log line, then carry on.

Planned surface (small on purpose):

| Group | Members | Notes |
|---|---|---|
| Events | `PrimaMateria.on('sifted', e => ...)`, `'crushed'`, `'vatFinished'`, `'melted'`, `'cast'`, `'driveWound'` | `e` has `level`, `pos`, `player` (may be null), the input, and a mutable `drops` list for `sifted` and `crushed`. `e.cancel()` on `sifted` returns the load unprocessed. |
| State (read-only) | `PrimaMateria.sieveAt(level, pos)`, `vatAt`, `crucibleAt`, `driveAt` | Snapshot records: mesh id and tier, load, progress, contents, amount, heat, turns. Null when the block is something else. |
| Content | `PrimaMateria.sifting.add({...})`, `.remove(filter)`, `.replace(filter, {...})`; the same for `crushing`, `soaking`, `mixing`, `steeping`, `growing`, `melting`, `casting` | Same field names as the JSON. Applied after each datapack reload, in script order. |
| Data | `PrimaMateria.meshes.add/remove`, `PrimaMateria.heat.add/remove`, `PrimaMateria.melts.add/remove`, `PrimaMateria.compost.add/remove` | Mirrors the four data folders. |
| Config | `PrimaMateria.config.get('sieve.max_load')` | Read-only. |

Docs: `docs/scripting.md` and the wiki page `scripting.md`. Tests: unit tests against the stub ModJS classes in `common/src/test`, plus one headless run with the real ModJS jar on one build.

## 5. BEI integration

BEI is optional (compile against the API jar in `<loader>/libs/`, load the plugin only when BEI is present). API classes: `IBeiPlugin`, `ICategoryRegistration`, `IRecipeRegistration`, `RecipeCategory`, `CustomRecipe`, `SlotSpec`, `CardPainter` (see `BarelyCrafting/version-26.2/.../compat/CraftingBeiPlugin.java`).

| Category id | Icon | One card shows |
|---|---|---|
| `primamateria:sifting` | Sieve | One input block and one mesh, with every drop it can give as a result slot, each with its count range and chance as a value under the slot. One card per input and mesh tier pair that differs. Residue shown in a separate slot. |
| `primamateria:crushing` | Wooden hammer | Input block, lowest hammer, results with chances. |
| `primamateria:vat` | Wooden vat | Composting, soaking, mixing, steeping and growing, one card layout with fluid slots, an item slot, a result and a time value. A small vat icon variant marks recipes that need the clay vat. |
| `primamateria:crucible` | Crucible | Melting (input, result fluid or melt with amount) and casting (melt amount, result, time). Heat sources are listed as catalysts with their heat value. |

Cards carry names and values only: counts, chances, amounts, times. No sentences. Melts are shown with a tinted melt icon drawn by a `CardPainter`.

JEI, EMI and REI plugins are later.

## 6. Tests

| Layer | What | Where |
|---|---|---|
| Unit | Sift math (shake counts, load factor, mesh speed), drop rolling with a fixed seed, mesh tier filtering, residue, compost math, grow slots, crucible rates, drive math, config parsing, every data reader with good and bad JSON | `common/src/test`, JUnit, run in all four modules |
| Unit | Animation: JSON loading, each easing mode against known values, clip blending, looping, the contract-bone check | `common/src/test` |
| Unit | Script bridge with the stub ModJS classes and Rhino | `common/src/test` |
| Data | A script check that every shipped recipe and data file parses, every id in `DESIGN.md` exists in `ModContent`, every lang key exists, and no lang value contains more than a name | `scripts/check_data.py` |
| Headless | `SelfTest` on a dev server per build: place each block, run each recipe family end to end, hopper in and out, drives, world preset creates the island, datapack reload, a pack that overrides a drop | `version-*/src/dev`, `scripts/server_check.py` |
| Client | `ShotTour` through `launch-isolated.ps1`: each block placed next to vanilla, the sift sequence in 8 frames, vat stages, crucible and tray, drives, BEI cards, animations `full`, `reduced`, `off`, and the 200-sieve hall timing | `scripts/client_tour.py`, frames in `docs/frames` |

Test results go in `docs/testing`, `PROJECT_STATUS.md` and the changelog, never in the CurseForge description.

## 7. Work split

One short scaffold step first, done by one builder, then four workstreams in parallel. Each workstream owns its files; nobody edits another stream's files. Requests across streams go through the interfaces below or through the PM.

### Step 0: scaffold (one builder, before anything else)

Creates the four Gradle modules, `common/`, `version-26.2`, the sync script, and these shared files, which are then **frozen** (changes only through the PM):

- `ModContent.java` with every registry entry from `DESIGN.md` sections 4 and 5, registered as plain placeholder blocks and items with the final ids.
- The interface files listed under "Interfaces" below, with empty or trivial bodies.
- `lang/en_us.json` with every name.
- A placeholder cube model and blockstate for every block, so the game runs from the first day.

Done when all four modules build and a dev server starts with every id present.

### Workstream A: logic and data

Owns: `common/.../{sift,vat,crucible,drive,data}/`, `Settings.java`, `version-26.2/.../{block,blockentity,item,recipe,datapack,net,world}/`, everything under `data/primamateria/`, loader capability wiring, `SelfTest`, `scripts/check_data.py`, `scripts/server_check.py`.

Delivers: all gameplay working with placeholder cubes and no animation. Block entities fill the render-state records and fire the gameplay events.

### Workstream B: blocks, models and textures

Owns: `branding/models/*.bbmodel` (model geometry and textures, not the animations), `assets/primamateria/{blockstates,models,items,textures}/`, selection shapes (one `Shapes.java` file of voxel shape constants that A's blocks read), `branding/icon/`.

Delivers: every model and texture in `ART.md`, in order: sieve first (for Casey's approval of the style), then vats, crucible and tray, drives, items, particles' textures, icon.

### Workstream C: animation renderer

Owns: `common/.../anim/`, `scripts/export_anims.py`, `assets/primamateria/primamateria_anim/`, the animations inside the `.bbmodel` files, `version-26.2/.../client/` (renderers, particles, client setup, block color handlers), `sounds.json` and `sounds/`, the client part of `ShotTour`.

Delivers: the loader and sampler with tests, then the sieve renderer against a gray-box sieve model of their own (so C does not wait for B), then the other renderers, particles and sounds, then the performance scene.

### Workstream D: integrations and docs

Owns: `common/.../script/`, `version-26.2/.../compat/`, `docs/scripting.md`, the wiki pages, `curseforge/DESCRIPTION.md`, `branding/LISTING.md`, `branding/banners/` and `branding/source/`, `README.md`, `CHANGELOG.md`, `.github/` (workflows written for the PM to enable), issue templates.

Delivers: ModJS bridge, BEI plugin, wiki, listing text, CI files. The CurseForge description describes the mod only: what it does, features, settings, compatibility, a short FAQ. No install steps, no test notes.

### Interfaces (frozen in step 0)

| Interface | Between | Content |
|---|---|---|
| `ModContent` ids | all | Final registry ids. Nobody renames one. |
| `SieveView` (record) | A fills, C reads | `meshId`, `meshTexture`, `loadBlock`, `load` (0 to 4), `progress` (0 to 1), `shakeCount`, `lastShakeTick`, `driveKind` (hand, windup, furnace, none), `blocked`, `seed`, `finds` (item, revealAt, spot)[] |
| `VatView`, `CrucibleView`, `TrayView`, `DriveView` (records) | A fills, C reads | Contents id and kind, amount, color, stage and stage progress, grow slots (crystal texture, progress)[], heat, pouring-to side, cooling progress, turns, burning |
| Block state properties | A sets, B models, C reads | `sieve`: `moving` (bool), `mesh` (0 to 8), `fill` (0 to 4). `furnace_drive`: `lit`. `facing` on drives and the casting tray. Listed in one `StateProps.java`. |
| Bone contract | B builds, C animates | Per model, the bone names in `SIFTING.md` section 2 (sieve) and one table per other model kept in `docs/BONES.md`, written by C in its first day and followed by B. The exporter fails when a contract bone is missing. |
| `Shapes` | B writes, A reads | One voxel shape constant per block. |
| `RecipeLookup` | A writes, D reads | Read-only lists of each recipe type and the four data maps, on both sides, for BEI and the script bridge. |
| `GameEvents` | A fires, D listens | `sifted`, `crushed`, `vatFinished`, `melted`, `cast`, `driveWound`, each with a small event record. Plain Java listener lists in `common/`. |
| Sounds and particles | C registers assets, A triggers | A plays sound events and sends shake packets by id from `ModContent`; C supplies the files and the client side. |

### Order

1. Step 0 scaffold (about one session).
2. A, B, C and D start together.
   - A: sieve logic first, then hammer, vat, crucible and tray, drives, world preset.
   - B: sieve model first, approval frame, then the rest.
   - C: loader and sampler, sieve renderer on a gray box, then the bone contract doc, then the rest.
   - D: script bridge and BEI against `RecipeLookup` and `GameEvents` (usable as soon as A's recipe types exist), docs last.
3. Integration (one builder): B's real models into C's renderers, `sync_versions.py` to 26.3 with its patches, full self test on four builds, client tour on one Forge and one NeoForge build.
4. Casey plays; balance pass on the data files only.
5. The PM pushes, runs CI, creates the repository and the CurseForge project, tags `2026.11.1` and releases. Builders commit locally and never push.
