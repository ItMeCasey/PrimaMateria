"""Copies version-26.2/src/{main,dev}/java to version-26.3 and re-applies the 26.3 differences.

26.2 is the source of truth for the game-side code; version-26.3's Java is never edited by hand. Each known difference between
the two Minecraft versions is one patch function below (see BarelyCrafting/scripts/sync_versions.py for examples: block codecs,
Player.drop). Resources that differ (loot tables) are separate files per version and are not touched.

Run after every change to version-26.2:  python scripts/sync_versions.py
"""
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "version-26.2" / "src"
DST = ROOT / "version-26.3" / "src"
PKG = "com/itmecasey/primamateria"

NL = chr(10)


def patch_block_codec(name, cls):
    """26.3 blocks no longer declare a codec: drop the CODEC field (any constructor form) and the codec() override."""
    p = DST / "main/java" / PKG / "block" / (name + ".java")
    s = p.read_text(encoding="utf-8")
    s = s.replace("import com.mojang.serialization.MapCodec;" + NL, "")
    s = re.sub(r"    private static final MapCodec<" + cls + r"> CODEC = [^;]*;" + NL + "(?:" + NL + ")?", "", s)
    s = re.sub(r"    @Override" + NL + r"    protected MapCodec<\? extends \w+> codec\(\) \{" + NL + "        return CODEC;" + NL + "    }" + NL + NL, "", s)
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


def patch_structure_manager():
    """26.3 renamed the level's structure template manager accessor."""
    for rel in ("main/java" + "/" + PKG + "/world/StartIslandPlacer.java", "dev/java" + "/" + PKG + "/dev/WorldSelfTest.java"):
        p = DST / rel
        s = p.read_text(encoding="utf-8")
        assert "level.getStructureManager()" in s, rel
        p.write_text(s.replace("level.getStructureManager()", "level.getStructureTemplateManager()"), encoding="utf-8", newline=NL)


def patch_push_reaction():
    """26.3 renamed the push reaction DESTROY (a block that is broken when pushed, as liquids are) to POPPED."""
    p = DST / "main/java" / PKG / "fluid/ModFluids.java"
    s = p.read_text(encoding="utf-8")
    assert "PushReaction.DESTROY" in s
    s = s.replace("PushReaction.DESTROY", "PushReaction.POPPED")
    p.write_text(s, encoding="utf-8", newline=NL)


# Add patch functions here and list them in PATCHES. Each reads a file under DST, replaces text, asserts the old form is gone
# and writes it back with LF line endings.
PATCHES = [patch_drive_fuel, patch_structure_manager,
           lambda: patch_block_codec("SieveBlock", "SieveBlock"), lambda: patch_block_codec("BarrelBlock", "BarrelBlock"),
           lambda: patch_block_codec("AutoHammerBlock", "AutoHammerBlock"), lambda: patch_block_codec("DriveBlock", "DriveBlock"),
           lambda: patch_block_codec("SievePreviewBlock", "SievePreviewBlock"),
           lambda: patch_block_codec("CrucibleBlock", "CrucibleBlock"), lambda: patch_block_codec("CastingTrayBlock", "CastingTrayBlock"),
           lambda: patch_block_codec("MachinePreviewBlock", "MachinePreviewBlock"), patch_push_reaction]


def copy_tree(name):
    src = SRC / name / "java"
    dst = DST / name / "java"
    if dst.exists():
        shutil.rmtree(dst)
    if src.exists():
        shutil.copytree(src, dst)


def copy_dev_resources():
    src = SRC / "dev" / "resources"
    dst = DST / "dev" / "resources"
    if dst.exists():
        shutil.rmtree(dst)
    if src.exists():
        shutil.copytree(src, dst)


def copy_loader_entries():
    """The loader entry classes (registration, reload listener) and dev hooks are identical in 26.2 and 26.3: copy them too, so a
    registration added for 26.2 can never be missing in 26.3. The client classes and the integration plugins are not touched."""
    names = {
        "neoforge": ["main/java/com/itmecasey/primamateria/neoforge/NeoForgePrimaMateria.java",
                     "main/java/com/itmecasey/primamateria/neoforge/NeoForgeFluids.java",
                     "main/java/com/itmecasey/primamateria/neoforge/NeoForgeFluidClient.java",
                     "main/java/com/itmecasey/primamateria/neoforge/MachineFluidHandler.java",
                     "main/java/com/itmecasey/primamateria/neoforge/LookupPayload.java",
                     "main/java/com/itmecasey/primamateria/neoforge/NeoForgeItemFluids.java",
                     "dev/java/com/itmecasey/primamateria/neoforge/dev/NeoForgeDev.java"],
        "forge": ["main/java/com/itmecasey/primamateria/forge/ForgePrimaMateria.java",
                  "main/java/com/itmecasey/primamateria/forge/ForgeFluids.java",
                  "main/java/com/itmecasey/primamateria/forge/ForgeFluidClient.java",
                  "main/java/com/itmecasey/primamateria/forge/MachineFluidHandler.java",
                  "main/java/com/itmecasey/primamateria/forge/LookupMessage.java",
                  "main/java/com/itmecasey/primamateria/forge/ForgeItemFluids.java",
                  "dev/java/com/itmecasey/primamateria/forge/dev/ForgeDev.java"],
    }
    for loader, files in names.items():
        for rel in files:
            src = ROOT / f"{loader}-26.2" / "src" / rel
            dst = ROOT / f"{loader}-26.3" / "src" / rel
            if src.exists():
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(src, dst)


def main():
    copy_tree("main")
    copy_tree("dev")
    copy_dev_resources()
    copy_loader_entries()
    for patch in PATCHES:
        patch()
    print(f"version-26.3 synced from version-26.2 ({len(PATCHES)} patches)")


if __name__ == "__main__":
    main()
