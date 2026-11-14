"""Writes the shipped crucible data: the melting and casting recipes, the heat sources, and the crafting and smelting recipes of
the crucible and the casting tray (docs/DESIGN.md section 9).

The tables below ARE the balance. Change a number here (or in a datapack), rerun, and the files are rewritten.

Usage: python scripts/make_crucible_data.py
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "common/src/main/resources/data/primamateria"
MC = "minecraft:"
PM = "primamateria:"

# input -> (fluid, mB). Item ids only: the convention tags differ between 26.x builds and mods.
MELTING = {
    "cobblestone": (MC + "cobblestone", MC + "lava", 200),
    "stone": (MC + "stone", MC + "lava", 200),
    "gravel": (MC + "gravel", MC + "lava", 200),
    "netherrack": (MC + "netherrack", MC + "lava", 500),
    "copper_dust": (PM + "copper_dust", PM + "molten_copper", 20),
    "iron_dust": (PM + "iron_dust", PM + "molten_iron", 20),
    "gold_dust": (PM + "gold_dust", PM + "molten_gold", 20),
    "raw_copper": (MC + "raw_copper", PM + "molten_copper", 100),
    "raw_iron": (MC + "raw_iron", PM + "molten_iron", 100),
    "raw_gold": (MC + "raw_gold", PM + "molten_gold", 100),
    "copper_ingot": (MC + "copper_ingot", PM + "molten_copper", 100),
    "iron_ingot": (MC + "iron_ingot", PM + "molten_iron", 100),
    "gold_ingot": (MC + "gold_ingot", PM + "molten_gold", 100),
}

# fluid -> (ingot, mB, ticks)
CASTING = {
    "copper_ingot": (PM + "molten_copper", MC + "copper_ingot", 100, 60),
    "iron_ingot": (PM + "molten_iron", MC + "iron_ingot", 100, 60),
    "gold_ingot": (PM + "molten_gold", MC + "gold_ingot", 100, 60),
}

# heat sources: (file, block or tag, state, heat)
HEAT = [
    ("torch", {"block": MC + "torch"}, 1),
    ("wall_torch", {"block": MC + "wall_torch"}, 1),
    ("campfire", {"block": MC + "campfire", "state": {"lit": "true"}}, 2),
    ("magma_block", {"block": MC + "magma_block"}, 3),
    ("fire", {"block": MC + "fire"}, 3),
    ("lava", {"block": MC + "lava"}, 4),
    ("soul_campfire", {"block": MC + "soul_campfire", "state": {"lit": "true"}}, 4),
    ("soul_fire", {"block": MC + "soul_fire"}, 5),
]


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def main():
    for sub in ("recipe/melting", "recipe/casting", "primamateria/heat"):
        shutil.rmtree(DATA / sub, ignore_errors=True)
    for name, (item, fluid, amount) in MELTING.items():
        write(DATA / "recipe/melting" / f"{name}.json", {"type": PM + "melting", "input": item, "result": fluid, "amount": amount})
    for name, (fluid, item, amount, ticks) in CASTING.items():
        write(DATA / "recipe/casting" / f"{name}.json", {"type": PM + "casting", "fluid": fluid, "amount": amount, "result": item, "time": ticks})
    for name, body, heat in HEAT:
        write(DATA / "primamateria/heat" / f"{name}.json", {**body, "heat": heat})

    def shaped(name, pattern, key, result):
        write(DATA / "recipe" / f"{name}.json", {"type": MC + "crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
                                                  "result": {"id": result, "count": 1}})

    def smelting(name, ingredient, result):
        write(DATA / "recipe" / f"{name}.json", {"type": MC + "smelting", "category": "misc", "cookingtime": 200, "experience": 0.1,
                                                  "ingredient": ingredient, "result": {"id": result}})

    clay = {"C": MC + "clay_ball"}
    shaped("raw_crucible", ["C C", "CCC"], clay, PM + "raw_crucible")
    shaped("raw_casting_tray", ["CCC"], clay, PM + "raw_casting_tray")
    smelting("crucible_smelting", PM + "raw_crucible", PM + "crucible")
    smelting("casting_tray_smelting", PM + "raw_casting_tray", PM + "casting_tray")
    print(f"crucible data: {len(MELTING)} melting, {len(CASTING)} casting, {len(HEAT)} heat sources, 4 crafting and smelting recipes")


if __name__ == "__main__":
    main()
