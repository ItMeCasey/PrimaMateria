"""Writes the Prima Materia wiki pages that describe the game (the integration pages, scripting and compatibility, are written by
hand elsewhere). Drop tables, hammer recipes, config keys and barrel recipes are read from the shipped data and Settings.java, so
the wiki never drifts from the game. Plain reference: names, numbers and tables, no hints.

Usage: python scripts/make_wiki.py [wiki folder]
Default folder: G:\\Other computers\\My Computer\\Projects\\ItMeCasey\\website\\d-minimal\\wiki\\primamateria
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "common/src/main/resources/data/primamateria"
WIKI = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r"G:\Other computers\My Computer\Projects\ItMeCasey\website\d-minimal\wiki\primamateria")


def load(path):
    return json.loads(Path(path).read_text(encoding="utf-8"))


def name(item_id):
    return item_id.split(":", 1)[1].replace("_", " ").title() if ":" in item_id else item_id


def pct(x):
    s = f"{x * 100:.1f}".rstrip("0").rstrip(".")
    return s + "%"


def count(c):
    if isinstance(c, list):
        return f"{c[0]} to {c[1]}"
    return str(c)


def page(slug, title, summary, body):
    text = f"---\ntitle: {title}\nsummary: {summary}\n---\n{body.strip()}\n"
    (WIKI / "pages").mkdir(parents=True, exist_ok=True)
    (WIKI / "pages" / f"{slug}.md").write_text(text, encoding="utf-8", newline="\n")


def settings_table(prefix):
    text = (ROOT / "common/src/main/java/com/itmecasey/primamateria/Settings.java").read_text(encoding="utf-8")
    rows = []
    for m in re.finditer(r'def\("([a-z_.]+)",\s*([^,]+),\s*"((?:[^"\\]|\\.)*)"\);', text):
        key, default, comment = m.group(1), m.group(2).strip().strip('"'), m.group(3)
        if key.startswith(prefix):
            rows.append(f"| `{key}` | `{default}` | {comment} |")
    return rows


def recipes_of(folder):
    out = []
    for f in sorted((DATA / "recipe" / folder).rglob("*.json")):
        out.append((f.relative_to(DATA / "recipe" / folder).as_posix(), load(f)))
    return out


# ---------------------------------------------------------------- drop tables
def drop_tables():
    by_input = {}
    for rel, r in recipes_of("sifting"):
        by_input.setdefault(r["input"], []).append(r)
    res = {}
    for rel, r in recipes_of("sifting_residue"):
        res.setdefault(r["input"], []).append(r)
    lines = ["Every shipped sifting recipe, one table per sifted block. The mesh column is the range of mesh tiers that can give the drop "
             "(1 cloth, 2 copper, 3 iron, 4 diamond, 5 netherite). Chances are per block sifted. A datapack, KubeJS or ModJS can change "
             "any of these; see [[recipes]].", ""]
    for item in sorted(by_input):
        lines += [f"## {name(item)}", "", "| Drop | Count | Chance | Mesh |", "|---|---|---|---|"]
        for r in by_input[item]:
            lo, hi = r.get("mesh_min", 1), r.get("mesh_max", 0)
            mesh = f"{lo}" if lo == hi else (f"{lo} and finer" if not hi else f"{lo} to {hi}")
            lines.append(f"| {name(r['result'])} | {count(r.get('count', 1))} | {pct(r['chance'])} | {mesh} |")
        for r in res.get(item, []):
            lines.append(f"| {name(r['result'])} (falls through as residue) | 1 | {pct(r['chance'])} | any |")
        lines.append("")
    page("drop-tables", "Sifting drop tables", "What every siftable block gives, by mesh tier.", "\n".join(lines))


# ---------------------------------------------------------------- hammers
def hammers():
    crush = ["| Block | Result | Lowest hammer |", "|---|---|---|"]
    for rel, r in recipes_of("crushing"):
        if "/" in rel or rel.startswith("_"):
            pass
        results = ", ".join(f"{name(x['item'])}" + (f" ({count(x['count'])})" if x.get("count", 1) != 1 else "") +
                            (f" {pct(x['chance'])}" if x.get("chance", 1) != 1 else "") for x in r["results"])
        crush.append(f"| {name(r['input'].lstrip('#'))} | {results} | {r.get('hammer_tier', 1)} |")
    body = f"""
A hammer is a tool. Breaking a block that has a crushing recipe with a hammer of high enough tier drops the recipe's results instead of the block's normal drop, uses the hammer's durability and can give an extra result.

## Hammers

| Hammer | Tier | Durability | Crafting |
|---|---|---|---|
| Wooden Hammer | 1 | 96 | 2 planks, 2 sticks: `HH` / ` S` / ` S` |
| Stone Hammer | 2 | 192 | the same with cobblestone |
| Copper Hammer | 3 | 288 | the same with copper ingots |
| Iron Hammer | 4 | 512 | the same with iron ingots |
| Diamond Hammer | 5 | 2048 | the same with diamonds |
| Netherite Hammer | 6 | 2560 | a diamond hammer and a netherite ingot (shapeless) |

Hammers mine stone, sand, gravel and leaves at tool speed. Tiers above 2 add a chance of one extra result: `hammer.bonus_per_tier` for each tier above 2.

## What a hammer crushes

{chr(10).join(crush)}

Where two recipes match one block, the one that names fewer items wins, so a recipe for oak leaves beats the leaves tag. Plant fiber: three make one string.

## Auto Hammer

Sits on a drive. Use a hammer on it to fit the hammer (sneak with an empty hand takes it out). Blocks go in by hand or by hopper, up to 16. A block takes `auto_hammer.strokes_per_block` strokes (one per drive shake), the hammer loses 1 durability per block, and the results follow the same crushing recipes, bonus included. They go into the drive below. Without a drive it does nothing.

Crafting: `PHP` / `SIS` / `PPP` with planks (P), an iron hammer (H), sticks (S) and an iron ingot (I).
"""
    page("hammers", "Hammers and the Auto Hammer", "Six hammer tiers, what they crush, and the machine that swings one for you.", body)


# ---------------------------------------------------------------- sieve
def sieve():
    meshes = ["| Mesh | Tier | Speed | Crafting |", "|---|---|---|---|"]
    for f in sorted((DATA / "primamateria/mesh").glob("*.json")):
        m = load(f)
        craft = {"cloth_mesh": "5 plant fiber, 4 sticks in the corners", "copper_mesh": "5 copper ingots, 4 sticks in the corners",
                 "iron_mesh": "5 iron ingots, 4 sticks in the corners", "diamond_mesh": "5 diamonds, 4 sticks in the corners",
                 "netherite_mesh": "a diamond mesh and a netherite ingot (shapeless)"}[f.stem]
        meshes.append(f"| {name(m['item'])} | {m['tier']} | {m['speed']} | {craft} |")
    rows = settings_table("sieve.")
    body = f"""
The sieve turns a block into its drops. It is used in the world; it has no screen.

## Using it

- **Fit a mesh:** use a mesh item on the sieve. A sieve without a mesh takes no material. Sneak with an empty hand to take the mesh out of an empty sieve. Using a different mesh on an empty sieve swaps them.
- **Put a block in:** use a siftable block on the sieve. It holds one block at a time and has no storage.
- **Shake:** hold use. The sieve shakes once every 4 ticks. A block needs `sieve.shakes_per_block` shakes at mesh speed 1.0; the mesh speed divides that. When a block finishes and you hold more of the same block, the sieve takes the next one.
- **Drops:** rolled when the block goes in, shown on the mesh as the layer thins, and handed out when the block is done: into the container below the sieve (a chest, a hopper, a drive) or hopping toward you. Finer residue (gravel leaves sand) falls straight down.
- **Linked sieves:** sieves within `sieve.link_radius` blocks at the same height (1 is a 3 by 3 square) are shaken and filled together, each with its own mesh and drops. `sieve.link=false` switches it off. A drive shakes only the sieve on top of it.
- **Hoppers:** a hopper inserts one siftable block from above or the sides. A comparator reads the progress of the current block (1 to 15; 0 when empty).
- **Breaking** the sieve drops it, its mesh and the unfinished block.

Crafting: `P P` / `PSP` / `S S` with planks (P) and sticks (S).

## Meshes

{chr(10).join(meshes)}

A finer mesh still gives everything the coarser ones give unless a recipe sets a highest tier.

## Adding a material

A sifting material is only a recipe file. A datapack, KubeJS or ModJS adds a file of type `primamateria:sifting` with an input ingredient (an item, a `#tag` or a list), a result, a count, a chance and a mesh range. The sieve then accepts that item, rolls the drops, shows the block it holds and hands the results out. Nothing about a material is written in the mod's code: what the sieve looks like while it works comes from the loaded block itself. The format is on [[recipes]], every shipped table is on [[drop-tables]].

## Settings

| Key | Default | Meaning |
|---|---|---|
{chr(10).join(rows)}
"""
    page("sieve", "The sieve and meshes", "How the sieve works, the five meshes, linked sieves, and adding a material.", body)


# ---------------------------------------------------------------- barrels
def barrels():
    def table(folder, head, fn):
        rows = [head[0], head[1]]
        for rel, r in recipes_of(folder):
            rows.append(fn(r))
        return "\n".join(rows)

    soak = table("soaking", ("| Fluid | Item | Result | Time |", "|---|---|---|---|"),
                 lambda r: f"| {r['amount']} mB {name(r['fluid'])} | {name(r['input'])} | {name(r['result'])} | {r['time'] / 20:g} s |")
    steep = table("steeping", ("| Fluid | Item | Result | Time |", "|---|---|---|---|"),
                  lambda r: f"| {name(r['fluid'])} | {r['count']} {name(r['input'])} | {name(r['result_fluid'])} | {r['time'] / 20:g} s |")
    grow = table("growing", ("| Seed | Fluid | Result | Time |", "|---|---|---|---|"),
                 lambda r: f"| {name(r['seed'])} | {r['amount']} mB {name(r['fluid'])} | {r.get('count', 1)} {name(r['result'])} | {r['time'] / 20:g} s |")
    mix = table("mixing", ("| Base | Added | Result | Needs |", "|---|---|---|---|"),
                lambda r: f"| {name(r['base_fluid'])} | {name(r['added_fluid'])} | {name(r['result'])} | {'clay barrel' if r.get('needs_heatproof') else 'either barrel'} |")
    comp_rows = ["| Item | Compost units |", "|---|---|"]
    for f in sorted((DATA / "primamateria/compost").glob("*.json")):
        c = load(f)
        what = c.get("item") or c.get("tag")
        comp_rows.append(f"| {name(what.lstrip('#')) if ':' in what else what} | {c['units']} |")
    comp = recipes_of("composting")
    result = name(comp[0][1]["result"]) if comp else "Dirt"
    rows = settings_table("barrel.")
    body = f"""
Barrels are used in the world; they have no screen. A barrel holds either one fluid (up to 1000 mB) or compost (up to 1000 units), never both, plus one item being soaked or steeped and up to four growing crystals.

- **Wooden Barrel:** `P P` / `P P` / `PPP` with planks. Holds cool fluids; it cannot hold lava or a molten metal.
- **Clay Barrel:** `C C` / `C C` / `CBC` with clay balls and a clay block gives an Unfired Clay Barrel; smelt it. Also holds lava and does the mixing that needs a heatproof barrel.
- **Buckets:** use a full bucket to pour in 1000 mB (it must fit); use an empty bucket to take 1000 mB out when nothing is working.
- **Items:** use an item on the barrel to put it in. An empty hand takes a finished result. A hopper inserts one item from above or the sides and takes results from the bottom. A comparator reads how full it is.
- **Dew and rain:** a barrel with open sky above it gains water by itself (`barrel.dew_ticks_per_mb`, faster in rain by `barrel.rain_multiplier`).
- **Breaking** a barrel drops waiting results; its contents are lost.

In this release brine and the molten metals are internal fluids held by barrels, the crucible and the casting tray and moved by bucket; see [[known-gaps]].

## Composting

Organic items add compost units. At 1000 units the barrel matures, then holds one {result}. Bone meal halves the maturing time once.

{chr(10).join(comp_rows)}

## Soaking

{soak}

## Mixing

{mix}

## Steeping

{steep}

Brine is made only this way.

## Growing

Up to four seeds grow at once, each on its own timer.

{grow}

## Settings

| Key | Default | Meaning |
|---|---|---|
{chr(10).join(rows)}
"""
    page("barrels", "Barrels", "Composting, soaking, mixing, steeping brine and growing crystals.", body)


# ---------------------------------------------------------------- drives
def drives():
    rows = settings_table("drive.")
    body = f"""
A drive sits directly under a sieve or an Auto Hammer and works it. Each shake or stroke is one step of work. A drive has a 9-slot output buffer: the machine above delivers its results into it, and hoppers or pipes take them out from the bottom and sides. A drive stops when the machine has nothing to do, when fewer than 3 buffer slots are free, when it has no power, or when it receives a redstone signal. A drive works only the machine on top of it; linking does not apply.

| Drive | Power | Speed | Crafting |
|---|---|---|---|
| Windup Drive | Hold use on it to crank: one turn per 4 ticks, stores up to `drive.windup_max_turns`. Each shake uses one turn. | one shake per `drive.windup_ticks_per_shake` ticks | `PCP` / `CIC` / `PSP` with oak planks, copper ingots (C), an iron ingot (I) and a stick |
| Furnace Drive | Furnace fuel in its fuel slot (by hand or hopper). It burns only while the machine above has work, and lights up. | one shake per `drive.furnace_ticks_per_shake` ticks | a furnace, a windup drive, 4 iron ingots and 2 gold ingots (shapeless) |
| Powered Drive | Energy (FE) from any power mod, accepted on every side. It never gives energy back. | one shake per `drive.powered_ticks_per_shake` ticks, `drive.powered_fe_per_shake` FE each | a furnace drive, 4 redstone, 2 diamonds and 2 copper ingots (shapeless) |

An empty hand takes the buffer's contents; on the windup drive sneak to do that, because an empty hand cranks.

## Settings

| Key | Default | Meaning |
|---|---|---|
{chr(10).join(rows)}
"""
    page("drives", "Drives", "Windup, furnace and powered drives that shake sieves and swing the Auto Hammer.", body)



# ---------------------------------------------------------------- crucible
def crucible():
    heat = ["| Block | Heat |", "|---|---|"]
    for f in sorted((DATA / "primamateria/heat").glob("*.json")):
        h = load(f)
        what = h.get("block") or "#" + h.get("tag", "")
        state = ", ".join(f"{k}={v}" for k, v in h.get("state", {}).items())
        heat.append(f"| {name(what.lstrip('#'))}{' (' + state + ')' if state else ''} | {h['heat']} |")
    melt = ["| Item | Result | Amount |", "|---|---|---|"]
    for rel, r in recipes_of("melting"):
        melt.append(f"| {name(r['input'])} | {name(r['result'])} | {r['amount']} mB |")
    cast = ["| Fluid | Amount | Result | Time |", "|---|---|---|---|"]
    for rel, r in recipes_of("casting"):
        cast.append(f"| {name(r['fluid'])} | {r['amount']} mB | {name(r['result'])} | {r['time'] / 20:g} s |")
    rows = settings_table("crucible.")
    body = f"""
## Crucible

A fired pot that melts solids into lava or a molten metal, 4000 mB, one fluid at a time. It needs a heat source in the block directly below it; the highest matching source counts, and with none it does not melt. It melts `crucible.mb_per_tick_per_heat` mB per tick for each point of heat.

- **Fire it:** `C C` / `CCC` with clay balls gives an Unfired Crucible; smelt it.
- **Solids:** use an item on the crucible to put it in (4 slots), or insert from a hopper. An empty hand takes the last one back. Breaking the crucible drops the solids.
- **Buckets:** a lava bucket or a molten metal bucket fills it; an empty bucket takes 1000 mB of a bucketable fluid out. A water bucket does not go in.
- **Pouring:** it pours into casting trays placed against its four sides, in the order north, east, south, west, while the tray is empty and it holds enough of a fluid that has a casting recipe.

In this release the molten metals are internal fluids; see [[known-gaps]].

## Heat sources

{chr(10).join(heat)}

## Melting

{chr(10).join(melt)}

## Casting Tray

`CCC` with clay balls gives an Unfired Casting Tray; smelt it. Placed against the side of a crucible it is filled by it, then the fluid cools for the recipe time and the tray holds the result. An empty hand or a hopper below takes it out. One crucible feeds up to four trays. A hot bucket used on a tray casts as many results as the bucket's 1000 mB makes.

{chr(10).join(cast)}

## Settings

| Key | Default | Meaning |
|---|---|---|
{chr(10).join(rows)}
"""
    page("crucible", "Crucible and Casting Tray", "Heat, melting stone and dust, and casting ingots.", body)


# ---------------------------------------------------------------- start, progression, config
def start():
    rows = settings_table("start.")
    body = f"""
## The Void Island world type

Choose **Void Island** on the create-world screen. The Overworld is empty void with normal biomes for weather and mobs; the Nether and the End are normal. A 7 by 7 island, two layers of dirt under a grass top, with one oak tree, is placed at the world spawn the first time the world loads, and the spawn is set on it. There is no chest and there are no starter items: one tree gives the first wood.

The island is placed once. A saved flag in the world keeps it from being placed again, even if you mine it away. One shared island at the spawn serves everyone on a server.

## Setting

| Key | Default | Meaning |
|---|---|---|
{chr(10).join(rows)}

The mod works in a normal world too; nothing requires the world type.
"""
    page("start", "Void Island", "The world type and the start island.", body)


def progression():
    body = """
A plain reference of what the materials come from. Numbers are the shipped defaults; every one is data.

| Stage | Material | Where it comes from |
|---|---|---|
| 1 | Plant fiber | Hammer any leaves (35% a second one), or sift dirt |
| 1 | Dirt | A barrel full of compost |
| 1 | Rock chips, seeds, saplings, flint, coal, copper dust | Sift dirt and gravel with a cloth mesh |
| 1 | Cobblestone | Four rock chips |
| 2 | Gravel, sand, stone meal | Hammer cobblestone, gravel and sand |
| 2 | Clay | Soak stone meal in water in a barrel |
| 2 | Lava | Melt cobblestone, stone or gravel in a crucible over a heat source |
| 2 | Copper, iron and gold ingots | Melt the dust or raw metal in a crucible and pour it into a casting tray |
| 3 | Iron dust, salt dust, redstone, gunpowder | Sift gravel, sand and stone meal with a copper mesh |
| 3 | Gold dust, glowstone dust, quartz and amethyst and lapis seeds | An iron mesh |
| 4 | Brine | Four salt dust in a barrel of water |
| 4 | Quartz, amethyst, lapis, diamond, emerald | Grow the seed in a barrel of brine |
| 4 | Diamond seeds, more lapis, emerald seeds | A diamond mesh (diamond seeds also from an iron mesh) |
| 5 | Scoria | Hammer netherrack with a copper hammer or better |
| 5 | Pale sand | Hammer end stone with a diamond hammer or better |
| 5 | Soul sand | Soak scoria in brine |
| 5 | Mob drops (bone, flesh, string, gunpowder, spider eyes, slime, ghast tears) | Sift soul sand |
| 5 | Ender pearls, chorus fruit | Sift pale sand |
| 5 | Netherite scrap | Sift scoria with a diamond or netherite mesh |
| 5 | Obsidian | Pour water into a clay barrel of lava |

Automation: the windup drive, then the furnace drive, then the powered drive under a sieve; the Auto Hammer on a drive for crushing. See [[drives]] and [[hammers]]. Every table: [[drop-tables]].
"""
    page("progression", "Progression reference", "Where every material comes from, in order.", body)


def config():
    text = (ROOT / "common/src/main/java/com/itmecasey/primamateria/Settings.java").read_text(encoding="utf-8")
    rows = []
    for m in re.finditer(r'def\("([a-z_.]+)",\s*([^,]+),\s*"((?:[^"\\]|\\.)*)"\);', text):
        rows.append(f"| `{m.group(1)}` | `{m.group(2).strip().strip(chr(34))}` | {m.group(3)} |")
    body = f"""
`config/primamateria.properties`, written with every key and a comment on the first start. Edit a value and restart; delete a line to restore its default. `client.*` keys are read on the client only.

| Key | Default | Meaning |
|---|---|---|
{chr(10).join(rows)}
"""
    page("config", "Config file", "Every setting and its default.", body)


def main():
    drop_tables()
    hammers()
    sieve()
    barrels()
    drives()
    crucible()
    start()
    progression()
    config()
    print("wiki pages written to", WIKI)


if __name__ == "__main__":
    main()
