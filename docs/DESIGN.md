# Prima Materia: game design

Mod id `primamateria`. Forge and NeoForge, Minecraft 26.2 and 26.3 only. First release `2026.11.1`.

Prima Materia is a skyblock resource mod. You start on a small island in the void with one tree, and you make every resource in the game by breaking matter down and sorting it: crush it with a hammer, shake it through a sieve, soak and grow it in a vat, melt and cast it in a crucible. The name is the alchemists' "first matter", the raw stuff everything else is made from, and the look follows it: aged wood, fired clay, brass trim and a violet crystal accent.

Every number in this document is our own first pass, made to be fun in a few hours, and every number lives in data files so packs can change it.

## 1. Design pillars

1. **The sieve is the star.** Sifting is the action the player repeats most, so it has the best animation and sound in the mod (see `SIFTING.md`). You see the material thin out, the fines fall through and the finds appear on the mesh.
2. **No screens.** Every block is used in the world: put things in by hand, take them out by hand, read the state from the model. The mod has no container screens at all, so there is nothing to explain and no text to read.
3. **No hints.** No tutorial text, no tooltips that explain mechanics, no advancements, no chat messages. Items and blocks show their name only. Recipes are discovered through the recipe viewer (BEI).
4. **Everything is data.** Each mechanic is a recipe type or a small data file in a datapack. Nothing about what drops from what is in Java.
5. **Random early, certain late.** Early sifting is a roll of the dice. The late game (crystals, diamonds) is a slow and certain process you set up and wait for, so the end of the progression does not depend on luck.
6. **A few hours, not a few weeks.** A new player reaches iron in about 45 minutes, a Nether portal in about 2 hours and diamond tools in about 3 hours.

## 2. Core loop

```
tree -> hammer -> gravel / sand / stone meal
              -> sieve (4 mesh tiers) -> seeds, rock chips, metal grains, salts, crystal seeds
              -> vat: compost -> dirt | water + stone meal -> clay | brine + seed -> crystals
              -> crucible: stone -> lava | grains -> molten metal -> casting tray -> ingots
              -> drives: sieves that run without you
```

## 3. Progression

Four stages in the first release. The stage names are for this document and the wiki; the game never shows them.

| Stage | Goal | What you do | Unlocks |
|---|---|---|---|
| 1. Wood and fiber (0 to 20 min) | First sieve, first dirt, first stone | Hammer leaves for plant fiber. Build a wooden hammer, a sieve and a fiber mesh. Compost leaves and saplings in a wooden vat for more dirt. Sift dirt for rock chips, seeds and saplings. Craft rock chips into cobblestone. | Cobblestone, farming, stone hammer, furnace |
| 2. Clay and fire (20 to 45 min) | Crucible and copper | Hammer cobblestone down to gravel, sand and stone meal. Sift gravel for flint, coal and copper grains. Let a vat fill with dew, soak stone meal for clay. Fire a crucible and a casting tray. Melt copper grains, cast ingots. Melt stone for lava. | Copper tools, copper mesh, lava, cobblestone generator, obsidian |
| 3. Metal (45 min to 2 h) | Iron and gold, automation | Copper mesh brings iron grains, mineral salt and redstone. Iron mesh brings gold and the first crystal seeds. Build a windup drive, then a furnace drive, and feed sieves with hoppers. | Iron and gold tools, buckets, hoppers, Nether portal, drives |
| 4. Crystal (2 to 3 h) | Diamonds and the top mesh | Steep mineral salt in water for brine. Grow quartz, amethyst and lapis from seeds in a vat. Make the resonant mesh, sift stone meal for diamond seeds, grow diamonds. | Diamonds, resonant mesh, enchanting |

Later (not in the first release): Nether and End materials (scoria, pale sand), netherite, a stamp mill that hammers without you, a drive that runs on energy, more mold shapes, mob-drop sources.

## 4. Blocks

All ids are `primamateria:<id>`. "v1" is the first release, "later" is planned and not built for `2026.11.1`.

| Id | Name | What it does | Scope |
|---|---|---|---|
| `sieve` | Sieve | Holds one mesh and a load of 1 to 4 blocks of one material. Shaking it (by hand or by a drive below it) turns the load into drops. Section 6. | v1 |
| `wooden_vat` | Wooden Vat | Open tub. Holds 1000 mB of a cool fluid or 1000 units of compost. Collects dew and rain. Composting, soaking, steeping, growing. Cannot hold lava. Section 8. | v1 |
| `clay_vat` | Clay Vat | Fired vat. Same as the wooden vat, and also holds lava, so it can do mixing recipes with a hot fluid. | v1 |
| `crucible` | Crucible | Fired pot. Melts solids over a heat source into lava or molten metal, 4000 mB. Section 9. | v1 |
| `casting_tray` | Casting Tray | Sits next to a crucible. Takes 100 mB of molten metal and cools it into an ingot. | v1 |
| `windup_drive` | Windup Drive | Goes under a sieve. You crank it by hand to store turns; each turn is one shake. Section 7. | v1 |
| `furnace_drive` | Furnace Drive | Goes under a sieve. Burns furnace fuel and shakes the sieve faster than a hand. | v1 |
| `stone_meal` | Stone Meal | The finest crushed stone. A falling block, like sand. Sifted for late materials, soaked for clay. | v1 |
| `scoria` | Scoria | Crushed netherrack, sifted for Nether materials. | later |
| `pale_sand` | Pale Sand | Crushed end stone, sifted for End materials. | later |
| `stamp_mill` | Stamp Mill | Crushes blocks without a player, driven like a sieve. | later |
| `flux_drive` | Flux Drive | A drive that runs on energy (FE), for packs with a power mod. | later |

Unfired clay pieces are items, not blocks (section 5).

## 5. Items

| Id | Name | What it does | Scope |
|---|---|---|---|
| `wooden_hammer` | Wooden Hammer | Hammer tier 1. Durability 96. | v1 |
| `stone_hammer` | Stone Hammer | Tier 2. Durability 192. | v1 |
| `copper_hammer` | Copper Hammer | Tier 3. Durability 288. Faster than stone. | v1 |
| `iron_hammer` | Iron Hammer | Tier 4. Durability 512. | v1 |
| `diamond_hammer` | Diamond Hammer | Tier 5. Durability 2048. | v1 |
| `fiber_mesh` | Fiber Mesh | Mesh tier 1 (coarse). | v1 |
| `copper_mesh` | Copper Mesh | Mesh tier 2 (medium). | v1 |
| `iron_mesh` | Iron Mesh | Mesh tier 3 (fine). | v1 |
| `resonant_mesh` | Resonant Mesh | Mesh tier 4 (finest). Gold wire strung with amethyst. | v1 |
| `plant_fiber` | Plant Fiber | From hammered leaves. Makes the fiber mesh; 3 make 1 string. | v1 |
| `rock_chip` | Rock Chip | From sifted dirt. 4 (2 by 2) make 1 cobblestone. | v1 |
| `copper_grains` | Copper Grains | Sifted metal. 20 mB of molten copper each when melted (5 per ingot). | v1 |
| `iron_grains` | Iron Grains | Same, for iron. | v1 |
| `gold_grains` | Gold Grains | Same, for gold. | v1 |
| `mineral_salt` | Mineral Salt | Sifted from sand. 4 steeped in a vat of water make brine. | v1 |
| `quartz_seed` | Quartz Seed | Grows into 2 quartz in brine. | v1 |
| `amethyst_seed` | Amethyst Seed | Grows into 2 amethyst shards in brine. | v1 |
| `lapis_seed` | Lapis Seed | Grows into 3 lapis lazuli in brine. | v1 |
| `diamond_seed` | Diamond Seed | Grows into 1 diamond in brine, slowly. | v1 |
| `raw_crucible` | Unfired Crucible | Smelt in a furnace to get the crucible block. | v1 |
| `raw_casting_tray` | Unfired Casting Tray | Smelt to get the casting tray block. | v1 |
| `raw_clay_vat` | Unfired Clay Vat | Smelt to get the clay vat block. | v1 |
| `emerald_seed` | Emerald Seed | Grows into 1 emerald. | later |
| `netherite_hammer` | Netherite Hammer | Tier 6. | later |
| `star_mesh` | Star Mesh | Mesh tier 5, for Nether and End materials. | later |
| `mold_*` | Molds | Other casting shapes (nugget, block, plate). | later |

Vanilla items the mod hands out directly: flint, coal, redstone, bone meal, gunpowder, glowstone dust, seeds, saplings, cactus, sugar cane, bamboo, carrot, potato, clay, dirt, obsidian, lava, ingots, quartz, amethyst shards, lapis lazuli, diamonds.

## 6. Sieve

- **Mesh.** Use a mesh item on an empty sieve to fit it. Sneak-use with an empty hand on an empty sieve to take it out. A sieve without a mesh takes no material.
- **Load.** Use a siftable block on the sieve to add it, up to 4 of the same block. The load shows as a layer in the tray, one quarter of the tray height per block.
- **Shake.** Hold use on a loaded sieve with any item or an empty hand. While you hold, the sieve shakes once every 6 ticks. Nothing happens when you only tap; holding is the action. You may hold a stack of the same material in your hand: when a load finishes, the sieve reloads from that stack, so you never stop.
- **Work.** A load of 1 block needs 8 shakes with a mesh of speed 1.0. Each extra block in the load adds half as much again: 8, 12, 16, 20 shakes for loads of 1, 2, 3, 4. A full tray takes 2.5 times as long as a single block and gives 4 times the rolls, so filling the tray is 1.6 times as efficient. The mesh `speed` divides the shake count.
- **Drops.** The server rolls every drop when the load starts, from the sifting recipes that match the block and the mesh tier, once per block in the load. The rolled drops are revealed on the mesh as the layer thins and are handed out when the load finishes: into the inventory below the sieve (or below its drive) when there is one, otherwise they hop off the tray toward the player who shook it.
- **Residue.** Some materials leave fines: a chance per block to drop the next finer block (gravel leaves sand, sand leaves stone meal). Residue falls straight down through the mesh, into the inventory below or onto the ground under the sieve.
- **Automation.** Hoppers and pipes may insert siftable blocks from the top and sides (same rules as by hand). Drops and residue leave through the bottom. The mesh slot is never automated.
- **Comparator.** Signal 0 when empty, 1 to 15 for the progress of the current load.
- **Break.** Breaking the sieve drops the block, the mesh and the unprocessed load. Drops already rolled for the load are discarded and rolled again later.

### Mesh tiers

Mesh tiers are data (`data/<namespace>/primamateria/mesh/<id>.json`). A sifting recipe names the lowest tier that can give its drop, and may name a highest tier. First release:

| Tier | Id | Recipe (shaped, 3 by 3) | Speed | Gives access to |
|---|---|---|---|---|
| 1 | `fiber_mesh` | 4 sticks in the corners, 5 plant fiber | 1.0 | Rock chips, seeds, saplings, flint, coal, copper grains, bone meal |
| 2 | `copper_mesh` | 4 sticks in the corners, 5 copper ingots | 1.0 | Iron grains, mineral salt, redstone, gunpowder, crops |
| 3 | `iron_mesh` | 4 sticks in the corners, 5 iron ingots | 1.25 | Gold grains, glowstone dust, quartz, amethyst and lapis seeds |
| 4 | `resonant_mesh` | 4 amethyst shards in the corners, 4 gold ingots on the edges, 1 iron mesh in the center | 1.5 | Diamond seeds, loose lapis and quartz |

Meshes have no durability. A finer mesh still gives everything the coarser ones give unless a recipe sets a highest tier.

## 7. Hammer and drives

### Hammer

A hammer is a tool. Mining a block that has a crushing recipe with a hammer of a high enough tier drops the recipe's result in place of the block's normal drop. Hammers mine stone, sand, gravel and leaves at tool speed; on other blocks they are slow like a bare hand.

Crushing chain, first release:

| Input | Result | Lowest hammer tier |
|---|---|---|
| `minecraft:cobblestone`, `minecraft:stone` | 1 gravel | 1 |
| `minecraft:gravel` | 1 sand | 1 |
| `minecraft:sand` | 1 stone meal | 2 |
| `#minecraft:leaves` | 1 plant fiber, 35% a second one, 8% the tree's sapling | 1 |
| `minecraft:sandstone` | 2 sand | 1 |
| `minecraft:netherrack` | 1 scoria | 3 (later) |
| `minecraft:end_stone` | 1 pale sand | 4 (later) |

Hammer tiers add yield: tiers 3, 4 and 5 give a 10%, 20% and 30% chance of one extra result item (field `bonus_per_tier` in the config, applied above tier 2).

Recipes: 2 planks over 1 stick over 1 stick for wood; the same shape with cobblestone, copper ingots, iron ingots or diamonds for the others.

### Drives

A drive sits directly under a sieve and shakes it. Drives have no screen.

| Drive | Power | Rate | Details |
|---|---|---|---|
| Hand | Hold use | 1 shake per 6 ticks | Reloads from the stack in your hand. |
| `windup_drive` | Hold use on the drive to crank: 1 turn per 5 ticks, stores up to 48 turns, shown by a brass dial on the front that sweeps a quarter circle | 1 shake per 8 ticks, 1 turn per shake | Runs while it has turns and the sieve has a load. Recipe: 4 planks, 3 copper ingots, 1 iron ingot, 1 stick. |
| `furnace_drive` | Furnace fuel, put in by hand or hopper (1 slot, 64 items); the firebox glows while burning | 1 shake per 4 ticks | Burns only while the sieve above has a load. Recipe: 1 furnace, 1 windup drive, 4 iron ingots, 2 gold ingots. |

Both drives keep a 9-slot output buffer for the sieve's drops and residue. Hoppers and pipes take from the buffer through the bottom and sides. When the buffer is full the sieve stops after the current load. A redstone signal stops a drive.

## 8. Vat

A vat holds either one fluid (1000 mB) or compost (1000 units), never both. It has one "soak slot" for the item or block being soaked, steeped or grown. Everything is done by hand in the world or through hoppers and pipes (items in from the top and sides, results out through the bottom; fluids through the loader's fluid handler).

- **Dew and rain.** A vat with open sky above it gains 1 mB of water every 4 ticks (a full vat in about 3 minutes 20 seconds) and 8 times that in rain. Config: `vat.dew_ticks_per_mb`, `vat.rain_multiplier`. This is how water exists before the first bucket.
- **Buckets and bottles** fill and empty a vat as usual.

Vat recipe families:

| Family | Recipe type | What happens | First-release recipes |
|---|---|---|---|
| Composting | `primamateria:composting` | Each organic item adds compost units. At 1000 the vat matures for 20 seconds, the surface darkening from green to brown, then holds 1 result block, taken by hand or hopper. | Result `minecraft:dirt`. Values: sapling 100, leaves 80, plant fiber 50, seeds 60, wheat 80, apple 100, flower 80, cactus 100, sugar cane 80, rotten flesh 100, bread 160, bone meal 40 (and it halves the maturing time once). Tag-based where a tag exists. |
| Soaking | `primamateria:soaking` | An item or block put into a vat of fluid turns into something else and uses some fluid. Takes a few seconds. | Water (250 mB) + stone meal = clay block, 5 s. Water (250 mB) + dirt = mud, 5 s. |
| Mixing | `primamateria:mixing` | A second fluid poured into a vat that holds a fluid makes a block. Uses both. | Clay vat with 1000 mB lava + 1000 mB water = obsidian. Vat with 1000 mB water + 1000 mB lava = stone. |
| Steeping | `primamateria:steeping` | An item dissolved in a fluid over time changes the fluid. | 1000 mB water + 4 mineral salt = 1000 mB brine, 30 s. |
| Growing | `primamateria:growing` | A seed placed in a vat of the right fluid grows slowly into a result and uses fluid. Up to 4 seeds grow at once, each on its own timer, each shown as a crystal that gets bigger. | In brine, 250 mB each: quartz seed = 2 quartz, 90 s. Amethyst seed = 2 amethyst shards, 120 s. Lapis seed = 3 lapis lazuli, 120 s. Diamond seed = 1 diamond, 300 s. |

**Brine** (`primamateria:brine`) is a real fluid with a bucket (`primamateria:brine_bucket`), so pipes and tanks of other mods can move it. It is the only new world fluid in the first release.

## 9. Crucible and casting

- **Heat.** A crucible needs a heat source in the block below it. Heat sources are data (`data/<namespace>/primamateria/heat/<id>.json`: block or block tag, optional block state, heat value). First release: torch 1, campfire 2, magma block 3, fire 3, lava 4, soul campfire 4, soul fire 5.
- **Melting.** Put solids in by hand or hopper (buffer of 4 items). The crucible melts 2 mB per tick per point of heat. It holds one kind of content at a time, 4000 mB.
- **Contents.** The crucible holds either a real fluid (lava) or a **melt**. A melt is a data-defined molten material, not a world fluid (`data/<namespace>/primamateria/melt/<id>.json`: color, glow, name key). Packs add a new metal with a melt file, a melting recipe and a casting recipe, and no Java. First release melts: `primamateria:molten_copper`, `primamateria:molten_iron`, `primamateria:molten_gold`.
- **Lava** leaves by bucket or through the fluid handler (pipes).
- **Casting.** A casting tray placed against the side of a crucible is fed by it: while the tray is empty and the crucible holds at least 100 mB of a melt with a casting recipe, the crucible pours 100 mB (a visible stream), the tray glows and cools for 3 seconds, then holds the ingot, taken by hand or by a hopper below. One crucible feeds up to 4 trays.

| Family | Recipe type | First-release recipes |
|---|---|---|
| Melting | `primamateria:melting` | Cobblestone, stone, gravel = 200 mB lava. Netherrack = 500 mB lava. Copper, iron, gold grains = 20 mB of their melt. Raw copper, iron, gold and their ingots = 100 mB. |
| Casting | `primamateria:casting` | 100 mB molten copper, iron, gold = 1 ingot of that metal, 3 s. |

Crafting recipes for the blocks (rows top to bottom, `.` is empty):

| Result | Shape | Then |
|---|---|---|
| `sieve` | `P.P` / `PSP` / `S.S` (P plank, S stick) | |
| `wooden_vat` | `P.P` / `P.P` / `PPP` | |
| `raw_clay_vat` | `C.C` / `C.C` / `CBC` (C clay ball, B clay block) | Smelt to `clay_vat` |
| `raw_crucible` | `C.C` / `CCC` (C clay ball) | Smelt to `crucible` |
| `raw_casting_tray` | `CCC` (C clay ball) | Smelt to `casting_tray` |

## 10. Sifting drop tables (first pass)

One sifting recipe is one drop: input block, lowest mesh tier, optional highest tier, result, count range and chance. The tables below are the shipped defaults, per block of load. "T2+" means mesh tier 2 or finer.

**Dirt** (`sifting/dirt/*`)

| Drop | Count | Chance | Mesh |
|---|---|---|---|
| Rock chip | 1 to 2 | 70% | T1+ |
| Rock chip | 1 | 35% | T1+ |
| Plant fiber | 1 | 15% | T1+ |
| Wheat seeds | 1 | 12% | T1+ |
| Bone meal | 1 | 8% | T1+ |
| Beetroot seeds | 1 | 6% | T1+ |
| Melon seeds, pumpkin seeds | 1 | 4% each | T1+ |
| Birch, spruce, oak sapling | 1 | 3% each | T1+ |
| Sugar cane, bamboo | 1 | 4% each | T2+ |
| Carrot, potato | 1 | 4% each | T2+ |

**Gravel** (`sifting/gravel/*`), residue: sand 15%

| Drop | Count | Chance | Mesh |
|---|---|---|---|
| Flint | 1 | 22% | T1+ |
| Copper grains | 1 to 2 | 14% | T1+ |
| Coal | 1 | 9% | T1+ |
| Iron grains | 1 | 10% | T2 only |
| Iron grains | 1 to 2 | 16% | T3+ |
| Gold grains | 1 | 5% | T3+ |

**Sand** (`sifting/sand/*`), residue: stone meal 15%

| Drop | Count | Chance | Mesh |
|---|---|---|---|
| Copper grains | 1 | 8% | T1+ |
| Cactus | 1 | 4% | T1+ |
| Mineral salt | 1 | 12% | T2+ |
| Iron grains | 1 | 7% | T2+ |
| Gold grains | 1 | 8% | T3+ |
| Quartz seed | 1 | 6% | T3+ |

**Stone meal** (`sifting/stone_meal/*`)

| Drop | Count | Chance | Mesh |
|---|---|---|---|
| Bone meal | 1 | 15% | T1+ |
| Redstone | 1 to 2 | 10% | T2+ |
| Gunpowder | 1 | 6% | T2+ |
| Quartz seed | 1 | 8% | T3+ |
| Glowstone dust | 1 | 5% | T3+ |
| Lapis seed | 1 | 5% | T3+ |
| Amethyst seed | 1 | 5% | T3+ |
| Lapis lazuli | 1 to 2 | 6% | T4+ |
| Diamond seed | 1 | 3% | T4+ |

Rough pacing check: one iron ingot is 5 grains. With the copper mesh, gravel gives 0.10 grains per block, so about 50 gravel per ingot by hand (about 4 minutes with full trays); with the iron mesh 0.24 per block, about 21 gravel per ingot. A diamond is one seed (3% of stone meal on the resonant mesh, about 33 blocks) plus 5 minutes of growing, and four grow at once. These are the numbers Casey tunes after playing.

## 11. Starting on a void island

- **World preset** `primamateria:void_island`, shown in the world creation screen as "Void Island". The Overworld is empty void with normal biomes for weather and mob rules. The Nether and the End are normal in the first release.
- **Start island** is a structure file, `data/primamateria/structure/start_island.nbt`, placed at the world spawn: a 7 by 7 dirt island 3 blocks deep with a grass top, one oak tree and nothing else. No chest, no starter items: one tree is enough (hammer, sieve, vat, fiber).
- A datapack replaces the structure file to change the island. The config has `start.island` (`preset_only`, `always`, `never`) for packs that use another void world type and still want our island, or the other way around.
- Multiplayer: one shared island at spawn in the first release. Per-player or per-team islands are later.

The mod works in a normal world too. Nothing requires the preset.

## 12. Recipe families and data files

Recipe types (in `data/<namespace>/recipe/`, so datapacks, KubeJS and ModJS handle them like any recipe):

| Type | Used by | Fields (summary) |
|---|---|---|
| `primamateria:crushing` | Hammer | `input` (block or block tag), `results` (item, count, chance)[], `hammer_tier` |
| `primamateria:sifting` | Sieve | `input` (item ingredient), `result`, `count` (min, max), `chance`, `mesh_min`, `mesh_max` |
| `primamateria:sifting_residue` | Sieve | `input`, `result`, `chance` |
| `primamateria:composting` | Vat | `result` (block). Compost values are a data map, below. |
| `primamateria:soaking` | Vat | `fluid`, `amount`, `input`, `result`, `time` |
| `primamateria:mixing` | Vat | `base_fluid`, `added_fluid`, `amount`, `result`, `needs_heatproof` |
| `primamateria:steeping` | Vat | `fluid`, `input`, `count`, `result_fluid`, `time` |
| `primamateria:growing` | Vat | `fluid`, `amount`, `seed`, `result`, `count`, `time` |
| `primamateria:melting` | Crucible | `input`, `result` (fluid or melt id), `amount` |
| `primamateria:casting` | Casting tray | `melt`, `amount`, `result`, `time` |

Data files that are not recipes (in `data/<namespace>/primamateria/`):

| Folder | One file is | Fields |
|---|---|---|
| `mesh/` | A mesh tier | `item`, `tier`, `speed`, `texture` |
| `heat/` | A heat source | `block` or `tag`, `state` (optional), `heat` |
| `melt/` | A molten material | `color`, `glow`, `name` |
| `compost/` | A compost value | `item` or `tag`, `units`, `mature_boost` (optional) |

Exact formats with examples are in `ARCHITECTURE.md`.

## 13. Config

`config/primamateria.properties`, written with defaults on first start (the house format).

| Key | Default | Meaning |
|---|---|---|
| `sieve.shakes_per_block` | 8 | Shakes for a load of 1 at speed 1.0 |
| `sieve.extra_block_factor` | 0.5 | Extra work per extra block of load |
| `sieve.max_load` | 4 | Blocks per load, 1 to 4 |
| `sieve.drop_multiplier` | 1.0 | Scales every sifting chance |
| `hammer.bonus_per_tier` | 0.10 | Extra-result chance per hammer tier above 2 |
| `drive.windup_max_turns` | 48 | Stored turns |
| `drive.windup_ticks_per_shake` | 8 | |
| `drive.furnace_ticks_per_shake` | 4 | |
| `vat.dew_ticks_per_mb` | 4 | 0 switches dew off |
| `vat.rain_multiplier` | 8 | |
| `vat.time_multiplier` | 1.0 | Scales compost, soak, steep and grow times |
| `crucible.mb_per_tick_per_heat` | 2 | |
| `start.island` | `preset_only` | `preset_only`, `always`, `never` |
| `client.animations` | `full` | `full`, `reduced`, `off` (see `SIFTING.md`) |
| `client.particles` | `true` | |

## 14. First release versus later

**In `2026.11.1`:** the sieve with 4 meshes and the full animation; 5 hammers and the crushing chain to stone meal; wooden and clay vats with all five recipe families; brine; 4 crystal seeds; the crucible, 3 melts, the casting tray with ingots; windup and furnace drives; the Void Island preset and start island; the properties config; BEI categories; the ModJS `PrimaMateria` global; wiki pages.

**Later, in rough order:** Nether and End chain (scoria, pale sand, star mesh, netherite hammer, quartz and glowstone in bulk, netherite scrap, ender pearls, emerald seed); stamp mill; flux drive; mold shapes; a mob-drop source; per-team islands; a void Nether and End; wood variants of the sieve and vat; JEI, EMI and REI plugins; SMWTI and Jade lines.

## 15. Originality

Prima Materia is a clean-room design. Nobody working on it opens the code, assets, language files or data of any other sifting mod, and nothing here is taken from one. The genre's general ideas (crush stone finer, sift it, compost for dirt, melt stone for lava, start in the void) are common ground; the design, names, numbers, art and code are ours. What is intentionally different from the usual way these mods work:

1. **A load, not a block.** The sieve takes up to 4 blocks in one tray and rewards filling it (1.6 times the efficiency). Sieves do not link to their neighbors.
2. **Hold, do not click.** Sifting is one held button with a steady rhythm, and the sieve reloads from the stack in your hand.
3. **Drops are rolled first and revealed.** You watch the finds appear on the mesh as the material thins, and fines fall through as residue.
4. **Our own material and tier names.** Stone meal, rock chips, plant fiber, metal grains, mineral salt, crystal seeds; fiber, copper, iron and resonant meshes. Four mesh tiers.
5. **No leaf tool, no insects, no cursed water, no mob dolls.** Plant fiber comes from the hammer. There is no special leaf-harvesting tool and no mob-making item.
6. **Vats, not barrels.** A different block with a soak slot, dew collection and five recipe families of our own, with a wooden and a fired version.
7. **Crystals are grown.** Quartz, amethyst, lapis and diamonds come from seeds growing in brine on a timer, four at a time, visible in the vat. The end of the progression is not a dice roll.
8. **Melts are data.** Molten metals are data-defined crucible contents with a color, not registered fluids, so a pack adds a metal with three JSON files.
9. **Casting is its own block.** Metal comes out as sifted grains, is melted, poured into a tray and cooled. There is no crafting of ore pieces into ore blocks.
10. **Mechanical drives.** Automation is a spring you wind and a firebox, placed under the sieve. No energy system is needed.
11. **No screens and no text.** Every block is read from its model and used by hand.
12. **32x art and a keyframed sieve.** Our own textures, Blockbench models and animation system (see `ART.md` and `SIFTING.md`).

It is also different from our own Barely Prospecting, which is hand-only panning with trays, a basin and a lure. Prima Materia is about sieves, vats, casting and automation, and shares no items or ids with it.
