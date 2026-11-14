"""Writes the shipped barrel data: soaking, mixing, steeping, growing and composting recipes, the compost values, and the crafting
and smelting recipes of the two barrels (docs/DESIGN.md section 8).

The tables below ARE the balance. Change a number here (or in a datapack), rerun, and the files are rewritten. Times are in
game ticks (20 per second).

Usage: python scripts/make_barrel_data.py
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "common/src/main/resources/data/primamateria"
MC = "minecraft:"
PM = "primamateria:"

# name: (fluid, amount mB, input item, result item, ticks)
SOAKING = {
    "clay_from_stone_meal": (MC + "water", 250, PM + "stone_meal", MC + "clay", 100),
    "mud_from_dirt": (MC + "water", 250, MC + "dirt", MC + "mud", 100),
    "soul_sand_from_scoria": (PM + "brine", 250, PM + "scoria", MC + "soul_sand", 100),
}

# name: (base fluid, added fluid, amount each, result item, needs heatproof barrel)
MIXING = {
    "obsidian": (MC + "lava", MC + "water", 1000, MC + "obsidian", True),
    "stone": (MC + "water", MC + "lava", 1000, MC + "stone", True),
}

# name: (fluid, min amount, input, count, result fluid, ticks)
STEEPING = {
    "brine": (MC + "water", 1000, PM + "salt_dust", 4, PM + "brine", 600),
}

# name: (seed, result item, result count, ticks, crystal texture)
GROWING = {
    "quartz": (PM + "quartz_seed", MC + "quartz", 2, 1800, PM + "block/crystal_quartz"),
    "amethyst": (PM + "amethyst_seed", MC + "amethyst_shard", 2, 2400, PM + "block/crystal_amethyst"),
    "lapis": (PM + "lapis_seed", MC + "lapis_lazuli", 3, 2400, PM + "block/crystal_lapis"),
    "diamond": (PM + "diamond_seed", MC + "diamond", 1, 6000, PM + "block/crystal_diamond"),
    "emerald": (PM + "emerald_seed", MC + "emerald", 1, 6000, PM + "block/crystal_emerald"),
}
GROWING_FLUID = (PM + "brine", 250)

COMPOSTING_RESULT = (MC + "dirt", 400)

# name: (item or #tag, units, mature boost)
COMPOST = {
    "sapling": ("#minecraft:saplings", 100, 0),
    "leaves": ("#minecraft:leaves", 80, 0),
    "plant_fiber": (PM + "plant_fiber", 50, 0),
    "wheat_seeds": (MC + "wheat_seeds", 60, 0),
    "beetroot_seeds": (MC + "beetroot_seeds", 60, 0),
    "melon_seeds": (MC + "melon_seeds", 60, 0),
    "pumpkin_seeds": (MC + "pumpkin_seeds", 60, 0),
    "wheat": (MC + "wheat", 80, 0),
    "apple": (MC + "apple", 100, 0),
    "flowers": ("#minecraft:flowers", 80, 0),
    "cactus": (MC + "cactus", 100, 0),
    "sugar_cane": (MC + "sugar_cane", 80, 0),
    "rotten_flesh": (MC + "rotten_flesh", 100, 0),
    "bread": (MC + "bread", 160, 0),
    "bone_meal": (MC + "bone_meal", 40, 0.5),
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def main():
    for sub in ("recipe/soaking", "recipe/mixing", "recipe/steeping", "recipe/growing", "recipe/composting", "primamateria/compost"):
        shutil.rmtree(DATA / sub, ignore_errors=True)
    n = 0
    for name, (fluid, amount, item, result, time) in SOAKING.items():
        write(DATA / "recipe/soaking" / f"{name}.json", {"type": PM + "soaking", "fluid": fluid, "amount": amount, "input": item,
                                                       "result": result, "time": time})
        n += 1
    for name, (base, added, amount, result, hot) in MIXING.items():
        write(DATA / "recipe/mixing" / f"{name}.json", {"type": PM + "mixing", "base_fluid": base, "added_fluid": added, "amount": amount,
                                                      "result": result, "needs_heatproof": hot})
        n += 1
    for name, (fluid, amount, item, count, result_fluid, time) in STEEPING.items():
        write(DATA / "recipe/steeping" / f"{name}.json", {"type": PM + "steeping", "fluid": fluid, "amount": amount, "input": item,
                                                        "count": count, "result_fluid": result_fluid, "time": time})
        n += 1
    for name, (seed, result, count, time, crystal) in GROWING.items():
        write(DATA / "recipe/growing" / f"{name}.json", {"type": PM + "growing", "fluid": GROWING_FLUID[0], "amount": GROWING_FLUID[1],
                                                       "seed": seed, "result": result, "count": count, "time": time, "crystal": crystal})
        n += 1
    write(DATA / "recipe/composting/dirt.json", {"type": PM + "composting", "result": COMPOSTING_RESULT[0], "time": COMPOSTING_RESULT[1]})
    n += 1
    for name, (what, units, boost) in COMPOST.items():
        body = {"tag": what[1:]} if what.startswith("#") else {"item": what}
        body["units"] = units
        if boost:
            body["mature_boost"] = boost
        write(DATA / "primamateria/compost" / f"{name}.json", body)
        n += 1

    # crafting: the wooden barrel, the unfired clay barrel, and firing it
    write(DATA / "recipe/wooden_barrel.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", "P P", "PPP"],
                                               "key": {"P": MC + "oak_planks"}, "result": {"id": PM + "wooden_barrel", "count": 1}})
    write(DATA / "recipe/raw_clay_barrel.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["C C", "C C", "CBC"],
                                                 "key": {"C": MC + "clay_ball", "B": MC + "clay"},
                                                 "result": {"id": PM + "raw_clay_barrel", "count": 1}})
    write(DATA / "recipe/clay_barrel_smelting.json", {"type": "minecraft:smelting", "category": "misc", "cookingtime": 200, "experience": 0.1,
                                                      "ingredient": PM + "raw_clay_barrel", "result": {"id": PM + "clay_barrel"}})
    n += 3
    print(f"barrel data: {n} files")


if __name__ == "__main__":
    main()
