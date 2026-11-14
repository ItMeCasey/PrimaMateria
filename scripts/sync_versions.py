"""Copies version-26.2/src/{main,dev}/java to version-26.3 and re-applies the 26.3 differences.

26.2 is the source of truth for the game-side code; version-26.3's Java is never edited by hand. Each known difference between
the two Minecraft versions is one patch function below (see BarelyCrafting/scripts/sync_versions.py for examples: block codecs,
Player.drop). Resources that differ (loot tables) are separate files per version and are not touched.

Run after every change to version-26.2:  python scripts/sync_versions.py
"""
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "version-26.2" / "src"
DST = ROOT / "version-26.3" / "src"
PKG = "com/itmecasey/primamateria"


# Add patch functions here and list them in PATCHES. Each reads a file under DST, replaces text, asserts the old form is gone
# and writes it back with newline="\n".
def patch_preview_blocks():
    """26.3 blocks have no codec: drop the MapCodec field and the codec() override from the preview blocks."""
    nl = chr(10)
    for cls in ("SievePreviewBlock", "MachinePreviewBlock"):
        p = DST / "main/java" / PKG / f"block/{cls}.java"
        s = p.read_text(encoding="utf-8")
        lines = s.split(nl)
        out = []
        skip = 0
        for i, line in enumerate(lines):
            if skip:
                skip -= 1
                continue
            if "import com.mojang.serialization.MapCodec;" in line or "private static final MapCodec" in line:
                continue
            if line.strip() == "@Override" and i + 1 < len(lines) and "MapCodec<? extends Block> codec()" in lines[i + 1]:
                skip = 4
                continue
            out.append(line)
        s = nl.join(out)
        assert "MapCodec" not in s, cls
        p.write_text(s, encoding="utf-8", newline=nl)


PATCHES = [patch_preview_blocks]


def copy_tree(name):
    src = SRC / name / "java"
    dst = DST / name / "java"
    if dst.exists():
        shutil.rmtree(dst)
    if src.exists():
        shutil.copytree(src, dst)


def main():
    copy_tree("main")
    copy_tree("dev")
    for patch in PATCHES:
        patch()
    print(f"version-26.3 synced from version-26.2 ({len(PATCHES)} patches)")


if __name__ == "__main__":
    main()
