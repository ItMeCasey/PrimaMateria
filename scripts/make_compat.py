"""Writes the ore compat: sifting, crushing, melting and casting data for the materials other mods add, and the molten fluid of
every metal (assets, language file, the Java table).

Source of the material list: scripts/compat_materials.json (the union of the 26.2 and 26.3 lists in Mod-Playbook/ore-matrix/
PRIMA_COMPAT_LISTS.md: material, its forms, the mods that carry it).

Everything it writes is plain data a pack can edit or remove:
  - data/primamateria/recipe/**           recipes that name items by convention tag (#c:ingots/tin ...), so any mod that fills the tag is
                                          picked up; a tag that no installed mod fills resolves to nothing and the entry does nothing
  - data/c/tags/item/**                   optional tag entries (required: false) for items of mods that have no convention tag
  - assets/primamateria/**                the molten fluid textures, models, bucket icon and names of every metal
  - common/.../fluid/Metals.java          the metal table the fluid code reads

Usage: python scripts/make_compat.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))
RES = ROOT / "common/src/main/resources"
DATA = RES / "data/primamateria"
ASSETS = RES / "assets/primamateria"

# metal id (the tag stem: c:ingots/<id>) -> display name, color, hot glow (light level 0-15), temperature in kelvin
METALS = {
    "copper": ("Copper", "D8742A", 12, 1358), "iron": ("Iron", "E6C9A8", 13, 1811), "gold": ("Gold", "F5C623", 13, 1337),
    "netherite": ("Netherite", "5B4A4C", 12, 2000),
    "tin": ("Tin", "C8D2D4", 11, 505), "lead": ("Lead", "6B6F86", 11, 600), "silver": ("Silver", "D6E0EA", 12, 1235),
    "nickel": ("Nickel", "B9C2A8", 12, 1728), "uranium": ("Uranium", "7FA63A", 13, 1405), "aluminum": ("Aluminum", "CFD6DC", 12, 933),
    "titanium": ("Titanium", "8F9BB0", 13, 1941), "palladium": ("Palladium", "B4A8C8", 12, 1828), "steel": ("Steel", "8D949C", 12, 1700),
    "bronze": ("Bronze", "C28A3C", 12, 1190), "electrum": ("Electrum", "E6CF6A", 12, 1300), "invar": ("Invar", "A8B4AA", 12, 1700),
    "cupronickel": ("Cupronickel", "C9A07A", 12, 1500), "advanced_alloy": ("Advanced Alloy", "D9B45A", 12, 1500),
    "crystallized_alloy": ("Crystallized Alloy", "8FE0D0", 12, 1500), "energized_alloy": ("Energized Alloy", "E8D870", 12, 1500),
    "redstone_alloy": ("Redstone Alloy", "C8402C", 12, 1500), "netherite_alloy": ("Netherite Alloy", "4A3F40", 12, 1900),
    "shielding_alloy": ("Shielding Alloy", "5A7AA0", 12, 1600), "fluxsteel": ("Fluxsteel", "6FD0E0", 13, 1700),
    "aerogold": ("Aerogold", "F0D890", 13, 1337), "arsonist": ("Arsonist", "E0602A", 14, 1600), "allthemodium": ("Allthemodium", "D9B020", 13, 2100),
    "unobtainium": ("Unobtainium", "8A4FC8", 13, 2300), "vibranium": ("Vibranium", "6AD0D0", 13, 2200), "enderite": ("Enderite", "3A2A4A", 12, 2000),
    "enderium": ("Enderium", "1F8F8A", 12, 1900), "iesnium": ("Iesnium", "9FD0FF", 13, 1800), "necoium": ("Necoium", "C04060", 12, 1700),
    "neptunium": ("Neptunium", "4A90C8", 12, 1700), "nerosium": ("Nerosium", "4CC0A0", 12, 1700), "nerosteel": ("Nerosteel", "2F6F6F", 12, 1700),
    "vulpus": ("Vulpus", "D07030", 13, 1700), "zurite": ("Zurite", "A0E0F0", 12, 1700), "azure_electrum": ("Azure Electrum", "7AB0E0", 12, 1300),
    "calorite": ("Calorite", "E04830", 13, 1800), "desh": ("Desh", "E07A4A", 12, 1700), "ostrum": ("Ostrum", "C04A6A", 12, 1700),
}
# tier of the sifting drop: (source block, mesh_min (1 cloth .. 5 netherite), chance)
COMMON = ("minecraft:gravel", 2, 0.10)
UNCOMMON = ("minecraft:gravel", 3, 0.06)
RARE = ("primamateria:stone_meal", 4, 0.04)
EXOTIC = ("primamateria:pale_sand", 4, 0.04)
NETHER = ("primamateria:scoria", 3, 0.05)
METAL_TIER = {"tin": COMMON, "lead": COMMON, "aluminum": COMMON, "nickel": COMMON, "silver": UNCOMMON, "uranium": UNCOMMON,
              "titanium": UNCOMMON, "palladium": UNCOMMON, "nerosium": NETHER, "nerosteel": NETHER, "calorite": NETHER, "desh": EXOTIC,
              "ostrum": EXOTIC, "netherite": NETHER}
HAMMER_TIER = {COMMON: 2, UNCOMMON: 3, RARE: 4, EXOTIC: 4, NETHER: 3}
GEM_RARE = {"ruby", "sapphire", "topaz", "tanzanite", "alexandrite", "peridot", "aquamarine", "ametrine", "citrine", "kyanite", "iolite",
            "tanzanite", "heliodor", "moldavite", "chaos", "ammolite", "cindrite", "glacite", "jade"}
EXISTING = {"copper", "iron", "gold"}  # their own recipes already exist; compat adds the tag forms only

# items of mods that fill no convention tag: optional tag entries (a missing mod leaves the tag as it was)
GAP_TAGS = {
    "ingots/arsonist": ["aerialhell:arsonist_ingot"], "ingots/aerogold": ["aeroflux:aerogold_ingot"],
    "ingots/fluxsteel": ["aeroflux:fluxsteel_ingot"], "ingots/necoium": ["mythicupgrades:necoium_ingot"],
    "raw_materials/necoium": ["mythicupgrades:raw_necoium"], "raw_materials/volucite": ["aerialhell:raw_volucite"],
    "raw_materials/azurite": ["aerialhell:raw_azurite_crystal"], "raw_materials/ruby": ["aerialhell:raw_ruby"],
    "gems/ruby": ["aerialhell:ruby", "exotelcraft:ruby", "mythicupgrades:ruby"], "gems/opal": ["exotelcraft:opal"],
    "gems/jade": ["mythicupgrades:jade"], "gems/peridot": ["mythicupgrades:peridot"], "gems/sapphire": ["mythicupgrades:sapphire"],
    "gems/topaz": ["mythicupgrades:topaz"], "gems/azurite": ["aerialhell:azurite_crystal"],
    "gems/smoky_quartz": ["aerialhell:smoky_quartz"],
    "ores/azurite": ["aerialhell:azurite_ore"], "ores/fluorite": ["aerialhell:fluorite_ore"], "ores/ruby": ["aerialhell:ruby_ore"],
    "ores/volucite": ["aerialhell:volucite_ore"], "ores/smoky_quartz": ["aerialhell:smoky_quartz_ore"],
}


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8", newline="\n")


def recipes():
    mats = json.loads((Path(__file__).resolve().parent / "compat_materials.json").read_text(encoding="utf-8"))
    # the vanilla metals take part with their convention tags
    for m in ("copper", "iron", "gold", "netherite"):
        mats.setdefault(m, {"forms": ["ingot", "nugget", "ore", "raw"] if m != "netherite" else ["ingot"], "mods": ["minecraft"]})
    count = 0
    for name, info in sorted(mats.items()):
        forms = set(info["forms"])
        metal = name in METALS
        product = ("raw_materials", "raw") if "raw" in forms else ("dusts", "dust") if "dust" in forms else ("gems", "gem") if "gem" in forms else None
        if name in EXISTING:
            product = ("raw_materials", "raw")
        ptag = f"#c:{product[0]}/{name}" if product else None
        if metal:
            tier = METAL_TIER.get(name, EXOTIC if name not in EXISTING and name not in ("steel", "bronze", "electrum", "invar", "cupronickel") else None)
            # melting: every form of the metal that other mods add (inputs are tags, a tag nobody fills never matches)
            for tag, amount in (("ingots", 100), ("raw_materials", 100), ("dusts", 100), ("nuggets", 11)):
                if name in EXISTING and tag == "dusts":
                    continue
                write_json(DATA / f"recipe/melting/compat_{name}_{tag}.json",
                           {"type": "primamateria:melting", "input": f"#c:{tag}/{name}", "result": f"primamateria:molten_{name}", "amount": amount})
                count += 1
            write_json(DATA / f"recipe/casting/compat_{name}_ingot.json",
                       {"type": "primamateria:casting", "fluid": f"primamateria:molten_{name}", "amount": 100, "result": f"#c:ingots/{name}", "time": 60})
            count += 1
        # crushing: the ore block (any mod's, by tag) gives its product, twice
        if ptag and ("ore" in forms or name in EXISTING):
            tier = METAL_TIER.get(name, EXOTIC if (metal and name not in EXISTING) else (RARE if name in GEM_RARE else UNCOMMON))
            write_json(DATA / f"recipe/crushing/compat_{name}_ore.json",
                       {"type": "primamateria:crushing", "input": f"#c:ores/{name}", "hammer_tier": HAMMER_TIER.get(tier, 3),
                        "results": [{"item": ptag, "count": 2}]})
            count += 1
        # sifting: the metal's raw material or dust, or the gem, from a source block by rarity
        if ptag and name not in EXISTING and ("ore" in forms or product[1] in ("gem",)) and not (metal and "ore" not in forms and "raw" not in forms and "dust" not in forms):
            if metal:
                tier = METAL_TIER.get(name, EXOTIC)
            else:
                tier = RARE if name in GEM_RARE else ("primamateria:stone_meal", 3, 0.05)
            source, mesh_min, chance = tier
            write_json(DATA / f"recipe/sifting/{source.split(':')[1]}/compat_{name}.json",
                       {"type": "primamateria:sifting", "input": source, "result": ptag, "chance": chance, "mesh_min": mesh_min})
            count += 1
    # optional tag entries for items without a convention tag
    for tag, ids in GAP_TAGS.items():
        write_json(RES / f"data/c/tags/item/{tag}.json", {"replace": False, "values": [{"id": i, "required": False} for i in ids]})
        count += 1
    # every tag a recipe takes as an input must exist even when no mod fills it (an input naming a missing tag does not load), so each
    # one gets an empty tag file; mods that fill the tag add to it
    used = set()
    for path in (DATA / "recipe").rglob("compat_*.json"):
        d = json.loads(path.read_text(encoding="utf-8"))
        if isinstance(d.get("input"), str) and d["input"].startswith("#c:"):
            used.add(d["input"][3:])
    for tag in sorted(used):
        target = RES / f"data/c/tags/item/{tag}.json"
        if not target.exists():
            write_json(target, {"replace": False, "values": []})
            count += 1
    return count


def fluids():
    import make_fluid_textures as ft
    import make_textures as mt
    java = ["package com.itmecasey.primamateria.fluid;", "", "import java.util.List;", "",
            "/** Generated by scripts/make_compat.py: the metals that have a molten fluid, a bucket and a crucible look. */",
            "public final class Metals {",
            "    /** id is the convention tag stem (c:ingots/&lt;id&gt;) and the fluid is primamateria:molten_&lt;id&gt;. */",
            "    public record Metal(String id, int color, int glow, int temperature) {",
            "        public String fluid() {", '            return "molten_" + id;', "        }", "    }", "",
            "    public static final List<Metal> ALL = List.of("]
    lang = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    entries = []
    seed = 100
    for mid, (display, hexcolor, glow, temp) in METALS.items():
        seed += 7
        color = int(hexcolor, 16)
        entries.append(f'            new Metal("{mid}", 0x{hexcolor}, {glow}, {temp})')
        fid = f"molten_{mid}"
        lang[f"item.primamateria.{fid}_bucket"] = f"Molten {display} Bucket"
        lang[f"fluid.primamateria.{fid}"] = f"Molten {display}"
        lang[f"block.primamateria.{fid}"] = f"Molten {display}"
        if mid in ("copper", "iron", "gold"):
            continue  # hand-tuned assets exist
        base = tuple(int(hexcolor[i:i + 2], 16) for i in (0, 2, 4))

        def shade(f):
            return tuple(max(0, min(255, round(c * f if f <= 1 else c + (255 - c) * (f - 1)))) for c in base)
        colors = [shade(0.45), shade(0.7), shade(1.0), shade(1.25), shade(1.55)]
        ft.save(ft.molten_still(colors, seed), fid + "_still", 4, 16)
        ft.save(ft.molten_flow(colors, seed + 1), fid + "_flow", 3, 32)
        # bucket icon: the shared bucket drawn with the metal's ember ramp as the fill
        fill = [tuple(round(c * f) if f <= 1 else round(c + (255 - c) * (f - 1))
                      for c in base) + (255,) for f in (0.4, 0.62, 0.85, 1.05, 1.3, 1.55)]
        img = mt.sprite(mt.BUCKET, mt.R["bucket"], fill)
        path = ASSETS / f"textures/item/{fid}_bucket.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
        write_json(ASSETS / f"blockstates/{fid}.json", {"variants": {"": {"model": f"primamateria:block/{fid}"}}})
        write_json(ASSETS / f"items/{fid}_bucket.json", {"model": {"type": "minecraft:model", "model": f"primamateria:item/{fid}_bucket"}})
        write_json(ASSETS / f"models/block/{fid}.json", {"textures": {"particle": f"primamateria:block/{fid}_still"}})
        write_json(ASSETS / f"models/item/{fid}_bucket.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"primamateria:item/{fid}_bucket"}})
    java.append(",\n".join(entries))
    java += ["    );", "", "    private Metals() {", "    }", "}", ""]
    out = ROOT / "common/src/main/java/com/itmecasey/primamateria/fluid/Metals.java"
    out.write_text("\n".join(java).replace("&lt;", "<").replace("&gt;", ">"), encoding="utf-8", newline="\n")
    ordered = dict(sorted(lang.items(), key=lambda kv: list(lang).index(kv[0])))
    write_json(ASSETS / "lang/en_us.json", ordered)
    return len(METALS)


def main():
    n = recipes()
    f = fluids()
    print(f"compat: {n} data files, {f} metal fluids")


if __name__ == "__main__":
    main()
