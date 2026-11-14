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


def patch_block_codec(name, cls, factory=None):
    """26.3 blocks no longer declare a codec: drop the CODEC field and the codec() override."""
    p = DST / "main/java" / PKG / "block" / (name + ".java")
    s = p.read_text(encoding="utf-8")
    s = s.replace("import com.mojang.serialization.MapCodec;" + NL, "")
    s = s.replace("    private static final MapCodec<" + cls + "> CODEC = simpleCodec(" + (factory or cls + "::new") + ");" + NL + NL, "")
    s = s.replace("    @Override" + NL + "    protected MapCodec<? extends BaseEntityBlock> codec() {" + NL + "        return CODEC;" + NL + "    }" + NL + NL, "")
    assert "MapCodec" not in s, name
    p.write_text(s, encoding="utf-8", newline=NL)


def patch_drive_fuel():
    """26.3 has no fuel table on the level: an item's burn time is a cooking fuel component resolved with a loot context."""
    p = DST / "main/java" / PKG / "blockentity/DriveBlockEntity.java"
    s = p.read_text(encoding="utf-8")
    begin = "        // FUEL-VERSION-BEGIN" + NL
    end = "        // FUEL-VERSION-END" + NL
    i = s.index(begin)
    j = s.index(end) + len(end)
    body = (
        "        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {" + NL
        + "            return 0;" + NL
        + "        }" + NL
        + "        net.minecraft.world.level.storage.loot.LootContext context = new net.minecraft.world.level.storage.loot.LootContext.Builder("
        + NL + "                new net.minecraft.world.level.storage.loot.LootParams.Builder(serverLevel)"
        + NL + "                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY))"
        + NL + "                .create(java.util.Optional.empty());" + NL
        + "        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(stack,"
        + NL + "                net.minecraft.core.component.DataComponents.COOKING_FUEL,"
        + NL + "                net.minecraft.world.item.component.CookingFuel::burnTime, context, 0);" + NL
    )
    s = s[:i] + body + s[j:]
    assert "fuelValues" not in s
    p.write_text(s, encoding="utf-8", newline=NL)


# Add patch functions here and list them in PATCHES. Each reads a file under DST, replaces text, asserts the old form is gone
# and writes it back with LF line endings.
PATCHES = [
    patch_drive_fuel,
    lambda: patch_block_codec("SieveBlock", "SieveBlock"),
    lambda: patch_block_codec("DriveBlock", "DriveBlock", "p -> new DriveBlock(p, DriveKind.WINDUP)"),
]


def copy_tree(name):
    src = SRC / name / "java"
    dst = DST / name / "java"
    if dst.exists():
        shutil.rmtree(dst)
    if src.exists():
        shutil.copytree(src, dst)


def copy_loader_entries():
    """The loader entry classes (registration, reload listener, capabilities) are identical on 26.2 and 26.3: keep them in step."""
    for loader, name in (("neoforge", "NeoForgePrimaMateria"), ("forge", "ForgePrimaMateria")):
        src = ROOT / f"{loader}-26.2" / "src/main/java" / PKG / loader / (name + ".java")
        dst = ROOT / f"{loader}-26.3" / "src/main/java" / PKG / loader / (name + ".java")
        dst.write_text(src.read_text(encoding="utf-8"), encoding="utf-8", newline=NL)


def main():
    copy_tree("main")
    copy_tree("dev")
    copy_loader_entries()
    for patch in PATCHES:
        patch()
    print(f"version-26.3 synced from version-26.2 ({len(PATCHES)} patches)")


if __name__ == "__main__":
    main()
