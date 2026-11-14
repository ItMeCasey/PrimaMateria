# Fluid visibility: how a player gets and sees brine and molten metal (2026-11-14)

Result of reading the code and data (no client run). Nothing in the code hides the fluids; the survival route is long, and one
tint bug made fluids in the barrel and crucible look darker than their own textures.

## What a player does, no commands

- **Creative tab "Prima Materia"**: shows every registered item (`ModContent.ITEMS`, from the shared `ModContent.creativeTab()` that
  both loaders register). That includes `brine_bucket`, `molten_copper_bucket`, `molten_iron_bucket`, `molten_gold_bucket`, the
  barrels, crucible, casting tray and the unfired forms. The liquid blocks are not items (like water and lava), so the buckets are
  the way to place the fluids. Same on NeoForge and Forge, 26.2 and 26.3.
- **Brine**: sift sand with the copper mesh for salt dust, fill a wooden or clay barrel with a water bucket (1000 mB), put in 4 salt
  dust and wait (recipe `steeping/brine.json`, 600 ticks). The barrel then holds brine; an empty bucket on the barrel gives a
  `brine_bucket`; a full bucket on the ground places the real brine source block.
- **Molten metal**: bake a Raw Crucible (recipe in `raw_crucible.json`, smelt it into a Crucible), set it on a heat source, drop in
  ingots, dust or raw ore (`melting/*.json`). The metal shows in the crucible. An empty bucket on the crucible gives the metal bucket
  (1000 mB); placing it makes a real molten block; a casting tray next to the crucible, or a bucket on the tray, casts it.
- Recipes for the containers (wooden barrel, raw and fired clay barrel, crucible, tray) exist as data and are in the recipe book and
  in BEI, JEI, REI and EMI through the mod's own categories.

## What showed and what hid

- Bucket items: item definitions, models, textures and lang names exist for all four buckets. Nothing missing.
- Fluid blocks: fluid models are registered per loader (`NeoForgeFluidClient`, `ForgeFluidClient`) with the animated still and flowing
  textures; the earlier fluid frames (docs/testing/fluids.md) show the translucent brine and the lit molten metals.
- Machines: `MachineRenderer` draws the barrel, crucible and tray surfaces with the fluid's own still texture at a height that
  follows the amount. Fluid height is visible whenever the machine holds anything. The 2026.11.1 jar had internal fluids only, so a
  player on that jar saw no fluid items or blocks at all.
- **Bug fixed**: the barrel and crucible tinted the fluid's own texture (brine, molten metals in a barrel or crucible fluid slot)
  with the fluid color on top, which darkened it a second time. Now only the plain water texture (a fluid with no texture of its
  own) is tinted; lava and our fluids draw as their textures.

## Changed

- `MachineRenderer` (version-26.2, synced to 26.3 with `scripts/sync_versions.py`): `ownLook` check for the tint.
- No data, recipe or `Ids` changes.

## Checked

`PRIMA_PORT_BASE=8738 python scripts/server_check.py <layer>`: SERVER PASS on neoforge-26.2, forge-26.2, neoforge-26.3 and forge-26.3.
No client was started.
