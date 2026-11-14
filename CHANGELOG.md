# Changelog

## 2026.11.1 (not released)

First release. In development.

### Interfaces

Changes to the frozen ids, registries, view records and data formats, newest first. Other workstreams follow this list.

- 2026-11-13: crucible and casting tray (additions, nothing renamed). Recipe types `primamateria:melting` (`input`, `result` = fluid id, `amount`) and `primamateria:casting` (`fluid`, `amount`, `result`, `time`), looked up with `CrucibleRecipes.meltFor` and `castFor`. Data folder `primamateria/heat` (`HeatSources`, `HeatRule`). `CrucibleView` and `TrayView` are filled by `CrucibleBlockEntity.view()` and `CastingTrayBlockEntity.view()`; `TrayView.Stage.FILLING` lasts `TrayState.FILL_TICKS` (10) ticks, then COOLING for the recipe time. `ModContent.CRUCIBLE_BE` and `TRAY_BE`. Fluids stay internal strings plus mB (`CrucibleState`); a hot bucket on a tray casts as many items as 1000 mB makes.
- 2026-11-13: sieve logic added (additions, nothing renamed). Pure logic in `common/.../sift/` (`SiftState`, `DropRoller`, `SiftMath`, `DropEntry`, `ResidueEntry`, `Drop`). Game side: recipe types `primamateria:sifting` and `primamateria:sifting_residue` (`recipe/PrimaRecipes.siftsFor` for lookups), mesh data loader (`data/Meshes`, `data/PrimaReload`), `SieveBlock`, `SieveBlockEntity` (`state()` and `view()` give a `SieveView`), `SieveLogic` (`interact`, `insert`, `shakeByDrive(level, sieve, minTicks)` for drives). `ModContent.SIEVE_BE` is the sieve's block entity type; `ModContent.registerBlockEntities` registers it. Hand shaking is one shake per 4 ticks (`SiftMath.HAND_MIN_TICKS` is the 3 tick guard). `RecipeLookup` is not filled yet.
- 2026-11-13: sieve rules. One block at a time, no storage: `SieveView.load` (0 to 4) becomes `SieveView.loaded` (boolean), `GameEvents.Sifted.load` is removed. Linked sieves: config keys `sieve.max_load` and `sieve.extra_block_factor` are replaced by `sieve.link` (true) and `sieve.link_radius` (1, range 1 to 3).
- 2026-11-13: molten metals are real fluids. `Ids.MELTS` and `Ids.DATA_MELT` removed; `Ids.FLUIDS` added (`brine`, `molten_copper`, `molten_iron`, `molten_gold`) and three bucket items (`molten_copper_bucket`, `molten_iron_bucket`, `molten_gold_bucket`). The `data/<ns>/primamateria/melt/` folder and `RecipeLookup.Melt` are removed. `primamateria:melting` has `result` as a plain fluid id, `primamateria:casting` has `fluid` in place of `melt`. `CrucibleView.isMelt` and `GameEvents.Melted.isMelt` are removed.
- 2026-11-13: `copper_grains`, `iron_grains`, `gold_grains` renamed to `copper_dust`, `iron_dust`, `gold_dust`; `mineral_salt` renamed to `salt_dust` (`Ids.COPPER_DUST`, `IRON_DUST`, `GOLD_DUST`, `SALT_DUST`).
- 2026-11-13: `flux_drive` renamed to `powered_drive` (`Ids.POWERED_DRIVE`, `DriveKind.POWERED`, `StateProps.CHARGE`, config keys `drive.powered_*`).
- 2026-11-13: `stamp_mill` renamed to `auto_hammer` (`Ids.AUTO_HAMMER`, `AutoHammerView`, sound `auto_hammer.strike`, config key `auto_hammer.strokes_per_block`).
- 2026-11-13: block models use vanilla textures by reference; our own textures are 16x16 (`docs/ART.md`).
- 2026-11-13: scaffold. `Ids`, `ModContent`, view records, `StateProps`, `ModShapes`, `GameEvents`, `RecipeLookup` created.
