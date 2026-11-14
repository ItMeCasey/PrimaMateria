package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.world.IslandData;
import com.itmecasey.primamateria.world.IslandPlan;
import com.itmecasey.primamateria.world.StartIslandPlacer;
import com.itmecasey.primamateria.world.StartMode;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.storage.LevelData;

/**
 * Server checks of the void island start: the preset, the placement function in every mode, the saved flag, the spawn, and (only
 * when the server was started with the Void Island preset, which scripts/server_check.py does in its second pass) the island in
 * a freshly generated void world.
 */
final class WorldSelfTest {
    private WorldSelfTest() {
    }

    static void run(MinecraftServer server, ServerLevel level) {
        ResourceKey<WorldPreset> key = ResourceKey.create(Registries.WORLD_PRESET, ModContent.id("void_island"));
        Optional<Holder.Reference<WorldPreset>> preset = server.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).get(key);
        boolean voidOverworld = preset.isPresent() && preset.get().value().overworld()
                .map(stem -> stem.generator() instanceof FlatLevelSource flat && flat.settings().getLayersInfo().isEmpty()).orElse(false);
        SelfTest.verdict("void_preset_registered_with_an_empty_flat_overworld", voidOverworld, preset.isPresent() ? "found" : "missing");
        SelfTest.verdict("nether_and_end_exist", server.getLevel(Level.NETHER) != null && server.getLevel(Level.END) != null, "both loaded");

        boolean voidWorld = System.getenv("PRIMAMATERIA_VOID_WORLD") != null;
        SelfTest.verdict("world_kind_matches_the_start_mode", StartIslandPlacer.usesVoidPreset(level) == voidWorld,
                voidWorld ? "void preset world" : "flat test world");

        BlockPos c = SelfTest.area(level, 8);
        // the test world is kept between runs: clear the three test spots (and the margin around them) first
        for (BlockPos spot : new BlockPos[] {c, c.offset(20, 0, 0), c.offset(0, 0, 20)}) {
            for (int dx = -4; dx <= 6; dx++) {
                for (int dz = -4; dz <= 6; dz++) {
                    for (int dy = -4; dy <= 9; dy++) {
                        level.setBlockAndUpdate(spot.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        LevelData.RespawnData spawnBefore = level.getRespawnData();
        IslandData fresh = new IslandData();
        SelfTest.verdict("never_places_nothing", !StartIslandPlacer.apply(level, StartMode.NEVER, true, fresh, c, true)
                && level.getBlockState(c).isAir() && !fresh.placed(), "never");
        SelfTest.verdict("preset_only_skips_a_world_without_the_preset", !StartIslandPlacer.apply(level, StartMode.PRESET_ONLY, false, fresh, c, true)
                && level.getBlockState(c).isAir(), "preset_only, other world");
        boolean placed = StartIslandPlacer.apply(level, StartMode.ALWAYS, false, fresh, c, false);
        SelfTest.verdict("always_places_the_island", placed && fresh.placed(), "placed " + placed);
        {
            String where = "none";
            for (int dx = -12; dx <= 12 && where.equals("none"); dx++) {
                for (int dy = -12; dy <= 12 && where.equals("none"); dy++) {
                    for (int dz = -12; dz <= 12; dz++) {
                        if (level.getBlockState(c.offset(dx, dy, dz)).is(Blocks.GRASS_BLOCK)) {
                            where = dx + "," + dy + "," + dz;
                            break;
                        }
                    }
                }
            }
            SelfTest.verdict("island_first_grass_probe", true, "first grass at offset " + where);
        }
        SelfTest.verdict("island_layers", level.getBlockState(c).is(Blocks.GRASS_BLOCK) && level.getBlockState(c.offset(3, 0, -3)).is(Blocks.GRASS_BLOCK)
                && level.getBlockState(c.below()).is(Blocks.DIRT) && level.getBlockState(c.below(2)).is(Blocks.DIRT)
                && level.getBlockState(c.below(3)).isAir() && level.getBlockState(c.offset(4, 0, 0)).isAir()
                && level.getBlockState(c.above()).isAir(), "7 by 7, grass over two dirt");
        SelfTest.verdict("island_tree", level.getBlockState(c.offset(2, 1, 2)).is(Blocks.OAK_LOG) && level.getBlockState(c.offset(2, 4, 2)).is(Blocks.OAK_LOG)
                && level.getBlockState(c.offset(2, 5, 2)).is(Blocks.OAK_LEAVES) && level.getBlockState(c.offset(2, 6, 2)).is(Blocks.OAK_LEAVES),
                "oak, four logs and a crown");
        int solid = 0;
        for (IslandPlan.Part p : IslandPlan.parts()) {
            if (!level.getBlockState(c.offset(p.dx(), p.dy(), p.dz())).isAir()) {
                solid++;
            }
        }
        SelfTest.verdict("every_planned_block_is_there", solid == IslandPlan.parts().size(), solid + " of " + IslandPlan.parts().size());
        SelfTest.verdict("island_structure_file_loads", level.getStructureManager().get(com.itmecasey.primamateria.ModContent.id(StartIslandPlacer.STRUCTURE)).isPresent(),
                "data/primamateria/structure/start_island.nbt");
        SelfTest.verdict("not_placed_twice", !StartIslandPlacer.apply(level, StartMode.ALWAYS, true, fresh, c.offset(20, 0, 0), true)
                && level.getBlockState(c.offset(20, 0, 0)).isAir(), "flag set");
        SelfTest.verdict("spawn_unchanged_when_not_asked", level.getRespawnData().equals(spawnBefore), "spawn kept");

        IslandData other = new IslandData();
        BlockPos c2 = c.offset(0, 0, 20);
        StartIslandPlacer.apply(level, StartMode.ALWAYS, true, other, c2, true);
        BlockPos expected = c2.above();
        SelfTest.verdict("spawn_set_above_the_center", level.getRespawnData().pos().equals(expected), level.getRespawnData().pos().toShortString());
        level.setRespawnData(spawnBefore);

        if (voidWorld) {
            IslandData saved = level.getDataStorage().computeIfAbsent(IslandData.TYPE);
            BlockPos center = StartIslandPlacer.CENTER;
            SelfTest.verdict("void_world_island_at_spawn", saved.placed() && level.getBlockState(center).is(Blocks.GRASS_BLOCK)
                    && level.getBlockState(center.below(2)).is(Blocks.DIRT) && level.getBlockState(center.offset(2, 1, 2)).is(Blocks.OAK_LOG)
                    && level.getBlockState(center.below(3)).isAir(), "island at " + center.toShortString());
            SelfTest.verdict("void_world_spawn_on_the_island", level.getRespawnData().pos().equals(center.above()),
                    level.getRespawnData().pos().toShortString());
            SelfTest.verdict("void_world_has_nothing_else", level.getBlockState(center.offset(10, 0, 0)).isAir()
                    && level.getBlockState(center.offset(0, -1, 10)).isAir(), "only the island");
        }
    }
}
