# Scripting

Everything in Prima Materia that decides what drops from what is data, so there are three ways to change it. All three are optional: without ModJS or KubeJS nothing is loaded and nothing changes.

| Way | Good for | Needs |
|---|---|---|
| Datapack | Adding, replacing and removing recipes and data files | Nothing |
| KubeJS | The same as a datapack from a script, plus the `PrimaMateria` global for events and reads | KubeJS |
| ModJS | The `PrimaMateria` global: events, reads, and adding, replacing and removing recipes and data | ModJS |

The recipe and data file formats are on `docs/ARCHITECTURE.md` section 2 and the wiki page "Recipes and data files".

## Datapack

Put a file at the same path to replace one. To add one, use a new path. To remove one, replace it:

- a recipe: `{ "type": "primamateria:none" }`
- a data file (mesh, heat, melt, compost): `{ "remove": true }`

Example, a datapack file `data/mypack/recipe/gold_from_sand.json`:

```json
{ "type": "primamateria:sifting",
  "input": "minecraft:sand",
  "result": "minecraft:gold_nugget",
  "count": [1, 2],
  "chance": 0.02,
  "mesh_min": 4 }
```

## KubeJS

Every Prima Materia recipe type works with `ServerEvents.recipes` like any other recipe type:

```js
ServerEvents.recipes(event => {
  event.custom({ type: 'primamateria:sifting', input: 'minecraft:sand', result: 'minecraft:gold_nugget', chance: 0.02, mesh_min: 4 })
  event.remove({ type: 'primamateria:sifting', input: 'minecraft:gravel' })
})
```

The four data folders (mesh, heat, melt, compost) are written with KubeJS's generated-data event.

When KubeJS is installed, Prima Materia writes `kubejs/startup_scripts/primamateria.js` once. An existing file is never overwritten. It sets `global.PrimaMateria`, the same object ModJS scripts get:

```js
global.PrimaMateria.on('sifted', e => {
  if (e.input() == 'minecraft:gravel') e.addDrop('minecraft:flint', 1)
})
```

Listeners are cleared each time the helper script loads, so `/reload` does not stack them. See `docs/known-gaps.md` for the KubeJS versions.

## ModJS

ModJS server scripts get a global named `PrimaMateria`. Client scripts do not (the machines run on the server).

```js
PrimaMateria.reset()   // first line: a reload does not stack listeners or changes

PrimaMateria.on('sifted', e => {
  if (e.input() == 'minecraft:gravel' && e.tier() >= 3) e.addDrop('minecraft:diamond', 1)
})
PrimaMateria.sifting.add({ input: 'minecraft:sand', result: 'minecraft:gold_nugget', chance: 0.02, mesh_min: 4 })
PrimaMateria.sifting.remove({ result: 'primamateria:iron_dust' })
```

### Events

`PrimaMateria.on(name, e => ...)`. An unknown name is refused with one log line.

| Name | When | Can change |
|---|---|---|
| `sifted` | a sieve load is about to hand out its drops | `e.drops()`, `e.cancel()` (the load stays in the sieve) |
| `crushed` | a hammer or an Auto Hammer crushed a block | `e.drops()` |
| `barrelFinished` | a barrel finished a recipe | nothing |
| `melted` | a crucible melted one solid | nothing |
| `cast` | a casting tray finished cooling | nothing |
| `driveWound` | a player cranked a windup drive | nothing |

The event object has these methods. A field an event does not have returns an empty text or 0.

| Method | Returns |
|---|---|
| `name()` | the event name |
| `level()`, `x()`, `y()`, `z()` | world and block position |
| `player()` | the player, or null for a machine |
| `input()` | block or item that went in (`sifted`, `crushed`, `melted`) |
| `result()` | what came out (`barrelFinished`, `cast`); fluid or melt id (`melted`) |
| `family()` | `composting`, `soaking`, `mixing`, `steeping` or `growing` (`barrelFinished`) |
| `hammerId()` | the hammer (`crushed`) |
| `meshId()` | the mesh (`sifted`); the melt id (`cast`) |
| `count()` | blocks in the load (`sifted`), results (`barrelFinished`, `cast`), total turns (`driveWound`) |
| `amount()` | mB made (`melted`) |
| `tier()` | mesh tier (`sifted`) or hammer tier (`crushed`) |
| `flag()` | made by an Auto Hammer (`crushed`); the result is a melt (`melted`) |
| `drops()` | the live list of `{item(), count()}`; changing it changes the drops |
| `addDrop(item, count)`, `removeDrops(item)`, `clearDrops()` | change `drops()` |
| `cancel()`, `canceled()` | cancel where allowed; other events ignore it |

### Reading machines

All return `null` when the block there is something else. They read a snapshot and change nothing.

| Call | Returns |
|---|---|
| `PrimaMateria.sieveAt(level, x, y, z)` | mesh, load block, load, progress, shake count, drive, blocked, finds |
| `PrimaMateria.barrelAt(...)` | clay or wood, contents, amount, stage and progress, soak item, result, crystals |
| `PrimaMateria.crucibleAt(...)` | contents, amount, solids, heat |
| `PrimaMateria.trayAt(...)` | melt, stage, progress, result |
| `PrimaMateria.driveAt(...)` | kind, charge, running, blocked |
| `PrimaMateria.autoHammerAt(...)` | hammer, input block, queued, progress |

Each also accepts a block position instead of `x, y, z`.

### Changing recipes and data

`PrimaMateria.<family>.add(object)`, `.remove(filter)` and `.replace(filter, object)`. The fields are the ones of the JSON on `docs/ARCHITECTURE.md` section 2 and the wiki page "Recipes and data files", and a filter matches every entry whose fields equal the filter's. A call that is not valid is refused with one log line and the rest of the script goes on. The changes are applied in script order after each datapack reload.

Families: `crushing`, `sifting`, `residue`, `composting`, `soaking`, `mixing`, `steeping`, `growing`, `melting`, `casting`, `meshes`, `heat`, `melts`, `compost`.

For `heat` and `compost` give a `tag` instead of `block` or `item` to match a tag. A melting result is `{ melt: 'id' }` or `{ fluid: 'id' }`.

### Config

`PrimaMateria.config.get('sieve.max_load')` returns the setting as text, `.number(key)` as a number and `.bool(key)` as true or false. It is read only. An unknown key returns an empty text.

### Reload

`PrimaMateria.reset()` drops every listener and change a script made. Put it first in a server script that loads again on `/reload`.

### Errors

An error in a listener or a call is one plain log line. The game and the other listeners carry on.

## Developer notes

- Code: `common/src/main/java/com/itmecasey/primamateria/script/` (`PrimaMateriaApi` is the one object both bridges hand to scripts; `ModJsBridge`, `KubeJsBridge`, `ScriptContent`, `ScriptEvent`) and `view/ViewAccess`.
- Both bridges are optional by reflection. `PrimaMateria.init` calls `ModJsBridge.install()`, which sets up the game state lookup, the KubeJS helper and the ModJS global, and never throws.
- Tests: `common/src/test/.../script/` run real JavaScript through Rhino (the engine ModJS uses) against stub ModJS classes; one test loads the bridge in a class loader that hides ModJS to prove the mod runs without it. The headless self test (`scripts/server_check.py`) runs on every layer without ModJS and, with `PM_GRADLE_ARGS=-Pviewers=modjs`, with the real ModJS jar and the script `scripts/fixtures/modjs_primamateria_check.js`.
- Applying content changes: workstream A calls `ScriptContent.apply(snapshot)` after each reload (see `ARCHITECTURE.md`, section 8).

