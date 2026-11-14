# Real fluids: test results (branch b-fluids, 2026-11-13)

Brine and the molten copper, iron and gold are real fluids on Forge and NeoForge, on 26.2 and 26.3: a fluid type, a source and a
flowing fluid (`primamateria:flowing_<id>`), a liquid block and a real bucket item each. The barrels, the crucible and the casting
tray offer the loader's fluid handler.

## What it is

- **Brine** is water's own fluid class with its own look: it swims, puts fires out, carries boats, flows like water and follows the
  game's water source rule. It is in the `minecraft:water` fluid tag, density 1100, viscosity 1000, 295 K, no light.
- **The molten metals** are lava's own fluid class, so everything lava's code does they do too: they burn and set what is in them
  alight, spread fire to flammable blocks near them (random tick), make stone where they meet water, and are in the
  `minecraft:lava` fluid tag. They differ from lava in one thing: they never turn into new sources (lava's own rule is the game rule
  `lavaSourceConversion`, which is off by default anyway). Copper 1358 K, density 3400, viscosity 6000, light 12; iron 1811 K, 3200,
  7000, light 13; gold 1337 K, 3800, 6500, light 13 (the light values are the ones the crucible already used). They are heavier than
  lava (3000, 6000).
- **Buckets** (`brine_bucket`, `molten_copper_bucket`, `molten_iron_bucket`, `molten_gold_bucket`, ids unchanged) are real
  `BucketItem`s: they place a source block, pick it up again, empty back to a vanilla bucket and work in dispensers.
- **Textures** (`block/<id>_still` 16x16 frames, `block/<id>_flow` 32x32 frames like the game's own flowing fluids, `block/brine_overlay`)
  are drawn by `scripts/make_fluid_textures.py` from the palette of `docs/ART.md`: brine in the teal ramp (translucent), copper in
  an orange-red ember ramp, iron in a pale orange-white ramp, gold in a yellow ramp. The fluid models are registered per loader
  (`NeoForgeFluidClient`, `ForgeFluidClient`).
- **Fluid handlers.** Each machine offers one tank. Wooden and clay barrel 1000 mB, crucible 4000 mB, casting tray one cast (100 mB for
  the shipped recipes). The pure adapters (`BarrelTank`, `CrucibleTank`, `TrayTank`, `common/.../fluid/`) sit over the id-and-mB
  states; the loader classes (`MachineFluidHandler`) only translate: NeoForge `ResourceHandler<FluidResource>` with a rollback
  journal, Forge `IFluidHandler`. The wooden barrel refuses lava and molten metals; one fluid kind at a time; a pipe cannot drain a
  barrel that is soaking, steeping or growing; the crucible takes lava, the molten metals and any fluid (of any mod) that has a casting
  recipe; the tray takes exactly one cast of a fluid that has a casting recipe and starts it. Every executed or committed change calls
  the machine's `changed()`.

## Tests

- **Unit tests** (`common/src/test/.../fluid/MachineTankTest`): 15 tests: simulate answers what execute does and changes nothing,
  capacity, the one-kind rule, hot fluids and the wooden barrel, compost, a working barrel cannot be drained, rollback snapshots for
  all three machines, the tray's one cast. 156 tests per module in total.
- **Headless self test** (`dev/FluidSelfTest`, one `SelfTest.run` line, plus each loader's dev hook): registries (four fluids times
  source, flowing, fluid type, block, bucket), the links between them, a bucket places a real source block and scoops it back,
  spreading on a floor, random tick fire only for the molten metals, an animal standing in each fluid (burns in the metals, swims in
  brine, same as in lava and water), the fluid tags, dispensers, the loader's fluid types (temperature, density, viscosity, light,
  sounds), the fluid capability on the crucible, both barrels and the tray (and none elsewhere), pipe-style fill and drain in many
  small steps, overfill refused, rolled back and simulated calls change nothing, committed ones are saved and call `changed()`, the
  wooden barrel refuses lava, a second kind and a drain while soaking, the tray takes one cast and gives nothing back, and the
  round trip crucible, bucket, real source block, bucket, crucible with real buckets. The existing barrel and crucible self tests
  run in the same pass.
- **Results.** All four builds: `gradlew build` passes with 156 unit tests each (15 new) and the release jars contain no dev classes;
  `PRIMA_PORT_BASE=8738 python scripts/server_check.py <layer>` gives SERVER PASS on neoforge-26.2 (435 checks), forge-26.2 (429),
  neoforge-26.3 (435) and forge-26.3 (438).
- **Frames** (`PRIMA_TOUR=fluids python scripts/client_tour.py <layer>`, isolated launcher, `FluidTour` in `dev/`): TOUR PASS on
  neoforge-26.2 and forge-26.3, 12 frames each plus a bucket icon strip, saved as `docs/frames/fluid_<layer>_*.png`. Every frame was read.
  - `brine_still`, `molten_copper_still`, `molten_iron_still`, `molten_gold_still`: a source pool behind glass (glass on the near side),
    the fluid's bucket in hand and all four bucket icons in the hotbar; teal translucent brine, orange-red copper, pale orange iron, yellow
    gold, animated, full bright, other pools visible behind.
  - `<fluid>_flowing`: a source on a stone block spilling down its sides and across the floor (the flowing texture, lit).
  - `brine_inside`, `molten_gold_inside`: the camera inside an open-top tank (water fog and bubbles; lava fog).
  - `crucibles`: three crucibles holding molten copper, iron and gold at different levels; `trays`: a tray filling with molten iron
    and one with molten gold part way through cooling.
  - `bucket_icons`: the hotbar strip of a real frame, enlarged 3x.
- **Bugs found and fixed while doing this:** Forge refused to start because `BucketItem(Fluid)` looks the fluid up in the registry
  before it is registered (each loader now builds its own bucket, Forge with a supplier); 26.3 renamed `PushReaction.DESTROY` to `POPPED`
  (new sync patch); the first flowing textures were twice as coarse as the still ones (the flowing face shows a half-size window of its
  32x32 frame; redrawn finer); the first test layout put sources next to the edge of the cleared area, where water-like fluids flow toward
  the drop, and the crucible camera was too far away to see the fluid.

## Known differences

- NeoForge counts an entity as "in lava" only for lava's own fluid type (`isInLava()` is keyed by the type), so a molten metal does not
  make `isInLava()` true on NeoForge; the burning, the fire spread and the heavy movement all work through lava's fluid class and the
  fluid type. On Forge `isInLava()` follows the lava tag.
- The molten metals use lava's fluid class and so are lava-like in every way that class decides; the only change is that they never
  convert to sources.
