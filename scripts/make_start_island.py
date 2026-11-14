"""Writes the start island as a structure file: data/primamateria/structure/start_island.nbt, once per Minecraft version.

The file is what the game places on a new Void Island world; a datapack can replace it by shipping its own
data/primamateria/structure/start_island.nbt. The plan below is the same as common/.../world/IslandPlan.java (the code plan is the
fallback when no structure file can be loaded). The file's origin corner is the lowest, north-west block of the island; the placer
puts the origin at center + (MIN_X, MIN_Y, MIN_Z), which this script prints in the same form.

Usage: python scripts/make_start_island.py
"""
import gzip
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RADIUS, TREE, TRUNK = 3, 2, 4
# world_version of each Minecraft version's version.json, and whether its palette entries use the lowercase keys id and properties (26.3)
DATA_VERSIONS = {"version-26.2": (4903, False), "version-26.3": (5023, True)}

GRASS = ("minecraft:grass_block", {"snowy": "false"})
DIRT = ("minecraft:dirt", {})
LOG = ("minecraft:oak_log", {"axis": "y"})
LEAVES = ("minecraft:oak_leaves", {"distance": "7", "persistent": "false", "waterlogged": "false"})


def plan():
    out = []
    for dy in range(-2, 1):
        for dx in range(-RADIUS, RADIUS + 1):
            for dz in range(-RADIUS, RADIUS + 1):
                out.append((dx, dy, dz, GRASS if dy == 0 else DIRT))
    for dy in range(1, TRUNK + 1):
        out.append((TREE, dy, TREE, LOG))
    for dy in range(TRUNK - 1, TRUNK + 1):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                corner = abs(dx) == 2 and abs(dz) == 2
                trunk = dx == 0 and dz == 0 and dy <= TRUNK
                if not corner and not trunk:
                    out.append((TREE + dx, dy, TREE + dz, LEAVES))
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            out.append((TREE + dx, TRUNK + 1, TREE + dz, LEAVES))
    cap = TRUNK + 2
    for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        out.append((TREE + dx, cap, TREE + dz, LEAVES))
    return out


# --- a tiny NBT writer: enough for a structure file ---
def name(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def tag_int(v):
    return struct.pack(">i", v)


def tag_string(s):
    return name(s)


def tag_list(tag_id, payloads):
    return struct.pack(">bi", tag_id, len(payloads)) + b"".join(payloads)


def tag_compound(fields):
    """fields: list of (name, tag_id, payload)."""
    return b"".join(struct.pack(">b", t) + name(n) + p for n, t, p in fields) + b"\x00"


def build(data_version, new_palette):
    parts = plan()
    mx = min(p[0] for p in parts)
    my = min(p[1] for p in parts)
    mz = min(p[2] for p in parts)
    size = (max(p[0] for p in parts) - mx + 1, max(p[1] for p in parts) - my + 1, max(p[2] for p in parts) - mz + 1)
    palette_states = []
    for p in parts:
        if p[3] not in palette_states:
            palette_states.append(p[3])
    palette = []
    for blockname, props in palette_states:
        fields = [("id" if new_palette else "Name", 8, tag_string(blockname))]
        if props:
            fields.append(("properties" if new_palette else "Properties", 10, tag_compound([(k, 8, tag_string(v)) for k, v in props.items()])))
        palette.append(tag_compound(fields))
    blocks = []
    for dx, dy, dz, state in parts:
        blocks.append(tag_compound([
            ("pos", 9, tag_list(3, [tag_int(dx - mx), tag_int(dy - my), tag_int(dz - mz)])),
            ("state", 3, tag_int(palette_states.index(state))),
        ]))
    root = tag_compound([
        ("DataVersion", 3, tag_int(data_version)),
        ("size", 9, tag_list(3, [tag_int(s) for s in size])),
        ("palette", 9, tag_list(10, palette)),
        ("blocks", 9, tag_list(10, blocks)),
        ("entities", 9, tag_list(10, [])),
    ])
    return b"\x0a" + name("") + root, (mx, my, mz), size


def main():
    for folder, (version, new_palette) in DATA_VERSIONS.items():
        data, origin, size = build(version, new_palette)
        path = ROOT / folder / "src/main/resources/data/primamateria/structure/start_island.nbt"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(gzip.compress(data, mtime=0))
        print(f"{path.relative_to(ROOT)}: size {size}, origin offset from the island center {origin}")


if __name__ == "__main__":
    main()
