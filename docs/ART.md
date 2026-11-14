# Prima Materia: art direction

**The textures look like Minecraft textures.** (Casey, 2026-11-13.) Prima Materia is a Minecraft mod, so its blocks and items must look as if Mojang drew them: the same materials, the same colors, the same way of shading, at the same 16x16 size, and wherever possible the very same textures. Placed next to oak planks, stone, terracotta, a cauldron or an anvil, nothing of ours should look like it came from another game or another art style.

The theme inside that rule: an alchemist's workshop built by a careful carpenter. Wood, fired clay and stone, held together with metal bands, with amethyst as the one accent for the late game.

Everything is drawn by us. No texture, model or sound is taken or traced from another mod. Vanilla textures are the style reference and the color source; they are not copied into our files.

## 1. Resolution and where textures come from

Decided by Casey on 2026-11-13: **vanilla textures for the blocks, 16x16 for everything we draw.**

- **Blocks use Minecraft's own textures, by reference.** The sieve, barrels, crucible, casting tray, drives and auto hammer are models whose faces point at `minecraft:block/oak_planks`, `stripped_oak_log`, `barrel_side`, `terracotta`, `smooth_stone`, `iron_block`, `gold_block`, `blast_furnace_front` and so on. Nothing is copied into the mod. The blocks are Minecraft's pixels exactly, and they follow whatever resource pack the player uses.
- **We draw only what Minecraft has no texture for,** at 16x16: the 26 items, the 5 mesh textures, the 3 crushed blocks (stone meal, scoria, pale sand), the brine fluid and the particles. Each starts from its nearest vanilla relative: hammers from the shovel, pickaxe and mace; dust and salt from sugar and raw ore; seeds from the diamond, emerald and amethyst shard; unfired clay from the clay ball and flower pot; crushed blocks from gravel, sand, netherrack and end stone.
- Models are made in Blockbench (`java_block` format, per-face UV, no box UV) at the vanilla texture size of 16. Elements snap to whole or half units.
- The construction reference is the Barely Crafting station (`BarelyCrafting/branding/models/station.bbmodel`): a foot, a body, a rim, corner hardware. Prima Materia keeps that language and stays as plain as the vanilla cauldron, composter, grindstone and lectern.
- `scripts/make_placeholders.py` writes simple stand-in models from vanilla textures so the game is playable now; `scripts/make_textures.py` draws our 16x16 textures and can print a review sheet with the vanilla textures beside ours.

## 2. Palette

**Colors come from vanilla.** Each material's five tones are sampled from the vanilla texture of the same material, not invented:

| Our material | Sample the ramp from | Used for |
|---|---|---|
| Wood | `oak_planks`, `oak_log` (bark for dark parts), `stripped_oak_log` | Sieve frame and tray, wooden barrel, hammer handles, auto hammer |
| Dark wood | `spruce_planks`, `barrel_side` | Inner faces, end grain, shadowed rails |
| Stone | `stone`, `smooth_stone`, `cobblestone` | Drive bases, stone hammer, trough |
| Fired clay | `terracotta`, `bricks`, `flower_pot` | Crucible, casting tray, clay barrel |
| Raw clay | `clay` | Unfired pieces |
| Band metal ("brass" in these docs) | `gold_block` and `bell_side`, slightly darkened | Corner brackets, rivets, bands, dials. The signature trim |
| Copper | `copper_block`, `cut_copper` | Copper mesh, hammer, dust |
| Iron | `iron_block`, `anvil`, `cauldron_side`, `iron_bars` | Iron mesh, hammer, firebox, hoops, dust |
| Gold | `gold_block`, `raw_gold` | Gold dust |
| Diamond | `diamond_block`, `diamond` item | Diamond mesh, hammer, seed |
| Netherite | `netherite_block` | Netherite mesh and hammer |
| Fiber and cloth | `hay_block_side`, `white_wool` tinted, `string` item | Cloth mesh, plant fiber |
| Amethyst | `amethyst_block`, `amethyst_cluster` | Crystal seeds, crystals, the powered drive strip. The accent |
| Brine | `water_still` shifted toward teal | Brine, salt dust tint |
| Ember | `lava_still`, `magma` | Molten metals, firebox glow, cooling ingots |
| Crushed blocks | `gravel`, `sand`, `netherrack`, `end_stone` | Stone meal, scoria, pale sand: each is a finer-grained version of its source block |

The table below is a first approximation of those ramps, darkest to lightest (tone 3 is the base). The artist replaces each row with the values actually sampled from the vanilla texture before drawing; when the two differ, vanilla wins.

| Material | 1 shadow | 2 | 3 base | 4 | 5 highlight |
|---|---|---|---|---|---|
| Wood | `#3b2a1c` | `#5a4028` | `#7d5c38` | `#a37f4f` | `#c8a56e` |
| Dark wood | `#231a14` | `#38291d` | `#4f3a28` | `#684e36` | `#846848` |
| Stone | `#3d3f48` | `#555862` | `#71747e` | `#9396a0` | `#b6b8c0` |
| Fired clay | `#6b3526` | `#8f4a35` | `#b0654a` | `#cb8563` | `#e0a888` |
| Raw clay | `#6f7480` | `#8a909c` | `#a4abb6` | `#bfc5ce` | `#d9dde3` |
| Band metal | `#5a4318` | `#8a6a24` | `#b8923a` | `#dcbb5c` | `#f3df94` |
| Copper | `#6a3423` | `#9b4f32` | `#c56f45` | `#e39766` | `#f5c29a` |
| Iron | `#3a3d45` | `#5b5f69` | `#858a94` | `#b1b5bd` | `#dadde2` |
| Gold | `#7a5410` | `#b07d18` | `#dba928` | `#f2cf55` | `#fbeba0` |
| Fiber | `#5a5930` | `#7c7a42` | `#a19e58` | `#c2bf7c` | `#dedba8` |
| Amethyst | `#2e2350` | `#4c3a82` | `#7458b8` | `#a183e0` | `#cdb8f5` |
| Brine | `#1d4a55` | `#2c6f7a` | `#45979c` | `#78c0bd` | `#b5e3db` |
| Ember | `#7a1f0c` | `#b83a12` | `#e8691c` | `#f8a23a` | `#fdd97a` |

## 3. Shading rules

These are the habits of the current vanilla textures, written down so every texture follows them.

1. **Light comes from the top left.** Top and left edges of a shape take tone 4 or 5, bottom and right edges take tone 2 or 1.
2. **Few tones per material.** Five or fewer, like vanilla. No gradients, no dithering fills, no airbrush, no noise filter, no photo texture.
3. **Hue shifts with value.** Shadows lean cooler and slightly more saturated, highlights lean warmer, as vanilla ramps do. Never darken by adding black.
4. **Clusters, not specks.** Detail is drawn the way vanilla does at 16x: every pixel is placed on purpose.
5. **Vanilla's own motifs.** Planks have vanilla plank seams and nail dots. Metal bands look like the bands of the vanilla barrel and the rim of the cauldron. Clay looks like terracotta and the flower pot. Stone looks like smooth stone. When vanilla already draws a thing, draw it the vanilla way.
6. **One-pixel bevel on separate parts.** A tone 4 line on the top and left edge and a tone 2 line on the bottom and right, so parts read as separate pieces.
7. **Contact shadow.** Where one part sits on another, the lower part gets a 1 px tone 1 line under the joint.
8. **Metals have a glint, everything else is matte.** One tone 5 glint per metal piece.
9. **Emissive is rare.** Only molten metals, the firebox, cooling ingots and growing crystals glow.

## 4. The vanilla test

Every texture must pass all four before it is committed:

1. **Side by side.** Rendered in the game between the vanilla blocks of the same material (wood next to oak planks, clay next to terracotta, iron next to an anvil and a cauldron), it reads as the same material.
2. **Same size.** It is 16x16, with the same amount of detail per pixel as the vanilla texture beside it: no filters, no gradients, no anti-aliasing.
3. **Vanilla shapes.** Items sit in the slot the way vanilla items do: tools run from the lower left to the upper right, heaps and gems are centered, everything has a dark outline of its own hue.
4. **Same values and saturation.** Base tones match the brightness and saturation of the vanilla texture they were sampled from, so our blocks are neither brighter nor grayer than the room they are in.

Also:

- **The loaded material is the real block.** The layer in a sieve, the compost in a barrel and the solids in a crucible use the textures of the actual blocks and items.
- **Silhouette.** Each block has a distinct outline from ten blocks away: the sieve is a table with a hanging tray, the barrel is a wide banded tub, the crucible is a pot on three feet, the tray is flat with a lip, the drives are squat boxes with a key, a firebox door or a charge strip, the auto hammer is a frame with two stamps.
- **Full blocks stay simple.** Stone meal, scoria and pale sand are calm textures that tile without a visible grid, each clearly the finer cousin of gravel, netherrack and end stone.
- **Items look like vanilla items.** Dark outline on the lower right, lighter on the upper left, the same viewing angle as vanilla tools and materials. Hammers are held like vanilla tools.

The first pass of every item, mesh and crushed block texture is in the repository, drawn by `scripts/make_textures.py`. Casey reviews them next to vanilla; each one he rejects is redrawn by hand in Blockbench.

## 5. Model style

- Every face of a model block uses a vanilla texture (section 1).
- A little more modeled than a vanilla block, in the station's language and never busier than the vanilla grindstone, lectern or brewing stand: a base or feet, a body, a rim, corner hardware, studs or rivets. Target 20 to 40 elements per block.
- Layered materials, the way the vanilla barrel, cauldron, lectern and grindstone combine them: wood with metal bands and brackets; clay with an iron hoop; stone with metal plates.
- Rivets are 0.5 by 0.5 by 0.5 unit cubes in brass, placed at bracket corners. Corner brackets are 2 by 2 units, 0.5 thick, standing proud of the wood.
- Wood parts are slightly inset from the frame's corner posts so the posts read as posts.
- Elements stay inside the block's 16 units and use only the rotations the block model format allows (22.5 degree steps on one axis). Parts that the renderer animates are free of that limit but stay inside the block too, including at the far end of a swing.
- Every block gets a selection shape that follows the model, hand and inventory display transforms, and a matching item.
- Sources live in `branding/models/<name>.bbmodel`. Exports go to `common/src/main/resources/assets/primamateria/`.

## 6. Models to make

All made in Blockbench. "Anim" lists the animations inside the file (see `SIFTING.md` for the sieve).

| Model file | Block | Elements (target) | Description | Anim |
|---|---|---|---|---|
| `sieve.bbmodel` | `sieve` | 34 (16 frame, 18 moving) | Four legs with wooden feet, top rails, brass corner brackets with rivets, four straps; hanging tray with a lip, brass rivets on the tray corners, mesh plane with a 1 unit sag skirt | 8 clips |
| `wooden_barrel.bbmodel` | `wooden_barrel` | 28 | Wide tub of 12 staves (3 per side), two brass bands with rivets, thick rim, flat base on two skids; inner surface locator for fluid, compost and crystals | ripple, mature, grow |
| `clay_barrel.bbmodel` | `clay_barrel` | 22 | Same footprint in fired clay, thicker wall, one iron hoop, rolled lip | ripple, grow |
| `crucible.bbmodel` | `crucible` | 24 | Pot on three stub feet, waist band, flared rim, four pouring lips (one per side); inner surface locator | surface, pour |
| `casting_tray.bbmodel` | `casting_tray` | 14 | Low clay tray (5 units high) with an ingot-shaped hollow, a notch on the fed side, four tiny feet | fill, cool |
| `windup_drive.bbmodel` | `windup_drive` | 30 | Stone base, wooden case, brass plate with a dial on the front, large brass key on one side, two cams on top that meet the sieve | key, dial, cams |
| `furnace_drive.bbmodel` | `furnace_drive` | 32 | Stone base, iron firebox with a hinged grate door, brass banding, stub chimney at the back, two cams on top | door, glow, cams |
| `crystal.bbmodel` | (drawn inside barrels) | 6 per crystal, 5 kinds by texture | A cluster of 3 prisms on a small base, scaled 0.2 to 1.0 as it grows | none (scaled by the renderer) |

| `auto_hammer.bbmodel` | `auto_hammer` | 30 | Wooden frame with two brass-shod stamps over a stone trough, a clamp holding the fitted hammer head, cam followers at the base | stamp, idle |
| `powered_drive.bbmodel` | `powered_drive` | 30 | Stone base, iron case with brass banding, a violet charge strip on the front, a coil at the back, two cams on top | strip, cams |

Plain cubes: `stone_meal`, `scoria`, `pale_sand` (one texture on all sides).

## 7. Textures to make

All 16x16 PNG. Model blocks need none: they use vanilla textures by reference (section 1).

### Block textures (`textures/block/`)

| File | Content | Vanilla relative |
|---|---|---|
| `mesh_cloth.png`, `mesh_copper.png`, `mesh_iron.png`, `mesh_diamond.png`, `mesh_netherite.png` | The weave on a transparent ground: cloth a wide woven grid, copper and iron a finer woven grid of the same hole size, diamond a diagonal grid, netherite a dark diagonal grid with gold crossings | `iron_bars`, `cobweb`, `string` |
| `stone_meal.png` | Pale gray, sand-fine | `gravel`, `sand` |
| `scoria.png` | Netherrack broken small | `netherrack`, `gravel` |
| `pale_sand.png` | End stone as sand | `end_stone`, `sand` |
| `brine_still.png`, `brine_flow.png` | Animated strips like water, teal | `water_still`, `water_flow` |
| `molten_metal_still.png`, `molten_metal_flow.png` | Animated gray molten surface, tinted per metal (copper, iron, gold) by the fluid's color; emissive | `lava_still`, `lava_flow` |
| `compost.png` | Used for a barrel's compost, tinted from green to brown as it matures | `composter_compost` |
| `crystal_quartz.png`, `crystal_amethyst.png`, `crystal_lapis.png`, `crystal_diamond.png`, `crystal_emerald.png` | Small crystal clusters for the growing model | `amethyst_cluster`, `small_amethyst_bud` |

Where a vanilla texture already does the job it is used directly and no file is made: the compost can be `minecraft:block/composter_compost`, the amethyst crystal can be `minecraft:block/amethyst_cluster`.

### Item textures (`textures/item/`)

| File | Content | Vanilla relative |
|---|---|---|
| `wooden_hammer.png`, `stone_hammer.png`, `copper_hammer.png`, `iron_hammer.png`, `diamond_hammer.png`, `netherite_hammer.png` | A mallet held like a vanilla tool: stick handle from the lower left, a block head at the upper right. One outline, six head materials | Shovel, pickaxe, mace |
| `cloth_mesh.png`, `copper_mesh.png`, `iron_mesh.png`, `diamond_mesh.png`, `netherite_mesh.png` | A square oak hoop with the weave inside | Item frame, painting, string |
| `plant_fiber.png` | A tied hank of green fiber | Wheat, kelp |
| `rock_chip.png` | Three angular chips | Flint |
| `copper_dust.png`, `iron_dust.png`, `gold_dust.png` | A small heap of dust in each raw ore's colors | Sugar, gunpowder, raw ores |
| `salt_dust.png` | A pale heap | Sugar |
| `quartz_seed.png`, `amethyst_seed.png`, `lapis_seed.png`, `diamond_seed.png`, `emerald_seed.png` | One small faceted crystal | Diamond, emerald, amethyst shard |
| `raw_crucible.png`, `raw_casting_tray.png`, `raw_clay_barrel.png` | The shapes in clay gray | Clay ball, flower pot, brick |
| `brine_bucket.png` | Bucket with teal fill | Water bucket |

### Particles (`textures/particle/`)

`fines_0` to `fines_2` (8x8 specks), `sift_dust_0` to `sift_dust_3` (8x8 puff frames), `pour` (4x16 stream). White, tinted at runtime.

### Counts

10 Blockbench models that use vanilla textures; about 17 block textures of ours; 26 item textures; 8 particle textures.

## 8. Icon

House style (see `branding/icon` in Barely Crafting): 32x32 pixel art drawn on a dark navy rounded square (`#1a1830` fill, `#3a3658` 1 px border, corners cut by 2 px), exported as `icon.svg` and PNG at 32, 64, 128, 256, 512 and 1024 with crisp edges.

**Concept: the sieve tray caught mid-shake.** A wooden tray with brass corners, seen from the front and slightly above, tilted 10 degrees. A low mound of gray stone meal sits in it. Three thin streams of specks fall from the mesh toward the bottom of the icon, and among them one violet crystal seed falls with a single white glint. The tray uses the wood ramp, the corners the band metal, the streams stone tones 4 and 5, the seed amethyst tones 3 to 5. It is drawn like a vanilla item sprite. Nothing else: no text, no border art.

It reads at 32 px as "tray, falling dust, one purple gem", which is the mod in one picture: plain matter goes in, something precious falls out.

Banners and the social image follow the sibling mods (`branding/source/banners.html` rendered by `render_banners.py`, Silkscreen font, same navy and gold frame), with the violet accent replacing gold in the title underline.

## 9. Review steps for art

1. Every texture passes the vanilla test (section 4) before it is committed.
2. Every model is shown in three frames from a real client: placed between vanilla oak planks, stone and terracotta in daylight; in the hand; in an inventory slot.
3. One frame of all blocks in a row at night with a torch, to check the emissive parts and that nothing else glows.
4. Casey approves the sieve model and the palette first (one frame set), before the other nine models are made.
