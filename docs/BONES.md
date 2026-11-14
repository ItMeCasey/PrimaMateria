# Bone contract

The names the renderer relies on. `scripts/export_anims.py` fails when one is missing from a `.bbmodel`; `AnimModel.requireBones` fails again at load.

## Sieve (`branding/models/sieve.bbmodel`, exported to `primamateria_anim/sieve.json`)

| Bone | Parent | Pivot | Notes |
|---|---|---|---|
| `frame` | none | (8, 0, 8) | Static. Its cubes go into the block model, not the renderer. |
| `tray` | `frame` | (8, 13, 8) | Moves on X and Z (up to 1 unit each way, the clearance inside the rails), rolls a few degrees about its hang point. Walls and rivets. |
| `strap_nw`, `strap_ne`, `strap_sw`, `strap_se` | `frame` | top of the strap | Brass hangers; rotate a few degrees to follow the tray. |
| `mesh` | `tray` | (8, 10, 8) | One plate; its texture is replaced at run time by the fitted mesh's texture. Sags and bounces. |
| `layer` | `tray` | (8, 10, 8) | Locator. The renderer builds the material layer from here (7 by 7 units, base at y = 10). |
| `finds` | `mesh` | (8, 10, 8) | Locator. Finds are placed on a 3 by 2 grid around it. |
| `chute` | `frame` | (8, 8, 8) | Locator under the tray where fines spawn. |

Rules for authors: keyframe values are plain numbers (no expressions); interpolation is `linear`, `step`, `catmullrom` or `bezier`; rotation order is Z, then Y, then X (as Blockbench groups); cubes in moving bones are axis aligned and use one texture each; at most 40 cubes in moving bones (now 13).

Clips: `idle`, `load`, `shake_hand`, `shake_windup`, `shake_furnace`, `finish`, `mesh_swap`, `blocked` (lengths and loops in `SIFTING.md`). A shake clip's stops (the hard ends of each swing) are at multiples of half its length; the client snaps its clock to the nearest stop on every shake packet.

## Other models

The barrels, crucible, auto hammer and powered drive are static block models for now (no animated bones yet). Their groups are `body` (cubes) and `inner` (a locator at the inner surface of barrels and the crucible, for fluid and contents). The powered drive's charge strip is five separate `charge_1` to `charge_5` cubes, exported as `powered_drive_charge_<k>.json` (segments 1 to k) for the blockstate to show by `charge`.
