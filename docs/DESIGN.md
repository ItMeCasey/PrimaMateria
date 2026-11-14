# Prima Materia: game design

Mod id `primamateria`. Forge and NeoForge, Minecraft 26.2 and 26.3 only. First release `2026.11.1`.

Prima Materia is a skyblock resource mod. You start on a small island in the void with one tree, and you make every resource in the game by breaking matter down and sorting it: crush it with a hammer, shake it through a sieve, soak and grow it in a barrel, melt and cast it in a crucible. The name is the alchemists' "first matter", the raw stuff everything else is made from, and the look follows it: wood, fired clay, metal bands and an amethyst accent, drawn in the style of vanilla Minecraft textures at 16x.

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
              -> sieve (5 mesh tiers) -> seeds, rock chips, metal dust, salts, crystal seeds
              -> barrel: compost -> dirt | water + stone meal -> clay | brine + seed -> crystals
              -> crucible: stone -> lava | dust -> molten metal -> casting tray -> ingots
              -> drives: sieves that run without you
```

## 3. Progression

Five stages. The stage names are for this document and the wiki; the game never shows them.

| Stage | Goal | What you do | Unlocks |
|---|---|---|---|
| 1. Wood and fiber (0 to 20 min) | First sieve, first dirt, first stone | Hammer leaves for plant fiber. Build a wooden hammer, a sieve and a fiber mesh. Compost leaves and saplings in a wooden barrel for more dirt. Sift dirt for rock chips, seeds and saplings. Craft rock chips into cobblestone. | Cobblestone, farming, stone hammer, furnace |
| 2. Clay and fire (20 to 45 min) | Crucible and copper | Hammer cobblestone down to gravel, sand and stone meal. Sift gravel for flint, coal and copper dust. Let a barrel fill with dew, soak stone meal for clay. Fire a crucible and a casting tray. Melt copper dust, cast ingots. Melt stone for lava. | Copper tools, copper mesh, lava, cobblestone generator, obsidian |
| 3. Metal (45 min to 2 h) | Iron and gold, automation | Copper mesh brings iron dust, salt dust and redstone. Iron mesh brings gold and the first crystal seeds. Build a windup drive, then a furnace drive, and feed sieves with hoppers. | Iron and gold tools, buckets, hoppers, Nether portal, drives |
| 4. Crystal (2 to 3 h) | Diamonds | Steep salt dust in water for brine. Grow quartz, amethyst and lapis from seeds in a barrel. Sift stone meal with the iron mesh for the first diamond seeds, grow diamonds, make the diamond mesh. | Diamonds, diamond mesh, enchanting, powered drive |
| 5. Beyond (3 h on) | Nether and End materials, netherite | Crush netherrack into scoria and end stone into pale sand and sift them. Soak scoria in brine for soul sand and sift that for mob drops. Collect netherite scrap, make the netherite mesh and hammer. Auto hammers crush without you. | Quartz and glowstone in bulk, mob drops, ender pearls, emeralds, netherite |

Later (not in the first release): more mold shapes, per-team islands, a void Nether and End.

## 4. Blocks

All ids are `primamateria:<id>`. "v1" is the first release, `2026.11.1`. "later" is planned and not built for it.

| Id | Name | What it does | Scope |
|---|---|---|---|
| `sieve` | Sieve | Holds one mesh and a load of 1 to 4 blocks of one material. Shaking it (by hand or by a drive below it) turns the load into drops. Section 6. | v1 |
| `wooden_barrel` | Wooden Barrel | Open tub. Holds 1000 mB of a cool fluid or 1000 units of compost. Collects dew and rain. Composting, soaking, steeping, growing. Cannot hold lava. Section 8. | v1 |
| `clay_barrel` | Clay Barrel | Fired barrel. Same as the wooden barrel, and also holds lava, so it can do mixing recipes with a hot fluid. | v1 |
| `crucible` | Crucible | Fired pot. Melts solids over a heat source into lava or molten metal, 4000 mB. Section 9. | v1 |
| `casting_tray` | Casting Tray | Sits next to a crucible. Takes 100 mB of molten metal and cools it into an ingot. | v1 |
| `windup_drive` | Windup Drive | Goes under a sieve. You crank it by hand to store turns; each turn is one shake. Section 7. | v1 |
| `furnace_drive` | Furnace Drive | Goes under a sieve. Burns furnace fuel and shakes the sieve faster than a hand. | v1 |
| `stone_meal` | Stone Meal | The finest crushed stone. A falling block, like sand. Sifted for late materials, soaked for clay. | v1 |
| `scoria` | Scoria | Crushed netherrack, a falling block. Sifted for Nether materials, soaked in brine for soul sand. | v1 |
| `pale_sand` | Pale Sand | Crushed end stone, a falling block. Sifted for End materials. | v1 |
| `auto_hammer` | Auto Hammer | Sits on a drive like a sieve. Holds one hammer and crushes the blocks fed to it. Section 7. | v1 |
| `powered_drive` | Powered Drive | A drive that runs on energy (FE) from any power mod. The fastest drive. | v1 |

Unfired clay pieces are items, not blocks (section 5).

## 5. Items

| Id | Name | What it does | Scope |
|---|---|---|---|
| `wooden_hammer` | Wooden Hammer | Hammer tier 1. Durability 96. | v1 |
| `stone_hammer` | Stone Hammer | Tier 2. Durability 192. | v1 |
| `copper_hammer` | Copper Hammer | Tier 3. Durability 288. Faster than stone. | v1 |
| `iron_hammer` | Iron Hammer | Tier 4. Durability 512. | v1 |
| `diamond_hammer` | Diamond Hammer | Tier 5. Durability 2048. | v1 |
| `cloth_mesh` | Cloth Mesh | Mesh tier 1 (coarse). | v1 |
| `copper_mesh` | Copper Mesh | Mesh tier 2 (medium). | v1 |
| `iron_mesh` | Iron Mesh | Mesh tier 3 (fine). | v1 |
| `diamond_mesh` | Diamond Mesh | Mesh tier 4. | v1 |
| `netherite_mesh` | Netherite Mesh | Mesh tier 5 (finest). | v1 |
| `plant_fiber` | Plant Fiber | From hammered leaves. Makes the fiber mesh; 3 make 1 string. | v1 |
| `rock_chip` | Rock Chip | From sifted dirt. 4 (2 by 2) make 1 cobblestone. | v1 |
| `copper_dust` | Copper Dust | Sifted metal. 20 mB of molten copper each when melted (5 per ingot). | v1 |
| `iron_dust` | Iron Dust | Same, for iron. | v1 |
| `gold_dust` | Gold Dust | Same, for gold. | v1 |
| `salt_dust` | Salt Dust | Sifted from sand. 4 steeped in a barrel of water make brine. | v1 |
| `quartz_seed` | Quartz Seed | Grows into 2 quartz in brine. | v1 |
| `amethyst_seed` | Amethyst Seed | Grows into 2 amethyst shards in brine. | v1 |
| `lapis_seed` | Lapis Seed | Grows into 3 lapis lazuli in brine. | v1 |
| `diamond_seed` | Diamond Seed | Grows into 1 diamond in brine, slowly. | v1 |
| `raw_crucible` | Unfired Crucible | Smelt in a furnace to get the crucible block. | v1 |
| `raw_casting_tray` | Unfired Casting Tray | Smelt to get the casting tray block. | v1 |
| `raw_clay_barrel` | Unfired Clay Barrel | Smelt to get the clay barrel block. | v1 |
| `emerald_seed` | Emerald Seed | Grows into 1 emerald in brine. | v1 |
| `netherite_hammer` | Netherite Hammer | Tier 6. Durability 2560. | v1 |
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
| 1 | `cloth_mesh` | 4 sticks in the corners, 5 plant fiber | 1.0 | Rock chips, seeds, saplings, flint, coal, copper dust, bone meal |
| 2 | `copper_mesh` | 4 sticks in the corners, 5 copper ingots | 1.0 | Iron dust, salt dust, redstone, gunpowder, crops |
| 3 | `iron_mesh` | 4 sticks in the corners, 5 iron ingots | 1.25 | Gold dust, glowstone dust, quartz, amethyst and lapis seeds |
| 4 | `diamond_mesh` | 4 sticks in the corners, 5 diamonds | 1.5 | More diamond seeds, loose lapis, emerald seeds, netherite scrap, ender pearls |
| 5 | `netherite_mesh` | 1 diamond mesh and 1 netherite ingot (shapeless) | 2.0 | Best chances everywhere, shulker shells, wither skeleton skulls |

Meshes have no durability. The cloth mesh is woven from plant fiber. A finer mesh still gives everything the coarser ones give unless a recipe sets a highest tier.

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
| `minecraft:netherrack` | 1 scoria | 3 |
| `minecraft:end_stone` | 1 pale sand | 4 |

Hammer tiers add yield: tiers 3, 4, 5 and 6 give a 10%, 20%, 30% and 40% chance of one extra result item (field `bonus_per_tier` in the config, applied above tier 2).

Recipes: 2 planks over 1 stick over 1 stick for wood; the same shape with cobblestone, copper ingots, iron ingots or diamonds for the others. The netherite hammer is a diamond hammer and a netherite ingot (shapeless).

### Drives

A drive sits directly under a sieve and shakes it. Drives have no screen.

| Drive | Power | Rate | Details |
|---|---|---|---|
| Hand | Hold use | 1 shake per 6 ticks | Reloads from the stack in your hand. |
| `windup_drive` | Hold use on the drive to crank: 1 turn per 5 ticks, stores up to 48 turns, shown by a brass dial on the front that sweeps a quarter circle | 1 shake per 8 ticks, 1 turn per shake | Runs while it has turns and the sieve has a load. Recipe: 4 planks, 3 copper ingots, 1 iron ingot, 1 stick. |
| `furnace_drive` | Furnace fuel, put in by hand or hopper (1 slot, 64 items); the firebox glows while burning | 1 shake per 4 ticks | Burns only while the sieve above has a load. Recipe: 1 furnace, 1 windup drive, 4 iron ingots, 2 gold ingots. |

| `powered_drive` | Energy (FE) through the loader's energy handler, buffer of 10,000 FE; a strip on the front lights in five steps with the charge | 1 shake per 2 ticks, 40 FE per shake | Runs while it has energy and the machine above has work. Recipe: 1 furnace drive, 4 redstone, 2 diamonds, 2 copper ingots. |

A drive works the machine above it: a sieve or an auto hammer.

### Auto hammer

An auto hammer sits on a drive. Use a hammer on it to fit the hammer (sneak-use with an empty hand takes it out); the hammer's tier decides what the auto hammer can crush, and it loses 1 durability per block. Blocks go in by hand or hopper (buffer of 16). One block takes 6 strokes, and one stroke is one drive shake. Results follow the crushing recipes, bonus yield included, and go to the drive's buffer. Without a drive it does nothing.

All drives keep a 9-slot output buffer for drops, residue and crushed blocks. Hoppers and pipes take from the buffer through the bottom and sides. When the buffer is full the sieve stops after the current load. A redstone signal stops a drive.

## 8. Barrel

A barrel holds either one fluid (1000 mB) or compost (1000 units), never both. It has one "soak slot" for the item or block being soaked, steeped or grown. Everything is done by hand in the world or through hoppers and pipes (items in from the top and sides, results out through the bottom; fluids through the loader's fluid handler).

- **Dew and rain.** A barrel with open sky above it gains 1 mB of water every 4 ticks (a full barrel in about 3 minutes 20 seconds) and 8 times that in rain. Config: `barrel.dew_ticks_per_mb`, `barrel.rain_multiplier`. This is how water exists before the first bucket.
- **Buckets and bottles** fill and empty a barrel as usual.

Barrel recipe families:

| Family | Recipe type | What happens | First-release recipes |
|---|---|---|---|
| Composting | `primamateria:composting` | Each organic item adds compost units. At 1000 the barrel matures for 20 seconds, the surface darkening from green to brown, then holds 1 result block, taken by hand or hopper. | Result `minecraft:dirt`. Values: sapling 100, leaves 80, plant fiber 50, seeds 60, wheat 80, apple 100, flower 80, cactus 100, sugar cane 80, rotten flesh 100, bread 160, bone meal 40 (and it halves the maturing time once). Tag-based where a tag exists. |
| Soaking | `primamateria:soaking` | An item or block put into a barrel of fluid turns into something else and uses some fluid. Takes a few seconds. | Water (250 mB) + stone meal = clay block, 5 s. Water (250 mB) + dirt = mud, 5 s. Brine (250 mB) + scoria = soul sand, 5 s. |
| Mixing | `primamateria:mixing` | A second fluid poured into a barrel that holds a fluid makes a block. Uses both. | Clay barrel with 1000 mB lava + 1000 mB water = obsidian. Barrel with 1000 mB water + 1000 mB lava = stone. |
| Steeping | `primamateria:steeping` | An item dissolved in a fluid over time changes the fluid. | 1000 mB water + 4 salt dust = 1000 mB brine, 30 s. |
| Growing | `primamateria:growing` | A seed placed in a barrel of the right fluid grows slowly into a result and uses fluid. Up to 4 seeds grow at once, each on its own timer, each shown as a crystal that gets bigger. | In brine, 250 mB each: quartz seed = 2 quartz, 90 s. Amethyst seed = 2 amethyst shards, 120 s. Lapis seed = 3 lapis lazuli, 120 s. Diamond seed = 1 diamond, 300 s. Emerald seed = 1 emerald, 300 s. |

**Brine** (`primamateria:brine`) is a real fluid with a bucket (`primamateria:brine_bucket`), so pipes and tanks of other mods can move it. It is the only new world fluid in the first release.

## 9. Crucible and casting

- **Heat.** A crucible needs a heat source in the block below it. Heat sources are data (`data/<namespace>/primamateria/heat/<id>.json`: block or block tag, optional block state, heat value). First release: torch 1, campfire 2, magma block 3, fire 3, lava 4, soul campfire 4, soul fire 5.
- **Melting.** Put solids in by hand or hopper (buffer of 4 items). The crucible melts 2 mB per tick per point of heat. It holds one kind of content at a time, 4000 mB.
- **Contents.** The crucible holds either a real fluid (lava) or a **melt**. A melt is a data-defined molten material, not a world fluid (`data/<namespace>/primamateria/melt/<id>.json`: color, glow, name key). Packs add a new metal with a melt file, a melting recipe and a casting recipe, and no Java. First release melts: `primamateria:molten_copper`, `primamateria:molten_iron`, `primamateria:molten_gold`.
- **Lava** leaves by bucket or through the fluid handler (pipes).
- **Casting.** A casting tray placed against the side of a crucible is fed by it: while the tray is empty and the crucible holds at least 100 mB of a melt with a casting recipe, the crucible pours 100 mB (a visible stream), the tray glows and cools for 3 seconds, then holds the ingot, taken by hand or by a hopper below. One crucible feeds up to 4 trays.

| Family | Recipe type | First-release recipes |
|---|---|---|
| Melting | `primamateria:melting` | Cobblestone, stone, gravel = 200 mB lava. Netherrack = 500 mB lava. Copper, iron, gold dust = 20 mB of their melt. Raw copper, iron, gold and their ingots = 100 mB. |
| Casting | `primamateria:casting` | 100 mB molten copper, iron, gold = 1 ingot of that metal, 3 s. |

Crafting recipes for the blocks (rows top to bottom, `.` is empty):

| Result | Shape | Then |
|---|---|---|
| `sieve` | `P.P` / `PSP` / `S.S` (P plank, S stick) | |
| `wooden_barrel` | `P.P` / `P.P` / `PPP` | |
| `raw_clay_barrel` | `C.C` / `C.C` / `CBC` (C clay ball, B clay block) | Smelt to `clay_barrel` |
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
| Copper dust | 1 to 2 | 14% | T1+ |
| Coal | 1 | 9% | T1+ |
| Iron dust | 1 | 10% | T2 only |
| Iron dust | 1 to 2 | 16% | T3+ |
| Gold dust | 1 | 5% | T3+ |

**Sand** (`sifting/sand/*`), residue: stone meal 15%

| Drop | Count | Chance | Mesh |
|---|---|---|---|
| Copper dust | 1 | 8% | T1+ |
| Cactus | 1 | 4% | T1+ |
| Salt dust | 1 | 12% | T2+ |
| Iron dust | 1 | 7% | T2+ |
| Gold dust | 1 | 8% | T3+ |
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
| Diamond seed | 1 | 2% | T3 only |
| Diamond seed | 1 | 4% | T4+ |
| Emerald seed | 1 | 2% | T4+ |

**Scoria** (`sifting/scoria/*`)

| Drop | Count | Chance | Mesh |
|---|---|---|---|
| Quartz | 1 to 2 | 18% | T2+ |
| Glowstone dust | 1 | 10% | T2+ |
| Gold dust | 1 | 10% | T3+ |
| Blaze powder | 1 | 6% | T3+ |
| Nether wart | 1 | 4% | T3+ |
| Netherite scrap | 1 | 1% | T4 only |
| Netherite scrap | 1 | 2.5% | T5+ |

**Soul sand** (`sifting/soul_sand/*`), the mob-drop source

| Drop | Count | Chance | Mesh |
|---|---|---|---|
| Bone | 1 | 15% | T2+ |
| Rotten flesh | 1 | 12% | T2+ |
| String | 1 | 12% | T2+ |
| Gunpowder | 1 | 8% | T2+ |
| Spider eye | 1 | 6% | T2+ |
| Slime ball | 1 | 5% | T3+ |
| Ghast tear | 1 | 1% | T4+ |
| Wither skeleton skull | 1 | 0.3% | T5+ |

**Pale sand** (`sifting/pale_sand/*`)

| Drop | Count | Chance | Mesh |
|---|---|---|---|
| Chorus fruit | 1 | 8% | T4+ |
| Ender pearl | 1 | 6% | T4+ |
| Emerald seed | 1 | 4% | T4+ |
| Shulker shell | 1 | 1.5% | T5+ |

Rough pacing check: one iron ingot is 5 dust. With the copper mesh, gravel gives 0.10 dust per block, so about 50 gravel per ingot by hand (about 4 minutes with full trays); with the iron mesh 0.24 per block, about 21 gravel per ingot. A diamond is one seed (2% of stone meal on the iron mesh, about 50 blocks; 4% on the diamond mesh) plus 5 minutes of growing, and four grow at once. These are the numbers Casey tunes after playing.

## 11. Starting on a void island

- **World preset** `primamateria:void_island`, shown in the world creation screen as "Void Island". The Overworld is empty void with normal biomes for weather and mob rules. The Nether and the End are normal in the first release.
- **Start island** is a structure file, `data/primamateria/structure/start_island.nbt`, placed at the world spawn: a 7 by 7 dirt island 3 blocks deep with a grass top, one oak tree and nothing else. No chest, no starter items: one tree is enough (hammer, sieve, barrel, fiber).
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
| `primamateria:composting` | Barrel | `result` (block). Compost values are a data map, below. |
| `primamateria:soaking` | Barrel | `fluid`, `amount`, `input`, `result`, `time` |
| `primamateria:mixing` | Barrel | `base_fluid`, `added_fluid`, `amount`, `result`, `needs_heatproof` |
| `primamateria:steeping` | Barrel | `fluid`, `input`, `count`, `result_fluid`, `time` |
| `primamateria:growing` | Barrel | `fluid`, `amount`, `seed`, `result`, `count`, `time` |
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

**Scripting and packs.** Because every recipe, drop, mesh tier, heat source, melt and compost value is a datapack file, datapacks and KubeJS add, change and remove them directly. ModJS scripts get a `PrimaMateria` global (events, reads, content calls), and KubeJS gets the same events and reads through a helper script. Both are optional. Details and the per-version availability table are in `ARCHITECTURE.md` sections 4 and 5.

**Recipe viewers.** BEI, JEI, REI and EMI each get six categories (sifting, crushing, composting, barrel, melting, casting) and an information page per siftable block with its full drop table by mesh tier. SMWTI and Jade show block state in the world.

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
| `drive.powered_ticks_per_shake` | 2 | |
| `drive.powered_fe_per_shake` | 40 | |
| `drive.powered_capacity` | 10000 | |
| `auto_hammer.strokes_per_block` | 6 | |
| `barrel.dew_ticks_per_mb` | 4 | 0 switches dew off |
| `barrel.rain_multiplier` | 8 | |
| `barrel.time_multiplier` | 1.0 | Scales compost, soak, steep and grow times |
| `crucible.mb_per_tick_per_heat` | 2 | |
| `start.island` | `preset_only` | `preset_only`, `always`, `never` |
| `client.animations` | `full` | `full`, `reduced`, `off` (see `SIFTING.md`) |
| `client.particles` | `true` | |

## 14. First release versus later

**In `2026.11.1`:** the sieve with 5 meshes and the full animation; 6 hammers and the crushing chain to stone meal, scoria and pale sand; soul sand sifting for mob drops; the auto hammer; the powered drive; wooden and clay barrels with all five recipe families; brine; 5 crystal seeds; the crucible, 3 melts, the casting tray with ingots; windup and furnace drives; the Void Island preset and start island; the properties config; BEI, JEI, REI and EMI categories and drop table information pages (each where a build exists for the version and loader); SMWTI and Jade lines; KubeJS support; the ModJS `PrimaMateria` global; wiki pages.

**Later:** mold shapes; per-team islands; a void Nether and End; wood variants of the sieve and barrel.

## 15. Originality

Prima Materia is a clean-room design. Nobody working on it opens the code, assets, language files or data of any other sifting mod, and nothing here is taken from one. The genre's general ideas (crush stone finer, sift it, compost for dirt, melt stone for lava, start in the void) are common ground; the design, names, numbers, art and code are ours. What is intentionally different from the usual way these mods work:

1. **A load, not a block.** The sieve takes up to 4 blocks in one tray and rewards filling it (1.6 times the efficiency). Sieves do not link to their neighbors.
2. **Hold, do not click.** Sifting is one held button with a steady rhythm, and the sieve reloads from the stack in your hand.
3. **Drops are rolled first and revealed.** You watch the finds appear on the mesh as the material thins, and fines fall through as residue.
4. **Our own material and tier names.** Stone meal, rock chips, plant fiber, metal dust, salt dust, crystal seeds; scoria, pale sand. Five mesh tiers: cloth, copper, iron, diamond, netherite.
5. **No leaf tool, no insects, no cursed water, no mob dolls.** Plant fiber comes from the hammer. There is no special leaf-harvesting tool and no mob-making item; mob drops are sifted from soul sand.
6. **Our own barrel.** An open tub with a soak slot, dew collection and five recipe families of our own, with a wooden and a fired version.
7. **Crystals are grown.** Quartz, amethyst, lapis and diamonds come from seeds growing in brine on a timer, four at a time, visible in the barrel. The end of the progression is not a dice roll.
8. **Melts are data.** Molten metals are data-defined crucible contents with a color, not registered fluids, so a pack adds a metal with three JSON files.
9. **Casting is its own block.** Metal comes out as sifted dust, is melted, poured into a tray and cooled. There is no crafting of ore pieces into ore blocks.
10. **Mechanical drives.** Automation is a spring you wind and a firebox, placed under the sieve. No energy system is needed; the powered drive is there for packs that have one.
11. **No screens and no text.** Every block is read from its model and used by hand.
12. **Vanilla look and a keyframed sieve.** Blocks built from Minecraft's own textures, 16x16 items in the Minecraft style, Blockbench models and animation system (see `ART.md` and `SIFTING.md`).

It is also different from our own Barely Prospecting, which is hand-only panning with trays, a basin and a lure. Prima Materia is about sieves, barrels, casting and automation, and shares no items or ids with it.
