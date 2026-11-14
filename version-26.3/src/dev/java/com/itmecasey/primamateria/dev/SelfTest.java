package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Dev;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.blockentity.SieveBlockEntity;
import com.itmecasey.primamateria.data.Meshes;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.sieve.SieveLogic;
import com.itmecasey.primamateria.sift.SiftMath;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
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

    /** Checks only a loader can do (its own capability system); each loader's dev class sets this before the server starts. */
    public static java.util.function.Consumer<MinecraftServer> loaderChecks = server -> {
    };

    /** A server player for tests (it has no network connection, so anything that sends it packets must be avoided). */
    public static ServerPlayer testPlayer(MinecraftServer server, ServerLevel level, String name) {
        return new ServerPlayer(server, level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
    }

    /** A clear spot in the test world far from spawn, per feature: {@code slot} 0 to 9 gives separate areas. */
    public static BlockPos area(ServerLevel level, int slot) {
        BlockPos spawn = level.getRespawnData().pos();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ()) + 30;
        return new BlockPos(spawn.getX() + 40 + slot * 40, y, spawn.getZ());
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
            SieveSelfTest.run(server, server.overworld());
            BarrelSelfTest.run(server, server.overworld());
            HammerSelfTest.run(server, server.overworld());
            DriveSelfTest.run(server, server.overworld());
            loaderChecks.accept(server);
            CrucibleSelfTest.run(server, server.overworld());
            // each feature adds one line here: FeatureSelfTest.run(server, server.overworld());
            failures += IntegrationCheck.run(server);
            WorldSelfTest.run(server, server.overworld());
            AnimChecks.run(server.overworld(), SelfTest::verdict);
            LookupSelfTest.run(server, server.overworld());
            FluidSelfTest.run(server, server.overworld());
            LookupSyncSelfTest.run(server);
        } catch (RuntimeException e) {
            LOGGER.error("SELFTEST error", e);
            verdict("selftest_ran_clean", false, e.toString());
        }
        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    /** Records one check: prints SELFTEST PASS or FAIL. Used by every feature test class. */
    public static void verdict(String name, boolean ok, String detail) {
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
