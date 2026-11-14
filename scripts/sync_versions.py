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
def patch_sieve_preview_block():
    """26.3 blocks have no codec: drop the MapCodec field and the codec() override."""
    p = DST / "main/java" / PKG / "block/SievePreviewBlock.java"
    s = p.read_text(encoding="utf-8")
    s = s.replace("import com.mojang.serialization.MapCodec;\n", "")
    s = s.replace("    private static final MapCodec<SievePreviewBlock> CODEC = simpleCodec(SievePreviewBlock::new);\n\n", "")
    s = s.replace("    @Override\n    protected MapCodec<? extends Block> codec() {\n        return CODEC;\n    }\n\n", "")
    assert "MapCodec" not in s
    p.write_text(s, encoding="utf-8", newline="\n")


PATCHES = [patch_sieve_preview_block]


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
