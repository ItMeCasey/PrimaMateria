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

NL = chr(10)


def patch_block_codec(name, cls):
    """26.3 blocks no longer declare a codec: drop the CODEC field and the codec() override."""
    p = DST / "main/java" / PKG / "block" / (name + ".java")
    s = p.read_text(encoding="utf-8")
    s = s.replace("import com.mojang.serialization.MapCodec;" + NL, "")
    s = s.replace("    private static final MapCodec<" + cls + "> CODEC = simpleCodec(" + cls + "::new);" + NL + NL, "")
    s = s.replace("    @Override" + NL + "    protected MapCodec<? extends BaseEntityBlock> codec() {" + NL + "        return CODEC;" + NL + "    }" + NL + NL, "")
    assert "MapCodec" not in s, name
    p.write_text(s, encoding="utf-8", newline=NL)


# Add patch functions here and list them in PATCHES. Each reads a file under DST, replaces text, asserts the old form is gone
# and writes it back with LF line endings.
PATCHES = [lambda: patch_block_codec("SieveBlock", "SieveBlock"), lambda: patch_block_codec("AutoHammerBlock", "AutoHammerBlock")]


def copy_tree(name):
    src = SRC / name / "java"
    dst = DST / name / "java"
    if dst.exists():
        shutil.rmtree(dst)
    if src.exists():
        shutil.copytree(src, dst)


def copy_loaders():
    """The Forge and NeoForge entry classes are identical in 26.2 and 26.3, so 26.2 is copied over 26.3 too."""
    for loader in ("neoforge", "forge"):
        for part in ("main", "dev"):
            src = ROOT / f"{loader}-26.2" / "src" / part / "java"
            dst = ROOT / f"{loader}-26.3" / "src" / part / "java"
            if src.exists():
                if dst.exists():
                    shutil.rmtree(dst)
                shutil.copytree(src, dst)


def main():
    copy_loaders()
    copy_tree("main")
    copy_tree("dev")
    for patch in PATCHES:
        patch()
    print(f"version-26.3 synced from version-26.2 ({len(PATCHES)} patches)")


if __name__ == "__main__":
    main()
