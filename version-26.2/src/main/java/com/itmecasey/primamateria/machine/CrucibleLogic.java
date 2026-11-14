package com.itmecasey.primamateria.machine;

import com.itmecasey.primamateria.blockentity.CastingTrayBlockEntity;
import com.itmecasey.primamateria.blockentity.CrucibleBlockEntity;
import com.itmecasey.primamateria.fluid.FluidBuckets;
import com.itmecasey.primamateria.recipe.CrucibleRecipes;
import com.itmecasey.primamateria.util.Transfer;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** What happens when a player uses a crucible or a casting tray with something in hand. Server side. */
public final class CrucibleLogic {
    private CrucibleLogic() {
    }

    /** Lava and the molten metals may go into a crucible; water and brine may not. */
    public static boolean hot(String fluid) {
        return fluid.equals("minecraft:lava") || fluid.startsWith("primamateria:molten_");
    }

    // ---- crucible ----

    public static InteractionResult useCrucible(ServerLevel level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            String back = be.state().removeLastSolid();
            if (back == null) {
                return InteractionResult.PASS;
            }
            be.changed();
            Transfer.giveBack(player, Transfer.stackOf(back, 1));
            return InteractionResult.SUCCESS;
        }
        String id = Transfer.itemId(stack);
        Optional<String> bucketFluid = FluidBuckets.fluidOf(id);
        if (bucketFluid.isPresent()) {
            String fluid = bucketFluid.get();
            if (!hot(fluid)) {
                return InteractionResult.PASS;
            }
            if (be.state().fill(fluid, FluidBuckets.BUCKET_AMOUNT)) {
                be.changed();
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.8F, 1.0F);
                swapForEmptyBucket(player, hand, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (id.equals(FluidBuckets.EMPTY_BUCKET)) {
            Optional<String> full = FluidBuckets.bucketOf(be.state().fluid());
            if (full.isPresent() && be.state().drain(FluidBuckets.BUCKET_AMOUNT)) {
                be.changed();
                level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.8F, 1.0F);
                swapFor(player, hand, stack, full.get());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (be.state().canAddSolid() && CrucibleRecipes.meltFor(level.recipeAccess(), stack) != null) {
            be.state().addSolid(id);
            be.changed();
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6F, 0.9F);
            Transfer.consume(player, stack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ---- casting tray ----

    public static InteractionResult useTray(ServerLevel level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof CastingTrayBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            if (!be.state().done()) {
                return InteractionResult.PASS;
            }
            String item = be.state().result();
            int taken = be.state().removeCount(be.state().count());
            be.changed();
            Transfer.giveBack(player, Transfer.stackOf(item, taken));
            return InteractionResult.SUCCESS;
        }
        Optional<String> bucketFluid = FluidBuckets.fluidOf(Transfer.itemId(stack));
        if (bucketFluid.isEmpty() || !hot(bucketFluid.get())) {
            return InteractionResult.PASS;
        }
        CrucibleRecipes.Cast cast = CrucibleRecipes.castFor(level.recipeAccess(), bucketFluid.get());
        if (cast == null || !be.state().canAccept()) {
            return InteractionResult.SUCCESS;
        }
        // a bucket holds 1000 mB: it casts as many items as fit that amount (and one stack), so nothing is lost
        ItemStack sample = Transfer.stackOf(cast.result(), 1);
        int count = Math.max(1, Math.min(FluidBuckets.BUCKET_AMOUNT / cast.amount(), sample.isEmpty() ? 1 : sample.getMaxStackSize()));
        if (be.pour(bucketFluid.get(), cast, count)) {
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.8F, 1.0F);
            swapForEmptyBucket(player, hand, stack);
        }
        return InteractionResult.SUCCESS;
    }

    // ---- buckets ----

    private static void swapForEmptyBucket(Player player, InteractionHand hand, ItemStack stack) {
        swapFor(player, hand, stack, FluidBuckets.EMPTY_BUCKET);
    }

    /** Replaces one item of the hand stack with another (a full bucket for an empty one and back). Creative keeps the stack. */
    private static void swapFor(Player player, InteractionHand hand, ItemStack stack, String itemId) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack replacement = Transfer.stackOf(itemId, 1);
        if (stack.getCount() == 1) {
            player.setItemInHand(hand, replacement);
        } else {
            stack.shrink(1);
            Transfer.giveBack(player, replacement);
        }
    }
}
