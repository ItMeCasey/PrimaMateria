# Changelog

## 2026.11.1 (not released)

First release. In development.

### Interfaces

Changes to the frozen ids, registries, view records and data formats, newest first. Other workstreams follow this list.

- 2026-11-13: drives added (additions, nothing renamed). `DriveBlock` (one class, windup, furnace and powered kinds, all with `StateProps.FACING`, `LIT` and `CHARGE`), `DriveBlockEntity` (`view()` gives a `DriveView`; `state()` is the pure `common/.../drive/DriveState`), block entity types `ModContent.WINDUP_DRIVE_BE`, `FURNACE_DRIVE_BE`, `POWERED_DRIVE_BE` and `ModContent.driveType(kind)`. A drive is a `WorldlyContainer`: slots 0 to 8 are the output buffer (the machine above delivers through the top; hoppers empty it through the bottom and sides), the furnace drive also has fuel slot 9 (sides only). A machine above implements `machine/Drivable`; a drive works it through `drive(level, 0)` and stops when `hasWork` is false, the buffer has fewer than 3 empty slots, there is no power or a redstone signal. The powered drive takes energy through each loader's energy capability (insert only). `SelfTest.loaderChecks` lets a loader's dev class add checks that run inside `SelfTest.run`.
- 2026-11-13: sieve logic added (additions, nothing renamed). Pure logic in `common/.../sift/` (`SiftState`, `DropRoller`, `SiftMath`, `DropEntry`, `ResidueEntry`, `Drop`). Game side: recipe types `primamateria:sifting` and `primamateria:sifting_residue` (`recipe/PrimaRecipes.siftsFor` for lookups), mesh data loader (`data/Meshes`, `data/PrimaReload`), `SieveBlock`, `SieveBlockEntity` (`state()` and `view()` give a `SieveView`), `SieveLogic` (`interact`, `insert`, `shakeByDrive(level, sieve, minTicks)` for drives). `ModContent.SIEVE_BE` is the sieve's block entity type; `ModContent.registerBlockEntities` registers it. Hand shaking is one shake per 4 ticks (`SiftMath.HAND_MIN_TICKS` is the 3 tick guard). `RecipeLookup` is not filled yet.
- 2026-11-13: sieve rules. One block at a time, no storage: `SieveView.load` (0 to 4) becomes `SieveView.loaded` (boolean), `GameEvents.Sifted.load` is removed. Linked sieves: config keys `sieve.max_load` and `sieve.extra_block_factor` are replaced by `sieve.link` (true) and `sieve.link_radius` (1, range 1 to 3).
- 2026-11-13: molten metals are real fluids. `Ids.MELTS` and `Ids.DATA_MELT` removed; `Ids.FLUIDS` added (`brine`, `molten_copper`, `molten_iron`, `molten_gold`) and three bucket items (`molten_copper_bucket`, `molten_iron_bucket`, `molten_gold_bucket`). The `data/<ns>/primamateria/melt/` folder and `RecipeLookup.Melt` are removed. `primamateria:melting` has `result` as a plain fluid id, `primamateria:casting` has `fluid` in place of `melt`. `CrucibleView.isMelt` and `GameEvents.Melted.isMelt` are removed.
- 2026-11-13: `copper_grains`, `iron_grains`, `gold_grains` renamed to `copper_dust`, `iron_dust`, `gold_dust`; `mineral_salt` renamed to `salt_dust` (`Ids.COPPER_DUST`, `IRON_DUST`, `GOLD_DUST`, `SALT_DUST`).
- 2026-11-13: `flux_drive` renamed to `powered_drive` (`Ids.POWERED_DRIVE`, `DriveKind.POWERED`, `StateProps.CHARGE`, config keys `drive.powered_*`).
- 2026-11-13: `stamp_mill` renamed to `auto_hammer` (`Ids.AUTO_HAMMER`, `AutoHammerView`, sound `auto_hammer.strike`, config key `auto_hammer.strokes_per_block`).
- 2026-11-13: block models use vanilla textures by reference; our own textures are 16x16 (`docs/ART.md`).
- 2026-11-13: scaffold. `Ids`, `ModContent`, view records, `StateProps`, `ModShapes`, `GameEvents`, `RecipeLookup` created.
