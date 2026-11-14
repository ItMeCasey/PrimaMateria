"""Writes the scaffold's stand-in assets and data for every id: block models, blockstates, item definitions and item models,
the language file, loot tables (both layouts) and the mineable tags.

Block models here are simple stand-ins built only from Minecraft's own textures, by reference (`minecraft:block/...`), so they
look like the game and follow the player's resource pack. Workstream B replaces them with Blockbench models that use the same
vanilla textures (docs/ART.md). Textures the game has no equivalent for are drawn by scripts/make_textures.py.

It never overwrites a file that exists unless --force is given.

Usage: python scripts/make_placeholders.py [--force]
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/primamateria"
DATA = ROOT / "common/src/main/resources/data"
FORCE = "--force" in sys.argv
MOD = "primamateria"
MC = "minecraft:block/"


def box(frm, to, tex, faces="north south east west up down"):
    return {"from": frm, "to": to, "faces": {f: {"texture": "#" + tex} for f in faces.split()}}


def tub(wall, floor, inside):
    """An open tub 14 wide and 14 high: four walls, a floor 2 up."""
    return [
        box([1, 0, 1], [15, 14, 3], wall), box([1, 0, 13], [15, 14, 15], wall),
        box([1, 0, 3], [3, 14, 13], wall), box([13, 0, 3], [15, 14, 13], wall),
        box([3, 1, 3], [13, 2, 13], floor, "up down"),
        box([3, 2, 3], [13, 14, 3.01], inside, "south"), box([3, 2, 12.99], [13, 14, 13], inside, "north"),
        box([3, 2, 3], [3.01, 14, 13], inside, "east"), box([12.99, 2, 3], [13, 14, 13], inside, "west"),
    ]


# id: (display name, mining tool, model or None for a plain cube of our own texture)
BLOCKS = {
    "sieve": ("Sieve", "axe", {
        "textures": {"particle": MC + "oak_planks", "wood": MC + "oak_planks", "leg": MC + "stripped_oak_log"},
        "elements": [
            box([1, 0, 1], [3, 12, 3], "leg"), box([13, 0, 1], [15, 12, 3], "leg"),
            box([1, 0, 13], [3, 12, 15], "leg"), box([13, 0, 13], [15, 12, 15], "leg"),
            box([0, 12, 0], [16, 16, 2], "wood"), box([0, 12, 14], [16, 16, 16], "wood"),
            box([0, 12, 2], [2, 16, 14], "wood"), box([14, 12, 2], [16, 16, 14], "wood"),
        ]}),
    "wooden_barrel": ("Wooden Barrel", "axe", {
        "textures": {"particle": MC + "barrel_side", "wall": MC + "barrel_side", "floor": MC + "barrel_bottom", "in": MC + "spruce_planks"},
        "elements": tub("wall", "floor", "in")}),
    "clay_barrel": ("Clay Barrel", "pickaxe", {
        "textures": {"particle": MC + "terracotta", "wall": MC + "terracotta", "floor": MC + "terracotta", "in": MC + "brown_terracotta"},
        "elements": tub("wall", "floor", "in")}),
    "crucible": ("Crucible", "pickaxe", {
        "textures": {"particle": MC + "terracotta", "wall": MC + "terracotta", "in": MC + "brown_terracotta"},
        "elements": [
            box([2, 0, 2], [5, 3, 5], "wall"), box([11, 0, 2], [14, 3, 5], "wall"),
            box([2, 0, 11], [5, 3, 14], "wall"), box([11, 0, 11], [14, 3, 14], "wall"),
            box([1, 3, 1], [15, 5, 15], "wall"),
            box([1, 5, 1], [15, 16, 3], "wall"), box([1, 5, 13], [15, 16, 15], "wall"),
            box([1, 5, 3], [3, 16, 13], "wall"), box([13, 5, 3], [15, 16, 13], "wall"),
            box([3, 5, 3], [13, 5.01, 13], "in", "up"),
        ]}),
    "casting_tray": ("Casting Tray", "pickaxe", {
        "textures": {"particle": MC + "terracotta", "wall": MC + "terracotta", "in": MC + "brown_terracotta"},
        "elements": [
            box([1, 0, 1], [15, 2, 15], "wall"),
            box([1, 2, 1], [15, 4, 3], "wall"), box([1, 2, 13], [15, 4, 15], "wall"),
            box([1, 2, 3], [3, 4, 13], "wall"), box([13, 2, 3], [15, 4, 13], "wall"),
            box([3, 2, 3], [13, 2.01, 13], "in", "up"),
        ]}),
    "windup_drive": ("Windup Drive", "axe", {
        "textures": {"particle": MC + "oak_planks", "wood": MC + "oak_planks", "stone": MC + "smooth_stone", "metal": MC + "gold_block",
                     "key": MC + "iron_block"},
        "elements": [
            box([0, 0, 0], [16, 4, 16], "stone"), box([1, 4, 1], [15, 16, 15], "wood"),
            box([5, 7, 0], [11, 13, 1], "metal"), box([7, 9, 15], [9, 11, 16], "key"), box([5, 9.5, 16], [11, 10.5, 16.01], "key", "south"),
        ]}),
    "furnace_drive": ("Furnace Drive", "pickaxe", {
        "parent": "minecraft:block/orientable",
        "textures": {"top": MC + "blast_furnace_top", "front": MC + "blast_furnace_front", "side": MC + "blast_furnace_side"}}),
    "powered_drive": ("Powered Drive", "pickaxe", {
        "textures": {"particle": MC + "iron_block", "case": MC + "iron_block", "stone": MC + "smooth_stone", "strip": MC + "redstone_block"},
        "elements": [
            box([0, 0, 0], [16, 4, 16], "stone"), box([1, 4, 1], [15, 16, 15], "case"),
            box([3, 8, 0], [13, 11, 1], "strip"),
        ]}),
    "auto_hammer": ("Auto Hammer", "axe", {
        "textures": {"particle": MC + "oak_planks", "wood": MC + "oak_planks", "post": MC + "stripped_oak_log", "stone": MC + "smooth_stone",
                     "head": MC + "iron_block"},
        "elements": [
            box([0, 0, 0], [16, 3, 16], "stone"),
            box([1, 3, 6], [3, 16, 10], "post"), box([13, 3, 6], [15, 16, 10], "post"),
            box([3, 13, 7], [13, 15, 9], "wood"),
            box([7, 8, 7], [9, 13, 9], "wood"), box([5, 4, 5], [11, 8, 11], "head"),
        ]}),
    "stone_meal": ("Stone Meal", "shovel", None),
    "scoria": ("Scoria", "shovel", None),
    "pale_sand": ("Pale Sand", "shovel", None),
}

# id: (display name, held like a tool)
ITEMS = {
    "wooden_hammer": ("Wooden Hammer", True), "stone_hammer": ("Stone Hammer", True), "copper_hammer": ("Copper Hammer", True),
    "iron_hammer": ("Iron Hammer", True), "diamond_hammer": ("Diamond Hammer", True), "netherite_hammer": ("Netherite Hammer", True),
    "cloth_mesh": ("Cloth Mesh", False), "copper_mesh": ("Copper Mesh", False), "iron_mesh": ("Iron Mesh", False),
    "diamond_mesh": ("Diamond Mesh", False), "netherite_mesh": ("Netherite Mesh", False),
    "plant_fiber": ("Plant Fiber", False), "rock_chip": ("Rock Chip", False), "copper_dust": ("Copper Dust", False),
    "iron_dust": ("Iron Dust", False), "gold_dust": ("Gold Dust", False), "salt_dust": ("Salt Dust", False),
    "quartz_seed": ("Quartz Seed", False), "amethyst_seed": ("Amethyst Seed", False), "lapis_seed": ("Lapis Seed", False),
    "diamond_seed": ("Diamond Seed", False), "emerald_seed": ("Emerald Seed", False),
    "raw_crucible": ("Unfired Crucible", False), "raw_casting_tray": ("Unfired Casting Tray", False),
    "raw_clay_barrel": ("Unfired Clay Barrel", False), "brine_bucket": ("Brine Bucket", False),
    "molten_copper_bucket": ("Molten Copper Bucket", False), "molten_iron_bucket": ("Molten Iron Bucket", False),
    "molten_gold_bucket": ("Molten Gold Bucket", False),
}

EXTRA_LANG = {
    "itemGroup.primamateria": "Prima Materia",
    "fluid.primamateria.brine": "Brine",
    "block.primamateria.brine": "Brine",
    "fluid.primamateria.molten_copper": "Molten Copper",
    "fluid.primamateria.molten_iron": "Molten Iron",
    "fluid.primamateria.molten_gold": "Molten Gold",
    "block.primamateria.molten_copper": "Molten Copper",
    "block.primamateria.molten_iron": "Molten Iron",
    "block.primamateria.molten_gold": "Molten Gold",
    "generator.primamateria.void_island": "Void Island",
}

# How a model block sits in the hand and in a slot: the vanilla block item transforms.
BLOCK_PARENT = "minecraft:block/block"


def write(path, content):
    if path.exists() and not FORCE:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, indent=2) + "\n", encoding="utf-8", newline="\n")
    return True


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
    for bid, (name, tool, model) in BLOCKS.items():
        lang[f"block.{MOD}.{bid}"] = name
        tools.setdefault(tool, []).append(f"{MOD}:{bid}")
        if model is None:
            model = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{bid}"}}
        elif "parent" not in model:
            model = {"parent": BLOCK_PARENT, **model}
        made += write(ASSETS / f"models/block/{bid}.json", model)
        made += write(ASSETS / f"blockstates/{bid}.json", {"variants": {"": {"model": f"{MOD}:block/{bid}"}}})
        made += write(ASSETS / f"items/{bid}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{bid}"}})
        made += write(ROOT / f"version-26.2/src/main/resources/data/{MOD}/loot_table/blocks/{bid}.json", loot(bid, False))
        made += write(ROOT / f"version-26.3/src/main/resources/data/{MOD}/loot_table/blocks/{bid}.json", loot(bid, True))
    for iid, (name, handheld) in ITEMS.items():
        lang[f"item.{MOD}.{iid}"] = name
        parent = "minecraft:item/handheld" if handheld else "minecraft:item/generated"
        made += write(ASSETS / f"models/item/{iid}.json", {"parent": parent, "textures": {"layer0": f"{MOD}:item/{iid}"}})
        made += write(ASSETS / f"items/{iid}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{iid}"}})
    lang.update(EXTRA_LANG)
    made += write(ASSETS / "lang/en_us.json", lang)
    made += write(ASSETS / "sounds.json", {})
    for tool, values in tools.items():
        made += write(DATA / f"minecraft/tags/block/mineable/{tool}.json", {"replace": False, "values": values})
    print(f"stand-ins: {made} files written, {len(BLOCKS)} blocks, {len(ITEMS)} items")


if __name__ == "__main__":
    main()
