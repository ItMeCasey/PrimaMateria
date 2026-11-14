package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.blockentity.AutoHammerBlockEntity;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.hammer.HammerLogic;
import com.itmecasey.primamateria.item.HammerItem;
import com.itmecasey.primamateria.recipe.CrushingRecipe;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;

/** Server checks of the hammers, the crushing recipes and the auto hammer. */
final class HammerSelfTest {
    private HammerSelfTest() {
    }

    private static int items(ServerLevel level, BlockPos pos, Item item) {
        int n = 0;
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2.5))) {
            if (e.getItem().is(item)) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    private static void clearItems(ServerLevel level, BlockPos pos) {
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4))) {
            e.discard();
        }
    }

    private static int count(Container c, Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(item)) {
                n += c.getItem(i).getCount();
            }
        }
        return n;
    }

    private static ItemStack hammer(String name) {
        return new ItemStack(ModContent.item(name));
    }

    static void run(MinecraftServer server, ServerLevel level) {
        // recipes and items
        long crushing = level.recipeAccess().getRecipes().stream().filter(h -> h.value() instanceof CrushingRecipe).count();
        SelfTest.verdict("crushing_recipes_loaded", crushing == 21, crushing + " recipes");
        PrimaRecipes.Crush cobble = PrimaRecipes.crushFor(level.recipeAccess(), new ItemStack(Items.COBBLESTONE));
        PrimaRecipes.Crush sand = PrimaRecipes.crushFor(level.recipeAccess(), new ItemStack(Items.SAND));
        PrimaRecipes.Crush nether = PrimaRecipes.crushFor(level.recipeAccess(), new ItemStack(Items.NETHERRACK));
        PrimaRecipes.Crush end = PrimaRecipes.crushFor(level.recipeAccess(), new ItemStack(Items.END_STONE));
        PrimaRecipes.Crush oak = PrimaRecipes.crushFor(level.recipeAccess(), new ItemStack(Items.OAK_LEAVES));
        PrimaRecipes.Crush anyLeaves = PrimaRecipes.crushFor(level.recipeAccess(), new ItemStack(Items.AZALEA_LEAVES));
        SelfTest.verdict("crushing_tiers_follow_the_design", cobble != null && cobble.hammerTier() == 1 && sand != null && sand.hammerTier() == 2
                && nether != null && nether.hammerTier() == 3 && end != null && end.hammerTier() == 4, "tiers 1, 2, 3, 4");
        SelfTest.verdict("leaves_give_fiber_and_their_sapling", oak != null && oak.results().size() == 3 && anyLeaves != null,
                oak == null ? "none" : oak.results().size() + " results");
        SelfTest.verdict("dirt_is_not_crushable", PrimaRecipes.crushFor(level.recipeAccess(), new ItemStack(Items.DIRT)) == null, "dirt");
        boolean tiers = true;
        for (int i = 0; i < Ids.HAMMERS.size(); i++) {
            ItemStack h = hammer(Ids.HAMMERS.get(i));
            tiers &= HammerItem.tierOf(h) == i + 1 && h.getMaxDamage() == Ids.HAMMER_DURABILITY.get(i) && h.getMaxStackSize() == 1;
        }
        SelfTest.verdict("six_hammers_have_their_tier_and_durability", tiers && HammerItem.tierOf(new ItemStack(Items.IRON_PICKAXE)) == 0, "tiers 1 to 6");

        ServerPlayer player = SelfTest.testPlayer(server, level, "hammertest");
        BlockPos base = SelfTest.area(level, 1);
        clearItems(level, base);

        // the tier gate and the crushed result
        BlockPos sandPos = base;
        level.setBlockAndUpdate(sandPos, Blocks.SAND.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer("wooden_hammer"));
        SelfTest.verdict("weak_hammer_is_refused", !HammerLogic.onBreak(player, level, sandPos, level.getBlockState(sandPos))
                && level.getBlockState(sandPos).is(Blocks.SAND), "wooden on sand");
        ItemStack stone = hammer("stone_hammer");
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        Settings.set("hammer.bonus_per_tier", 0.0);
        boolean crushed = HammerLogic.onBreak(player, level, sandPos, level.getBlockState(sandPos));
        SelfTest.verdict("stone_hammer_crushes_sand_to_stone_meal", crushed && level.getBlockState(sandPos).isAir()
                && items(level, sandPos, ModContent.item(Ids.STONE_MEAL)) == 1 && stone.getDamageValue() == 1,
                "stone meal " + items(level, sandPos, ModContent.item(Ids.STONE_MEAL)) + ", damage " + stone.getDamageValue());
        level.setBlockAndUpdate(sandPos, Blocks.NETHERRACK.defaultBlockState());
        SelfTest.verdict("stone_hammer_is_refused_on_netherrack_tier_three", !HammerLogic.onBreak(player, level, sandPos, level.getBlockState(sandPos))
                && level.getBlockState(sandPos).is(Blocks.NETHERRACK), "stone on netherrack");
        ItemStack copper = hammer("copper_hammer");
        player.setItemInHand(InteractionHand.MAIN_HAND, copper);
        boolean scoria = HammerLogic.onBreak(player, level, sandPos, level.getBlockState(sandPos));
        SelfTest.verdict("copper_hammer_crushes_netherrack_to_scoria", scoria && items(level, sandPos, ModContent.item(Ids.SCORIA)) == 1
                && copper.getDamageValue() == 1, "scoria " + items(level, sandPos, ModContent.item(Ids.SCORIA)));

        // plain items without a block do nothing, and a non-hammer never crushes
        level.setBlockAndUpdate(sandPos, Blocks.COBBLESTONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        SelfTest.verdict("pickaxe_does_not_crush", !HammerLogic.onBreak(player, level, sandPos, level.getBlockState(sandPos)), "pickaxe");

        // bonus yield and the crushed event
        clearItems(level, base);
        Settings.set("hammer.bonus_per_tier", 1.0);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer("netherite_hammer"));
        boolean[] byMill = {true};
        GameEvents.CRUSHED.clear();
        GameEvents.CRUSHED.listen(e -> {
            byMill[0] = e.byMill;
            e.drops.add(new GameEvents.Drop("minecraft:stick", 3));
        });
        HammerLogic.onBreak(player, level, sandPos, level.getBlockState(sandPos));
        GameEvents.CRUSHED.clear();
        SelfTest.verdict("netherite_bonus_gives_an_extra_gravel", items(level, sandPos, Items.GRAVEL) == 2, "gravel " + items(level, sandPos, Items.GRAVEL));
        SelfTest.verdict("crushed_event_can_add_drops_and_knows_it_is_by_hand", items(level, sandPos, Items.STICK) == 3 && !byMill[0],
                "sticks " + items(level, sandPos, Items.STICK));
        Settings.reset();
        clearItems(level, base);

        // the auto hammer
        BlockPos machine = base.offset(10, 0, 0);
        level.setBlockAndUpdate(machine.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(machine.below(), Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(machine, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(machine, ModContent.block(Ids.AUTO_HAMMER).defaultBlockState());
        AutoHammerBlockEntity be = (AutoHammerBlockEntity) level.getBlockEntity(machine);
        Container chest = HopperBlockEntity.getContainerAt(level, machine.below());
        boolean queuedByHopper = HopperBlockEntity.addItem(null, be, new ItemStack(Items.COBBLESTONE), Direction.UP).isEmpty()
                && be.state().queued() == 1;
        SelfTest.verdict("auto_hammer_takes_a_block_from_a_hopper", queuedByHopper, "queued " + be.state().queued());
        SelfTest.verdict("auto_hammer_refuses_uncrushable_blocks", !be.canPlaceItem(0, new ItemStack(Items.DIRT)), "dirt");
        SelfTest.verdict("auto_hammer_does_nothing_without_a_hammer", !be.hasWork(level) && !be.drive(level, 0) && be.state().strokes() == 0, "no hammer");
        ItemStack fitted = hammer("stone_hammer");
        Item hammerItem = fitted.getItem();
        be.swapHammer(fitted);
        SelfTest.verdict("auto_hammer_without_a_drive_does_not_move", be.state().strokes() == 0 && count(chest, Items.GRAVEL) == 0, "idle");
        int strokes = 0;
        while (be.hasWork(level) && strokes < 20) {
            be.drive(level, 0);
            strokes++;
        }
        SelfTest.verdict("auto_hammer_crushes_a_block_in_six_strokes", strokes == 6 && count(chest, Items.GRAVEL) == 1 && be.state().queued() == 0
                && be.hammer().getDamageValue() == 1 && be.hammer().getItem() == hammerItem,
                strokes + " strokes, gravel " + count(chest, Items.GRAVEL) + ", damage " + be.hammer().getDamageValue());
        SelfTest.verdict("auto_hammer_state_shows_moving", level.getBlockState(machine).getValue(StateProps.MOVING), "moving");

        // a hammer that is too weak for the queued block waits; a stronger one is accepted and works
        be.state().offer("minecraft:netherrack");
        SelfTest.verdict("auto_hammer_waits_for_a_strong_enough_hammer", !be.hasWork(level), "stone hammer on netherrack");
        be.swapHammer(hammer("copper_hammer"));
        Settings.set("hammer.bonus_per_tier", 0.0);
        int sandStrokes = 0;
        while (be.hasWork(level) && sandStrokes < 20) {
            be.drive(level, 0);
            sandStrokes++;
        }
        SelfTest.verdict("copper_hammer_in_the_machine_crushes_netherrack", sandStrokes == 6 && count(chest, ModContent.item(Ids.SCORIA)) == 1,
                "scoria " + count(chest, ModContent.item(Ids.SCORIA)));
        Settings.reset();

        // events from the machine, the queue limit, and a hammer that wears out
        boolean[] machineEvent = {false};
        GameEvents.CRUSHED.clear();
        GameEvents.CRUSHED.listen(e -> machineEvent[0] = e.byMill);
        be.state().offer("minecraft:gravel");
        while (be.hasWork(level)) {
            be.drive(level, 0);
        }
        GameEvents.CRUSHED.clear();
        SelfTest.verdict("machine_crush_event_says_by_mill", machineEvent[0] && count(chest, Items.SAND) == 1, "sand " + count(chest, Items.SAND));
        for (int i = 0; i < 16; i++) {
            be.state().offer("minecraft:cobblestone");
        }
        SelfTest.verdict("auto_hammer_queue_holds_sixteen", !be.canPlaceItem(0, new ItemStack(Items.COBBLESTONE)) && be.state().queued() == 16, "16");
        be.state().clear();
        be.state().offer("minecraft:cobblestone");
        ItemStack worn = hammer("copper_hammer");
        worn.setDamageValue(worn.getMaxDamage() - 1);
        be.swapHammer(worn);
        while (be.hasWork(level)) {
            be.drive(level, 0);
        }
        SelfTest.verdict("worn_out_hammer_breaks_in_the_machine", be.hammer().isEmpty(), "hammer gone");
    }
}
