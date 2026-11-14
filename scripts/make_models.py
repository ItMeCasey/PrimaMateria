#!/usr/bin/env python3
"""Writes the Prima Materia block models as Blockbench sources (branding/models/*.bbmodel) from one spec, plus the animations
of the sieve. scripts/export_anims.py then turns the sieve .bbmodel into game files.

Every element sits on the 16 unit grid (half units at the smallest). Faces point at Minecraft's own block textures by id and
use Minecraft's default world aligned UVs, so the pixels are Mojang's and follow any resource pack. No vanilla pixel is embedded
in the committed .bbmodel files; run with --embed <folder of extracted vanilla textures> to write local viewing copies (into
branding/models/_local, ignored by git) that show the textures inside Blockbench.

    python scripts/make_models.py [--embed <dir>]
"""
import json
import sys
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "branding" / "models"
NS = uuid.UUID("6f1c2f5e-3b8e-4c47-9a53-5b0a9a7c1d11")

TEX = {  # key -> (namespace, folder, name)
    "wood": ("minecraft", "block", "oak_planks"),
    "dark": ("minecraft", "block", "spruce_planks"),
    "log": ("minecraft", "block", "stripped_oak_log"),
    "log_top": ("minecraft", "block", "stripped_oak_log_top"),
    "brass": ("minecraft", "block", "gold_block"),
    "iron": ("minecraft", "block", "iron_block"),
    "stone": ("minecraft", "block", "smooth_stone"),
    "clay": ("minecraft", "block", "terracotta"),
    "brick": ("minecraft", "block", "bricks"),
    "copper": ("minecraft", "block", "copper_block"),
    "amethyst": ("minecraft", "block", "amethyst_block"),
    "sift": ("minecraft", "block", "white_concrete_powder"),
    "mesh": ("primamateria", "block", "mesh_cloth"),
    "crystal": ("minecraft", "block", "amethyst_cluster"),
}
MESH_TIERS = ["cloth", "copper", "iron", "diamond", "netherite"]


def uid(*parts):
    return str(uuid.uuid5(NS, "/".join(str(p) for p in parts)))


def auto_uv(frm, to):
    """Minecraft's default UV for each face of a box, in pixels of a 16 texture (world aligned)."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    return {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        "up": [x0, z0, x1, z1],
        "down": [x0, 16 - z1, x1, 16 - z0],
    }


class Cube:
    def __init__(self, name, frm, to, tex, top=None, tint=None, uv=None):
        self.name, self.frm, self.to, self.tex, self.top, self.tint = name, list(frm), list(to), tex, top, tint
        self.uv = uv

    def check(self):
        for i in range(3):
            assert 0 <= self.frm[i] < self.to[i] <= 16, f"{self.name} leaves the block"
            for v in (self.frm[i], self.to[i]):
                assert (v * 2) == int(v * 2), f"{self.name} is off the half unit grid"


class Group:
    def __init__(self, name, pivot, cubes=(), children=(), static=False):
        self.name, self.pivot, self.cubes, self.children, self.static = name, list(pivot), list(cubes), list(children), static


def box(name, x0, y0, z0, x1, y1, z1, tex, **kw):
    return Cube(name, (x0, y0, z0), (x1, y1, z1), tex, **kw)


# ---------------------------------------------------------------- sieve

def sieve_model():
    frame = []
    for n, (x, z) in {"nw": (1, 1), "ne": (13, 1), "sw": (1, 13), "se": (13, 13)}.items():
        frame.append(box("post_" + n, x, 0, z, x + 2, 13, z + 2, "log", top="log_top"))
    for n, (x, z) in {"nw": (0.5, 0.5), "ne": (12.5, 0.5), "sw": (0.5, 12.5), "se": (12.5, 12.5)}.items():
        frame.append(box("foot_" + n, x, 0, z, x + 3, 1, z + 3, "wood"))
    frame += [
        box("rail_n", 3, 10, 1, 13, 13, 2.5, "wood"), box("rail_s", 3, 10, 13.5, 13, 13, 15, "wood"),
        box("rail_w", 1, 10, 3, 2.5, 13, 13, "wood"), box("rail_e", 13.5, 10, 3, 15, 13, 13, "wood"),
        box("stretcher_n", 3, 4, 1.5, 13, 5.5, 2.5, "wood"), box("stretcher_s", 3, 4, 13.5, 13, 5.5, 14.5, "wood"),
    ]
    for n, (x, z0, z1) in {"nw": (1, 0.5, 1), "ne": (13, 0.5, 1), "sw": (1, 15, 15.5), "se": (13, 15, 15.5)}.items():
        frame.append(box("bracket_" + n, x, 10.5, z0, x + 2, 12.5, z1, "brass"))
    for n, (x, z0) in {"nw": (1.5, 0), "ne": (13.5, 0), "sw": (1.5, 15.5), "se": (13.5, 15.5)}.items():
        frame.append(box("rivet_" + n, x, 11, z0, x + 0.5, 11.5, z0 + 0.5, "brass"))

    tray = [
        box("tray_n", 3.5, 10, 3.5, 12.5, 15, 4.5, "wood"), box("tray_s", 3.5, 10, 11.5, 12.5, 15, 12.5, "wood"),
        box("tray_w", 3.5, 10, 4.5, 4.5, 15, 11.5, "wood"), box("tray_e", 11.5, 10, 4.5, 12.5, 15, 11.5, "wood"),
        box("tray_rivet_nw", 3.5, 15, 3.5, 4, 15.5, 4, "brass"), box("tray_rivet_ne", 12, 15, 3.5, 12.5, 15.5, 4, "brass"),
        box("tray_rivet_sw", 3.5, 15, 12, 4, 15.5, 12.5, "brass"), box("tray_rivet_se", 12, 15, 12, 12.5, 15.5, 12.5, "brass"),
    ]
    straps = []
    for n, (x, z0, z1) in {"nw": (3.5, 3, 3.5), "ne": (11.5, 3, 3.5), "sw": (3.5, 12.5, 13), "se": (11.5, 12.5, 13)}.items():
        straps.append(Group("strap_" + n, (x + 0.5, 13, (z0 + z1) / 2), [box("strap_" + n, x, 10.5, z0, x + 1, 13, z1, "brass")]))
    mesh = Group("mesh", (8, 11.5, 8), [box("mesh", 4, 11, 4, 12, 11.5, 12, "mesh")],
                 [Group("finds", (8, 11.5, 8))])
    layer = Group("layer", (8, 11.5, 8))
    tray_g = Group("tray", (8, 13, 8), tray, [mesh, layer])
    chute = Group("chute", (8, 8, 8))
    return Group("frame", (8, 0, 8), frame, [tray_g] + straps + [chute], static=True)


def kf(time, pos=None, rot=None, scale=None, mode="linear", left=None, right=None):
    return dict(time=time, pos=pos, rot=rot, scale=scale, mode=mode, left=left, right=right)


def swing(length, stops, amp_x, amp_z=0.0, roll=0.0, wobble=0.0, diagonal=False):
    """Tray swing with a fast-in, hard-stop curve between stops. Returns keyframes for tray position and rotation."""
    n = len(stops)
    keys = []
    xs = [(-amp_x if i % 2 == 0 else amp_x) for i in range(n)]
    zs = [(amp_z if (diagonal and i % 2 == 1) else 0.0) for i in range(n)]
    for i, t in enumerate(stops):
        prev = (xs[i] - xs[i - 1]) if i > 0 else (xs[0] - xs[-2] if n > 2 else -(xs[1] - xs[0]))
        seg = (stops[i] - stops[i - 1]) if i > 0 else (stops[1] - stops[0])
        dz = (zs[i] - zs[i - 1]) if i > 0 else 0.0
        left = dict(t=-0.04 * seg, v=[-0.12 * prev, 0, -0.12 * dz])
        right = dict(t=0.4 * seg, v=[0, 0, 0])
        keys.append(kf(t, pos=[xs[i], 0, zs[i]], rot=[0, 0, -roll * xs[i] / max(amp_x, 0.001)], mode="bezier", left=left, right=right))
    return keys


def sieve_animations():
    A = {}

    def clip(name, length, loop, **bones):
        A[name] = dict(length=length, loop=loop, bones=bones)

    clip("idle", 0, "hold")
    clip("load", 0.25, "once",
         tray=[kf(0, pos=[0, 0, 0], mode="catmullrom"), kf(0.08, pos=[0, -0.5, 0], mode="catmullrom"), kf(0.25, pos=[0, 0, 0])],
         mesh=[kf(0, pos=[0, 0, 0], mode="catmullrom"), kf(0.08, pos=[0, -0.3, 0], mode="catmullrom"), kf(0.25, pos=[0, 0, 0])])
    straps = lambda k: {s: k for s in ("strap_nw", "strap_ne", "strap_sw", "strap_se")}

    def strap_keys(tray_keys, k=6.0):
        out = []
        for key in tray_keys:
            p = key["pos"]
            out.append(dict(key, pos=None, rot=[-k * p[2], 0, k * p[0]], scale=None))
        return out

    hand = swing(0.6, [0, 0.3, 0.6], 1.0, amp_z=0.7, roll=2.0, diagonal=True)
    mesh_b = [kf(0, pos=[0, -0.15, 0], mode="catmullrom"), kf(0.15, pos=[0, 0, 0], mode="catmullrom"),
              kf(0.3, pos=[0, -0.15, 0], mode="catmullrom"), kf(0.45, pos=[0, 0, 0], mode="catmullrom"), kf(0.6, pos=[0, -0.15, 0])]
    clip("shake_hand", 0.6, "loop", tray=hand, mesh=mesh_b, **straps(strap_keys(hand)))

    # windup: even and metronomic with a spring wobble after each stop
    w = []
    for i, (t, x) in enumerate([(0, -1.0), (0.4, 1.0), (0.8, -1.0)]):
        sgn = 1 if x > 0 else -1
        prev_x = -x
        left = dict(t=-0.016, v=[-0.12 * (x - prev_x), 0, 0])
        right = dict(t=0.1, v=[0, 0, 0])
        w.append(kf(t, pos=[x, 0, 0], rot=[0, 0, -2.0 * x], mode="bezier", left=left, right=right))
        if t < 0.8:
            w.append(kf(t + 0.07, pos=[x - sgn * 0.14, 0, 0], rot=[0, 0, -2.0 * (x - sgn * 0.14)], mode="catmullrom"))
            w.append(kf(t + 0.14, pos=[x - sgn * 0.02, 0, 0], rot=[0, 0, -2.0 * x], mode="bezier", right=dict(t=0.1, v=[0, 0, 0]),
                        left=dict(t=-0.03, v=[0, 0, 0])))
    w.sort(key=lambda k: k["time"])
    clip("shake_windup", 0.8, "loop", tray=w, **straps(strap_keys(w)))

    # furnace: short fast swing, hard stops, constant tremble on Z
    f = []
    for i, (t, x) in enumerate([(0, -0.75), (0.2, 0.75), (0.4, -0.75)]):
        f.append(kf(t, pos=[x, 0, 0.25 if i % 2 == 0 else -0.25], rot=[0, 0, -1.0 * x / 0.75], mode="bezier",
                    left=dict(t=-0.01, v=[-0.24 * x, 0, 0]),
                    right=dict(t=0.08, v=[0, 0, 0])))
    for t, z in ((0.05, -0.25), (0.1, 0.25), (0.15, -0.25), (0.25, -0.25), (0.3, 0.25), (0.35, -0.25)):
        base = [k for k in f if k["time"] < t][-1]
        f.append(kf(t, pos=[base["pos"][0], 0, z], rot=base["rot"], mode="linear"))
    f.sort(key=lambda k: k["time"])
    clip("shake_furnace", 0.4, "loop", tray=f, **straps(strap_keys(f, 4.0)))

    clip("finish", 0.5, "once",
         tray=[kf(0, pos=[0, 0, 0], mode="catmullrom"), kf(0.12, pos=[0, -0.8, 0], mode="catmullrom"),
               kf(0.28, pos=[0, 0.25, 0], mode="catmullrom"), kf(0.5, pos=[0, 0, 0])],
         mesh=[kf(0, pos=[0, 0, 0], mode="catmullrom"), kf(0.12, pos=[0, -0.4, 0], mode="catmullrom"),
               kf(0.28, pos=[0, 0.15, 0], mode="catmullrom"), kf(0.5, pos=[0, 0, 0])])
    clip("mesh_swap", 0.3, "once",
         mesh=[kf(0, pos=[0, 2, 0], mode="linear"), kf(0.12, pos=[0, 0, 0], mode="catmullrom"),
               kf(0.2, pos=[0, 0.4, 0], mode="catmullrom"), kf(0.3, pos=[0, 0, 0])])
    sway = [kf(0, rot=[0, 0, 0], mode="catmullrom"), kf(0.3, rot=[0, 0, 1.2], mode="catmullrom"), kf(0.6, rot=[0, 0, 0], mode="catmullrom"),
            kf(0.9, rot=[0, 0, -1.2], mode="catmullrom"), kf(1.2, rot=[0, 0, 0])]
    clip("blocked", 1.2, "loop", **straps(sway))
    return A


# ---------------------------------------------------------------- the other blocks

def barrel_model():
    c = [box("skid_w", 2, 0, 1, 4, 1, 15, "dark"), box("skid_e", 12, 0, 1, 14, 1, 15, "dark"), box("floor", 1.5, 1, 1.5, 14.5, 2, 14.5, "dark")]
    for i, (a, b) in enumerate([(2, 6), (6, 10), (10, 14)]):
        d = 1
        c += [box(f"stave_n{i}", a, 1, d, b, 15, 2.5, "wood", top="log_top"), box(f"stave_s{i}", a, 1, 13.5, b, 15, 16 - d, "wood", top="log_top"),
              box(f"stave_w{i}", d, 1, a, 2.5, 15, b, "wood", top="log_top"), box(f"stave_e{i}", 13.5, 1, a, 16 - d, 15, b, "wood", top="log_top")]
    c += [box("rim_n", 1, 15, 1, 15, 16, 2.5, "log", top="log_top"), box("rim_s", 1, 15, 13.5, 15, 16, 15, "log", top="log_top"),
          box("rim_w", 1, 15, 2.5, 2.5, 16, 13.5, "log", top="log_top"), box("rim_e", 13.5, 15, 2.5, 15, 16, 13.5, "log", top="log_top")]
    for tag, y0, y1 in (("low", 4, 5.5), ("high", 10.5, 12)):
        c += [box(f"band_{tag}_n", 2, y0, 0.5, 14, y1, 1.0, "brass"), box(f"band_{tag}_s", 2, y0, 15.0, 14, y1, 15.5, "brass"),
              box(f"band_{tag}_w", 0.5, y0, 2, 1.0, y1, 14, "brass"), box(f"band_{tag}_e", 15.0, y0, 2, 15.5, y1, 14, "brass")]
    c += [box("rivet_a", 7.5, 4.5, 0, 8, 5, 0.5, "brass"), box("rivet_b", 7.5, 11, 0, 8, 11.5, 0.5, "brass")]
    return Group("body", (8, 0, 8), c, [Group("inner", (8, 14, 8))], static=True)


def clay_barrel_model():
    c = [box("foot", 1.5, 0, 1.5, 14.5, 1, 14.5, "brick"), box("floor", 2, 1, 2, 14, 2, 14, "clay")]
    c += [box("wall_n", 2.5, 1, 1.5, 13.5, 14, 3.5, "clay"), box("wall_s", 2.5, 1, 12.5, 13.5, 14, 14.5, "clay"),
          box("wall_w", 1.5, 1, 3.5, 3.5, 14, 12.5, "clay"), box("wall_e", 12.5, 1, 3.5, 14.5, 14, 12.5, "clay")]
    for n, (x, z) in {"nw": (2, 2), "ne": (12, 2), "sw": (2, 12), "se": (12, 12)}.items():
        c.append(box("corner_" + n, x, 1, z, x + 2, 14, z + 2, "clay"))
    c += [box("lip_n", 1, 14, 1, 15, 16, 2.5, "brick"), box("lip_s", 1, 14, 13.5, 15, 16, 15, "brick"),
          box("lip_w", 1, 14, 2.5, 2.5, 16, 13.5, "brick"), box("lip_e", 13.5, 14, 2.5, 15, 16, 13.5, "brick")]
    c += [box("hoop_n", 1.5, 6, 1.0, 14.5, 8, 1.5, "iron"), box("hoop_s", 1.5, 6, 14.5, 14.5, 8, 15.0, "iron"),
          box("hoop_w", 1.0, 6, 1.5, 1.5, 8, 14.5, "iron"), box("hoop_e", 14.5, 6, 1.5, 15.0, 8, 14.5, "iron")]
    return Group("body", (8, 0, 8), c, [Group("inner", (8, 14, 8))], static=True)


def crucible_model():
    c = [box("foot_a", 3.5, 0, 3.5, 5.5, 2, 5.5, "brick"), box("foot_b", 10.5, 0, 3.5, 12.5, 2, 5.5, "brick"), box("foot_c", 7, 0, 10.5, 9, 2, 12.5, "brick"),
         box("belly", 3, 2, 3, 13, 8, 13, "clay")]
    c += [box("band_n", 2.5, 6.5, 2.5, 13.5, 8, 3, "iron"), box("band_s", 2.5, 6.5, 13, 13.5, 8, 13.5, "iron"),
          box("band_w", 2.5, 6.5, 3, 3, 8, 13, "iron"), box("band_e", 13, 6.5, 3, 13.5, 8, 13, "iron")]
    c += [box("wall_n", 2, 8, 2, 14, 14, 3.5, "clay"), box("wall_s", 2, 8, 12.5, 14, 14, 14, "clay"),
          box("wall_w", 2, 8, 3.5, 3.5, 14, 12.5, "clay"), box("wall_e", 12.5, 8, 3.5, 14, 14, 12.5, "clay")]
    c += [box("rim_n", 1.5, 14, 1.5, 14.5, 15.5, 3, "brick"), box("rim_s", 1.5, 14, 13, 14.5, 15.5, 14.5, "brick"),
          box("rim_w", 1.5, 14, 3, 3, 15.5, 13, "brick"), box("rim_e", 13, 14, 3, 14.5, 15.5, 13, "brick")]
    c += [box("lip_n", 6, 14, 0.5, 10, 15.5, 1.5, "brick"), box("lip_s", 6, 14, 14.5, 10, 15.5, 15.5, "brick"),
          box("lip_w", 0.5, 14, 6, 1.5, 15.5, 10, "brick"), box("lip_e", 14.5, 14, 6, 15.5, 15.5, 10, "brick")]
    return Group("body", (8, 0, 8), c, [Group("inner", (8, 11, 8))], static=True)


def auto_hammer_model():
    c = [box("base", 0.5, 0, 0.5, 15.5, 3, 15.5, "stone"),
         box("trough_floor", 3, 3, 3, 13, 3.5, 13, "dark"),
         box("trough_n", 3, 3.5, 2.5, 13, 5, 3.5, "stone"), box("trough_s", 3, 3.5, 12.5, 13, 5, 13.5, "stone"),
         box("trough_w", 2.5, 3.5, 3.5, 3.5, 5, 12.5, "stone"), box("trough_e", 12.5, 3.5, 3.5, 13.5, 5, 12.5, "stone")]
    for n, (x, z) in {"nw": (1.5, 1.5), "ne": (12.5, 1.5), "sw": (1.5, 12.5), "se": (12.5, 12.5)}.items():
        c.append(box("post_" + n, x, 3, z, x + 2, 15, z + 2, "log", top="log_top"))
    c += [box("beam_n", 3.5, 12, 1.5, 12.5, 15, 3.5, "wood"), box("beam_s", 3.5, 12, 12.5, 12.5, 15, 14.5, "wood"),
          box("brace_w", 1.5, 8, 3.5, 3.5, 9.5, 12.5, "wood"), box("brace_e", 12.5, 8, 3.5, 14.5, 9.5, 12.5, "wood"),
          box("guide_a", 4, 12, 3.5, 7, 15, 12.5, "dark"), box("guide_b", 9, 12, 3.5, 12, 15, 12.5, "dark")]
    c += [box("clamp", 7, 9, 5, 9, 11, 11, "iron"), box("clamp_bolt_a", 7.5, 11, 5.5, 8.5, 11.5, 6.5, "brass"),
          box("clamp_bolt_b", 7.5, 11, 9.5, 8.5, 11.5, 10.5, "brass"),
          box("cam_a", 5, 0, 1, 7, 1, 2.5, "brass"), box("cam_b", 9, 0, 1, 11, 1, 2.5, "brass"),
          box("hinge_n", 7, 13, 0.5, 9, 15, 1.5, "brass"), box("hinge_s", 7, 13, 14.5, 9, 15, 15.5, "brass")]
    return Group("body", (8, 0, 8), c, static=True)


def powered_drive_model():
    c = [box("base", 0, 0, 0, 16, 3, 16, "stone"), box("case", 1.5, 3, 1.5, 14.5, 11, 14.5, "iron"),
         box("band_a", 1, 4, 1, 15, 5, 15, "brass"), box("band_b", 1, 9, 1, 15, 10, 15, "brass"),
         box("top", 2.5, 11, 2.5, 13.5, 12, 13.5, "iron"),
         box("strip_frame", 3, 5.5, 1, 13, 8.5, 1.5, "dark"),
         box("coil_a", 4, 4, 14.5, 12, 6, 16, "copper"), box("coil_b", 4, 6.5, 14.5, 12, 8.5, 16, "copper"), box("coil_c", 4, 9, 14.5, 12, 11, 16, "copper"),
         box("cam_a", 4, 12, 5, 6, 14, 11, "brass"), box("cam_b", 10, 12, 5, 12, 14, 11, "brass"),
         box("cam_post_a", 4.5, 12, 7.5, 5.5, 14.5, 8.5, "iron"), box("cam_post_b", 10.5, 12, 7.5, 11.5, 14.5, 8.5, "iron"),
         box("rivet_a", 1.5, 6, 0.5, 2, 6.5, 1, "brass"), box("rivet_b", 14, 6, 0.5, 14.5, 6.5, 1, "brass"),
         box("rivet_c", 1.5, 7.5, 0.5, 2, 8, 1, "brass"), box("rivet_d", 14, 7.5, 0.5, 14.5, 8, 1, "brass")]
    return Group("body", (8, 0, 8), c, static=True)


def strip_segments():
    """Five amethyst segments of the powered drive's charge strip, as separate parts so the blockstate can show 0 to 5."""
    return [box(f"charge_{i + 1}", 3.5 + i * 2, 6, 0.5, 5 + i * 2, 8, 1, "amethyst") for i in range(5)]


def casting_tray_model():
    c = [box("foot_nw", 1, 0, 1, 3, 1, 3, "brick"), box("foot_ne", 13, 0, 1, 15, 1, 3, "brick"),
         box("foot_sw", 1, 0, 13, 3, 1, 15, "brick"), box("foot_se", 13, 0, 13, 15, 1, 15, "brick"),
         box("floor", 1, 1, 1, 15, 2, 15, "clay"),
         box("wall_n_a", 1, 2, 1, 6, 5, 2.5, "clay"), box("wall_n_b", 10, 2, 1, 15, 5, 2.5, "clay"),
         box("wall_s", 1, 2, 13.5, 15, 5, 15, "clay"), box("wall_w", 1, 2, 2.5, 2.5, 5, 13.5, "clay"),
         box("wall_e", 13.5, 2, 2.5, 15, 5, 13.5, "clay"),
         box("lip_n_a", 1, 5, 1, 6, 6, 2.5, "brick"), box("lip_n_b", 10, 5, 1, 15, 6, 2.5, "brick"),
         box("lip_s", 1, 5, 13.5, 15, 6, 15, "brick"), box("lip_w", 1, 5, 2.5, 2.5, 6, 13.5, "brick"), box("lip_e", 13.5, 5, 2.5, 15, 6, 13.5, "brick"),
         box("ingot_a", 4, 2, 5, 12, 2.5, 11, "dark"), box("fed_notch", 6, 2, 0.5, 10, 3, 1, "iron")]
    return Group("body", (8, 0, 8), c, [Group("inner", (8, 2.5, 8))], static=True)


def windup_drive_model():
    c = [box("base", 0, 0, 0, 16, 3, 16, "stone"), box("case", 1.5, 3, 1.5, 14.5, 11, 14.5, "wood"),
         box("band_a", 1, 4, 1, 15, 5, 15, "brass"), box("band_b", 1, 9, 1, 15, 10, 15, "brass"),
         box("top", 2.5, 11, 2.5, 13.5, 12, 13.5, "wood"),
         box("plate", 4, 5.5, 0.5, 12, 9.5, 1.5, "brass"), box("dial", 6.5, 6.5, 0, 9.5, 8.5, 0.5, "dark"),
         box("cam_a", 4, 12, 5, 6, 14, 11, "brass"), box("cam_b", 10, 12, 5, 12, 14, 11, "brass"),
         box("cam_post_a", 4.5, 12, 7.5, 5.5, 14.5, 8.5, "iron"), box("cam_post_b", 10.5, 12, 7.5, 11.5, 14.5, 8.5, "iron"),
         box("rivet_a", 4.5, 6, 0, 5, 6.5, 0.5, "brass"), box("rivet_b", 11, 6, 0, 11.5, 6.5, 0.5, "brass")]
    return Group("body", (8, 0, 8), c, static=True)


def furnace_drive_model():
    c = [box("base", 0, 0, 0, 16, 3, 16, "stone"), box("firebox", 1.5, 3, 1.5, 14.5, 11, 14.5, "iron"),
         box("band_a", 1, 4, 1, 15, 5, 15, "brass"), box("band_b", 1, 9, 1, 15, 10, 15, "brass"),
         box("door_frame", 4, 4.5, 0.5, 12, 9.5, 1.5, "dark"), box("grate_a", 5, 5, 0, 6, 9, 0.5, "iron"), box("grate_b", 7.5, 5, 0, 8.5, 9, 0.5, "iron"),
         box("grate_c", 10, 5, 0, 11, 9, 0.5, "iron"), box("hinge_a", 3.5, 5, 0, 4, 6, 0.5, "brass"), box("hinge_b", 3.5, 8, 0, 4, 9, 0.5, "brass"),
         box("top", 2.5, 11, 2.5, 13.5, 12, 13.5, "iron"),
         box("chimney", 11, 12, 11, 14, 16, 14, "brick"), box("chimney_rim", 10.5, 15, 10.5, 14.5, 16, 14.5, "iron"),
         box("cam_a", 3, 12, 4, 5, 14, 10, "brass"), box("cam_b", 7, 12, 4, 9, 14, 10, "brass"),
         box("cam_post_a", 3.5, 12, 6.5, 4.5, 14.5, 7.5, "iron"), box("cam_post_b", 7.5, 12, 6.5, 8.5, 14.5, 7.5, "iron")]
    return Group("body", (8, 0, 8), c, static=True)


def crystal_model():
    """Three prisms on a small base; the machine renderer draws the same prisms scaled 0.2 to 1.0 as a seed grows."""
    c = [box("base", 5, 0, 5, 11, 1, 11, "stone"), box("prism_a", 6, 1, 6, 8, 9, 8, "crystal"),
         box("prism_b", 8, 1, 7, 10, 6, 9, "crystal"), box("prism_c", 6, 1, 8, 8, 5, 10, "crystal")]
    return Group("body", (8, 0, 8), c, static=True)

# ---------------------------------------------------------------- output

def walk(group, parent=None):
    yield group, parent
    for ch in group.children:
        yield from walk(ch, group)


def all_cubes(root):
    for g, _ in walk(root):
        for c in g.cubes:
            yield g, c


def tex_keys(root, extra=()):
    keys = []
    for _, c in all_cubes(root):
        for k in (c.tex, c.top):
            if k and k not in keys:
                keys.append(k)
    for c in extra:
        for k in (c.tex, c.top):
            if k and k not in keys:
                keys.append(k)
    return keys


def face_entry(c, face, idx):
    uv = (c.uv or auto_uv(c.frm, c.to))[face]
    key = c.top if (c.top and face in ("up", "down")) else c.tex
    e = {"uv": uv, "texture": idx[key]}
    if c.tint is not None:
        e["tintindex"] = c.tint
    return e


def bb_texture(key, idx, embed):
    ns, folder, name = TEX[key]
    t = {"name": name, "path": "", "folder": folder, "namespace": ns, "id": str(idx), "group": "", "scope": 0, "width": 16, "height": 16,
         "uv_width": 16, "uv_height": 16, "particle": key == "wood", "use_as_default": False, "layers_enabled": False,
         "sync_to_project": "", "file_format": "png", "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited",
         "pbr_channel": "color", "fps": 7, "force_translucent": False, "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
         "frame_interpolate": False, "visible": True, "internal": embed is not None, "saved": False, "uuid": uid("tex", key)}
    if embed is not None:
        import base64
        p = None
        if ns == "minecraft":
            p = Path(embed) / "textures" / folder / (name + ".png")
        else:
            p = ROOT / "common/src/main/resources/assets/primamateria/textures" / folder / (name + ".png")
        if p.exists():
            t["source"] = "data:image/png;base64," + base64.b64encode(p.read_bytes()).decode()
    return t


DISPLAY = {
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
}


def bbmodel(name, root, extra=(), animations=None, embed=None):
    keys = tex_keys(root, extra)
    idx = {k: i for i, k in enumerate(keys)}
    elements = []
    for g, c in list(all_cubes(root)) + [(None, c) for c in extra]:
        c.check()
        elements.append({
            "name": c.name, "box_uv": False, "render_order": "default", "rescale": False, "locked": False, "shade_direction_override": "",
            "light_emission": 0, "export": True, "scope": 0, "allow_mirror_modeling": True, "from": c.frm, "to": c.to, "autouv": 0,
            "color": 8, "origin": [(c.frm[0] + c.to[0]) / 2, (c.frm[1] + c.to[1]) / 2, (c.frm[2] + c.to[2]) / 2],
            "faces": {f: face_entry(c, f, idx) for f in ("north", "east", "south", "west", "up", "down")},
            "type": "cube", "uuid": uid(name, "cube", c.name)})

    def outline(g):
        node = {"name": g.name, "origin": g.pivot, "color": 0, "uuid": uid(name, "group", g.name), "export": True, "isOpen": True,
                "locked": False, "visibility": True, "autouv": 0, "children": []}
        for c in g.cubes:
            node["children"].append(uid(name, "cube", c.name))
        for ch in g.children:
            node["children"].append(outline(ch))
        return node
    out = [outline(root)]
    for c in extra:
        out.append(uid(name, "cube", c.name))
    doc = {"meta": {"format_version": "5.0", "model_format": "java_block", "box_uv": False}, "name": name, "parent": "",
           "java_block_version": 2, "ambientocclusion": True, "front_gui_light": False, "visible_box": [1, 1, 0],
           "variable_placeholders": "", "variable_placeholder_buttons": [], "unhandled_root_fields": {},
           "resolution": {"width": 16, "height": 16}, "elements": elements, "groups": [], "outliner": out,
           "textures": [bb_texture(k, idx[k], embed) for k in keys], "display": DISPLAY}
    if animations:
        doc["animations"] = bb_animations(name, root, animations)
    return doc


def bb_animations(model, root, clips):
    gid = {g.name: uid(model, "group", g.name) for g, _ in walk(root)}
    out = []
    for cname, c in clips.items():
        animators = {}
        for bone, keys in c["bones"].items():
            kfs = []
            for k in keys:
                for channel, short in (("position", "pos"), ("rotation", "rot"), ("scale", "scale")):
                    v = k[short]
                    if v is None:
                        continue
                    e = {"channel": channel, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}], "uuid": uid(model, cname, bone, k["time"], channel),
                         "time": k["time"], "color": -1, "interpolation": k["mode"]}
                    if k["mode"] == "bezier":
                        L = k["left"] or dict(t=-0.05, v=[0, 0, 0])
                        R = k["right"] or dict(t=0.05, v=[0, 0, 0])
                        e.update({"bezier_linked": False, "bezier_left_time": [L["t"]] * 3, "bezier_left_value": L["v"],
                                  "bezier_right_time": [R["t"]] * 3, "bezier_right_value": R["v"]})
                    kfs.append(e)
            animators[gid[bone]] = {"name": bone, "type": "bone", "keyframes": kfs}
        out.append({"uuid": uid(model, "anim", cname), "name": cname, "loop": c["loop"], "override": False, "length": c["length"],
                    "snapping": 20, "selected": False, "saved": False, "path": "", "anim_time_update": "", "blend_weight": "",
                    "start_delay": "", "loop_delay": "", "animators": animators})
    return out


def main():
    embed = None
    if "--embed" in sys.argv:
        embed = sys.argv[sys.argv.index("--embed") + 1]
    target = OUT / "_local" if embed else OUT
    target.mkdir(parents=True, exist_ok=True)
    models = {
        "sieve": (sieve_model(), (), sieve_animations()),
        "wooden_barrel": (barrel_model(), (), None),
        "clay_barrel": (clay_barrel_model(), (), None),
        "crucible": (crucible_model(), (), None),
        "auto_hammer": (auto_hammer_model(), (), None),
        "powered_drive": (powered_drive_model(), strip_segments(), None),
        "casting_tray": (casting_tray_model(), (), None),
        "windup_drive": (windup_drive_model(), (), None),
        "furnace_drive": (furnace_drive_model(), (), None),
        "crystal": (crystal_model(), (), None),
    }
    for name, (root, extra, anims) in models.items():
        doc = bbmodel(name, root, extra, anims, embed)
        n = len(doc["elements"])
        assert n <= 44, f"{name} has {n} elements"
        (target / f"{name}.bbmodel").write_text(json.dumps(doc, indent=1), encoding="utf-8")
        print(f"{name}: {n} elements -> {target / (name + '.bbmodel')}")


if __name__ == "__main__":
    main()
