# Known gaps

Things Prima Materia cannot do yet, and why. Checked on CurseForge with the `curseforge-lookup` skill on 2026-11-13 (workstream D re-checked each row); recheck before each release.

## Optional mods without a build for our targets

| Mod | NeoForge 26.2 | Forge 26.2 | NeoForge 26.3 | Forge 26.3 | What it means |
|---|---|---|---|---|---|
| JEI | 30.48.0.244 (beta) | no build | 31.17.0.113 (beta) | no build | The JEI plugin is in the NeoForge jars only (`neoforge/compat/PrimaJeiPlugin`); added to Forge when JEI ports. Dev runs also need MezzConfig (standalone). |
| REI | 26.2.820 (beta) | no build | 26.3.824 (beta) | no build | Same as JEI. REI needs Architectury (21.1.12 on 26.2, 22.0.5 on 26.3) and Cloth Config (26.2.155, and 26.3.159 whose CurseForge file is tagged `26.3-snapshot` only). |
| EMI | no build | no build | no build | no build | The newest EMI is for 1.21.1. The plugin is written (`version-26.2/src/pending/emi/PrimaEmiPlugin.java`) and is in no source set: it compiles against nothing and is not tested. When EMI ports: move it into a NeoForge layer, add EMI `compileOnly`, fix API drift, run the viewer tour. |
| Jade | 26.2.10 | no build | 26.3.5 | no build | SMWTI covers in-world info on all four builds; Jade lines on NeoForge only (`neoforge/compat/PrimaJadePlugin`). |
| KubeJS | no build | no build | no build | no build | The newest NeoForge build is for 26.1.2 (8.0.8). All recipes and data are datapack JSON and recipe types, so KubeJS can edit them once it ports. The helper script (`kubejs/startup_scripts/primamateria.js`, global `PrimaMateria`) is unit tested with Rhino and the `Java.loadClass` shape only; it has had no real KubeJS run. |

Our own BEI, SMWTI and ModJS have builds for all four targets.

## Integration notes

| Note | Depends on | Plan |
|---|---|---|
| A BEI card shows an empty slot box for each unused output slot of its category (a sifting card with two drops shows ten boxes). BEI draws every slot of a category. | BEI | Acceptable for the first release; split categories by output count if it bothers anyone. |
| BEI shows fluids as their bucket and melts as a tinted square, since its slots hold items. JEI and REI show real fluid ingredients. | BEI | None needed. |

## Other

No gap is open here as of 2026.11.2. Molten metals and brine are real fluids with fluid handlers on the barrels, the crucible and the casting tray, and players of a dedicated server receive the recipe lookup. The items that still need Casey's decision are listed in `COMPLETION_REPORT.md`.
