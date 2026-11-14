package com.itmecasey.primamateria.world;

import com.itmecasey.primamateria.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.storage.LevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places the start island when a world is first loaded and sets the world spawn on it. One island at the world spawn, shared by
 * every player. The loader entry classes call {@link #onServerStarted}; everything else here is also used by the self test.
 */
public final class StartIslandPlacer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");

    /** The center of the island's top layer, in the Overworld. */
    public static final BlockPos CENTER = new BlockPos(0, IslandPlan.TOP_Y, 0);

    private StartIslandPlacer() {
    }

    /** Called once per server start with the loaded server. Never throws. */
    public static void onServerStarted(MinecraftServer server) {
        try {
            ServerLevel overworld = server.overworld();
            IslandData data = overworld.getDataStorage().computeIfAbsent(IslandData.TYPE);
            StartMode mode = StartMode.parse(Settings.getString("start.island"));
            apply(overworld, mode, usesVoidPreset(overworld), data, CENTER, true);
        } catch (RuntimeException e) {
            LOGGER.error("Could not place the start island: {}", e.toString());
        }
    }

    /**
     * True when the Overworld was made with the Void Island preset: a flat generator with no layers at all. The vanilla flat
     * presets always have at least one layer, so this does not catch them.
     */
    public static boolean usesVoidPreset(ServerLevel overworld) {
        if (overworld.getChunkSource().getGenerator() instanceof FlatLevelSource flat) {
            FlatLevelGeneratorSettings settings = flat.settings();
            return settings.getLayersInfo().isEmpty();
        }
        return false;
    }

    /**
     * Places the island when {@code mode} says so, records it in {@code data} and optionally moves the world spawn onto it.
     * Returns whether it was placed.
     */
    public static boolean apply(ServerLevel level, StartMode mode, boolean worldUsesVoidPreset, IslandData data, BlockPos center,
                                boolean setSpawn) {
        if (!mode.shouldPlace(worldUsesVoidPreset, data.placed())) {
            return false;
        }
        place(level, center);
        data.markPlaced();
        if (setSpawn) {
            int[] o = IslandPlan.spawnOffset();
            BlockPos spawn = center.offset(o[0], o[1], o[2]);
            level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, spawn, 0.0F, 0.0F));
        }
        LOGGER.info("Placed the start island at {}", center.toShortString());
        return true;
    }

    /** The structure file of the island. A datapack replaces it by shipping a file at the same path. */
    public static final String STRUCTURE = "start_island";

    /**
     * Builds the island and its tree around {@code center} (the middle of the grass layer). The structure file
     * {@code data/primamateria/structure/start_island.nbt} is used when the game can load it, with its lowest corner at
     * {@code center + (-3, -2, -3)}; otherwise the code plan builds the same island.
     */
    public static void place(ServerLevel level, BlockPos center) {
        try {
            var template = level.getStructureTemplateManager().get(com.itmecasey.primamateria.ModContent.id(STRUCTURE));
            if (template.isPresent()) {
                BlockPos origin = center.offset(-IslandPlan.RADIUS, -2, -IslandPlan.RADIUS);
                var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
                if (template.get().placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_ALL)) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("The start island structure file could not be used, building the island from code: {}", e.toString());
        }
        placeFromCode(level, center);
    }

    /** The code plan: the same island as the structure file. */
    public static void placeFromCode(ServerLevel level, BlockPos center) {
        for (IslandPlan.Part p : IslandPlan.parts()) {
            level.setBlock(center.offset(p.dx(), p.dy(), p.dz()), stateOf(p.kind()), Block.UPDATE_ALL);
        }
    }

    private static BlockState stateOf(IslandPlan.Kind kind) {
        return switch (kind) {
            case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
            case DIRT -> Blocks.DIRT.defaultBlockState();
            case LOG -> Blocks.OAK_LOG.defaultBlockState();
            case LEAVES -> Blocks.OAK_LEAVES.defaultBlockState();
        };
    }
}
