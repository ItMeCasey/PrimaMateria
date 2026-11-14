# Known gaps

Things Prima Materia cannot do yet, and why. Checked on CurseForge with the `curseforge-lookup` skill on 2026-11-13; recheck before each release.

## Optional mods without a build for our targets

| Mod | NeoForge 26.2 | Forge 26.2 | NeoForge 26.3 | Forge 26.3 | What it means |
|---|---|---|---|---|---|
| JEI | 30.48.0.244 (beta) | no build | 31.17.0.113 (beta) | no build | The JEI plugin ships in the NeoForge jars' code path only; added to Forge when JEI ports. |
| REI | 26.2.820 (beta) | no build | 26.3.824 (beta) | no build | Same as JEI. |
| EMI | no build | no build | no build | no build | The EMI plugin is written and compiled against the newest EMI API we can get, and is not client-tested. |
| Jade | 26.2.10 | no build | 26.3.5 | no build | SMWTI covers in-world info on all four builds; Jade lines on NeoForge only. |
| KubeJS | no build | no build | no build | no build | All recipes and data are datapack JSON, so they are ready for KubeJS. The helper bridge for events and reads is unit tested only. |

Our own BEI, SMWTI and ModJS have builds for all four targets.

## Other

| Gap | Since | Plan |
|---|---|---|
| No gameplay yet: every block and item is a placeholder | 2026-11-13 (scaffold) | Workstreams A to E, see `ARCHITECTURE.md` |
