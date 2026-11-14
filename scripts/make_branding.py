"""Draws the Prima Materia icon (32x32 pixel art on the studio's night tile) and the banner and social images.

Run from the repository root:  python scripts/make_branding.py
Writes branding/icon/icon_<size>.png, branding/icon/icon.svg, branding/banners/banner.png and social.png.

The picture: a wooden sieve with a dark mesh seen from above, gold grains falling through it onto a heap.
All art is original. The pixel font is Silkscreen (SIL Open Font License, see licenses/OFL-silkscreen.txt).
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "branding/icon"
BANNERS = ROOT / "branding/banners"
FONT = ROOT / "branding/source/fonts/silkscreen.ttf"
N = 32

NIGHT = (15, 14, 26)
NIGHT2 = (26, 24, 48)
EDGE = (46, 42, 70)
EDGE_TOP = (58, 54, 88)
GOLD = (242, 179, 61)
GOLD_HI = (255, 226, 140)
GOLD_LO = (178, 116, 32)
TEXT = (244, 242, 255)
MUTED = (169, 164, 200)
WOOD = (169, 131, 80)
WOOD_HI = (194, 160, 106)
WOOD_LO = (94, 69, 39)
MESH = (28, 24, 40)
MESH_HI = (74, 66, 96)
OUTLINE = (30, 20, 10)

px = [[None] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


def shade(c, f):
    return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)))


def tile():
    r = 4
    for y in range(N):
        for x in range(N):
            cx = min(max(x, r - 0.5), N - r - 0.5)
            cy = min(max(y, r - 0.5), N - r - 0.5)
            if math.hypot(x - cx, y - cy) <= r:
                px[y][x] = NIGHT2
    for y in range(N):
        for x in range(N):
            if px[y][x] is None:
                continue
            edge = any(not (0 <= x + dx < N and 0 <= y + dy < N) or px[y + dy][x + dx] is None for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                px[y][x] = EDGE_TOP if y < N // 2 else EDGE


def mesh_top(u, v):
    """The sieve seen from above: a wooden frame round a dark mesh with a crosshatch and a few gold flecks."""
    e = min(u, v, 1 - u, 1 - v)
    if e < 0.08:
        return WOOD_LO
    if e < 0.15:
        return WOOD_HI
    gu, gv = (u - 0.15) / 0.7, (v - 0.15) / 0.7
    line = (gu * 4) % 1 < 0.18 or (gv * 4) % 1 < 0.18
    if line:
        return MESH_HI
    fleck = (int(gu * 8) * 7 + int(gv * 8) * 3) % 11 == 0
    return GOLD if fleck else MESH


def draw_icon():
    tile()
    cx, yc, s, h = 16, 10.5, 12, 2
    owner = set()
    for y in range(N):
        for x in range(N):
            dx, dy = x + 0.5 - cx, y + 0.5 - yc
            col = None
            if abs(dx) / s + abs(dy) / (s / 2) <= 1:
                u = (dx / s + dy / (s / 2)) / 2 + 0.5
                v = (dx / s - dy / (s / 2)) / 2 + 0.5
                col = mesh_top(u, v)
            elif -s <= dx < 0:
                ytop = yc + (dx + s) * 0.5
                if 0 <= (y + 0.5 - ytop) / h < 1:
                    col = shade(WOOD, 0.82)
            elif 0 <= dx < s:
                ytop = yc + s / 2 - dx * 0.5
                if 0 <= (y + 0.5 - ytop) / h < 1:
                    col = shade(WOOD, 0.6)
            if col is not None:
                px[y][x] = col
                owner.add((x, y))
    # the outline round the sieve
    for (x, y) in list(owner):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in owner and 0 <= q[0] < N and 0 <= q[1] < N:
                put(q[0], q[1], OUTLINE)
    # grains falling through the mesh, then a heap
    for i, (gx, gy) in enumerate(((16, 19), (15, 21), (17, 22), (16, 24))):
        put(gx, gy, GOLD_HI if i % 2 == 0 else GOLD)
    heap = [(26, 3), (27, 7), (28, 11), (29, 15)]
    for k, (row, width) in enumerate(((26, 4), (27, 8), (28, 12), (29, 16))):
        x0 = 16 - width // 2
        for x in range(x0, x0 + width):
            c = GOLD_HI if (x + row) % 5 == 0 else (GOLD if (x + row) % 3 else GOLD_LO)
            put(x, row, c)
    for x in range(8, 24):
        put(x, 30, GOLD_LO)


def save_icons():
    ICON.mkdir(parents=True, exist_ok=True)
    base = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    for y in range(N):
        for x in range(N):
            if px[y][x] is not None:
                base.putpixel((x, y), px[y][x] + (255,))
    for size in (32, 64, 128, 256, 512, 1024):
        base.resize((size, size), Image.NEAREST).save(ICON / f"icon_{size}.png")
    logo = ROOT / "common/src/main/resources"
    if logo.is_dir():
        base.resize((256, 256), Image.NEAREST).save(logo / "primamateria_logo.png")
    rects = []
    for y in range(N):
        for x in range(N):
            if px[y][x] is not None:
                r, g, b = px[y][x]
                rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="#{r:02x}{g:02x}{b:02x}"/>')
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {N} {N}" shape-rendering="crispEdges">' + "".join(rects) + "</svg>\n"
    (ICON / "icon.svg").write_text(svg, encoding="utf-8")
    return base


def backdrop(w, h):
    img = Image.new("RGB", (w, h), NIGHT)
    d = ImageDraw.Draw(img)
    for y in range(h):  # soft vertical gradient
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=(int(NIGHT[0] + (NIGHT2[0] - NIGHT[0]) * t), int(NIGHT[1] + (NIGHT2[1] - NIGHT[1]) * t), int(NIGHT[2] + (NIGHT2[2] - NIGHT[2]) * t)))
    for x in range(0, w, 32):  # faint grid
        d.line([(x, 0), (x, h)], fill=(28, 26, 46))
    for y in range(0, h, 32):
        d.line([(0, y), (w, y)], fill=(28, 26, 46))
    glow = Image.new("RGB", (w, h), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([w * 0.1, h * 0.2, w * 0.5, h * 1.1], fill=(40, 30, 14))
    return Image.blend(img, Image.composite(glow, img, glow.convert("L")), 0.6)


def pixel_text(img, xy, text, size, fill, shadow=(8, 7, 16)):
    f = ImageFont.truetype(str(FONT), size)
    x, y = xy
    d = ImageDraw.Draw(img)
    d.text((x + size // 8, y + size // 8), text, font=f, fill=shadow)
    d.text((x, y), text, font=f, fill=fill)
    return d.textbbox((x, y), text, font=f)


def banners(base):
    BANNERS.mkdir(parents=True, exist_ok=True)
    # social preview 1280x640: icon above, title and one line below
    w, h = 1280, 640
    img = backdrop(w, h)
    ic = base.resize((256, 256), Image.NEAREST)
    img.paste(ic, ((w - 256) // 2, 70), ic)
    f = ImageFont.truetype(str(FONT), 72)
    title = "PRIMA MATERIA"
    tw = ImageDraw.Draw(img).textlength(title, font=f)
    pixel_text(img, ((w - tw) / 2, 360), title, 72, GOLD)
    tag = "Sieves, hammers, barrels and crucibles. Everything from nothing."
    f2 = ImageFont.truetype(str(FONT), 22)
    tgw = ImageDraw.Draw(img).textlength(tag, font=f2)
    pixel_text(img, ((w - tgw) / 2, 480), tag, 22, MUTED)
    img.save(BANNERS / "social.png")
    # wide banner 1280x320: icon left, text right
    w, h = 1280, 320
    img = backdrop(w, h)
    ic = base.resize((224, 224), Image.NEAREST)
    img.paste(ic, (64, 48), ic)
    pixel_text(img, (340, 84), "PRIMA MATERIA", 64, GOLD)
    pixel_text(img, (344, 188), "A sifting skyblock mod for Forge and NeoForge", 20, MUTED)
    img.save(BANNERS / "banner.png")


def main():
    draw_icon()
    base = save_icons()
    banners(base)
    print("icons and banners written")


if __name__ == "__main__":
    main()
