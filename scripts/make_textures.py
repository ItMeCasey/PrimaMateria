"""Draws the few textures Minecraft has no equivalent for: the items, the five meshes, the three crushed blocks and the
particles. Everything is 16x16 like the game (docs/ART.md). Model blocks need no textures of ours: their models use
Minecraft's own textures by reference.

Each sprite is a hand-placed pixel map (the text rows below) or a small pattern, colored with a ramp sampled from the vanilla
texture of the same material. No scaling filter, no vanilla pixels copied.

Usage: python scripts/make_textures.py                  (needs Pillow; overwrites the textures it owns)
       python scripts/make_textures.py --sheet out.png  also writes a review sheet; add --vanilla <folder of vanilla pngs>
                                                        to put real Minecraft textures next to ours for comparison
"""
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "common/src/main/resources/assets/primamateria/textures"
CLEAR = (0, 0, 0, 0)


def ramp(*hexes):
    return [tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,) for h in hexes]


# Six tones: index 0 is the outline, then 1 to 5 darkest to lightest. Sampled from the vanilla texture named in the comment.
R = {
    "wood": ramp("#3a2a12", "#67502c", "#9f844d", "#b8945f", "#c7a56c", "#d6b77c"),       # oak_planks
    "stone": ramp("#2f2f2f", "#5a5a5a", "#747474", "#8f8f8f", "#a8a8a8", "#c4c4c4"),      # stone, cobblestone
    "copper": ramp("#4a1e12", "#8a4129", "#b4563a", "#c26b4c", "#e3826c", "#f7b39e"),     # copper_block, raw_copper
    "iron": ramp("#353535", "#727272", "#a8a8a8", "#d8d8d8", "#ececec", "#ffffff"),       # iron_block, iron tools
    "rawiron": ramp("#3b3429", "#735f4d", "#af8e77", "#d8af93", "#ecc7ae", "#fedec8"),    # raw_iron
    "gold": ramp("#5a3606", "#b26411", "#dc9613", "#f5cc27", "#fdf55f", "#ffffff"),       # gold_ingot, raw_gold
    "diamond": ramp("#0e3f37", "#145e53", "#1aaaa7", "#2cdbd0", "#6ef5e6", "#ffffff"),    # diamond, diamond tools
    "netherite": ramp("#0e0b0c", "#241e1f", "#3c3232", "#4d494d", "#5f595d", "#7e767a"),  # netherite_ingot
    "amethyst": ramp("#2b1a4d", "#4b2f86", "#6b49b2", "#8d6acc", "#b38ef3", "#fecbe6"),   # amethyst_shard
    "quartz": ramp("#5c4c48", "#8f7f76", "#b6a48e", "#d4caba", "#eae5de", "#ffffff"),     # quartz
    "lapis": ramp("#0c1a4a", "#142a6e", "#1c4aa8", "#345ec3", "#5a82e2", "#a3bef2"),      # lapis_lazuli
    "emerald": ramp("#002d00", "#005300", "#00852a", "#17c544", "#41f384", "#d9ffeb"),    # emerald
    "fiber": ramp("#28300c", "#4f5a1e", "#6f7b2b", "#919a3a", "#b4bb55", "#d5d880"),      # wheat, kelp, hay
    "cloth": ramp("#55503f", "#8f8975", "#b3ad99", "#d2cdbc", "#e8e4d8", "#ffffff"),      # string, white_wool
    "clay": ramp("#373944", "#5a6274", "#757d90", "#9499a4", "#a1a7b1", "#b9bfc9"),       # clay_ball, clay
    "salt": ramp("#545468", "#8c93a8", "#b9b9cb", "#d5d5df", "#eaeaea", "#ffffff"),       # sugar
    "bucket": ramp("#1c1c1c", "#353535", "#727272", "#969696", "#c0c0c0", "#e6e6e6"),     # bucket
    "brine": ramp("#12343c", "#1d4a55", "#2c6f7a", "#45979c", "#78c0bd", "#b5e3db"),      # water, shifted to teal
    "meal": ramp("#5d5856", "#726b69", "#817f7f", "#979797", "#a9a7a5", "#bab8b6"),       # gravel, lighter
    "scoria": ramp("#2a0b0b", "#3d1010", "#511515", "#652828", "#723232", "#854242"),     # netherrack
    "pale": ramp("#a8a070", "#bdb682", "#cdc68b", "#d5da94", "#dee6a4", "#eef6b4"),       # end_stone
}
MOLTEN = {  # the bucket's fill: lava's orange and yellow, pulled toward each metal
    "copper": ramp("#7a2a10", "#b4481a", "#d9702a", "#ee9448", "#f7b878", "#fdd9a8"),
    "iron": ramp("#6a2a18", "#a8441f", "#d26a30", "#e89a5a", "#f1bf8c", "#f8dcc0"),
    "gold": ramp("#8a4a0a", "#c87c12", "#e8a820", "#f7c940", "#fbe070", "#fff3a8"),
}
STICK = ramp("#281e0b", "#493615", "#684e1e", "#896727")                                    # stick, tool handles


def new(w=16, h=16):
    return Image.new("RGBA", (w, h), CLEAR)


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), color)


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def sprite(rows, primary, secondary=None):
    """A 16x16 sprite from text rows: '0' to '5' index the primary ramp, 'a' to 'f' the secondary, 's' to 'v' the stick."""
    pal = {str(i): c for i, c in enumerate(primary)}
    pal.update({"stuv"[i]: c for i, c in enumerate(STICK)})
    if secondary:
        pal.update({"abcdef"[i]: c for i, c in enumerate(secondary)})
    img = new()
    assert len(rows) == 16, len(rows)
    for y, row in enumerate(rows):
        assert len(row) == 16, (row, len(row))
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), pal[ch])
    return img


# The growing crystals: a faceted face, lit from the top left, one pattern of tall facets (quartz, lapis, diamond, emerald) and a
# second one with shorter, tilted facets (amethyst, like the vanilla cluster). Placed by hand; the tones are the gem's own ramp.
CRYSTAL_A = ["4533211445332114", "5543221455432214", "5543221455432214", "5444322155443221", "5444322155443221", "5544322155443221",
             "5443321155433211", "5443321155433211", "5433221155433221", "5433221254332212", "4543322145433221", "4533221245332212",
             "4533221244332212", "3533221243332112", "3422211132221111", "3322111032211110"]
CRYSTAL_B = ["5443211254432112", "5544321155443211", "4554321144554321", "4455432114455432", "3445543211445543", "3344554322344554",
             "2334455432233445", "2233445543223344", "1223344554122334", "1122334455112233", "5112233445511223", "5511223344551122",
             "4551122334455112", "4455112233445511", "3445511223344551", "3344551122334455"]

# ------------------------------------------------------------------ item pixel maps

DUST = ["................", "................", "................", ".......11.......", "......1541......", ".....145431.....",
        "....14534320....", "...1453443210...", "..145344343210..", "..143543432310..", "..013434323210..", "...0132322100...",
        "....00121100....", "......0000......", "................", "................"]
SEED = ["................", "................", "................", ".......00.......", "......0540......", ".....054430.....",
        ".....044330.....", "....0543320.....", "....0433220.....", "....0432210.....", ".....032210.....", ".....022110.....",
        "......0110......", ".......00.......", "................", "................"]
CHIPS = ["................", "................", "................", ".........000....", "........04430...", "...00...0433200.",
         "..0450..0322110.", ".0443300.021100.", ".0433220..0000..", ".0322110........", "..021100.0000...", "...0000.04543200",
         "........04332210", ".........0221100", "..........00000.", "................"]
FIBER = ["................", "...........0.00.", "..........045050", "..........043420", ".........0433200", "........04332100",
         ".......0s332100.", "......0tst2100..", ".....043tst00...", "....0433s2100...", "...04332100.....", "..043321100.....",
         ".0433020100.....", ".040.00.00......", "..0.............", "................"]
RAW_CRUCIBLE = ["................", "................", "................", "..000000000000..", "..054444444420..", "..002222222200..",
                "...0433333210...", "...0433333210...", "...0433332210...", "...0433322110...", "....04332210....", "....04322110....",
                ".....022110.....", "....00000000....", "....00....00....", "................"]
RAW_TRAY = ["................", "................", "................", "................", "................", "................",
            ".00000000000000.", ".05444444444420.", ".04000000000010.", ".04322222221010.", ".04333333322110.", ".02222221111110.",
            "..000000000000..", "................", "................", "................"]
RAW_BARREL = ["................", "..000000000000..", "..054444444420..", "..002222222200..", "..043333333210..", "..043333332210..",
              "..000000000000..", "..043333332210..", "..043333332210..", "..043333322110..", "..043333322110..", "..000000000000..",
              "..043332221110..", "..022221111110..", "...0000000000...", "................"]
BUCKET = ["................", "................", "................", "..000000000000..", "..05deeeeeed20..", "..04cddddddc10..",
          "...0443332210...", "...0443322210...", "...0443322110...", "....04332210....", "....04332110....", "....04322110....",
          ".....022110.....", ".....000000.....", "................", "................"]


def item_hammer(head):
    """A sledgehammer held like a vanilla tool: a stick handle from the lower left and a long block head set squarely across
    its end, drawn pixel by pixel on the 16x16 grid."""
    img = new()
    for i in range(9):
        put(img, 1 + i, 14 - i, STICK[0])
        put(img, 2 + i, 14 - i, STICK[3] if i % 3 else STICK[2])
    put(img, 1, 15, STICK[0])
    for y in range(16):
        for x in range(16):
            across, along = x - y - 5, x + y - 15      # across the head's thickness, along its length
            if abs(across) > 3 or abs(along) > 5:
                continue
            if abs(across) == 3 or abs(along) == 5:
                tone = 0
            else:
                tone = {-2: 5, -1: 4, 0: 3, 1: 2, 2: 1}[across]
            if along == -4 and tone:
                tone = min(tone + 1, 5)
            if along == 4 and tone:
                tone = max(tone - 1, 1)
            put(img, x, y, head[tone])
    return img

def weave(kind, x, y):
    """Which tone (or None for an open hole) the mesh `kind` has at a pixel. Every mesh is one-pixel wires around open holes;
    finer tiers have smaller holes. Straight meshes are woven: each wire goes over one crossing and under the next."""
    if kind in ("diamond", "netherite"):                      # a diagonal lattice, like chain link
        a, b = (x + y) % 4 == 0, (x - y) % 4 == 0
        if a and b:
            return "gold" if kind == "netherite" else 5
        return 4 if a else (2 if b else None)
    pitch = {"cloth": 4, "copper": 3, "iron": 3}[kind]
    on_v, on_h = x % pitch == 0, y % pitch == 0
    if not (on_v or on_h):
        return None
    if on_v and on_h:
        return 5 if ((x // pitch) + (y // pitch)) % 2 == 0 else 2
    return 4 if on_h else 3


def mesh_color(r, tone):
    return R["gold"][3] if tone == "gold" else r[tone]


def block_mesh(kind, r):
    img = new()
    for y in range(16):
        for x in range(16):
            tone = weave(kind, x, y)
            if tone is not None:
                put(img, x, y, mesh_color(r, tone))
    return img


def item_mesh(kind, r):
    """A square piece of mesh with a bound edge and open holes, 14 by 14."""
    img = new()
    size = 12
    for y in range(size + 1):
        for x in range(size + 1):
            if x in (0, size) or y in (0, size):
                tone = 2 if (x == size or y == size) else 4
                if (x, y) in ((0, size), (size, 0)):
                    tone = 3
            else:
                tone = weave(kind, x, y)
            if tone is not None:
                put(img, x + (15 - size) // 2, y + (15 - size) // 2, mesh_color(r, tone))
    return img

def loose(r, seed, weights, streak=0.3):
    """A loose fine material like sand: per-pixel tones with short horizontal runs, tileable."""
    rnd = random.Random(seed)
    bag = [t for t, w in enumerate(weights) for _ in range(w)]
    img = new()
    for y in range(16):
        prev = rnd.choice(bag)
        for x in range(16):
            tone = prev if rnd.random() < streak else rnd.choice(bag)
            put(img, x, y, r[tone])
            prev = tone
    return img


def cinders(r, seed):
    """Scoria: netherrack broken small. Dark ground, small lumps with a dark lower-right edge."""
    rnd = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, r[rnd.choice([1, 2, 2, 1, 2])])
    for _ in range(20):
        cx, cy = rnd.randrange(16), rnd.randrange(16)
        w, h = rnd.choice([(2, 2), (3, 2), (2, 2), (3, 3), (2, 1)])
        tone = rnd.choice([3, 4, 3])
        for dy in range(h):
            for dx in range(w):
                c = r[tone]
                if (dy == h - 1 or dx == w - 1) and (w, h) != (2, 1):
                    c = r[0] if dx == w - 1 and dy == h - 1 else r[1]
                elif dx == 0 and dy == 0:
                    c = r[min(tone + 1, 5)]
                put(img, (cx + dx) % 16, (cy + dy) % 16, c)
    return img


def particle(size, cells):
    img = new(size, size)
    for x, y, a in cells:
        img.putpixel((x, y), (255, 255, 255, a))
    return img


def puff(frame):
    img = new(8, 8)
    cells = [[(3, 3), (4, 3), (3, 4), (4, 4)],
             [(3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (5, 4), (3, 5), (4, 5), (3, 3)],
             [(2, 1), (5, 1), (1, 3), (6, 3), (1, 5), (6, 4), (3, 6), (5, 6), (3, 3)],
             [(1, 1), (6, 0), (0, 4), (7, 5), (2, 7), (5, 7)]][frame]
    for x, y in cells:
        img.putpixel((x, y), (255, 255, 255, (255, 220, 170, 110)[frame]))
    return img


def build():
    out = {}
    b, i, p = "block/", "item/", "particle/"
    out[b + "stone_meal"] = loose(R["meal"], 5, (0, 1, 4, 6, 4, 1))
    out[b + "pale_sand"] = loose(R["pale"], 6, (0, 1, 3, 6, 4, 1))
    out[b + "scoria"] = cinders(R["scoria"], 7)
    for kind in ("cloth", "copper", "iron", "diamond", "netherite"):
        out[b + "mesh_" + kind] = block_mesh(kind, R[kind])
        out[i + kind + "_mesh"] = item_mesh(kind, R[kind])
    heads = {"wooden": "wood", "stone": "stone", "copper": "copper", "iron": "iron", "diamond": "diamond", "netherite": "netherite"}
    for name, r in heads.items():
        out[i + name + "_hammer"] = item_hammer(R[r])
    out[i + "plant_fiber"] = sprite(FIBER, R["fiber"])
    out[i + "rock_chip"] = sprite(CHIPS, R["stone"])
    out[i + "copper_dust"] = sprite(DUST, R["copper"])
    out[i + "iron_dust"] = sprite(DUST, R["rawiron"])
    out[i + "gold_dust"] = sprite(DUST, R["gold"])
    out[i + "salt_dust"] = sprite(DUST, R["salt"])
    for name in ("quartz", "amethyst", "lapis", "diamond", "emerald"):
        out[i + name + "_seed"] = sprite(SEED, R[name])
    for name in ("quartz", "amethyst", "lapis", "diamond", "emerald"):
        out[b + "crystal_" + name] = sprite(CRYSTAL_B if name == "amethyst" else CRYSTAL_A, R[name])
    out[i + "raw_crucible"] = sprite(RAW_CRUCIBLE, R["clay"])
    out[i + "raw_casting_tray"] = sprite(RAW_TRAY, R["clay"])
    out[i + "raw_clay_barrel"] = sprite(RAW_BARREL, R["clay"])
    out[i + "brine_bucket"] = sprite(BUCKET, R["bucket"], R["brine"])
    for metal in ("copper", "iron", "gold"):
        out[i + f"molten_{metal}_bucket"] = sprite(BUCKET, R["bucket"], MOLTEN[metal])
    out[p + "fines_0"] = particle(8, [(3, 3, 255)])
    out[p + "fines_1"] = particle(8, [(3, 3, 255), (4, 3, 255), (3, 4, 220), (4, 4, 220)])
    out[p + "fines_2"] = particle(8, [(3, 3, 255), (3, 4, 255), (4, 4, 220)])
    for f in range(4):
        out[p + f"sift_dust_{f}"] = puff(f)
    pour = new(4, 16)
    for y in range(16):
        for x in range(1, 3):
            pour.putpixel((x, y), (255, 255, 255, 255) if (x + y // 2) % 2 == 0 else (215, 215, 215, 255))
    out[p + "pour"] = pour
    return out


# For the review sheet: ours first, then the vanilla textures shown beside them.
COMPARE = [
    (["item/wooden_hammer", "item/stone_hammer", "item/copper_hammer", "item/iron_hammer", "item/diamond_hammer",
      "item/netherite_hammer"], ["wooden_shovel", "iron_pickaxe", "mace", "stick"]),
    (["item/cloth_mesh", "item/copper_mesh", "item/iron_mesh", "item/diamond_mesh", "item/netherite_mesh"], ["string"]),
    (["item/plant_fiber", "item/rock_chip", "item/copper_dust", "item/iron_dust", "item/gold_dust", "item/salt_dust"],
     ["wheat", "flint", "redstone", "glowstone_dust", "gunpowder", "sugar"]),
    (["item/quartz_seed", "item/amethyst_seed", "item/lapis_seed", "item/diamond_seed", "item/emerald_seed"],
     ["quartz", "amethyst_shard", "diamond", "emerald"]),
    (["item/raw_crucible", "item/raw_casting_tray", "item/raw_clay_barrel", "item/brine_bucket"],
     ["clay_ball", "flower_pot", "brick", "bucket", "water_bucket"]),
    (["block/stone_meal", "block/scoria", "block/pale_sand"], ["gravel", "sand", "netherrack", "end_stone"]),
]


def review_sheet(out, target, vanilla):
    """Ours (light slots, left) beside the vanilla textures they were drawn after (dark slots, right), each with its name."""
    from PIL import ImageDraw, ImageFont
    scale, label = 6, 34
    cw, ch = 16 * scale + 28, 16 * scale + 12 + label
    try:
        font = ImageFont.truetype("arial.ttf", 13)
    except OSError:
        font = ImageFont.load_default()
    cols = max(len(a) + len(v) for a, v in COMPARE) + 1
    sheet = Image.new("RGBA", (cols * cw + 12, len(COMPARE) * ch + 40), (198, 198, 198, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((12, 8), "Light slots: Prima Materia.   Dark slots, marked MC: real Minecraft textures, for comparison.", fill=(30, 30, 30, 255), font=font)

    def slot(img, n, row, ours, name):
        x, y = 12 + n * cw + 8, 36 + row * ch
        back = Image.new("RGBA", (16 * scale + 4, 16 * scale + 4), (139, 139, 139, 255) if ours else (104, 104, 120, 255))
        sheet.paste(back, (x - 2, y - 2))
        big = img.convert("RGBA").crop((0, 0, 16, 16)).resize((16 * scale, 16 * scale), Image.NEAREST)
        sheet.paste(big, (x, y), big)
        words = name.replace("_", " ").title().split()
        if not ours:
            words = ["MC:"] + words
        lines, line = [], ""
        for w in words:
            if line and draw.textlength(line + " " + w, font=font) > cw - 8:
                lines.append(line)
                line = w
            else:
                line = (line + " " + w).strip()
        lines.append(line)
        for k, text in enumerate(lines[:2]):
            tw = draw.textlength(text, font=font)
            draw.text((x + 8 * scale - tw / 2, y + 16 * scale + 4 + 14 * k), text, fill=(30, 30, 30, 255) if ours else (70, 70, 100, 255), font=font)

    for row, (ours, theirs) in enumerate(COMPARE):
        n = 0
        for name in ours:
            slot(out[name], n, row, True, name.split("/")[1])
            n += 1
        n += 1
        for name in theirs:
            f = Path(vanilla) / (name + ".png") if vanilla else None
            if f and f.exists():
                slot(Image.open(f), n, row, False, name)
                n += 1
    sheet.save(target)


def crystal_review(out, target, vanilla):
    """The five crystal textures (top row) over the vanilla gems they were sampled from (bottom row), each labeled."""
    from PIL import ImageDraw, ImageFont
    names = ["quartz", "amethyst", "lapis", "diamond", "emerald"]
    vanilla_names = ["quartz", "amethyst_shard", "lapis_lazuli", "diamond", "emerald"]
    scale, cw = 8, 16 * 8 + 40
    try:
        font = ImageFont.truetype("arial.ttf", 14)
    except OSError:
        font = ImageFont.load_default()
    sheet = Image.new("RGBA", (5 * cw + 20, 2 * (16 * scale + 50) + 50), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    d.text((12, 8), "Top: Prima Materia crystal textures (block/crystal_*.png).   Bottom: the real Minecraft gems they are sampled from.", fill=(30, 30, 30, 255), font=font)
    for n, (ours, theirs) in enumerate(zip(names, vanilla_names)):
        for row, (img, label, dark) in enumerate(((out["block/crystal_" + ours], "crystal_" + ours, False),
                                                   (Image.open(Path(vanilla) / "item" / (theirs + ".png")) if vanilla else None, "MC: " + theirs, True))):
            if img is None:
                continue
            x, y = 16 + n * cw, 40 + row * (16 * scale + 50)
            sheet.paste(Image.new("RGBA", (16 * scale + 8, 16 * scale + 8), (104, 104, 120, 255) if dark else (139, 139, 139, 255)), (x - 4, y - 4))
            big = img.convert("RGBA").crop((0, 0, 16, 16)).resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.paste(big, (x, y), big)
            d.text((x, y + 16 * scale + 10), label, fill=(70, 70, 100, 255) if dark else (30, 30, 30, 255), font=font)
    sheet.save(target)


def main():
    out = build()
    for name, img in out.items():
        path = TEX / (name + ".png")
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    print(f"textures: {len(out)} written")
    if "--crystal-review" in sys.argv:
        vanilla = sys.argv[sys.argv.index("--vanilla") + 1] if "--vanilla" in sys.argv else None
        crystal_review(out, sys.argv[sys.argv.index("--crystal-review") + 1], vanilla)
    if "--sheet" in sys.argv:
        vanilla = sys.argv[sys.argv.index("--vanilla") + 1] if "--vanilla" in sys.argv else None
        review_sheet(out, sys.argv[sys.argv.index("--sheet") + 1], vanilla)


if __name__ == "__main__":
    main()
