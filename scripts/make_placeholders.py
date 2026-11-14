"""Writes the scaffold's placeholder assets and data for every id: 32x32 textures, block and item models, blockstates, item
definitions, the language file, loot tables (both layouts) and the mineable tags.

These are stand-ins so the game runs with every block and item visible from day one. Workstream B replaces the models and
textures with the real ones (docs/ART.md) and then deletes the matching entries here, or stops running this script.
It never overwrites a file that exists unless --force is given, so real art is safe.

Usage: python scripts/make_placeholders.py [--force]      (needs Pillow)
"""
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/primamateria"
DATA = ROOT / "common/src/main/resources/data"
FORCE = "--force" in sys.argv
MOD = "primamateria"

# Five-tone ramps from docs/ART.md, darkest to lightest.
RAMPS = {
    "wood": ["#3b2a1c", "#5a4028", "#7d5c38", "#a37f4f", "#c8a56e"],
    "stone": ["#3d3f48", "#555862", "#71747e", "#9396a0", "#b6b8c0"],
    "clay": ["#6b3526", "#8f4a35", "#b0654a", "#cb8563", "#e0a888"],
    "raw_clay": ["#6f7480", "#8a909c", "#a4abb6", "#bfc5ce", "#d9dde3"],
    "brass": ["#5a4318", "#8a6a24", "#b8923a", "#dcbb5c", "#f3df94"],
    "copper": ["#6a3423", "#9b4f32", "#c56f45", "#e39766", "#f5c29a"],
    "iron": ["#3a3d45", "#5b5f69", "#858a94", "#b1b5bd", "#dadde2"],
    "gold": ["#7a5410", "#b07d18", "#dba928", "#f2cf55", "#fbeba0"],
    "fiber": ["#5a5930", "#7c7a42", "#a19e58", "#c2bf7c", "#dedba8"],
    "violet": ["#2e2350", "#4c3a82", "#7458b8", "#a183e0", "#cdb8f5"],
    "teal": ["#1d4a55", "#2c6f7a", "#45979c", "#78c0bd", "#b5e3db"],
    "diamond": ["#1c5a63", "#2f8f96", "#55c4c4", "#8fe6df", "#d2faf4"],
    "netherite": ["#1f1b1f", "#33292e", "#4a3f44", "#665a5f", "#8a7d80"],
    "quartz": ["#8f857c", "#b8aea4", "#d9d1c8", "#ede8e1", "#fbf9f5"],
    "lapis": ["#12265e", "#1c3d92", "#2c5bc4", "#5a86e0", "#9db8f2"],
    "emerald": ["#0d4f2a", "#168a44", "#2cc264", "#6fe296", "#b9f6cd"],
    "scoria": ["#2a1414", "#48201e", "#6b302a", "#8c463a", "#ab6452"],
    "pale": ["#a59f78", "#c4be96", "#dcd7b0", "#ece8c8", "#f8f5e2"],
    "meal": ["#74767c", "#94969c", "#b0b2b7", "#c8cace", "#dfe0e3"],
}

# id: (display name, base ramp, trim ramp, mining tool)
BLOCKS = {
    "sieve": ("Sieve", "wood", "brass", "axe"),
    "wooden_barrel": ("Wooden Barrel", "wood", "brass", "axe"),
    "clay_barrel": ("Clay Barrel", "clay", "iron", "pickaxe"),
    "crucible": ("Crucible", "clay", "clay", "pickaxe"),
    "casting_tray": ("Casting Tray", "clay", "stone", "pickaxe"),
    "windup_drive": ("Windup Drive", "wood", "brass", "axe"),
    "furnace_drive": ("Furnace Drive", "iron", "brass", "pickaxe"),
    "flux_drive": ("Flux Drive", "iron", "violet", "pickaxe"),
    "stamp_mill": ("Stamp Mill", "wood", "stone", "axe"),
    "stone_meal": ("Stone Meal", "meal", None, "shovel"),
    "scoria": ("Scoria", "scoria", None, "shovel"),
    "pale_sand": ("Pale Sand", "pale", None, "shovel"),
}

# id: (display name, ramp, shape, handheld)
ITEMS = {
    "wooden_hammer": ("Wooden Hammer", "wood", "hammer", True),
    "stone_hammer": ("Stone Hammer", "stone", "hammer", True),
    "copper_hammer": ("Copper Hammer", "copper", "hammer", True),
    "iron_hammer": ("Iron Hammer", "iron", "hammer", True),
    "diamond_hammer": ("Diamond Hammer", "diamond", "hammer", True),
    "netherite_hammer": ("Netherite Hammer", "netherite", "hammer", True),
    "cloth_mesh": ("Cloth Mesh", "fiber", "mesh", False),
    "copper_mesh": ("Copper Mesh", "copper", "mesh", False),
    "iron_mesh": ("Iron Mesh", "iron", "mesh", False),
    "diamond_mesh": ("Diamond Mesh", "diamond", "mesh", False),
    "netherite_mesh": ("Netherite Mesh", "netherite", "mesh", False),
    "plant_fiber": ("Plant Fiber", "fiber", "heap", False),
    "rock_chip": ("Rock Chip", "stone", "heap", False),
    "copper_grains": ("Copper Grains", "copper", "heap", False),
    "iron_grains": ("Iron Grains", "iron", "heap", False),
    "gold_grains": ("Gold Grains", "gold", "heap", False),
    "mineral_salt": ("Mineral Salt", "teal", "heap", False),
    "quartz_seed": ("Quartz Seed", "quartz", "gem", False),
    "amethyst_seed": ("Amethyst Seed", "violet", "gem", False),
    "lapis_seed": ("Lapis Seed", "lapis", "gem", False),
    "diamond_seed": ("Diamond Seed", "diamond", "gem", False),
    "emerald_seed": ("Emerald Seed", "emerald", "gem", False),
    "raw_crucible": ("Unfired Crucible", "raw_clay", "pot", False),
    "raw_casting_tray": ("Unfired Casting Tray", "raw_clay", "tray", False),
    "raw_clay_barrel": ("Unfired Clay Barrel", "raw_clay", "pot", False),
    "brine_bucket": ("Brine Bucket", "teal", "pot", False),
}

EXTRA_LANG = {
    "itemGroup.primamateria": "Prima Materia",
    "fluid.primamateria.brine": "Brine",
    "block.primamateria.brine": "Brine",
    "melt.primamateria.molten_copper": "Molten Copper",
    "melt.primamateria.molten_iron": "Molten Iron",
    "melt.primamateria.molten_gold": "Molten Gold",
    "generator.primamateria.void_island": "Void Island",
}

SHAPES = {
    # 16x16 masks, doubled to 32x32. '#' body, '+' light, '-' dark.
    "hammer": ["................", "......++++++....", ".....+######-...", ".....+######-...", ".....+######-...", ".....+######-...",
               "......------....", "........=-......", "........=-......", "........=-......", "........=-......", "........=-......",
               "........=-......", "........=-......", "................", "................"],
    "mesh": ["................", "..============..", "..=+#+#+#+#+#=..", "..=#.#.#.#.#.=..", "..=+#+#+#+#+#=..", "..=#.#.#.#.#.=..",
             "..=+#+#+#+#+#=..", "..=#.#.#.#.#.=..", "..=+#+#+#+#+#=..", "..=#.#.#.#.#.=..", "..=+#+#+#+#+#=..", "..=#.#.#.#.#.=..",
             "..=+#+#+#+#+#=..", "..============..", "................", "................"],
    "heap": ["................", "................", "................", "................", "................", "................",
             "......++........", ".....+##-..++...", "....+####-+##-..", "...+######+###-.", "..+###########-.", "..+############-",
             "..-------------.", "................", "................", "................"],
    "gem": ["................", "................", "................", ".......++.......", "......+##-......", ".....+####-.....",
            "....+######-....", "....+######-....", ".....+####-.....", "......+##-......", ".......--.......", "................",
            "................", "................", "................", "................"],
    "pot": ["................", "................", "...+........-...", "...+#......#-...", "...+#......#-...", "...+#......#-...",
            "...+#......#-...", "...+#......#-...", "...+#......#-...", "...+##....##-...", "...+########-...", "....+######-....",
            ".....------.....", "................", "................", "................"],
    "tray": ["................", "................", "................", "................", "................", "................",
             "................", ".+............-.", ".+############-.", ".+############-.", "..------------..", "................",
             "................", "................", "................", "................"],
}


def rgb(hex_color):
    return tuple(int(hex_color[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def write(path, content, binary=False):
    if path.exists() and not FORCE:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    if binary:
        content.save(path)
    else:
        path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8", newline="\n")
    return True


def block_texture(base, trim):
    r = [rgb(c) for c in RAMPS[base]]
    img = Image.new("RGBA", (32, 32), r[2])
    px = img.load()
    for i in range(32):
        px[i, 0] = px[0, i] = r[3]
        px[i, 31] = px[31, i] = r[1]
    # a calm two-tone grain so full cubes are not flat
    for y in range(2, 30, 4):
        for x in range(2 + (y % 8), 30, 8):
            px[x, y] = px[x + 1, y] = r[3]
            px[x, y + 1] = px[x + 1, y + 1] = r[1]
    if trim:
        t = [rgb(c) for c in RAMPS[trim]]
        for cx, cy in ((2, 2), (26, 2), (2, 26), (26, 26)):
            for dx in range(4):
                for dy in range(4):
                    px[cx + dx, cy + dy] = t[2]
            px[cx, cy] = px[cx + 1, cy] = px[cx, cy + 1] = t[4]
            px[cx + 3, cy + 3] = px[cx + 2, cy + 3] = px[cx + 3, cy + 2] = t[0]
    return img


def item_texture(ramp, shape):
    r = [rgb(c) for c in RAMPS[ramp]]
    wood = [rgb(c) for c in RAMPS["wood"]]
    key = {"#": r[2], "+": r[4], "-": r[0], "=": wood[2]}
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(SHAPES[shape]):
        for x, ch in enumerate(row):
            if ch in key:
                for dx in range(2):
                    for dy in range(2):
                        px[2 * x + dx, 2 * y + dy] = key[ch]
    return img


def loot(block_id, new_layout):
    entry = [{"type": "minecraft:item", "name": f"{MOD}:{block_id}"}]
    if new_layout:
        pool = {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": entry}
    else:
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": entry, "conditions": [{"condition": "minecraft:survives_explosion"}]}
    return {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{MOD}:blocks/{block_id}"}


def main():
    made = 0
    lang = {}
    tools = {}
    for bid, (name, base, trim, tool) in BLOCKS.items():
        lang[f"block.{MOD}.{bid}"] = name
        tools.setdefault(tool, []).append(f"{MOD}:{bid}")
        made += write(ASSETS / f"textures/block/{bid}.png", block_texture(base, trim), binary=True)
        made += write(ASSETS / f"models/block/{bid}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{bid}"}})
        made += write(ASSETS / f"blockstates/{bid}.json", {"variants": {"": {"model": f"{MOD}:block/{bid}"}}})
        made += write(ASSETS / f"items/{bid}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{bid}"}})
        made += write(ROOT / f"version-26.2/src/main/resources/data/{MOD}/loot_table/blocks/{bid}.json", loot(bid, False))
        made += write(ROOT / f"version-26.3/src/main/resources/data/{MOD}/loot_table/blocks/{bid}.json", loot(bid, True))
    for iid, (name, ramp, shape, handheld) in ITEMS.items():
        lang[f"item.{MOD}.{iid}"] = name
        made += write(ASSETS / f"textures/item/{iid}.png", item_texture(ramp, shape), binary=True)
        parent = "minecraft:item/handheld" if handheld else "minecraft:item/generated"
        made += write(ASSETS / f"models/item/{iid}.json", {"parent": parent, "textures": {"layer0": f"{MOD}:item/{iid}"}})
        made += write(ASSETS / f"items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{iid}"}})
    lang.update(EXTRA_LANG)
    made += write(ASSETS / "lang/en_us.json", lang)
    made += write(ASSETS / "sounds.json", {})
    for tool, values in tools.items():
        made += write(DATA / f"minecraft/tags/block/mineable/{tool}.json", {"replace": False, "values": values})
    print(f"placeholders: {made} files written, {len(BLOCKS)} blocks, {len(ITEMS)} items")


if __name__ == "__main__":
    main()
