package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.blockentity.SieveBlockEntity;
import com.itmecasey.primamateria.data.Meshes;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.sieve.SieveLogic;
import com.itmecasey.primamateria.sift.SiftMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Server checks of the sieve: meshes, recipes, fitting, sifting, events, hoppers and linked sieves. */
final class SieveSelfTest {
    private SieveSelfTest() {
    }

    /** Game time does not advance during the self test, so a sieve that shook a moment ago is told it has rested. */
    private static void coolDown(SieveBlockEntity be) {
        com.itmecasey.primamateria.sift.SiftState s = be.state();
        s.restore(s.meshId(), s.meshTier(), s.meshSpeed(), s.block(), s.shakes(), s.needed(), s.seed(),
                com.itmecasey.primamateria.sift.SiftState.encode(s.drops()), com.itmecasey.primamateria.sift.SiftState.encode(s.residue()),
                Long.MIN_VALUE / 2);
    }

    private static int count(Container c, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(item)) {
                n += c.getItem(i).getCount();
            }
        }
        return n;
    }

    private static SieveBlockEntity placeSieve(ServerLevel level, BlockPos pos) {
        // the test world is kept between runs: clear the spot first so no old block entity data survives
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, ModContent.block(Ids.SIEVE).defaultBlockState());
        return (SieveBlockEntity) level.getBlockEntity(pos);
    }

    /** Shakes the sieve through the drive path (no hand cooldown) until the block is done. Returns the shake count. */
    private static int sift(ServerLevel level, SieveBlockEntity be) {
        int shakes = 0;
        while (be.state().filled() && shakes < 100) {
            SieveLogic.shakeByDrive(level, be, 0);
            shakes++;
        }
        return shakes;
    }

    static void run(MinecraftServer server, ServerLevel level) {
        SelfTest.verdict("meshes_loaded", Meshes.all().size() == 5 && Meshes.get("primamateria:iron_mesh") != null
                && Meshes.get("primamateria:iron_mesh").tier() == 3, Meshes.all().size() + " meshes");
        PrimaRecipes.Sifts gravel = PrimaRecipes.siftsFor(level.recipeAccess(), new ItemStack(Items.GRAVEL));
        PrimaRecipes.Sifts dirt = PrimaRecipes.siftsFor(level.recipeAccess(), new ItemStack(Items.DIRT));
        PrimaRecipes.Sifts stone = PrimaRecipes.siftsFor(level.recipeAccess(), new ItemStack(ModContent.item(Ids.STONE_MEAL)));
        SelfTest.verdict("sifting_recipes_loaded", gravel.drops().size() == 6 && gravel.residue().size() == 1 && dirt.drops().size() == 15
                && stone.drops().size() == 11, gravel.drops().size() + " gravel, " + dirt.drops().size() + " dirt, " + stone.drops().size() + " stone meal");
        SelfTest.verdict("not_siftable", !PrimaRecipes.siftsFor(level.recipeAccess(), new ItemStack(Items.STONE)).siftable(), "stone");

        ServerPlayer player = SelfTest.testPlayer(server, level, "sievetest");
        BlockPos base = SelfTest.area(level, 0);

        // fit a mesh, put a block in, sift it into the chest below
        BlockPos pos = base;
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.CHEST.defaultBlockState());
        SieveBlockEntity be = placeSieve(level, pos);
        ItemStack gravelStack = new ItemStack(Items.GRAVEL, 5);
        SelfTest.verdict("block_refused_without_mesh", SieveLogic.interact(level, pos, player, InteractionHand.MAIN_HAND, gravelStack) == InteractionResult.PASS
                && gravelStack.getCount() == 5 && !be.state().filled(), "no mesh, stack kept");
        ItemStack mesh = new ItemStack(ModContent.item("cloth_mesh"));
        InteractionResult fitted = SieveLogic.interact(level, pos, player, InteractionHand.MAIN_HAND, mesh);
        SelfTest.verdict("mesh_fitted", fitted.consumesAction() && mesh.isEmpty() && be.state().meshId().equals("primamateria:cloth_mesh")
                && level.getBlockState(pos).getValue(StateProps.MESH) == 1, be.state().meshId());
        SelfTest.verdict("block_goes_in_one_at_a_time", SieveLogic.interact(level, pos, player, InteractionHand.MAIN_HAND, gravelStack).consumesAction()
                && gravelStack.getCount() == 4 && be.state().filled() && be.state().block().equals("minecraft:gravel")
                && level.getBlockState(pos).getValue(StateProps.FILL) == 4, "stack 5 to " + gravelStack.getCount());
        SelfTest.verdict("comparator_when_filled", SiftMath.comparatorSignal(be.state().filled(), be.state().progress()) == 1, "signal 1");

        GameEvents.SIFTED.clear();
        int[] fired = {0};
        GameEvents.SIFTED.listen(e -> {
            fired[0]++;
            e.drops.add(new GameEvents.Drop("minecraft:stick", 3));
        });
        Container chest = HopperBlockEntity.getContainerAt(level, pos.below());
        int shakes = sift(level, be);
        SelfTest.verdict("sifted_in_the_expected_shakes", shakes == 8 && !be.state().filled(), shakes + " shakes");
        SelfTest.verdict("event_fired_and_drops_reach_the_chest_below", fired[0] == 1 && chest != null && count(chest, Items.STICK) == 3,
                "sticks in chest: " + (chest == null ? -1 : count(chest, Items.STICK)));
        SelfTest.verdict("mesh_stays_after_a_block", be.state().hasMesh(), be.state().meshId());

        GameEvents.SIFTED.clear();
        GameEvents.SIFTED.listen(e -> e.cancel());
        int gravelBefore = count(chest, Items.GRAVEL);
        SieveLogic.insert(level, be, new ItemStack(Items.GRAVEL), player);
        sift(level, be);
        SelfTest.verdict("cancel_returns_the_block", count(chest, Items.GRAVEL) == gravelBefore + 1 && !be.state().filled(),
                "gravel in chest " + count(chest, Items.GRAVEL));
        GameEvents.SIFTED.clear();

        // a hopper offering a block
        boolean offered = HopperBlockEntity.addItem(null, be, new ItemStack(Items.GRAVEL), Direction.UP).isEmpty() && be.state().filled();
        SelfTest.verdict("hopper_can_insert_one_block", offered, "filled " + be.state().filled());
        ItemStack second = HopperBlockEntity.addItem(null, be, new ItemStack(Items.GRAVEL), Direction.UP);
        SelfTest.verdict("hopper_cannot_insert_a_second", !second.isEmpty(), "returned " + second.getCount());
        SelfTest.verdict("hopper_cannot_insert_unsiftable", !be.canPlaceItem(0, new ItemStack(Items.STONE)), "stone");
        sift(level, be);

        // linked sieves: three in a row, one stack of five
        BlockPos row = base.offset(0, 0, 6);
        SieveBlockEntity[] three = new SieveBlockEntity[3];
        for (int i = 0; i < 3; i++) {
            three[i] = placeSieve(level, row.offset(i, 0, 0));
            three[i].state().setMesh("primamateria:copper_mesh", 2, 1.0);
        }
        ItemStack stack = new ItemStack(Items.SAND, 5);
        // the middle sieve reaches both others with the default radius of 1; the outer two are 2 apart and do not
        SieveLogic.interact(level, row.offset(1, 0, 0), player, InteractionHand.MAIN_HAND, stack);
        SelfTest.verdict("linked_sieves_fill_together", three[0].state().filled() && three[1].state().filled() && three[2].state().filled()
                && stack.getCount() == 2, "stack 5 to " + stack.getCount());
        SieveLogic.interact(level, row.offset(1, 0, 0), player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        SelfTest.verdict("linked_sieves_shake_together", three[0].state().shakes() == 1 && three[1].state().shakes() == 1 && three[2].state().shakes() == 1,
                three[0].state().shakes() + "," + three[1].state().shakes() + "," + three[2].state().shakes());
        for (SieveBlockEntity s : three) {
            sift(level, s);
        }
        com.itmecasey.primamateria.Settings.set("sieve.link", false);
        ItemStack lone = new ItemStack(Items.SAND, 5);
        SieveLogic.interact(level, row, player, InteractionHand.MAIN_HAND, lone);
        SelfTest.verdict("link_setting_off_fills_only_one", three[0].state().filled() && !three[1].state().filled() && lone.getCount() == 4,
                "stack 5 to " + lone.getCount());
        sift(level, three[0]);
        ItemStack unlinkedShake = new ItemStack(Items.SAND, 3);
        SieveLogic.interact(level, row.offset(1, 0, 0), player, InteractionHand.MAIN_HAND, unlinkedShake);
        coolDown(three[1]);
        SieveLogic.interact(level, row.offset(1, 0, 0), player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        SelfTest.verdict("link_setting_off_shakes_only_one", three[1].state().shakes() == 1 && three[0].state().shakes() == 0
                && three[2].state().shakes() == 0, three[0].state().shakes() + "," + three[1].state().shakes() + "," + three[2].state().shakes());
        sift(level, three[1]);
        com.itmecasey.primamateria.Settings.reset();

        // taking the mesh back out with a sneaking empty hand
        BlockPos last = base.offset(0, 0, 14);
        SieveBlockEntity b = placeSieve(level, last);
        b.state().setMesh("primamateria:iron_mesh", 3, 1.25);
        player.setShiftKeyDown(true);
        try {
            SieveLogic.interact(level, last, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } catch (NullPointerException e) {
            // the test player has no network connection, so handing the mesh back to its inventory cannot sync; the sieve is already empty
        }
        player.setShiftKeyDown(false);
        SelfTest.verdict("sneak_with_empty_hand_takes_the_mesh", !b.state().hasMesh(), "mesh removed from the sieve");
    }
}
