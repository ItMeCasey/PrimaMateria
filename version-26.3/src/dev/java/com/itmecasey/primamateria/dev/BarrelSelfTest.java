package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.barrel.BarrelLogic;
import com.itmecasey.primamateria.barrel.BarrelState;
import com.itmecasey.primamateria.blockentity.BarrelBlockEntity;
import com.itmecasey.primamateria.data.CompostValues;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.recipe.BarrelRecipes;
import com.itmecasey.primamateria.sift.Drop;
import com.itmecasey.primamateria.view.BarrelView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Server checks of the barrels: recipes, buckets, compost, soaking, steeping, growing, mixing, hoppers and dew. */
final class BarrelSelfTest {
    private static final String WATER = "minecraft:water";
    private static final String LAVA = "minecraft:lava";
    private static final String BRINE = "primamateria:brine";

    private BarrelSelfTest() {
    }

    private static BarrelBlockEntity place(ServerLevel level, BlockPos pos, String id) {
        // the test world is kept between runs: clear the spot first so no old block entity data survives
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, ModContent.block(id).defaultBlockState());
        return (BarrelBlockEntity) level.getBlockEntity(pos);
    }

    private static ItemStack stack(String path, int n) {
        Item item = ModContent.item(path);
        return new ItemStack(item == null ? Items.AIR : item, n);
    }

    /** Lets every running timer finish and fires the finished-recipe events, like the barrel's own tick does. */
    private static void finish(ServerLevel level, BarrelBlockEntity be) {
        be.fire(level, be.state().tick(100000));
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

    static void run(MinecraftServer server, ServerLevel level) {
        Settings.reset();
        ServerPlayer player = SelfTest.testPlayer(server, level, "barreltest");
        BlockPos base = SelfTest.area(level, 2);

        // ---- the data is loaded ----
        var recipes = level.recipeAccess();
        SelfTest.verdict("barrel_recipes_loaded", BarrelRecipes.forItem(recipes, stack(Ids.STONE_MEAL, 1)).soaking().size() == 1
                && BarrelRecipes.forItem(recipes, new ItemStack(Items.DIRT)).soaking().size() == 1
                && BarrelRecipes.forItem(recipes, stack(Ids.SCORIA, 1)).soaking().size() == 1
                && BarrelRecipes.forItem(recipes, stack(Ids.SALT_DUST, 1)).steeping().size() == 1
                && BarrelRecipes.forItem(recipes, stack("diamond_seed", 1)).growing().size() == 1
                && BarrelRecipes.forItem(recipes, stack("emerald_seed", 1)).growing().size() == 1
                && BarrelRecipes.mixing(recipes).size() == 2 && BarrelRecipes.composting(recipes) != null,
                BarrelRecipes.mixing(recipes).size() + " mixing recipes");
        SelfTest.verdict("compost_values_loaded", CompostValues.count() == 15 && CompostValues.of(new ItemStack(Items.APPLE)).units() == 100
                && CompostValues.of(new ItemStack(Items.OAK_SAPLING)).units() == 100
                && CompostValues.of(new ItemStack(Items.BONE_MEAL)).boost() == 0.5 && CompostValues.of(new ItemStack(Items.STONE)) == null,
                CompostValues.count() + " values");

        // ---- buckets and the wooden barrel's limits ----
        BarrelBlockEntity wood = place(level, base, Ids.WOODEN_BARREL);
        SelfTest.verdict("wooden_barrel_refuses_lava", !BarrelLogic.pour(level, wood, LAVA) && wood.state().amount() == 0, "lava refused");
        SelfTest.verdict("bucket_fills_a_barrel", BarrelLogic.pour(level, wood, WATER) && wood.state().amount() == 1000
                && wood.view().contents() == BarrelView.Contents.FLUID && wood.view().contentId().equals(WATER), "1000 mB water");
        SelfTest.verdict("a_second_bucket_does_not_fit", !BarrelLogic.pour(level, wood, WATER), "full");
        SelfTest.verdict("lava_on_water_needs_a_clay_barrel", !BarrelLogic.pour(level, wood, LAVA) && wood.state().amount() == 1000,
                "wooden barrel kept its water");
        InteractionResult refused = InteractionResult.PASS;
        try {
            refused = BarrelLogic.interact(level, base, player, InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        } catch (NullPointerException e) {
            // the test player has no connection; a refused bucket never reaches the inventory code
        }
        SelfTest.verdict("a_refused_bucket_is_refused", refused == InteractionResult.FAIL, String.valueOf(refused));
        SelfTest.verdict("comparator_follows_the_fill", comparator(level, base) == 15, "full barrel 15");
        String scooped = BarrelLogic.scoop(level, wood);
        SelfTest.verdict("empty_bucket_takes_it_back", "minecraft:water_bucket".equals(scooped) && wood.state().amount() == 0, String.valueOf(scooped));

        // ---- composting ----
        wood = place(level, base, Ids.WOODEN_BARREL);
        Settings.set("barrel.time_multiplier", 0.01);
        int[] events = {0};
        GameEvents.BARREL_FINISHED.clear();
        GameEvents.BARREL_FINISHED.listen(e -> events[0]++);
        boolean allIn = true;
        for (int i = 0; i < 10; i++) {
            allIn &= BarrelLogic.offer(level, wood, new ItemStack(Items.APPLE));
        }
        SelfTest.verdict("ten_apples_fill_a_barrel_with_compost", allIn && wood.state().compost() == 1000 && wood.state().maturing()
                && wood.view().contents() == BarrelView.Contents.COMPOST, "compost " + wood.state().compost());
        SelfTest.verdict("compost_refuses_more_and_water", !BarrelLogic.offer(level, wood, new ItemStack(Items.APPLE))
                && !BarrelLogic.pour(level, wood, WATER), "full of compost");
        finish(level, wood);
        SelfTest.verdict("compost_becomes_dirt", wood.state().peekResult() != null && wood.state().peekResult().equals(new Drop("minecraft:dirt", 1))
                && events[0] == 1 && wood.view().stage() == BarrelView.Stage.DONE, "events " + events[0]);
        try {
            BarrelLogic.interact(level, base, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } catch (NullPointerException e) {
            // the result is taken out of the barrel before it is handed to the player, whose inventory needs a connection
        }
        SelfTest.verdict("an_empty_hand_takes_the_result", !wood.state().hasResult(), "result taken");

        // ---- bone meal brings a boost; the pure tests check the halving ----
        wood = place(level, base, Ids.WOODEN_BARREL);
        Settings.reset();
        for (int i = 0; i < 9; i++) {
            BarrelLogic.offer(level, wood, new ItemStack(Items.APPLE));
        }
        boolean boneMeal = BarrelLogic.offer(level, wood, new ItemStack(Items.BONE_MEAL));
        boolean lastApple = BarrelLogic.offer(level, wood, new ItemStack(Items.APPLE));
        SelfTest.verdict("bone_meal_adds_compost", boneMeal && lastApple && wood.state().compost() == 1000 && wood.state().maturing(),
                "compost " + wood.state().compost());
        Settings.set("barrel.time_multiplier", 0.01);

        // ---- soaking, with the result leaving through a hopper below ----
        BlockPos pos = base.offset(0, 0, 4);
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.HOPPER.defaultBlockState());
        wood = place(level, pos, Ids.WOODEN_BARREL);
        BarrelLogic.pour(level, wood, WATER);
        SelfTest.verdict("dirt_is_refused_by_a_dry_barrel", !BarrelLogic.accepts(level, place(level, base.offset(0, 0, 8), Ids.WOODEN_BARREL),
                new ItemStack(Items.DIRT)), "no water");
        SelfTest.verdict("hopper_offer_starts_a_soak", wood.canPlaceItem(0, stack(Ids.STONE_MEAL, 1)) && wood.canPlaceItemThroughFace(0, stack(Ids.STONE_MEAL, 1), Direction.UP)
                && !wood.canPlaceItemThroughFace(0, stack(Ids.STONE_MEAL, 1), Direction.DOWN)
                && HopperBlockEntity.addItem(null, wood, stack(Ids.STONE_MEAL, 1), Direction.UP).isEmpty()
                && wood.state().stage() == BarrelView.Stage.SOAKING && wood.state().amount() == 750, "soaking " + wood.state().amount());
        SelfTest.verdict("a_soaking_barrel_refuses_buckets_and_a_second_soak", wood.state().soakItem().equals("primamateria:stone_meal")
                && !wood.canPlaceItem(0, stack(Ids.STONE_MEAL, 1)) && BarrelLogic.scoop(level, wood) == null, "busy");
        finish(level, wood);
        HopperBlockEntity hopper = (HopperBlockEntity) level.getBlockEntity(pos.below());
        boolean sucked = HopperBlockEntity.suckInItems(level, hopper);
        SelfTest.verdict("a_hopper_below_takes_the_clay", sucked && count(hopper, Items.CLAY) == 1 && !wood.state().hasResult(),
                "clay in hopper " + count(hopper, Items.CLAY));

        // ---- mud and soul sand ----
        wood = place(level, base.offset(0, 0, 8), Ids.WOODEN_BARREL);
        BarrelLogic.pour(level, wood, WATER);
        BarrelLogic.offer(level, wood, new ItemStack(Items.DIRT));
        finish(level, wood);
        SelfTest.verdict("dirt_soaks_into_mud", new Drop("minecraft:mud", 1).equals(wood.state().takeResult()), "mud");

        // ---- steeping salt dust into brine, then brine does soul sand ----
        wood = place(level, base.offset(0, 0, 12), Ids.WOODEN_BARREL);
        BarrelLogic.pour(level, wood, WATER);
        boolean four = true;
        for (int i = 0; i < 4; i++) {
            four &= HopperBlockEntity.addItem(null, wood, stack(Ids.SALT_DUST, 1), Direction.UP).isEmpty();
        }
        SelfTest.verdict("four_salt_dust_start_the_steep", four && wood.state().stage() == BarrelView.Stage.STEEPING
                && !BarrelLogic.offer(level, wood, stack(Ids.SALT_DUST, 1)), "steeping");
        finish(level, wood);
        SelfTest.verdict("water_steeps_into_brine", wood.state().fluid().equals(BRINE) && wood.state().amount() == 1000, wood.state().fluid());
        BarrelLogic.offer(level, wood, stack(Ids.SCORIA, 1));
        finish(level, wood);
        SelfTest.verdict("scoria_soaks_into_soul_sand_in_brine", new Drop("minecraft:soul_sand", 1).equals(wood.state().takeResult())
                && wood.state().amount() == 750, "soul sand, brine left " + wood.state().amount());

        // ---- growing crystals, four at a time ----
        boolean planted = true;
        planted &= BarrelLogic.offer(level, wood, stack("quartz_seed", 1));
        planted &= BarrelLogic.offer(level, wood, stack("amethyst_seed", 1));
        planted &= BarrelLogic.offer(level, wood, stack("lapis_seed", 1));
        SelfTest.verdict("three_seeds_grow_at_once", planted && wood.state().crystalCount() == 3 && wood.state().amount() == 0
                && wood.view().stage() == BarrelView.Stage.GROWING && wood.view().crystals().size() == 3
                && wood.view().crystals().get(0).texture().equals("primamateria:block/crystal_quartz"), "crystals " + wood.state().crystalCount());
        SelfTest.verdict("no_seed_without_brine", !BarrelLogic.offer(level, wood, stack("diamond_seed", 1)), "brine used up");
        events[0] = 0;
        finish(level, wood);
        Drop first = wood.state().takeResult();
        Drop second = wood.state().takeResult();
        Drop third = wood.state().takeResult();
        SelfTest.verdict("crystals_give_their_gems", events[0] == 3 && first != null && second != null && third != null
                && first.count() + second.count() + third.count() == 7
                && first.item().equals("minecraft:quartz") && second.item().equals("minecraft:amethyst_shard") && third.item().equals("minecraft:lapis_lazuli"),
                "events " + events[0]);

        // ---- mixing in the clay barrel ----
        BarrelBlockEntity clay = place(level, base.offset(0, 0, 16), Ids.CLAY_BARREL);
        SelfTest.verdict("clay_barrel_takes_lava", BarrelLogic.pour(level, clay, LAVA) && clay.state().fluid().equals(LAVA)
                && clay.view().heatproof(), "lava");
        SelfTest.verdict("lava_then_water_makes_obsidian", BarrelLogic.pour(level, clay, WATER) && clay.state().amount() == 0
                && new Drop("minecraft:obsidian", 1).equals(clay.state().peekResult()), "obsidian");
        clay.state().takeResult();
        BarrelLogic.pour(level, clay, WATER);
        SelfTest.verdict("water_then_lava_makes_stone", BarrelLogic.pour(level, clay, LAVA)
                && new Drop("minecraft:stone", 1).equals(clay.state().peekResult()), "stone");
        clay.state().takeResult();
        BarrelLogic.pour(level, clay, WATER);
        SelfTest.verdict("brine_does_not_mix_with_water", !BarrelLogic.pour(level, clay, BRINE) && clay.state().amount() == 1000, "refused");

        // ---- dew ----
        Settings.reset();
        BarrelBlockEntity dewy = place(level, base.offset(0, 0, 20), Ids.WOODEN_BARREL);
        int added = dewy.dewTick(level, 40);
        SelfTest.verdict("open_sky_collects_dew", added == 10 && dewy.state().amount() == 10 && dewy.state().fluid().equals(WATER), "added " + added);
        Settings.set("barrel.dew_ticks_per_mb", 0);
        SelfTest.verdict("dew_can_be_switched_off", dewy.dewTick(level, 4000) == 0, "off");
        Settings.reset();
        BlockPos roofed = base.offset(0, 0, 24);
        BarrelBlockEntity dry = place(level, roofed, Ids.WOODEN_BARREL);
        level.setBlockAndUpdate(roofed.above(), Blocks.STONE.defaultBlockState());
        SelfTest.verdict("a_roof_stops_dew", dry.dewTick(level, 4000) == 0 && dry.state().amount() == 0, "roofed");
        level.setBlockAndUpdate(roofed.above(), Blocks.AIR.defaultBlockState());

        GameEvents.BARREL_FINISHED.clear();
        Settings.reset();
    }

    private static int comparator(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getAnalogOutputSignal(level, pos, Direction.NORTH);
    }
}
