"""Writes the shipped crushing data: one recipe file per crushable block (docs/DESIGN.md section 7), the crafting recipes of the
six hammers and the auto hammer, and the plant fiber to string recipe.

The tables below ARE the balance. Change a number here (or in a datapack), rerun, and the files are rewritten.

Usage: python scripts/make_crushing_data.py
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "common/src/main/resources/data/primamateria"
MC = "minecraft:"
PM = "primamateria:"

# file name: (input, hammer tier, [(result, count (n or (min, max)), chance)])
CRUSHING = {
    "cobblestone": (MC + "cobblestone", 1, [(MC + "gravel", 1, 1.0)]),
    "stone": (MC + "stone", 1, [(MC + "gravel", 1, 1.0)]),
    "gravel": (MC + "gravel", 1, [(MC + "sand", 1, 1.0)]),
    "sand": (MC + "sand", 2, [(PM + "stone_meal", 1, 1.0)]),
    "red_sand": (MC + "red_sand", 2, [(PM + "stone_meal", 1, 1.0)]),
    "sandstone": (MC + "sandstone", 1, [(MC + "sand", 2, 1.0)]),
    "red_sandstone": (MC + "red_sandstone", 1, [(MC + "red_sand", 2, 1.0)]),
    "netherrack": (MC + "netherrack", 3, [(PM + "scoria", 1, 1.0)]),
    "end_stone": (MC + "end_stone", 4, [(PM + "pale_sand", 1, 1.0)]),
    # any leaves: plant fiber, a second one 35% of the time
    "leaves": ("#minecraft:leaves", 1, [(PM + "plant_fiber", 1, 1.0), (PM + "plant_fiber", 1, 0.35)]),
}
# the leaves of each tree also give that tree's sapling 8% of the time (and win over the tag recipe above)
TREES = {
    "oak": "oak_sapling", "spruce": "spruce_sapling", "birch": "birch_sapling", "jungle": "jungle_sapling",
    "acacia": "acacia_sapling", "dark_oak": "dark_oak_sapling", "mangrove": "mangrove_propagule", "cherry": "cherry_sapling",
    "azalea": "azalea", "flowering_azalea": "flowering_azalea", "pale_oak": "pale_oak_sapling",
}
for tree, sapling in TREES.items():
    CRUSHING[tree + "_leaves"] = (MC + tree + "_leaves", 1, [(PM + "plant_fiber", 1, 1.0), (PM + "plant_fiber", 1, 0.35), (MC + sapling, 1, 0.08)])

# hammer id: head material
HAMMER_HEADS = {
    "wooden_hammer": "#minecraft:planks", "stone_hammer": MC + "cobblestone", "copper_hammer": MC + "copper_ingot",
    "iron_hammer": MC + "iron_ingot", "diamond_hammer": MC + "diamond",
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def main():
    shutil.rmtree(DATA / "recipe/crushing", ignore_errors=True)
    n = 0
    for name, (item, tier, results) in CRUSHING.items():
        body = {"type": PM + "crushing", "input": item}
        if tier != 1:
            body["hammer_tier"] = tier
        body["results"] = []
        for result, count, chance in results:
            entry = {"item": result}
            if count != 1:
                entry["count"] = count if isinstance(count, int) else list(count)
            if chance != 1.0:
                entry["chance"] = chance
            body["results"].append(entry)
        write(DATA / "recipe/crushing" / f"{name}.json", body)
        n += 1

    def shaped(name, pattern, key, result, category="equipment"):
        write(DATA / "recipe" / f"{name}.json", {"type": MC + "crafting_shaped", "category": category, "pattern": pattern, "key": key,
                                                  "result": {"id": result, "count": 1}})

    for hammer, head in HAMMER_HEADS.items():
        shaped(hammer, ["HH", " S", " S"], {"H": head, "S": MC + "stick"}, PM + hammer)
    write(DATA / "recipe/netherite_hammer.json", {"type": MC + "crafting_shapeless", "category": "equipment",
                                                  "ingredients": [PM + "diamond_hammer", MC + "netherite_ingot"],
                                                  "result": {"id": PM + "netherite_hammer", "count": 1}})
    shaped("auto_hammer", ["PHP", "SIS", "PPP"],
           {"P": "#minecraft:planks", "H": PM + "iron_hammer", "S": MC + "stick", "I": MC + "iron_ingot"}, PM + "auto_hammer", "misc")
    write(DATA / "recipe/string_from_plant_fiber.json", {"type": MC + "crafting_shapeless", "category": "misc",
                                                         "ingredients": [PM + "plant_fiber"] * 3,
                                                         "result": {"id": MC + "string", "count": 1}})
    print(f"crushing data: {n} crushing recipes, hammer, auto hammer and string recipes")


if __name__ == "__main__":
    main()
