package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.blockentity.CastingTrayBlockEntity;
import com.itmecasey.primamateria.blockentity.CrucibleBlockEntity;
import com.itmecasey.primamateria.crucible.CrucibleState;
import com.itmecasey.primamateria.data.HeatSources;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.machine.CrucibleLogic;
import com.itmecasey.primamateria.recipe.CrucibleRecipes;
import com.itmecasey.primamateria.view.TrayView;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Server checks of the crucible and the casting tray: recipes, heat, melting, buckets, hoppers, trays and events. */
final class CrucibleSelfTest {
    private CrucibleSelfTest() {
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state) {
        // flag 2 sends the change without neighbor updates, so a torch or soul fire with nothing to stand on is not removed
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos, state, 2);
    }

    private static CrucibleBlockEntity crucible(ServerLevel level, BlockPos pos, Block heat) {
        put(level, pos.below(), heat.defaultBlockState());
        put(level, pos, ModContent.block(Ids.CRUCIBLE).defaultBlockState());
        return (CrucibleBlockEntity) level.getBlockEntity(pos);
    }

    private static CastingTrayBlockEntity tray(ServerLevel level, BlockPos pos) {
        put(level, pos, ModContent.block(Ids.CASTING_TRAY).defaultBlockState());
        return (CastingTrayBlockEntity) level.getBlockEntity(pos);
    }

    private static void melt(ServerLevel level, CrucibleBlockEntity be, int maxTicks) {
        for (int i = 0; i < maxTicks && be.state().solidCount() > 0; i++) {
            be.tick(level);
        }
    }

    private static void cool(ServerLevel level, CastingTrayBlockEntity be, int ticks) {
        for (int i = 0; i < ticks; i++) {
            be.tick(level);
        }
    }

    static void run(MinecraftServer server, ServerLevel level) {
        ServerPlayer player = SelfTest.testPlayer(server, level, "crucibletest");
        BlockPos base = SelfTest.area(level, 3);
        var recipes = level.recipeAccess();

        // ---- recipes and heat sources are loaded ----
        CrucibleState.Melt stone = CrucibleRecipes.meltFor(recipes, new ItemStack(Items.COBBLESTONE));
        CrucibleState.Melt copper = CrucibleRecipes.meltFor(recipes, new ItemStack(ModContent.item("copper_dust")));
        CrucibleRecipes.Cast iron = CrucibleRecipes.castFor(recipes, "primamateria:molten_iron");
        SelfTest.verdict("melting_recipes_loaded", stone != null && stone.fluid().equals("minecraft:lava") && stone.amount() == 200
                && copper != null && copper.fluid().equals("primamateria:molten_copper") && copper.amount() == 20
                && CrucibleRecipes.meltFor(recipes, new ItemStack(Items.STICK)) == null, "stone and copper dust");
        SelfTest.verdict("casting_recipes_loaded", iron != null && iron.result().equals("minecraft:iron_ingot") && iron.amount() == 100
                && iron.time() == 60 && CrucibleRecipes.castFor(recipes, "minecraft:lava") == null, "iron");
        SelfTest.verdict("heat_sources_loaded", HeatSources.all().size() == 8, HeatSources.all().size() + " rules");

        BlockPos spot = base;
        int[] expected = {1, 2, 3, 3, 4, 5};
        Block[] sources = {Blocks.TORCH, Blocks.CAMPFIRE, Blocks.MAGMA_BLOCK, Blocks.FIRE, Blocks.LAVA, Blocks.SOUL_FIRE};
        for (int i = 0; i < sources.length; i++) {
            // fire is removed by the game when nothing supports it; soul fire needs soul sand
            put(level, spot.offset(i, -2, 8), sources[i] == Blocks.SOUL_FIRE ? Blocks.SOUL_SAND.defaultBlockState() : Blocks.STONE.defaultBlockState());
            put(level, spot.offset(i, -1, 8), sources[i].defaultBlockState());
            int heat = HeatSources.heatAt(level, spot.offset(i, -1, 8));
            SelfTest.verdict("heat_of_" + sources[i].getDescriptionId().replace("block.minecraft.", ""), heat == expected[i], "heat " + heat);
        }
        put(level, spot.offset(0, -1, 10), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        put(level, spot.offset(1, -1, 10), Blocks.STONE.defaultBlockState());
        SelfTest.verdict("no_heat_from_an_unlit_campfire_or_stone", HeatSources.heatAt(level, spot.offset(0, -1, 10)) == 0
                && HeatSources.heatAt(level, spot.offset(1, -1, 10)) == 0, "unlit campfire and stone give 0");

        // ---- melting stone to lava over magma, fired by hand ----
        List<GameEvents.Melted> melted = new ArrayList<>();
        GameEvents.MELTED.clear();
        GameEvents.MELTED.listen(melted::add);
        BlockPos pos = base;
        CrucibleBlockEntity be = crucible(level, pos, Blocks.MAGMA_BLOCK);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 3);
        SelfTest.verdict("solid_goes_in_by_hand", CrucibleLogic.useCrucible(level, pos, player, InteractionHand.MAIN_HAND, cobble).consumesAction()
                && cobble.getCount() == 2 && be.state().solidCount() == 1, "stack 3 to " + cobble.getCount());
        be.tick(level);
        SelfTest.verdict("heat_is_read_from_the_block_below", be.heat() == 3, "heat " + be.heat());
        melt(level, be, 400);
        SelfTest.verdict("stone_melts_to_lava", be.state().amount() == 200 && be.state().fluid().equals("minecraft:lava") && be.state().solidCount() == 0,
                be.state().amount() + " mB " + be.state().fluid());
        SelfTest.verdict("melted_event_fired", melted.size() == 1 && melted.get(0).fluid().equals("minecraft:lava") && melted.get(0).amount() == 200
                && melted.get(0).input().equals("minecraft:cobblestone"), melted.size() + " events");
        SelfTest.verdict("view_shows_the_contents", be.view().amount() == 200 && be.view().contentId().equals("minecraft:lava") && be.view().glow() == 15
                && be.view().heat() == 3, "view");

        // ---- a different fluid cannot melt into lava ----
        ItemStack dust = new ItemStack(ModContent.item("copper_dust"), 1);
        CrucibleLogic.useCrucible(level, pos, player, InteractionHand.MAIN_HAND, dust);
        melt(level, be, 100);
        SelfTest.verdict("one_fluid_kind_at_a_time", be.state().amount() == 200 && be.state().solidCount() == 1, "copper dust waits");
        SelfTest.verdict("empty_hand_takes_a_solid_back", tryTake(level, pos, player) && be.state().solidCount() == 0, "dust back out");

        // ---- no heat, no melting ----
        BlockPos cold = base.offset(4, 0, 0);
        CrucibleBlockEntity coldBe = crucible(level, cold, Blocks.STONE);
        coldBe.state().addSolid("minecraft:cobblestone");
        melt(level, coldBe, 100);
        SelfTest.verdict("no_heat_no_melting", coldBe.state().amount() == 0 && coldBe.state().solidCount() == 1, "stone stays solid");

        // ---- metal dust and a stack of dust ----
        BlockPos metal = base.offset(8, 0, 0);
        CrucibleBlockEntity metalBe = crucible(level, metal, Blocks.MAGMA_BLOCK);
        ItemStack three = new ItemStack(ModContent.item("copper_dust"), 3);
        for (int i = 0; i < 3; i++) {
            CrucibleLogic.useCrucible(level, metal, player, InteractionHand.MAIN_HAND, three);
        }
        melt(level, metalBe, 200);
        SelfTest.verdict("metal_dust_melts", metalBe.state().amount() == 60 && metalBe.state().fluid().equals("primamateria:molten_copper")
                && three.isEmpty(), metalBe.state().amount() + " mB " + metalBe.state().fluid());

        // ---- buckets ----
        BlockPos bucketPos = base.offset(12, 0, 0);
        CrucibleBlockEntity bucketBe = crucible(level, bucketPos, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        InteractionResult water = CrucibleLogic.useCrucible(level, bucketPos, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("water_does_not_go_in", water == InteractionResult.PASS && bucketBe.state().amount() == 0
                && player.getMainHandItem().is(Items.WATER_BUCKET), "water bucket kept");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        CrucibleLogic.useCrucible(level, bucketPos, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("lava_bucket_fills_the_crucible", bucketBe.state().amount() == 1000 && bucketBe.state().fluid().equals("minecraft:lava")
                && player.getMainHandItem().is(Items.BUCKET), "1000 mB, bucket empty");
        CrucibleLogic.useCrucible(level, bucketPos, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("empty_bucket_takes_1000_mb_out", bucketBe.state().amount() == 0 && player.getMainHandItem().is(Items.LAVA_BUCKET),
                "back to a lava bucket");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.item("molten_iron_bucket")));
        CrucibleLogic.useCrucible(level, bucketPos, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("molten_bucket_fills_the_crucible", bucketBe.state().fluid().equals("primamateria:molten_iron") && bucketBe.state().amount() == 1000,
                bucketBe.state().fluid());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        CrucibleLogic.useCrucible(level, bucketPos, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("a_second_fluid_kind_is_refused", bucketBe.state().fluid().equals("primamateria:molten_iron") && bucketBe.state().amount() == 1000
                && player.getMainHandItem().is(Items.LAVA_BUCKET), "lava stays in the bucket");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // ---- hoppers put solids in ----
        BlockPos hopperPos = base.offset(16, 0, 0);
        CrucibleBlockEntity hopperBe = crucible(level, hopperPos, Blocks.STONE);
        boolean first = HopperBlockEntity.addItem(null, hopperBe, new ItemStack(Items.COBBLESTONE), Direction.UP).isEmpty();
        SelfTest.verdict("hopper_can_insert_a_solid", first && hopperBe.state().solidCount() == 1, "1 solid");
        SelfTest.verdict("hopper_cannot_insert_what_does_not_melt", !hopperBe.canPlaceItem(0, new ItemStack(Items.STICK)), "stick");
        for (int i = 0; i < 3; i++) {
            HopperBlockEntity.addItem(null, hopperBe, new ItemStack(Items.COBBLESTONE), Direction.UP);
        }
        ItemStack fifth = HopperBlockEntity.addItem(null, hopperBe, new ItemStack(Items.COBBLESTONE), Direction.UP);
        SelfTest.verdict("buffer_holds_four_solids", hopperBe.state().solidCount() == 4 && !fifth.isEmpty(), hopperBe.state().solidCount() + " solids");

        // ---- a crucible pours into trays against its sides, they cool into ingots ----
        List<GameEvents.Cast> casts = new ArrayList<>();
        GameEvents.CAST.clear();
        GameEvents.CAST.listen(casts::add);
        BlockPos pourPos = base.offset(20, 0, 0);
        CrucibleBlockEntity pourBe = crucible(level, pourPos, Blocks.MAGMA_BLOCK);
        pourBe.state().fill("primamateria:molten_iron", 250);
        CastingTrayBlockEntity east = tray(level, pourPos.east());
        CastingTrayBlockEntity west = tray(level, pourPos.west());
        CastingTrayBlockEntity north = tray(level, pourPos.north());
        pourBe.tick(level);
        SelfTest.verdict("crucible_pours_into_trays_while_it_has_enough", pourBe.state().amount() == 50 && !east.state().canAccept()
                && !north.state().canAccept() && west.state().canAccept(), pourBe.state().amount() + " mB left, 2 trays poured");
        SelfTest.verdict("view_shows_the_pour", pourBe.view().pourSides() != 0 && east.view().stage() == TrayView.Stage.FILLING
                && east.view().fluidId().equals("primamateria:molten_iron"), "pour bits " + pourBe.view().pourSides());
        cool(level, east, 69);
        SelfTest.verdict("tray_is_still_cooling", east.state().stage() == TrayView.Stage.COOLING && casts.isEmpty(), "stage " + east.state().stage());
        cool(level, east, 1);
        cool(level, north, 70);
        SelfTest.verdict("tray_cools_to_an_ingot", east.state().done() && north.state().done() && east.getItem(0).is(Items.IRON_INGOT)
                && east.getItem(0).getCount() == 1, "stage " + east.state().stage());
        SelfTest.verdict("cast_event_fired", casts.size() == 2 && casts.get(0).result().equals("minecraft:iron_ingot")
                && casts.get(0).fluid().equals("primamateria:molten_iron") && casts.get(0).count() == 1, casts.size() + " events");
        SelfTest.verdict("a_full_tray_takes_no_more", !east.state().canAccept(), "done tray is not poured into");
        SelfTest.verdict("hopper_can_take_the_cast_out", east.canTakeItemThroughFace(0, ItemStack.EMPTY, Direction.DOWN)
                && east.removeItem(0, 1).is(Items.IRON_INGOT) && east.state().canAccept(), "tray empty again");
        SelfTest.verdict("tray_does_not_accept_items", !north.canPlaceItem(0, new ItemStack(Items.IRON_INGOT)), "no input");
        try {
            CrucibleLogic.useTray(level, pourPos.north(), player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } catch (NullPointerException e) {
            // the test player has no network connection, so handing the ingot to its inventory cannot sync; the tray is already empty
        }
        SelfTest.verdict("empty_hand_takes_the_cast", north.state().canAccept(), "tray empty again");

        pourBe.tick(level);
        SelfTest.verdict("not_enough_fluid_no_pour", pourBe.state().amount() == 50 && east.state().canAccept() && west.state().canAccept(),
                "50 mB is below the 100 mB of a cast");

        // ---- a hot bucket on a tray casts a bucket's worth ----
        BlockPos trayPos = base.offset(24, 0, 0);
        CastingTrayBlockEntity bucketTray = tray(level, trayPos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        CrucibleLogic.useTray(level, trayPos, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("lava_does_not_cast", bucketTray.state().canAccept() && player.getMainHandItem().is(Items.LAVA_BUCKET), "no recipe for lava");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.item("molten_gold_bucket")));
        CrucibleLogic.useTray(level, trayPos, player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("molten_bucket_fills_a_tray", bucketTray.state().stage() == TrayView.Stage.FILLING && bucketTray.state().count() == 10
                && player.getMainHandItem().is(Items.BUCKET), "10 ingots, bucket empty");
        cool(level, bucketTray, 70);
        SelfTest.verdict("a_bucket_casts_ten_ingots", bucketTray.state().done() && bucketTray.getItem(0).is(Items.GOLD_INGOT)
                && bucketTray.getItem(0).getCount() == 10, "count " + bucketTray.getItem(0).getCount());
        ItemStack one = bucketTray.removeItem(0, 1);
        SelfTest.verdict("hopper_takes_one_at_a_time", one.getCount() == 1 && bucketTray.state().count() == 9 && bucketTray.state().done(),
                "9 left");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        GameEvents.MELTED.clear();
        GameEvents.CAST.clear();
    }

    /** Empty hand on the crucible: the last solid comes back. The fake player cannot take items, so the state is what counts. */
    private static boolean tryTake(ServerLevel level, BlockPos pos, ServerPlayer player) {
        try {
            return CrucibleLogic.useCrucible(level, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY).consumesAction();
        } catch (NullPointerException e) {
            return true;
        }
    }
}
