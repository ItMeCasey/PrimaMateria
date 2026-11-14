# Integration tests (workstream D, 2026-11-13)

| Check | Result |
|---|---|
| Unit tests (common, run in each module): script bridge through Rhino with stub ModJS classes, content changes, KubeJS helper, no-ModJS class loader test, cards, layout, drop tables, info lines | pass (neoforge-26.2 run; same sources in all four modules) |
| Compile of all four modules with the plugins | pass |
| Headless self test, no optional mod | pass on neoforge-26.2, neoforge-26.3, forge-26.2, forge-26.3 |
| Headless self test with the real ModJS jar (2026.11.38) and `scripts/fixtures/modjs_primamateria_check.js` (listener changes drops, `sifting.add` reaches the snapshot, config read) | pass on all four layers |
| Client frames, NeoForge 26.2 (`scripts/integration_tour.py`, demo recipes): BEI, JEI, REI, 6 frames each in `docs/frames/` | pass; a first REI run found that the REI plugin loaded the JEI class and failed without JEI, fixed (`Cards.Category.station()`) and re-run clean |
| Client frames on NeoForge 26.3, SMWTI and Jade lines in a client, ModJS in a client | not done |
| EMI, KubeJS | no build for 26.2 or 26.3: not run (see `docs/known-gaps.md`) |

Frames use `DemoRecipes` (dev only), not real gameplay recipes, because workstream A has not filled `RecipeLookup` yet.
