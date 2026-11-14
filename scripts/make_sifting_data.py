"""Writes the shipped sifting data: one recipe file per drop (docs/DESIGN.md section 10), the residue recipes, the five mesh tier
files and the crafting recipes of the sieve and the meshes.

The tables below ARE the balance. Change a number here (or in a datapack), rerun, and the files are rewritten. Datapacks and
scripts change the same files at run time; nothing about drops is in Java.

Usage: python scripts/make_sifting_data.py
"""
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "common/src/main/resources/data/primamateria"
MC = "minecraft:"
PM = "primamateria:"

# input item -> list of (result, count (n or (min, max)), chance, mesh_min, mesh_max)  ; mesh_max 0 = no limit
SIFTING = {
    "dirt": ("minecraft:dirt", [
        (PM + "rock_chip", (1, 2), 0.70, 1, 0),
        (PM + "rock_chip", 1, 0.35, 1, 0),
        (PM + "plant_fiber", 1, 0.15, 1, 0),
        (MC + "wheat_seeds", 1, 0.12, 1, 0),
        (MC + "bone_meal", 1, 0.08, 1, 0),
        (MC + "beetroot_seeds", 1, 0.06, 1, 0),
        (MC + "melon_seeds", 1, 0.04, 1, 0),
        (MC + "pumpkin_seeds", 1, 0.04, 1, 0),
        (MC + "birch_sapling", 1, 0.03, 1, 0),
        (MC + "spruce_sapling", 1, 0.03, 1, 0),
        (MC + "oak_sapling", 1, 0.03, 1, 0),
        (MC + "sugar_cane", 1, 0.04, 2, 0),
        (MC + "bamboo", 1, 0.04, 2, 0),
        (MC + "carrot", 1, 0.04, 2, 0),
        (MC + "potato", 1, 0.04, 2, 0),
    ]),
    "gravel": ("minecraft:gravel", [
        (MC + "flint", 1, 0.22, 1, 0),
        (PM + "copper_dust", (1, 2), 0.14, 1, 0),
        (MC + "coal", 1, 0.09, 1, 0),
        (PM + "iron_dust", 1, 0.10, 2, 2),
        (PM + "iron_dust", (1, 2), 0.16, 3, 0),
        (PM + "gold_dust", 1, 0.05, 3, 0),
    ]),
    "sand": ("minecraft:sand", [
        (PM + "copper_dust", 1, 0.08, 1, 0),
        (MC + "cactus", 1, 0.04, 1, 0),
        (PM + "salt_dust", 1, 0.12, 2, 0),
        (PM + "iron_dust", 1, 0.07, 2, 0),
        (PM + "gold_dust", 1, 0.08, 3, 0),
        (PM + "quartz_seed", 1, 0.06, 3, 0),
    ]),
    "stone_meal": (PM + "stone_meal", [
        (MC + "bone_meal", 1, 0.15, 1, 0),
        (MC + "redstone", (1, 2), 0.10, 2, 0),
        (MC + "gunpowder", 1, 0.06, 2, 0),
        (PM + "quartz_seed", 1, 0.08, 3, 0),
        (MC + "glowstone_dust", 1, 0.05, 3, 0),
        (PM + "lapis_seed", 1, 0.05, 3, 0),
        (PM + "amethyst_seed", 1, 0.05, 3, 0),
        (MC + "lapis_lazuli", (1, 2), 0.06, 4, 0),
        (PM + "diamond_seed", 1, 0.02, 3, 3),
        (PM + "diamond_seed", 1, 0.04, 4, 0),
        (PM + "emerald_seed", 1, 0.02, 4, 0),
    ]),
    "scoria": (PM + "scoria", [
        (MC + "quartz", (1, 2), 0.18, 2, 0),
        (MC + "glowstone_dust", 1, 0.10, 2, 0),
        (PM + "gold_dust", 1, 0.10, 3, 0),
        (MC + "blaze_powder", 1, 0.06, 3, 0),
        (MC + "nether_wart", 1, 0.04, 3, 0),
        (MC + "netherite_scrap", 1, 0.01, 4, 4),
        (MC + "netherite_scrap", 1, 0.025, 5, 0),
    ]),
    "soul_sand": (MC + "soul_sand", [
        (MC + "bone", 1, 0.15, 2, 0),
        (MC + "rotten_flesh", 1, 0.12, 2, 0),
        (MC + "string", 1, 0.12, 2, 0),
        (MC + "gunpowder", 1, 0.08, 2, 0),
        (MC + "spider_eye", 1, 0.06, 2, 0),
        (MC + "slime_ball", 1, 0.05, 3, 0),
        (MC + "ghast_tear", 1, 0.01, 4, 0),
        (MC + "wither_skeleton_skull", 1, 0.003, 5, 0),
    ]),
    "pale_sand": (PM + "pale_sand", [
        (MC + "chorus_fruit", 1, 0.08, 4, 0),
        (MC + "ender_pearl", 1, 0.06, 4, 0),
        (PM + "emerald_seed", 1, 0.04, 4, 0),
        (MC + "shulker_shell", 1, 0.015, 5, 0),
    ]),
}

# input item -> list of (result, chance)
RESIDUE = {
    "gravel": ("minecraft:gravel", [(MC + "sand", 0.15)]),
    "sand": ("minecraft:sand", [(PM + "stone_meal", 0.15)]),
}

# id: (tier, speed, texture slot)
MESHES = {
    "cloth_mesh": (1, 1.0, 1),
    "copper_mesh": (2, 1.0, 2),
    "iron_mesh": (3, 1.25, 3),
    "diamond_mesh": (4, 1.5, 4),
    "netherite_mesh": (5, 2.0, 5),
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def count_field(count):
    return count if isinstance(count, int) else list(count)


def main():
    for sub in ("recipe/sifting", "recipe/sifting_residue", "primamateria/mesh"):
        shutil.rmtree(DATA / sub, ignore_errors=True)
    n = 0
    for name, (item, drops) in SIFTING.items():
        seen = {}
        for result, count, chance, lo, hi in drops:
            short = result.split(":")[1]
            seen[short] = seen.get(short, 0) + 1
            file = short if seen[short] == 1 else f"{short}_{seen[short]}"
            body = {"type": PM + "sifting", "input": item, "result": result, "chance": chance}
            if count != 1:
                body["count"] = count_field(count)
            if lo != 1:
                body["mesh_min"] = lo
            if hi:
                body["mesh_max"] = hi
            write(DATA / "recipe/sifting" / name / f"{file}.json", body)
            n += 1
    for name, (item, results) in RESIDUE.items():
        for result, chance in results:
            write(DATA / "recipe/sifting_residue" / name / f"{result.split(':')[1]}.json",
                  {"type": PM + "sifting_residue", "input": item, "result": result, "chance": chance})
            n += 1
    for mesh, (tier, speed, slot) in MESHES.items():
        write(DATA / "primamateria/mesh" / f"{mesh}.json",
              {"item": PM + mesh, "tier": tier, "speed": speed, "texture": PM + "block/mesh_" + mesh.replace("_mesh", ""), "slot": slot})
        n += 1

    def shaped(name, pattern, key, result, category="misc"):
        write(DATA / "recipe" / f"{name}.json", {"type": MC + "crafting_shaped", "category": category, "pattern": pattern, "key": key,
                                                  "result": {"id": result, "count": 1}})

    shaped("sieve", ["P.P", "PSP", "S.S"], {"P": MC + "oak_planks", "S": MC + "stick"}, PM + "sieve")
    # the pattern uses a space for an empty slot
    for name in ("sieve",):
        path = DATA / "recipe" / f"{name}.json"
        text = path.read_text(encoding="utf-8").replace('"P.P"', '"P P"').replace('"S.S"', '"S S"')
        path.write_text(text, encoding="utf-8", newline="\n")
    mesh_ingredients = {"cloth_mesh": PM + "plant_fiber", "copper_mesh": MC + "copper_ingot", "iron_mesh": MC + "iron_ingot",
                        "diamond_mesh": MC + "diamond"}
    for mesh, material in mesh_ingredients.items():
        shaped(mesh, ["SMS", "MMM", "SMS"], {"S": MC + "stick", "M": material}, PM + mesh)
    write(DATA / "recipe/netherite_mesh.json", {"type": MC + "crafting_shapeless", "category": "misc",
                                                "ingredients": [PM + "diamond_mesh", MC + "netherite_ingot"],
                                                "result": {"id": PM + "netherite_mesh", "count": 1}})
    print(f"sifting data: {n} files, plus the sieve and mesh crafting recipes")


if __name__ == "__main__":
    main()
