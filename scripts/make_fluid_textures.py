"""Draws the animated textures of the four fluids: brine and the molten copper, iron and gold.

Each fluid has a still texture (16x16 frames in one vertical strip), a flowing texture (32x32 frames, like the game's own flowing
fluids, because the flowing face shows a half-size window of it) and, for brine, a small overlay for the faces behind glass.
Every pixel is one of five tones of the fluid's palette (docs/ART.md): brine is the teal ramp, the molten metals are ember ramps
shifted toward each metal. The patterns are drawn from sums of waves with whole-number frequencies, so every strip loops without a
seam, and quantized to the five tones the way the game's own fluid textures are. No vanilla pixels are copied; lava_still and
water_still were only looked at as references.

Usage: python scripts/make_fluid_textures.py        (needs Pillow; overwrites the textures it owns)
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BLOCK = ROOT / "common/src/main/resources/assets/primamateria/textures/block"


def ramp(*hexes):
    return [tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) for h in hexes]


BRINE = ramp("#1d4a55", "#2c6f7a", "#45979c", "#78c0bd", "#b5e3db")
COPPER = ramp("#6b1a0c", "#a8300f", "#d9531a", "#f28430", "#fcc058")
IRON = ramp("#8a4a2a", "#c47a4a", "#e8a878", "#f6cfa8", "#fff0dc")
GOLD = ramp("#8a5a0c", "#c8880f", "#eeb520", "#f9d84a", "#fff4a0")

BRINE_ALPHA = 205


def waves(seed, count, kx_range, ky_range, kt_range):
    """A fixed set of waves (whole-number frequencies in x, y and time) so the pattern repeats exactly."""
    rnd = random.Random(seed)
    out = []
    for _ in range(count):
        out.append((rnd.randint(*kx_range) * rnd.choice((-1, 1)), rnd.randint(*ky_range) * rnd.choice((-1, 1)),
                    rnd.randint(*kt_range) * rnd.choice((-1, 1)), rnd.random(), rnd.uniform(0.6, 1.0)))
    return out


def field(ws, x, y, t, size, frames, drift=0):
    """The wave sum at pixel (x, y) and frame t, scaled to 0 to 1. {@code drift} moves the whole pattern down once around over the loop."""
    total = 0.0
    weight = 0.0
    for kx, ky, kt, ph, amp in ws:
        total += amp * math.cos(2 * math.pi * (kx * x / size + ky * (y / size - drift * t / frames) + kt * t / frames + ph))
        weight += amp
    return 0.5 + 0.5 * total / weight


def tone(value, cuts):
    for i, c in enumerate(cuts):
        if value < c:
            return i
    return len(cuts)


def strip(size, frames, pixel):
    img = Image.new("RGBA", (size, size * frames))
    for t in range(frames):
        for y in range(size):
            for x in range(size):
                img.putpixel((x, t * size + y), pixel(x, y, t))
    return img


def brine_still():
    ws = waves(11, 6, (1, 2), (1, 2), (1, 2))
    cuts = (0.22, 0.40, 0.62, 0.86)

    def pixel(x, y, t):
        i = tone(field(ws, x, y, t, 16, 16), cuts)
        return BRINE[min(4, max(0, i))] + (BRINE_ALPHA,)

    return strip(16, 16, pixel)


def brine_flow():
    # streaks that run down the face: tall waves (low in y, high in x) that slide one full turn over the loop
    ws = waves(12, 7, (1, 3), (1, 2), (0, 0))
    cuts = (0.22, 0.40, 0.62, 0.86)

    def pixel(x, y, t):
        i = tone(field(ws, x, y, t, 32, 16, drift=1), cuts)
        return BRINE[min(4, max(0, i))] + (BRINE_ALPHA,)

    return strip(32, 16, pixel)


def brine_overlay():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                img.putpixel((x, y), BRINE[4] + (170,))
            elif edge == 1:
                img.putpixel((x, y), BRINE[3] + (70,))
    return img


def molten_still(colors, seed):
    # a dark crust cut by bright seams and blobs, the way lava looks: low tones are wide, the highest tones are small
    ws = waves(seed, 8, (1, 3), (1, 3), (1, 2))
    cuts = (0.34, 0.52, 0.66, 0.80)

    def pixel(x, y, t):
        return colors[tone(field(ws, x, y, t, 16, 16), cuts)] + (255,)

    return strip(16, 16, pixel)


def molten_flow(colors, seed):
    ws = waves(seed, 9, (1, 4), (1, 2), (0, 0))
    cuts = (0.34, 0.52, 0.66, 0.80)

    def pixel(x, y, t):
        return colors[tone(field(ws, x, y, t, 32, 16, drift=1), cuts)] + (255,)

    return strip(32, 16, pixel)


def save(img, name, frametime=None, size=None):
    path = BLOCK / (name + ".png")
    img.save(path)
    if frametime is not None:
        meta = {"frametime": frametime}
        if size is not None:
            meta["width"] = size
            meta["height"] = size
        (BLOCK / (name + ".png.mcmeta")).write_text(json.dumps({"animation": meta}, indent=2) + "\n", encoding="utf-8", newline="\n")
    return path


def main():
    BLOCK.mkdir(parents=True, exist_ok=True)
    save(brine_still(), "brine_still", 3, 16)
    save(brine_flow(), "brine_flow", 3, 32)
    save(brine_overlay(), "brine_overlay")
    for name, colors, seed in (("molten_copper", COPPER, 21), ("molten_iron", IRON, 31), ("molten_gold", GOLD, 41)):
        save(molten_still(colors, seed), name + "_still", 4, 16)
        save(molten_flow(colors, seed + 1), name + "_flow", 3, 32)
    print("fluid textures written to", BLOCK)


if __name__ == "__main__":
    main()
