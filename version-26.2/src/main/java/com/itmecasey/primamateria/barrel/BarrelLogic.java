package com.itmecasey.primamateria.barrel;

import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.blockentity.BarrelBlockEntity;
import com.itmecasey.primamateria.data.CompostValues;
import com.itmecasey.primamateria.fluid.FluidBuckets;
import com.itmecasey.primamateria.recipe.BarrelRecipes;
import com.itmecasey.primamateria.recipe.CompostingRecipe;
import com.itmecasey.primamateria.sift.Drop;
import com.itmecasey.primamateria.util.Transfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Everything that happens to a barrel when it is used: buckets, items, an empty hand. Runs on the server only. All the rules
 * themselves are in {@link BarrelState}; this class finds the right recipe for what the player holds and applies it.
 */
public final class BarrelLogic {
    private BarrelLogic() {
    }

    private static double timeMult() {
        return Settings.getDouble("barrel.time_multiplier");
    }

    /** What an offered item would do. */
    private record Action(Kind kind, Object payload) {
        enum Kind {
            COMPOST, SOAK, STEEP, GROW
        }
    }

    /** Works out what an item would do in this barrel right now, or null when it would do nothing. */
    private static Action resolve(ServerLevel level, BarrelBlockEntity be, ItemStack stack) {
        BarrelState state = be.state();
        CompostValues.Value value = CompostValues.of(stack);
        if (value != null && state.canCompost(value.boost()) && BarrelRecipes.composting(level.recipeAccess()) != null) {
            return new Action(Action.Kind.COMPOST, value);
        }
        BarrelRecipes.ForItem recipes = BarrelRecipes.forItem(level.recipeAccess(), stack);
        for (BarrelRecipes.Held<com.itmecasey.primamateria.recipe.SoakingRecipe> h : recipes.soaking()) {
            if (state.canSoak(h.recipe().rule(h.id()))) {
                return new Action(Action.Kind.SOAK, h);
            }
        }
        for (BarrelRecipes.Held<com.itmecasey.primamateria.recipe.SteepingRecipe> h : recipes.steeping()) {
            if (state.canSteep(h.recipe().rule(h.id()))) {
                return new Action(Action.Kind.STEEP, h);
            }
        }
        for (BarrelRecipes.Held<com.itmecasey.primamateria.recipe.GrowingRecipe> h : recipes.growing()) {
            if (state.canPlant(h.recipe().rule(h.id()))) {
                return new Action(Action.Kind.GROW, h);
            }
        }
        return null;
    }

    /** Whether the barrel would take one of this item now (used by hoppers). */
    public static boolean accepts(ServerLevel level, BarrelBlockEntity be, ItemStack stack) {
        return !stack.isEmpty() && resolve(level, be, stack) != null;
    }

    /** Uses one of the item in the barrel (the caller takes it from the stack). Returns whether it was used. */
    @SuppressWarnings("unchecked")
    public static boolean offer(ServerLevel level, BarrelBlockEntity be, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Action action = resolve(level, be, stack);
        if (action == null) {
            return false;
        }
        BarrelState state = be.state();
        String itemId = Transfer.itemId(stack);
        boolean ok = switch (action.kind()) {
            case COMPOST -> {
                CompostValues.Value v = (CompostValues.Value) action.payload();
                BarrelRecipes.Held<CompostingRecipe> held = BarrelRecipes.composting(level.recipeAccess());
                int ticks = Math.max(1, (int) Math.round(held.recipe().time() * timeMult()));
                yield state.addCompost(v.units(), v.boost(), held.recipe().result().toString(), held.id(), ticks);
            }
            case SOAK -> {
                BarrelRecipes.Held<com.itmecasey.primamateria.recipe.SoakingRecipe> h =
                        (BarrelRecipes.Held<com.itmecasey.primamateria.recipe.SoakingRecipe>) action.payload();
                yield state.startSoak(h.recipe().rule(h.id()), itemId, timeMult());
            }
            case STEEP -> {
                BarrelRecipes.Held<com.itmecasey.primamateria.recipe.SteepingRecipe> h =
                        (BarrelRecipes.Held<com.itmecasey.primamateria.recipe.SteepingRecipe>) action.payload();
                yield state.addSteepItem(h.recipe().rule(h.id()), itemId, timeMult());
            }
            case GROW -> {
                BarrelRecipes.Held<com.itmecasey.primamateria.recipe.GrowingRecipe> h =
                        (BarrelRecipes.Held<com.itmecasey.primamateria.recipe.GrowingRecipe>) action.payload();
                yield state.plantSeed(h.recipe().rule(h.id()), timeMult());
            }
        };
        if (ok) {
            level.playSound(null, be.getBlockPos(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.3F, 1.4F);
            be.changed();
        }
        return ok;
    }

    /**
     * A full bucket of {@code fluid} used on the barrel: poured in when a whole bucket fits, or mixed with what is in. Returns
     * whether anything happened (the caller gives the empty bucket back).
     */
    public static boolean pour(ServerLevel level, BarrelBlockEntity be, String fluid) {
        BarrelState state = be.state();
        if (!state.fluid().isEmpty() && !state.fluid().equals(fluid)) {
            java.util.List<BarrelState.MixRule> rules = new java.util.ArrayList<>();
            for (BarrelRecipes.Held<com.itmecasey.primamateria.recipe.MixingRecipe> h : BarrelRecipes.mixing(level.recipeAccess())) {
                rules.add(h.recipe().rule(h.id()));
            }
            BarrelState.Finished done = state.mix(rules, fluid, FluidBuckets.BUCKET_AMOUNT);
            if (done != null) {
                be.fire(level, java.util.List.of(done));
                level.playSound(null, be.getBlockPos(), SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
                be.changed();
                return true;
            }
            return false;
        }
        if (BarrelState.CAPACITY - state.amount() < FluidBuckets.BUCKET_AMOUNT || !state.canFill(fluid)) {
            return false;
        }
        state.fill(fluid, FluidBuckets.BUCKET_AMOUNT);
        level.playSound(null, be.getBlockPos(), BarrelState.isHot(fluid) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY,
                SoundSource.BLOCKS, 0.8F, 1.0F);
        be.changed();
        return true;
    }

    /** An empty bucket used on the barrel: takes 1000 mB when the barrel holds that much and nothing is working. */
    @Nullable
    public static String scoop(ServerLevel level, BarrelBlockEntity be) {
        BarrelState state = be.state();
        String fluid = state.fluid();
        if (fluid.isEmpty() || FluidBuckets.bucketOf(fluid).isEmpty() || !state.canDrain(FluidBuckets.BUCKET_AMOUNT)) {
            return null;
        }
        Identifier bucket = Identifier.tryParse(FluidBuckets.bucketOf(fluid).get());
        if (bucket == null || !BuiltInRegistries.ITEM.containsKey(bucket)) {
            return null;
        }
        state.drain(FluidBuckets.BUCKET_AMOUNT);
        level.playSound(null, be.getBlockPos(), BarrelState.isHot(fluid) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL,
                SoundSource.BLOCKS, 0.8F, 1.0F);
        be.changed();
        return bucket.toString();
    }

    /** The barrel at {@code pos} used by a player with {@code stack} in hand (empty for an empty hand). */
    public static InteractionResult interact(ServerLevel level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return takeResult(level, be, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        String itemId = Transfer.itemId(stack);
        java.util.Optional<String> carried = FluidBuckets.fluidOf(itemId);
        if (carried.isPresent()) {
            if (pour(level, be, carried.get())) {
                swapBucket(player, hand, stack, new ItemStack(Items.BUCKET));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        if (itemId.equals(FluidBuckets.EMPTY_BUCKET)) {
            String full = scoop(level, be);
            if (full == null) {
                return InteractionResult.PASS;
            }
            swapBucket(player, hand, stack, Transfer.stackOf(full, 1));
            return InteractionResult.SUCCESS;
        }
        if (offer(level, be, stack)) {
            Transfer.consume(player, stack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** An empty hand takes the finished result (one kind at a time) into the inventory. */
    private static boolean takeResult(ServerLevel level, BarrelBlockEntity be, Player player) {
        Drop d = be.state().takeResult();
        if (d == null) {
            return false;
        }
        for (ItemStack stack : Transfer.split(d)) {
            Transfer.giveBack(player, stack);
        }
        be.changed();
        return true;
    }

    /** The bucket in hand turns into another (full into empty or the other way); creative mode keeps its stack. */
    private static void swapBucket(Player player, InteractionHand hand, ItemStack inHand, ItemStack replacement) {
        if (player.getAbilities().instabuild) {
            return;
        }
        if (inHand.getCount() <= 1) {
            player.setItemInHand(hand, replacement);
        } else {
            inHand.shrink(1);
            Transfer.giveBack(player, replacement);
        }
    }
}
