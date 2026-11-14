# Prima Materia: the sifting experience

Sifting is the action a player repeats thousands of times, so it gets the most care in the mod. The goal: a sieve that is satisfying to hold the button on, that tells you everything by looking at it, and that still runs well when a base has two hundred of them.

No text is involved anywhere. No progress bar, no overlay, no tooltip.

## 1. What the player sees, start to finish

One block, by hand, about 2.4 seconds. Linked sieves (`sieve.link`, DESIGN.md section 6) all do this at the same moment, each with its own mesh, drops and layer.

| Moment | Time | What happens |
|---|---|---|
| Load | 0.25 s | The block lands in the tray: the material layer rises from nothing to its height with an ease-out and a small overshoot (it "plops"), the tray dips 0.5 px and comes back, a soft thud plays, 4 to 6 dust specks puff up from the surface. |
| First shake | 0.3 s | The tray slides to one side and back. The layer lags a little behind the tray (it is loose material), so the surface slumps toward the trailing side and levels out again. |
| Shaking | every 0.2 s | Tray swings left, right, left. The swing is not a sine wave: it accelerates fast and stops hard at each end (ease-in, then a sharp stop), which is what makes shaking look like shaking. On each stop: a rasp sound, a burst of fines under the mesh, and the layer drops a step. |
| Thinning | whole block | The layer gets thinner with progress. Its surface texture is the block's own texture, so gravel looks like gravel. The thinner it gets, the more of the mesh shows through at the edges first (the layer is drawn slightly domed: thick in the middle, thin at the rim). |
| Fines falling | whole block | Under the mesh, thin streams of material-colored specks fall from the tray to the ground or into the container below. More while the layer is thick, fewer as it thins. |
| Reveal | at set points | A found item appears lying on the mesh as the layer sinks past it: first a corner pokes out, then the whole item, tilted flat. Each reveal has a quiet tick, pitched by its place in the block's drops. Finds stay on the mesh and jiggle with each shake. |
| Finish | 0.5 s | The last of the layer falls through. The tray gives one bigger settle (overshoot and back), a wood knock plays, and the finds hop off the tray in a small arc toward the player, or drop down through a brief opening motion into the container below. |
| Refill | 0.25 s | If the player holds more of the same block, the next block plops in and the rhythm does not stop. With linked sieves every empty one is refilled from the stack at the same moment. |

Progress is readable from the block alone in three ways: the layer thickness, how much mesh is visible, and how many finds are showing. From across a room you can tell a sieve is empty (bare mesh), loaded and waiting (full still layer), working (moving, dusty) or blocked (full still layer, drive not moving).

## 2. Animation design

### Parts (bones)

The sieve model has a static frame and moving parts. Bone names are a contract between the model and the renderer (see `ARCHITECTURE.md`).

| Bone | Moves | Notes |
|---|---|---|
| `frame` | never | Legs, rails, brass corner brackets. Drawn as a normal baked block model, not by the renderer. |
| `tray` | slides on X and Z, tiny roll and pitch | The wooden tray with brass rivets. Hangs from the frame on four short straps. |
| `strap_nw`, `strap_ne`, `strap_sw`, `strap_se` | rotate to follow the tray | Children of `frame`, each rotates a few degrees so the tray visibly hangs. |
| `mesh` | child of `tray`; sags | Scale Y keyframes make it sag under a fresh block and bounce at each stop. Its texture is the fitted mesh's. |
| `layer` | child of `tray` | A locator only. The renderer builds the material layer here (section 3). |
| `finds` | child of `mesh` | A locator. Found items are placed on a 3 by 2 grid of spots around it. |
| `chute` | child of `frame` | A locator under the tray where fines spawn. |

### Clips

Authored in Blockbench as animations inside `sieve.bbmodel`, at 20 keyframe steps per second, exported by our own script (section 4).

| Clip | Length | Loop | Used for |
|---|---|---|---|
| `idle` | 0 | hold | Rest pose. |
| `load` | 0.25 s | once | Tray dip and return, mesh sag in. |
| `shake_hand` | 0.6 s (two shakes) | loop | Hand sifting. Wide swing (1.5 px each way on X), a slight roll (2 degrees), and every second shake goes a little diagonal so it never looks mechanical. |
| `shake_windup` | 0.8 s (two shakes) | loop | Windup drive. Same swing on X only, even and metronomic, with a small spring wobble after each stop. |
| `shake_furnace` | 0.4 s (two shakes) | loop | Furnace drive. Short fast swing (1 px), hard stops, constant tremble of 0.25 px on Z. |
| `finish` | 0.5 s | once | Big settle, mesh bounce. |
| `mesh_swap` | 0.3 s | once | Mesh drops into the tray from 2 px above, bounces once. |
| `blocked` | 1.2 s | loop | Drive below is stopped with a full buffer: tray still, straps sway very slightly. |

### Easing

Blockbench keyframes carry an interpolation mode. Our loader supports four: `linear`, `step`, `catmullrom` (smooth) and `bezier` (with handles). The shake uses bezier handles to get the fast-in, hard-stop curve. Clips blend into each other over 3 ticks (linear blend of bone transforms), so starting, stopping and switching between hand and drive never snaps.

### Server and client time

The server owns the facts: the block, progress, shake count, the rolled finds and when they are revealed. The client owns the motion.

- The server sends the sieve's state when it changes: when a block goes in, on each shake, on finish. One small packet per shake per sieve, only to players tracking the chunk.
- The client keeps an animation clock per sieve. Each shake packet nudges the clip phase to the next "stop" point over 2 ticks, so the visual stop and the server shake line up without popping, even with lag. If packets stop arriving for 10 ticks, the clip blends to `idle`.
- Layer height on the client eases toward the server's progress (critically damped spring, about 4 ticks to settle), with a small extra drop at each stop. It never jumps.

## 3. The material layer, the fines and the finds

**Layer.** A box drawn by the renderer at the `layer` bone, textured with the loaded block's own top texture (looked up from the block's model, tinted like the block). Height = one block's full height times remaining progress. The top face is split into a 3 by 3 grid of vertices; the center vertex is higher than the rim (domed), and each vertex leans toward the trailing side of the swing by an amount taken from the tray's velocity, so the surface slumps and levels. The texture is offset by a pixel or two on alternate shakes so the material seems to shuffle. 18 triangles for the top, 4 quads for the sides.

**Fines.** A custom particle (`primamateria:fines`): a 1 px or 2 px speck colored from the block's texture, spawned at the `chute` bone in a few thin streams, falling with gravity and dying on the first block below. Count per shake = 1 to 3, scaled by layer thickness. If the recipe set has residue for this block, the streams are a little thicker; when a residue item is actually produced it falls as a normal small item along the same path.

**Dust.** A soft 4 px puff (`primamateria:sift_dust`), 1 per shake above the layer for dry materials (sand, stone meal, gravel), none for dirt. Material class comes from the block's sound type, so modded blocks work without data.

**Finds.** Rolled by the server when the block goes in. Each find has a reveal point (a progress value between 0.15 and 0.9, spread evenly, shuffled by the block's seed) and a spot on the 3 by 2 grid. The client draws a find as the flat item model lying on the mesh, scaled 0.4, rotated by the seed. No clipping is needed: the layer covers the find until its top sinks past it. At most 6 finds are drawn; more are still dropped at the end. On finish each find follows a short arc (0.4 s) and the real item entities or inventory inserts happen as the arc ends.

## 4. How it renders

**No animation library.** No GeckoLib and no other dependency. We write a small loader and sampler ourselves (workstream C in `ARCHITECTURE.md`).

### Pipeline

1. **Author.** `branding/models/sieve.bbmodel` holds the model (bones, cubes, per-face UV, 16x textures) and the animations listed above. Keyframe values must be plain numbers; expressions are not allowed.
2. **Export.** `scripts/export_anims.py` reads the `.bbmodel` (it is JSON) and writes two files into `common/src/main/resources/assets/primamateria/`:
   - `models/block/sieve_frame.json` (and the item model): the static bones as a normal Minecraft block model.
   - `primamateria_anim/sieve.json`: the moving bones (pivot, parent, cubes with UV) and every clip (per bone and channel, a list of keyframes: time, value, interpolation, bezier handles). The script fails with a clear message on an expression keyframe, an unknown interpolation, or a missing contract bone.
3. **Load.** On resource reload the client reads `primamateria_anim/*.json` into `AnimModel` and `AnimClip` objects (plain Java in `common/`, no game classes, unit tested). Cube geometry is baked once into vertex arrays per bone.
4. **Sample.** Each frame, for each sieve that is animating, `AnimSampler` evaluates the active clip (and the one it is blending from) at the sieve's clock: binary search per channel, then interpolate. The sieve has 7 bones and about 12 channels, so this is a few dozen float operations.
5. **Draw.** A block entity renderer draws: the baked bone cubes with the sampled transforms, the layer box, the finds. The static frame is part of the chunk mesh and costs nothing per frame.

The same loader is used for the barrel (surface ripple, crystal growth scale), the crucible (molten surface, pour stream), the casting tray (cooling glow) and the drives (windup key turning, furnace door flicker), each with a small `.bbmodel` animation set. The sieve is the first and the reference.

### Resting sieves cost nothing

The sieve has a block state property `moving` (true or false).

- `moving=false`: the tray and mesh are part of the normal block model (a block state variant that includes them at rest, with the fitted mesh chosen by a second property `mesh`: `none`, or one of 8 texture slots that the mesh data files map to). The renderer returns at once unless the sieve holds a load, in which case it draws only the still layer box and finds.
- `moving=true`: the variant has only the frame; the renderer draws the tray, mesh, straps, layer and finds.

The server sets `moving` when shaking starts and clears it 20 ticks after the last shake, so a sieve does not flip the state (and rebuild its chunk section) between blocks of a continuous run.

## 5. Sound

All sounds are our own, made by us (recorded or synthesized) and listed with their source in `licenses/`. Each event has 3 or 4 variations, and pitch varies by plus or minus 5% per play.

| Event | Sound | When |
|---|---|---|
| `primamateria:sieve.load` | Soft thud of loose material on a taut mesh | Block added |
| `primamateria:sieve.shake.coarse` | Rattle of stones on wire | Each stop, gravel-class material |
| `primamateria:sieve.shake.fine` | Dry hiss of sand | Each stop, sand-class material |
| `primamateria:sieve.shake.soil` | Muffled scrape | Each stop, dirt-class material |
| `primamateria:sieve.reveal` | One quiet tick, like a pebble on metal | A find shows. Pitch rises a semitone for each find in the load |
| `primamateria:sieve.finish` | Wood knock and the last fines trailing off | Load done |
| `primamateria:sieve.mesh` | Wire twang | Mesh fitted or removed. The diamond and netherite meshes add a soft chime |
| `primamateria:drive.crank` | Ratchet clicks | Each turn wound |
| `primamateria:drive.tick` | Spring clack | Each windup shake, quiet |

Shake volume falls as the layer thins, so the sound itself tells progress. Machine-driven shakes play at 60% volume. At most 4 sieve shake sounds play in the same tick on one client, nearest first; the rest are skipped.

## 6. Hand versus automated feel

| | Hand | Windup drive | Furnace drive |
|---|---|---|---|
| Rhythm | 1 shake per 0.2 s, lively, slightly irregular in direction | 1 per 0.4 s, even, with spring wobble | 1 per 0.2 s, tight tremble |
| Camera | None. The view never moves and the hand does not swing on every shake; the held item bobs gently | none | none |
| Player link | Finds hop to the player | Finds drop into the drive | Finds drop into the drive |
| Sound | Full volume | 60%, plus spring clack | 60%, plus low firebox rumble from the drive |
| Extra motion | none | Brass key on the drive turns back with every shake; dial sweeps down | Firebox glow pulses; heat shimmer specks |

By hand it should feel like your effort; with a drive it should look like a machine doing a duller, steadier version of the same job.

## 7. Performance budget

Target: 200 sieves in view, 64 of them moving, on a mid-range PC (the test PC), with a cost under 0.5 ms per frame on the render thread and no visible frame drop compared to the same scene with animations off.

| Rule | Value |
|---|---|
| Resting, empty sieve | 0 draw calls from the renderer, 0 sampling |
| Resting, filled sieve | 1 layer box, up to 6 finds |
| Moving sieve, within 24 blocks | Full: bones, domed layer, finds, particles, sound |
| Moving sieve, 24 to 48 blocks | Bones and a flat layer box; no finds, no particles, no dome; clip sampled every second frame |
| Moving sieve, beyond 48 blocks | Rest pose and a flat layer at the right height; no sampling |
| Geometry per moving sieve | At most 40 cubes in moving bones (target 24), 26 quads for the layer |
| Particles | At most 3 per shake per sieve; at most 150 sift particles alive per client, oldest dropped |
| Sounds | At most 4 shake sounds per tick per client |
| Network | 1 packet per shake per sieve, 12 bytes of payload, tracking players only; full state only when a block goes in, on finish and on chunk load |
| Allocation | None per frame: sampled transforms go into arrays owned by the sieve's render state |
| Shared sampling | Drive clips have no per-sieve variation, so sieves on the same drive type and the same phase tick share one sampled pose per frame |

`scripts/client_tour.py` gets a "sieve hall" scene (a 10 by 20 grid of sieves, 64 driven) that records average frame time with `client.animations` set to `full`, `reduced` and `off`. The numbers go into `docs/testing`.

## 8. Fallback when animations are off

`client.animations` in the config:

- **`full`** (default): everything above.
- **`reduced`**: no tray motion, no dome, no finds on the mesh, no particles. The layer still shrinks smoothly and sounds still play. For players who dislike motion and for weak PCs.
- **`off`**: the block entity renderer does nothing. The sieve uses block state only: the tray and mesh are always in the block model, and a block state property `fill` (0 to 4) picks one of five layer heights drawn with a generic "sift material" texture tinted by the block's map color (a block color handler that reads the block entity). The server updates `fill` only when the remaining amount crosses a quarter, so a block changes the chunk at most 4 times. Sounds still play. This is also what other render engines see if they skip block entity renderers.

Sounds have their own switch in the game's sound settings (the Blocks slider); `client.particles=false` removes fines and dust in any mode.

The game obeys the server in all three modes: nothing about drops, timing or progress depends on the client setting.
