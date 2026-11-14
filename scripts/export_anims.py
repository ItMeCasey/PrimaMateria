#!/usr/bin/env python3
"""Turns the Blockbench sources in branding/models into game files (the .bbmodel files are JSON).

    python scripts/export_anims.py

Writes into common/src/main/resources/assets/primamateria/:
  models/block/<name>.json                 the whole model at rest (also the item model)
  models/block/sieve_frame.json            sieve: the static frame only
  models/block/sieve_tray.json             sieve: tray and straps at rest (drawn by the block model when the sieve is not moving)
  models/block/sieve_mesh_<slot>.json      sieve: the mesh plate of each tier slot
  models/block/sieve_fill_<k>.json         sieve: the still layer of k quarters (used when animations are off)
  models/block/powered_drive_charge_<k>.json  powered drive: charge strip segments 1..k
  primamateria_anim/sieve.json             sieve: moving bones and clips for the renderer (see AnimJson in common)
  blockstates/sieve.json                   sieve multipart blockstate
Fails with a plain message on an expression keyframe, an unknown interpolation, a missing contract bone, a cube off the block or
a cube with a missing texture.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "branding" / "models"
ASSETS = ROOT / "common" / "src" / "main" / "resources" / "assets" / "primamateria"
CONTRACT = {"sieve": ["frame", "tray", "strap_nw", "strap_ne", "strap_sw", "strap_se", "mesh", "layer", "finds", "chute"]}
MESH_TEXTURES = ["mesh_cloth", "mesh_copper", "mesh_iron", "mesh_diamond", "mesh_netherite"]
FACE_ORDER = ["north", "east", "south", "west", "up", "down"]
INTERP = {"linear", "step", "catmullrom", "bezier"}
FILL_QUARTER = 0.8
DISPLAY_KEEP = ("thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "ground", "gui", "fixed")


class ExportError(Exception):
    pass


def tex_id(t):
    return f"{t['namespace']}:{t['folder']}/{t['name']}" if t.get("namespace") else f"{t['folder']}/{t['name']}"


def load(name):
    return json.loads((SRC / f"{name}.bbmodel").read_text(encoding="utf-8"))


def groups_of(doc):
    """Yields (group node, parent node or None, element list) in outliner order, parents first."""
    elements = {e["uuid"]: e for e in doc["elements"]}

    def walk(node, parent):
        cubes = [elements[c] for c in node["children"] if isinstance(c, str)]
        yield node, parent, cubes
        for ch in node["children"]:
            if isinstance(ch, dict):
                yield from walk(ch, node)
    for node in doc["outliner"]:
        if isinstance(node, dict):
            yield from walk(node, None)


def block_element(e, textures, keymap, tint=None):
    for i in range(3):
        if not (0 <= e["from"][i] < e["to"][i] <= 16):
            raise ExportError(f"cube {e['name']} leaves the block")
    faces = {}
    for f in FACE_ORDER:
        face = e["faces"][f]
        t = face.get("texture")
        if t is None:
            raise ExportError(f"cube {e['name']} face {f} has no texture")
        faces[f] = {"uv": face["uv"], "texture": "#" + keymap[t]}
        if tint is not None:
            faces[f]["tintindex"] = tint
    return {"from": e["from"], "to": e["to"], "faces": faces}


def keymap_of(doc):
    out = {}
    for i, t in enumerate(doc["textures"]):
        out[int(t["id"])] = t["name"]
    return out


def model_json(doc, cubes, tint=None, extra_display=True):
    km = keymap_of(doc)
    used = []
    elements = []
    for e in cubes:
        elements.append(block_element(e, doc["textures"], km, tint))
    for e in elements:
        for f in e["faces"].values():
            if f["texture"][1:] not in used:
                used.append(f["texture"][1:])
    tex = {}
    for t in doc["textures"]:
        if t["name"] in used:
            tex[t["name"]] = tex_id(t)
    tex["particle"] = tex[used[0]] if used else "minecraft:block/oak_planks"
    for pref in ("oak_planks", "gold_block", "iron_block", "smooth_stone", "terracotta"):
        for t in doc["textures"]:
            if t["name"] == pref and t["name"] in tex:
                tex["particle"] = tex[pref]
    out = {"parent": "minecraft:block/block", "textures": tex, "elements": elements}
    if extra_display:
        out["display"] = {k: v for k, v in doc["display"].items() if k in DISPLAY_KEEP}
    return out


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def static_models(name, doc):
    all_cubes = list(doc["elements"])
    write(ASSETS / "models/block" / f"{name}.json", model_json(doc, all_cubes))


def vec(dp, axis):
    v = dp[axis]
    try:
        return float(v)
    except (TypeError, ValueError):
        raise ExportError(f"keyframe value {v!r} is not a plain number (expressions are not allowed)")


def clips_json(doc, bone_names):
    clips = {}
    for anim in doc.get("animations", []):
        bones = {}
        for animator in anim["animators"].values():
            bname = animator["name"]
            if bname not in bone_names:
                raise ExportError(f"animation {anim['name']} animates {bname}, which is not a bone")
            channels = {}
            for k in animator["keyframes"]:
                ch = k["channel"]
                if ch not in ("position", "rotation", "scale"):
                    raise ExportError(f"animation {anim['name']}: channel {ch} is not supported")
                mode = k.get("interpolation", "linear")
                if mode not in INTERP:
                    raise ExportError(f"animation {anim['name']}: unknown interpolation {mode}")
                axes = channels.setdefault(ch, [[], [], []])
                dp = k["data_points"][0]
                for a, ax in enumerate("xyz"):
                    key = {"t": round(float(k["time"]), 5), "v": round(vec(dp, ax), 5), "i": mode}
                    if mode == "bezier":
                        key["lt"] = k["bezier_left_time"][a]
                        key["lv"] = k["bezier_left_value"][a]
                        key["rt"] = k["bezier_right_time"][a]
                        key["rv"] = k["bezier_right_value"][a]
                    axes[a].append(key)
            for axes in channels.values():
                for lst in axes:
                    lst.sort(key=lambda x: x["t"])
            bones[bname] = channels
        clips[anim["name"]] = {"length": anim["length"], "loop": anim["loop"], "bones": bones}
    return clips


def sieve(doc):
    name = "sieve"
    nodes = list(groups_of(doc))
    names = [n["name"] for n, _, _ in nodes]
    for need in CONTRACT[name]:
        if need not in names:
            raise ExportError(f"{name}.bbmodel is missing the contract bone {need}")
    km = keymap_of(doc)
    textures = {}
    frame_cubes, tray_cubes, mesh_cubes = [], [], []
    bones = []
    for node, parent, cubes in nodes:
        static = node["name"] == "frame"
        entry = {"name": node["name"], "parent": parent["name"] if parent else "", "pivot": node["origin"], "cubes": []}
        for e in cubes:
            if static:
                frame_cubes.append(e)
                continue
            if node["name"] == "mesh":
                mesh_cubes.append(e)
            else:
                tray_cubes.append(e)
            keys = {km[f["texture"]] for f in e["faces"].values()}
            if len(keys) != 1:
                raise ExportError(f"cube {e['name']} of a moving bone uses more than one texture")
            key = keys.pop()
            tid = next(tex_id(t) for t in doc["textures"] if t["name"] == key)
            tkey = "mesh" if key.startswith("mesh_") else key
            textures[tkey] = tid
            entry["cubes"].append({"from": e["from"], "to": e["to"], "texture": tkey,
                                   "uv": {f: e["faces"][f]["uv"] for f in FACE_ORDER}})
        bones.append(entry)
    anim = {"format": 1, "name": name, "textures": textures, "bones": bones, "clips": clips_json(doc, names)}
    write(ASSETS / "primamateria_anim" / "sieve.json", anim)

    write(ASSETS / "models/block/sieve_frame.json", model_json(doc, frame_cubes))
    write(ASSETS / "models/block/sieve_tray.json", model_json(doc, tray_cubes))
    cloth_mesh = mesh_cubes[0]
    for slot, tex in enumerate(MESH_TEXTURES, start=1):
        d = {"parent": "minecraft:block/block", "textures": {"mesh": f"primamateria:block/{tex}", "particle": f"primamateria:block/{tex}"},
             "elements": [{"from": cloth_mesh["from"], "to": cloth_mesh["to"],
                           "faces": {f: {"uv": cloth_mesh["faces"][f]["uv"], "texture": "#mesh"} for f in FACE_ORDER}}]}
        write(ASSETS / f"models/block/sieve_mesh_{slot}.json", d)
    # the still layer: a plain box on the tray floor, one quarter is 1.1 units, tinted by the load's map color (tintindex 0)
    for k in range(1, 5):
        top = round(11.5 + FILL_QUARTER * k, 2)
        faces = {f: {"uv": [4.5, 4.5, 11.5, 11.5] if f in ("up", "down") else [4.5, 0, 11.5, FILL_QUARTER * k],
                     "texture": "#sift", "tintindex": 0} for f in FACE_ORDER}
        write(ASSETS / f"models/block/sieve_fill_{k}.json",
              {"parent": "minecraft:block/block", "textures": {"sift": "minecraft:block/white_concrete_powder",
               "particle": "minecraft:block/oak_planks"},
               "elements": [{"from": [4.5, 11.5, 4.5], "to": [11.5, top, 11.5], "faces": faces}]})
    # the item: frame, tray and the cloth mesh
    full = model_json(doc, frame_cubes + tray_cubes + mesh_cubes)
    write(ASSETS / "models/block/sieve.json", full)
    mp = [{"apply": {"model": "primamateria:block/sieve_frame"}},
          {"when": {"moving": "false"}, "apply": {"model": "primamateria:block/sieve_tray"}}]
    for slot in range(1, 9):
        mp.append({"when": {"moving": "false", "mesh": str(slot)}, "apply": {"model": f"primamateria:block/sieve_mesh_{min(slot, 5)}"}})
    for k in range(1, 5):
        mp.append({"when": {"moving": "false", "fill": str(k)}, "apply": {"model": f"primamateria:block/sieve_fill_{k}"}})
    write(ASSETS / "blockstates/sieve.json", {"multipart": mp})


def powered_drive(doc):
    base = [e for e in doc["elements"] if not e["name"].startswith("charge_")]
    seg = sorted((e for e in doc["elements"] if e["name"].startswith("charge_")), key=lambda e: e["name"])
    write(ASSETS / "models/block/powered_drive.json", model_json(doc, base))
    for k in range(1, 6):
        write(ASSETS / f"models/block/powered_drive_charge_{k}.json", model_json(doc, seg[:k], extra_display=False))


def main():
    try:
        sieve(load("sieve"))
        for name in ("wooden_barrel", "clay_barrel", "crucible", "auto_hammer"):
            static_models(name, load(name))
        powered_drive(load("powered_drive"))
    except ExportError as e:
        print("EXPORT FAILED:", e)
        sys.exit(1)
    print("exported sieve (frame, tray, 5 mesh slots, 4 fill levels, animation file), barrels, crucible, auto hammer, powered drive")


if __name__ == "__main__":
    main()
