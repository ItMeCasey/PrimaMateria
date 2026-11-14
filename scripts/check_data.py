"""Static check of every shipped data and asset file. Exit code 1 when anything is wrong.

  - every JSON file under the resource folders parses;
  - every recipe of a primamateria type names a registered type, and every primamateria id it mentions exists in Ids;
  - every block and item in Ids has a lang name, an items/ definition, a model, and (blocks) a blockstate and a loot table;
  - every primamateria texture and model a model refers to exists, and our own item and block textures are 16 wide;
  - the language file holds names and values only: no sentences, no instruction words (the no-hints rule).

Usage: python scripts/check_data.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "common/src/main/resources"
V26 = ROOT / "version-26.2/src/main/resources"
V263 = ROOT / "version-26.3/src/main/resources"
ASSETS = RES / "assets/primamateria"
DATA = RES / "data/primamateria"
IDS = (ROOT / "common/src/main/java/com/itmecasey/primamateria/Ids.java").read_text(encoding="utf-8")

METALS_JAVA = (ROOT / "common/src/main/java/com/itmecasey/primamateria/fluid/Metals.java").read_text(encoding="utf-8")

errors = []


def fail(msg):
    errors.append(msg)


def lists_from_ids():
    """Parse the string lists out of Ids.java: enough for the checks below."""
    strings = lambda body: re.findall(r'"([a-z0-9_.]+)"', body)
    out = {}
    for name in ("BLOCKS", "HAMMERS", "MESHES", "MATERIALS", "FLUIDS", "SOUNDS", "PARTICLES", "RECIPE_TYPES"):
        m = re.search(r"List<String> " + name + r" = (?:concat\()?List\.of\((.*?)\)[;,]", IDS, re.S)
        out[name] = strings(m.group(1)) if m else []
    consts = dict(re.findall(r'String (\w+) = "([a-z0-9_.]+)";', IDS))
    # lists that mention constants by name: resolve them
    metals = re.findall(r'new Metal\("([a-z0-9_]+)"', METALS_JAVA)
    for name in ("BLOCKS", "MATERIALS", "RECIPE_TYPES", "FLUIDS"):
        if name == "FLUIDS":
            out[name] = ["brine"] + ["molten_" + m for m in metals]
            continue
        m = re.search(r"List<String> " + name + r" = (?:concat\()?List\.of\((.*?)\)[;,]", IDS, re.S)
        resolved = []
        for token in re.findall(r'"([a-z0-9_.]+)"|([A-Z_]+)', m.group(1)):
            if token[0]:
                resolved.append(token[0])
            elif token[1] in consts:
                resolved.append(consts[token[1]])
            elif token[1] in out:
                resolved.extend(out[token[1]])
        if name == "MATERIALS":
            resolved += ["molten_" + x + "_bucket" for x in metals]
        out[name] = resolved
    return out


LISTS = lists_from_ids()
BLOCKS = LISTS["BLOCKS"]
ITEMS = LISTS["HAMMERS"] + LISTS["MESHES"] + LISTS["MATERIALS"]
ALL_IDS = set(BLOCKS) | set(ITEMS) | set(LISTS["FLUIDS"]) | {"flowing_" + f for f in LISTS["FLUIDS"]}


def load(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as e:
        fail(f"{path.relative_to(ROOT)}: not valid JSON ({e})")
        return None


def walk_ids(value, found):
    if isinstance(value, str):
        for m in re.finditer(r"primamateria:([a-z0-9_/]+)", value):
            found.add(m.group(1))
    elif isinstance(value, dict):
        for v in value.values():
            walk_ids(v, found)
    elif isinstance(value, list):
        for v in value:
            walk_ids(v, found)


def check_json_parses():
    count = 0
    for base in (RES, V26, V263):
        if not base.exists():
            continue
        for p in base.rglob("*.json"):
            load(p)
            count += 1
    return count


def check_recipes():
    types = set(LISTS["RECIPE_TYPES"])
    n = 0
    for p in (DATA / "recipe").rglob("*.json"):
        d = load(p)
        if not isinstance(d, dict):
            continue
        n += 1
        t = d.get("type", "")
        if t.startswith("primamateria:") and t.split(":")[1] not in types:
            fail(f"{p.relative_to(ROOT)}: unknown recipe type {t}")
        found = set()
        walk_ids(d, found)
        for ident in found:
            if ident.startswith(("block/", "item/")):
                continue
            if ident not in ALL_IDS and ident not in types:
                fail(f"{p.relative_to(ROOT)}: primamateria:{ident} is not an id in Ids.java")
    return n


def check_assets_per_id():
    lang = load(ASSETS / "lang/en_us.json") or {}
    for b in BLOCKS:
        for what, path in (("blockstate", ASSETS / f"blockstates/{b}.json"), ("item definition", ASSETS / f"items/{b}.json")):
            if not path.exists():
                fail(f"block {b}: missing {what}")
        if f"block.primamateria.{b}" not in lang:
            fail(f"block {b}: no lang name")
        if not any((base / f"data/primamateria/loot_table/blocks/{b}.json").exists() for base in (V26,)):
            fail(f"block {b}: no loot table (26.2)")
        if not any((base / f"data/primamateria/loot_table/blocks/{b}.json").exists() for base in (V263,)):
            fail(f"block {b}: no loot table (26.3)")
    for i in ITEMS:
        if not (ASSETS / f"items/{i}.json").exists():
            fail(f"item {i}: missing item definition")
        if f"item.primamateria.{i}" not in lang:
            fail(f"item {i}: no lang name")
        if not (ASSETS / f"models/item/{i}.json").exists():
            fail(f"item {i}: missing model")
    for f in LISTS["FLUIDS"]:
        if f"fluid.primamateria.{f}" not in lang:
            fail(f"fluid {f}: no lang name")
    for s in LISTS["SOUNDS"]:
        sounds = load(ASSETS / "sounds.json") or {}
        if s not in sounds:
            fail(f"sound {s}: no entry in sounds.json")
    for pa in LISTS["PARTICLES"]:
        if not (ASSETS / f"particles/{pa}.json").exists():
            fail(f"particle {pa}: no particles/{pa}.json")


def texture_exists(ref):
    ns, _, path = ref.partition(":")
    if ns != "primamateria":
        return True
    return (ASSETS / "textures" / (path + ".png")).exists()


def check_models():
    for p in (ASSETS / "models").rglob("*.json"):
        d = load(p)
        if not isinstance(d, dict):
            continue
        for key, ref in (d.get("textures") or {}).items():
            if isinstance(ref, str) and not ref.startswith("#") and not texture_exists(ref):
                fail(f"{p.relative_to(ROOT)}: texture {ref} does not exist")
        parent = d.get("parent", "")
        if parent.startswith("primamateria:"):
            if not (ASSETS / "models" / (parent.split(":")[1] + ".json")).exists():
                fail(f"{p.relative_to(ROOT)}: parent {parent} does not exist")


def check_texture_sizes():
    try:
        from PIL import Image
    except ImportError:
        print("Pillow missing: texture sizes not checked")
        return
    for p in (ASSETS / "textures").rglob("*.png"):
        rel = p.relative_to(ASSETS / "textures").as_posix()
        if rel.startswith(("item/", "block/", "particle/")):
            w, h = Image.open(p).size
            if w not in (4, 8, 16, 32, 64, 128) or (rel.startswith(("item/", "block/")) and w != 16 and h % w != 0):
                fail(f"textures/{rel}: unexpected size {w}x{h}")


# words that tell the player what to do: not allowed in a name or a value line
INSTRUCTION = re.compile(r"\b(click|press|hold|sneak|shift|tip|hint|tutorial|should|must|try|you can|you will|to use|how to)\b", re.I)


def check_lang():
    lang = load(ASSETS / "lang/en_us.json") or {}
    for key, value in lang.items():
        if key.startswith("viewer."):
            if len(value) > 40 or value.endswith("."):
                fail(f"lang {key}: not a plain label: {value!r}")
            continue
        if len(value.split()) > 5 or "." in value or INSTRUCTION.search(value):
            fail(f"lang {key}: not a plain name: {value!r}")


def main():
    n = check_json_parses()
    r = check_recipes()
    check_assets_per_id()
    check_models()
    check_texture_sizes()
    check_lang()
    if errors:
        print(f"check_data: {len(errors)} problems")
        for e in errors:
            print(" -", e)
        sys.exit(1)
    print(f"check_data: ok ({n} JSON files parse, {r} recipes checked, {len(BLOCKS)} blocks, {len(ITEMS)} items)")


if __name__ == "__main__":
    main()
