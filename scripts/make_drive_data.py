"""Writes the drives' crafting recipes and their blockstates (a front that turns with the player, a lit furnace drive).

Recipes follow docs/DESIGN.md section 7. The blockstate files replace the stand-in ones written by make_placeholders.py, so run
this after that script when it is run with --force.

Usage: python scripts/make_drive_data.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "common/src/main/resources/data/primamateria/recipe"
ASSETS = ROOT / "common/src/main/resources/assets/primamateria"
MC = "minecraft:"
PM = "primamateria:"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def main():
    # windup drive: 4 planks, 3 copper ingots, 1 iron ingot, 1 stick
    write(DATA / "windup_drive.json", {
        "type": MC + "crafting_shaped", "category": "misc",
        "pattern": ["PCP", "CIC", "PSP"],
        "key": {"P": MC + "oak_planks", "C": MC + "copper_ingot", "I": MC + "iron_ingot", "S": MC + "stick"},
        "result": {"id": PM + "windup_drive", "count": 1}})
    # furnace drive: furnace + windup drive + 4 iron ingots + 2 gold ingots
    write(DATA / "furnace_drive.json", {
        "type": MC + "crafting_shapeless", "category": "misc",
        "ingredients": [MC + "furnace", PM + "windup_drive"] + [MC + "iron_ingot"] * 4 + [MC + "gold_ingot"] * 2,
        "result": {"id": PM + "furnace_drive", "count": 1}})
    # powered drive: furnace drive + 4 redstone + 2 diamonds + 2 copper ingots
    write(DATA / "powered_drive.json", {
        "type": MC + "crafting_shapeless", "category": "misc",
        "ingredients": [PM + "furnace_drive"] + [MC + "redstone"] * 4 + [MC + "diamond"] * 2 + [MC + "copper_ingot"] * 2,
        "result": {"id": PM + "powered_drive", "count": 1}})

    rotations = {"north": 0, "east": 90, "south": 180, "west": 270}

    def facing_variants(model, extra=""):
        out = {}
        for facing, y in rotations.items():
            v = {"model": model}
            if y:
                v["y"] = y
            out[f"facing={facing}{extra}"] = v
        return out

    write(ASSETS / "blockstates/windup_drive.json", {"variants": facing_variants(PM + "block/windup_drive")})
    write(ASSETS / "blockstates/powered_drive.json", {"variants": facing_variants(PM + "block/powered_drive")})
    variants = facing_variants(PM + "block/furnace_drive", ",lit=false")
    variants.update(facing_variants(PM + "block/furnace_drive_lit", ",lit=true"))
    write(ASSETS / "blockstates/furnace_drive.json", {"variants": variants})
    write(ASSETS / "models/block/furnace_drive_lit.json", {
        "parent": "minecraft:block/orientable",
        "textures": {"top": MC + "block/blast_furnace_top", "front": MC + "block/blast_furnace_front_on",
                     "side": MC + "block/blast_furnace_side"}})
    print("drive data written")


if __name__ == "__main__":
    main()
