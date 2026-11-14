package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Dev;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check on a real server. Runs once at server start when {@code PRIMAMATERIA_SELFTEST} is set and prints
 * {@code SELFTEST PASS/FAIL} lines ending in {@code SELFTEST DONE failures=N}.
 *
 * <p>Scaffold state: checks that every id in {@link Ids} is registered and that every block can be placed. Workstream A adds one
 * method per mechanic.
 */
public final class SelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");
    private static int failures;

    private SelfTest() {
    }

    public static boolean requested() {
        return System.getenv("PRIMAMATERIA_SELFTEST") != null && Dev.ENABLED;
    }

    public static int run(MinecraftServer server) {
        failures = 0;
        try {
            registered();
            placeable(server.overworld());
            settings();
        } catch (RuntimeException e) {
            LOGGER.error("SELFTEST error", e);
            verdict("selftest_ran_clean", false, e.toString());
        }
        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    private static void verdict(String name, boolean ok, String detail) {
        if (ok) {
            LOGGER.info("SELFTEST PASS {} ({})", name, detail);
        } else {
            failures++;
            LOGGER.error("SELFTEST FAIL {} ({})", name, detail);
        }
    }

    private static void registered() {
        for (String name : Ids.BLOCKS) {
            Block block = BuiltInRegistries.BLOCK.getValue(ModContent.id(name));
            verdict("block_registered_" + name, block == ModContent.block(name), String.valueOf(BuiltInRegistries.BLOCK.getKey(block)));
            Item item = BuiltInRegistries.ITEM.getValue(ModContent.id(name));
            verdict("block_item_registered_" + name, item instanceof BlockItem bi && bi.getBlock() == block, String.valueOf(BuiltInRegistries.ITEM.getKey(item)));
        }
        for (String name : Ids.ITEMS) {
            Item item = BuiltInRegistries.ITEM.getValue(ModContent.id(name));
            verdict("item_registered_" + name, item == ModContent.item(name), String.valueOf(BuiltInRegistries.ITEM.getKey(item)));
        }
        for (String name : Ids.SOUNDS) {
            verdict("sound_registered_" + name, BuiltInRegistries.SOUND_EVENT.containsKey(ModContent.id(name)), name);
        }
        verdict("creative_tab_registered", BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(ModContent.id(Ids.CREATIVE_TAB)), Ids.CREATIVE_TAB);
        verdict("id_counts", ModContent.BLOCKS.size() == 12 && ModContent.ITEMS.size() == 12 + 29,
                ModContent.BLOCKS.size() + " blocks, " + ModContent.ITEMS.size() + " items, " + ModContent.SOUNDS.size() + " sounds");
    }

    private static void placeable(ServerLevel level) {
        BlockPos spawn = level.getRespawnData().pos();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ()) + 20;
        int i = 0;
        for (String name : Ids.BLOCKS) {
            BlockPos pos = new BlockPos(spawn.getX() + 2 * i++, y, spawn.getZ());
            level.setBlockAndUpdate(pos, ModContent.block(name).defaultBlockState());
            verdict("block_placed_" + name, level.getBlockState(pos).is(ModContent.block(name)), pos.toShortString());
        }
    }

    private static void settings() {
        verdict("settings_defaults", Settings.getInt("sieve.shakes_per_block") > 0 && Settings.getInt("sieve.link_radius") >= 1,
                Settings.keys().size() + " keys");
    }
}
