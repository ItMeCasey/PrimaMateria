# Changelog

## 2026.11.1 (not released)

First release. In development.

### Interfaces

Changes to the frozen ids, registries, view records and data formats, newest first. Other workstreams follow this list.

- 2026-11-13: barrel added (additions, nothing renamed). Recipe types `primamateria:soaking`, `mixing`, `steeping`, `growing`, `composting` (records in `recipe/`, lookups in `recipe/BarrelRecipes`; the shared `recipe/PrimaRecipe` interface carries the grid-less defaults). Compost values are the data folder `primamateria/compost` (`data/CompostValues`, registered in `DataFolders.all`). Pure rules in `common/.../barrel/BarrelState` (+ `BarrelColors`); game side `block/BarrelBlock`, `blockentity/BarrelBlockEntity` (`state()`, `view()` gives a `BarrelView`), `barrel/BarrelLogic`. `ModContent.BARREL_BE` is the block entity type of both barrels, registered as `primamateria:barrel`. Fluids in barrels are internal ids and amounts: a full bucket (see `FluidBuckets`) pours 1000 mB, mixing recipes use the bucket's fluid. `steeping` has an optional `amount` (least fluid held, default 1000). `GameEvents.BARREL_FINISHED` fires with family `soaking`, `mixing`, `steeping`, `growing` or `composting`.
- 2026-11-13: sieve logic added (additions, nothing renamed). Pure logic in `common/.../sift/` (`SiftState`, `DropRoller`, `SiftMath`, `DropEntry`, `ResidueEntry`, `Drop`). Game side: recipe types `primamateria:sifting` and `primamateria:sifting_residue` (`recipe/PrimaRecipes.siftsFor` for lookups), mesh data loader (`data/Meshes`, `data/PrimaReload`), `SieveBlock`, `SieveBlockEntity` (`state()` and `view()` give a `SieveView`), `SieveLogic` (`interact`, `insert`, `shakeByDrive(level, sieve, minTicks)` for drives). `ModContent.SIEVE_BE` is the sieve's block entity type; `ModContent.registerBlockEntities` registers it. Hand shaking is one shake per 4 ticks (`SiftMath.HAND_MIN_TICKS` is the 3 tick guard). `RecipeLookup` is not filled yet.
- 2026-11-13: sieve rules. One block at a time, no storage: `SieveView.load` (0 to 4) becomes `SieveView.loaded` (boolean), `GameEvents.Sifted.load` is removed. Linked sieves: config keys `sieve.max_load` and `sieve.extra_block_factor` are replaced by `sieve.link` (true) and `sieve.link_radius` (1, range 1 to 3).
- 2026-11-13: molten metals are real fluids. `Ids.MELTS` and `Ids.DATA_MELT` removed; `Ids.FLUIDS` added (`brine`, `molten_copper`, `molten_iron`, `molten_gold`) and three bucket items (`molten_copper_bucket`, `molten_iron_bucket`, `molten_gold_bucket`). The `data/<ns>/primamateria/melt/` folder and `RecipeLookup.Melt` are removed. `primamateria:melting` has `result` as a plain fluid id, `primamateria:casting` has `fluid` in place of `melt`. `CrucibleView.isMelt` and `GameEvents.Melted.isMelt` are removed.
- 2026-11-13: `copper_grains`, `iron_grains`, `gold_grains` renamed to `copper_dust`, `iron_dust`, `gold_dust`; `mineral_salt` renamed to `salt_dust` (`Ids.COPPER_DUST`, `IRON_DUST`, `GOLD_DUST`, `SALT_DUST`).
- 2026-11-13: `flux_drive` renamed to `powered_drive` (`Ids.POWERED_DRIVE`, `DriveKind.POWERED`, `StateProps.CHARGE`, config keys `drive.powered_*`).
- 2026-11-13: `stamp_mill` renamed to `auto_hammer` (`Ids.AUTO_HAMMER`, `AutoHammerView`, sound `auto_hammer.strike`, config key `auto_hammer.strokes_per_block`).
- 2026-11-13: block models use vanilla textures by reference; our own textures are 16x16 (`docs/ART.md`).
- 2026-11-13: scaffold. `Ids`, `ModContent`, view records, `StateProps`, `ModShapes`, `GameEvents`, `RecipeLookup` created.
