# Prima Materia: art direction

One sentence: **an alchemist's workshop built by a careful carpenter** — aged wood, fired clay and fieldstone, held together with brass, with one violet crystal accent for the late game.

Everything is drawn by us. No texture, model or sound is taken or traced from another mod.

## 1. Resolution

- Block and item textures are **32x32** (2 texture pixels per model unit). Vanilla is 16x.
- Models are made in Blockbench (`java_block` format, per-face UV, no box UV) with the texture size set to 32 for a 16-unit UV space. Elements snap to half units (1 texture pixel).
- The house style reference is the Barely Crafting station (`BarelyCrafting/branding/models/station.bbmodel`): 18 elements, a stone foot with studs, a column, a slab with a rim and metal corner posts, and three small shared material textures (stone, wood, metal). Prima Materia keeps that construction language (foot, body, rim, corner hardware, studs) and doubles the texture resolution.

## 2. Palette

One palette for the whole mod. Each material is a ramp of five tones, darkest to lightest. Tone 3 is the base color.

| Material | 1 shadow | 2 | 3 base | 4 | 5 highlight | Used for |
|---|---|---|---|---|---|---|
| Aged wood | `#3b2a1c` | `#5a4028` | `#7d5c38` | `#a37f4f` | `#c8a56e` | Sieve frame and tray, wooden vat, hammer handles |
| Dark wood | `#231a14` | `#38291d` | `#4f3a28` | `#684e36` | `#846848` | Inner faces, end grain, shadowed rails |
| Fieldstone | `#3d3f48` | `#555862` | `#71747e` | `#9396a0` | `#b6b8c0` | Drive bases, stone hammer, stone meal |
| Fired clay | `#6b3526` | `#8f4a35` | `#b0654a` | `#cb8563` | `#e0a888` | Crucible, casting tray, clay vat |
| Raw clay | `#6f7480` | `#8a909c` | `#a4abb6` | `#bfc5ce` | `#d9dde3` | Unfired pieces |
| Brass | `#5a4318` | `#8a6a24` | `#b8923a` | `#dcbb5c` | `#f3df94` | Corner brackets, rivets, bands, dials. The signature trim |
| Copper | `#6a3423` | `#9b4f32` | `#c56f45` | `#e39766` | `#f5c29a` | Copper mesh, hammer, grains |
| Iron | `#3a3d45` | `#5b5f69` | `#858a94` | `#b1b5bd` | `#dadde2` | Iron mesh, hammer, firebox, grains |
| Gold | `#7a5410` | `#b07d18` | `#dba928` | `#f2cf55` | `#fbeba0` | Gold grains, resonant mesh wire |
| Fiber | `#5a5930` | `#7c7a42` | `#a19e58` | `#c2bf7c` | `#dedba8` | Fiber mesh, plant fiber |
| Resonant violet | `#2e2350` | `#4c3a82` | `#7458b8` | `#a183e0` | `#cdb8f5` | Amethyst, resonant mesh, crystal seeds. The accent |
| Brine teal | `#1d4a55` | `#2c6f7a` | `#45979c` | `#78c0bd` | `#b5e3db` | Brine, mineral salt tint |
| Ember | `#7a1f0c` | `#b83a12` | `#e8691c` | `#f8a23a` | `#fdd97a` | Melts, firebox glow, cooling ingots |

The stone and wood ramps start from the same hues as the Barely Crafting station textures, so the studio's mods sit together.

## 3. Shading rules

1. **Light comes from the top left.** Top and left edges of a shape take tone 4 or 5, bottom and right edges take tone 2 or 1.
2. **Five tones per material, no more.** No gradients, no dithering, no airbrush, no noise filter. Soft shading means wide bands of neighboring tones, not blur.
3. **Hue shifts with value.** Shadows lean cool (toward violet), highlights lean warm (toward yellow). The ramps above already do this; do not darken by adding black.
4. **Clusters, not specks.** Detail is drawn in clusters of at least 2 by 2 pixels. Single stray pixels are only for rivet glints.
5. **One-pixel bevel on every part.** Each face of a model part has a tone 4 line on its top and left edge and a tone 2 line on its bottom and right, so parts read as separate pieces even when they are the same material.
6. **Contact shadow.** Where one part sits on another, the lower part gets a 1 px tone 1 line under the joint.
7. **Brass is the only shiny thing.** Brass and the other metals get one tone 5 glint per piece. Wood, clay and stone are matte.
8. **Emissive is rare.** Only melts, the firebox, cooling ingots and growing crystals glow, and only with a small emissive layer.

## 4. Staying readable next to vanilla

- **Same values and saturation as vanilla.** Base tones match the brightness of vanilla oak planks, stone and terracotta, so our blocks do not look brighter or grayer than the room they are in.
- **Big shapes first.** At 32x the temptation is fine detail. Every texture must still read when viewed at half size: squint test, then downscale to 16x and check it looks like a plausible vanilla-style texture.
- **Vanilla-sized features.** Wood grain bands are 4 px wide (2 px at vanilla scale), planks are 8 px high, stones are 6 to 10 px across. Features are never thinner than 2 px except rivets and glints.
- **The loaded material is the real block.** The layer in a sieve, the compost in a vat and the solids in a crucible use the textures of the actual blocks and items, at whatever resolution they are.
- **Silhouette.** Each block has a distinct outline from ten blocks away: the sieve is a table with a hanging tray, the vat is a wide banded tub, the crucible is a pot on three feet, the tray is flat with a lip, the drives are squat boxes with a key or a firebox door.
- **Full blocks stay simple.** Stone meal, the one plain cube, is a calm texture that tiles without a visible grid.

## 5. Model style

- More parts than vanilla, in the station's language: a base or feet, a body, a rim, corner hardware, studs or rivets. Target 20 to 40 elements per block.
- Layered materials: wood with brass bands and brackets; clay with an iron hoop; stone with brass plates.
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
| `wooden_vat.bbmodel` | `wooden_vat` | 28 | Wide tub of 12 staves (3 per side), two brass bands with rivets, thick rim, flat base on two skids; inner surface locator for fluid, compost and crystals | ripple, mature, grow |
| `clay_vat.bbmodel` | `clay_vat` | 22 | Same footprint in fired clay, thicker wall, one iron hoop, rolled lip | ripple, grow |
| `crucible.bbmodel` | `crucible` | 24 | Pot on three stub feet, waist band, flared rim, four pouring lips (one per side); inner surface locator | surface, pour |
| `casting_tray.bbmodel` | `casting_tray` | 14 | Low clay tray (5 units high) with an ingot-shaped hollow, a notch on the fed side, four tiny feet | fill, cool |
| `windup_drive.bbmodel` | `windup_drive` | 30 | Stone base, wooden case, brass plate with a dial on the front, large brass key on one side, two cams on top that meet the sieve | key, dial, cams |
| `furnace_drive.bbmodel` | `furnace_drive` | 32 | Stone base, iron firebox with a hinged grate door, brass banding, stub chimney at the back, two cams on top | door, glow, cams |
| `crystal.bbmodel` | (drawn inside vats) | 6 per crystal, 4 kinds by texture | A cluster of 3 prisms on a small base, scaled 0.2 to 1.0 as it grows | none (scaled by the renderer) |

Plain cube: `stone_meal` (one texture on all sides).

## 7. Textures to make

All block and item textures are 32x32 PNG unless a size is given. Shared material sheets are used by several models, as the station does with its stone, wood and metal textures.

### Shared material sheets (`textures/block/`)

| File | Size | Content |
|---|---|---|
| `mat_wood.png` | 32x32 | Aged wood planks, grain along X |
| `mat_wood_end.png` | 32x32 | End grain and inner faces (dark wood) |
| `mat_stone.png` | 32x32 | Fieldstone, dressed |
| `mat_clay.png` | 32x32 | Fired clay with faint throwing lines |
| `mat_brass.png` | 32x32 | Brass sheet with bracket, band and rivet tiles |
| `mat_iron.png` | 32x32 | Iron sheet, grate bars, hoop |

### Block detail textures (`textures/block/`)

| File | Size | Content |
|---|---|---|
| `sieve_tray.png` | 32x32 | Tray boards and lip with corner rivets |
| `mesh_fiber.png`, `mesh_copper.png`, `mesh_iron.png`, `mesh_resonant.png` | 32x32 each, with transparency | Weave: fiber is a wide knotted grid (4 px holes), copper a square grid (3 px), iron a tight grid (2 px), resonant a diagonal gold grid with a violet bead at every other crossing |
| `sift_material.png` | 32x32 | Neutral grainy texture, used tinted when animations are off |
| `vat_inside.png` | 32x32 | Wet dark wood |
| `compost_fresh.png`, `compost_ripe.png` | 32x32 each | Green-brown plant matter; dark crumbly earth |
| `brine_still.png`, `brine_flow.png` | 32x512 and 64x1024 animated strips | Brine fluid, teal with slow white flecks |
| `melt.png` | 32x512 animated strip, gray | Molten surface, tinted by the melt's color; emissive |
| `crucible_inside.png` | 32x32 | Glazed, soot-dark clay |
| `casting_tray_top.png` | 32x32 | Hollow, notch and lip |
| `drive_dial.png` | 32x32 | Brass dial face with five tick marks and no numbers |
| `drive_firebox.png`, `drive_firebox_lit.png` | 32x32 each | Grate door, dark and glowing (lit is emissive) |
| `crystal_quartz.png`, `crystal_amethyst.png`, `crystal_lapis.png`, `crystal_diamond.png` | 32x32 each | Prism faces in each stone's ramp |
| `stone_meal.png` | 32x32 | Pale gray flour-fine stone, tiling |

### Item textures (`textures/item/`)

| File | Content |
|---|---|
| `wooden_hammer.png`, `stone_hammer.png`, `copper_hammer.png`, `iron_hammer.png`, `diamond_hammer.png` | A short-handled mallet with a large square head, brass collar where the head meets the handle; same outline for all five, head in each material |
| `fiber_mesh.png`, `copper_mesh.png`, `iron_mesh.png`, `resonant_mesh.png` | A square wooden hoop seen at a slight angle with the weave inside |
| `plant_fiber.png` | A twisted hank of green-tan fiber |
| `rock_chip.png` | Three angular chips |
| `copper_grains.png`, `iron_grains.png`, `gold_grains.png` | A small heap of rounded grains; same outline, three ramps |
| `mineral_salt.png` | A heap of pale teal-white cubes |
| `quartz_seed.png`, `amethyst_seed.png`, `lapis_seed.png`, `diamond_seed.png` | One small faceted seed crystal with a tone 5 glint; same outline, four ramps |
| `raw_crucible.png`, `raw_casting_tray.png`, `raw_clay_vat.png` | The fired shapes in raw clay gray, flat 2D |
| `brine_bucket.png` | Bucket with teal fill |

### Particles (`textures/particle/`)

| File | Size | Content |
|---|---|---|
| `fines_0.png` to `fines_2.png` | 8x8 each | 1 px, 2 px and L-shaped specks, white, tinted at runtime |
| `sift_dust_0.png` to `sift_dust_3.png` | 16x16 each | A soft puff in four fading frames, white, tinted |
| `pour.png` | 8x32 | Molten stream segment, tinted, emissive |

### Counts

8 Blockbench models, 6 shared sheets, 22 block detail textures, 23 item textures, 8 particle textures. Block items (sieve, vats, crucible, tray, drives, stone meal) use their block models in the inventory and need no item texture.

## 8. Icon

House style (see `branding/icon` in Barely Crafting): 32x32 pixel art drawn on a dark navy rounded square (`#1a1830` fill, `#3a3658` 1 px border, corners cut by 2 px), exported as `icon.svg` and PNG at 32, 64, 128, 256, 512 and 1024 with crisp edges.

**Concept: the sieve tray caught mid-shake.** A wooden tray with brass corners, seen from the front and slightly above, tilted 10 degrees. A low mound of gray stone meal sits in it. Three thin streams of specks fall from the mesh toward the bottom of the icon, and among them one violet crystal seed falls with a single white glint. The tray uses the aged wood ramp, the corners brass, the streams fieldstone tones 4 and 5, the seed resonant violet tones 3 to 5. Nothing else: no text, no border art.

It reads at 32 px as "tray, falling grains, one purple gem", which is the mod in one picture: plain matter goes in, something precious falls out.

Banners and the social image follow the sibling mods (`branding/source/banners.html` rendered by `render_banners.py`, Silkscreen font, same navy and gold frame), with the violet accent replacing gold in the title underline.

## 9. Review steps for art

1. Every texture passes the downscale check (section 4) before it is committed.
2. Every model is shown in three frames from a real client: placed between vanilla oak planks, stone and terracotta in daylight; in the hand; in an inventory slot.
3. One frame of all blocks in a row at night with a torch, to check the emissive parts and that nothing else glows.
4. Casey approves the sieve model and the palette first (one frame set), before the other seven models are made.
