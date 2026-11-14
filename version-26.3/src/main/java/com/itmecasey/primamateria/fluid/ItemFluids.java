package com.itmecasey.primamateria.fluid;

import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;

/**
 * How the machines fill and empty the item in a player's hand: through the loader's own item fluid handler, so a bucket, a tank
 * item or any other fluid container of any mod works with the barrels, the crucible and the casting tray. The loader entry class
 * installs its implementation at startup ({@code NeoForgeItemFluids}, {@code ForgeItemFluids}); until then the game's own bucket
 * class is the only container. There is no table of buckets here.
 */
public final class ItemFluids {
    /** What a full container holds: a fluid registry id and an amount in mB. */
    public record Held(String fluid, int amount) {
    }

    /** The loader side: the item fluid handler of a stack. */
    public interface Access {
        /** What one item of the stack holds, or empty for an empty or non-container item. Changes nothing. */
        Optional<Held> contents(ItemStack stack);

        /** Whether one item of the stack takes {@code amount} mB of the fluid (simulated; changes nothing). */
        boolean canFill(ItemStack stack, String fluid, int amount);

        /** Fills one item of the player's hand stack with {@code amount} mB and puts the filled container where the game puts it. */
        boolean fillHeld(Player player, InteractionHand hand, String fluid, int amount);

        /** Takes {@code amount} mB out of one item of the player's hand stack (a full bucket becomes an empty one). */
        boolean drainHeld(Player player, InteractionHand hand, int amount);
    }

    private static final Access VANILLA = new Access() {
        @Override
        public Optional<Held> contents(ItemStack stack) {
            if (stack.getItem() instanceof BucketItem bucket) {
                String id = FluidIds.idOf(bucket.getContent());
                return id.isEmpty() ? Optional.empty() : Optional.of(new Held(id, FluidBuckets.BUCKET_AMOUNT));
            }
            return Optional.empty();
        }

        private Item bucketFor(String fluid) {
            return FluidIds.fluidOf(fluid).map(net.minecraft.world.level.material.Fluid::getBucket).orElse(Items.AIR);
        }

        @Override
        public boolean canFill(ItemStack stack, String fluid, int amount) {
            Item bucket = bucketFor(fluid);
            return stack.is(Items.BUCKET) && amount >= FluidBuckets.BUCKET_AMOUNT && bucket != Items.AIR && bucket != Items.BUCKET;
        }

        @Override
        public boolean fillHeld(Player player, InteractionHand hand, String fluid, int amount) {
            ItemStack held = player.getItemInHand(hand);
            if (!canFill(held, fluid, amount)) {
                return false;
            }
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(bucketFor(fluid))));
            return true;
        }

        @Override
        public boolean drainHeld(Player player, InteractionHand hand, int amount) {
            ItemStack held = player.getItemInHand(hand);
            if (contents(held).isEmpty() || amount < FluidBuckets.BUCKET_AMOUNT) {
                return false;
            }
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
            return true;
        }
    };

    private static Access access = VANILLA;

    private ItemFluids() {
    }

    /** The game's own bucket handling, for loaders whose item fluid handler is not available. */
    public static Access vanilla() {
        return VANILLA;
    }

    /** Installed by the loader entry class. */
    public static void install(Access loader) {
        access = loader;
    }

    public static Optional<Held> contents(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : access.contents(stack);
    }

    /** The fluid a full bucket-sized container holds (1000 mB or more), or empty. */
    public static Optional<String> carriedBy(ItemStack stack) {
        return contents(stack).filter(h -> h.amount() >= FluidBuckets.BUCKET_AMOUNT).map(Held::fluid);
    }

    /** Whether the stack is a container that takes 1000 mB of the fluid. */
    public static boolean canFillWith(ItemStack stack, String fluid) {
        return !stack.isEmpty() && fluid != null && !fluid.isEmpty() && access.canFill(stack, fluid, FluidBuckets.BUCKET_AMOUNT);
    }

    /** Fills the hand item with 1000 mB of the fluid. */
    public static boolean fillHeld(Player player, InteractionHand hand, String fluid) {
        return access.fillHeld(player, hand, fluid, FluidBuckets.BUCKET_AMOUNT);
    }

    /** Pours 1000 mB out of the hand item. */
    public static boolean drainHeld(Player player, InteractionHand hand) {
        return access.drainHeld(player, hand, FluidBuckets.BUCKET_AMOUNT);
    }
}
