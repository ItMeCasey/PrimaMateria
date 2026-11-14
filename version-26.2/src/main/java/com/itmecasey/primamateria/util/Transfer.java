package com.itmecasey.primamateria.util;

import com.itmecasey.primamateria.sift.Drop;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared item moving for every machine: item ids to stacks, into containers, onto the ground, back to players. Server side. */
public final class Transfer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");

    private Transfer() {
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** A stack of the item with this id, or empty when the id is bad or the item does not exist (a removed mod). */
    public static ItemStack stackOf(String id, int count) {
        Identifier parsed = Identifier.tryParse(id);
        if (parsed == null || !BuiltInRegistries.ITEM.containsKey(parsed) || count < 1) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(parsed);
        return new ItemStack(item, count);
    }

    /** A drop as stacks no bigger than the item's stack size. A missing item gives no stacks. */
    public static List<ItemStack> split(Drop drop) {
        List<ItemStack> out = new ArrayList<>();
        ItemStack all = stackOf(drop.item(), drop.count());
        if (all.isEmpty()) {
            LOGGER.warn("Item {} does not exist, skipped", drop.item());
            return out;
        }
        int left = drop.count();
        int max = Math.max(1, all.getMaxStackSize());
        while (left > 0) {
            int n = Math.min(max, left);
            out.add(new ItemStack(all.getItem(), n));
            left -= n;
        }
        return out;
    }

    /** Puts a stack into a container from above; returns what did not fit. */
    public static ItemStack insert(@Nullable Container target, ItemStack stack) {
        return target == null ? stack : HopperBlockEntity.addItem(null, target, stack, Direction.UP);
    }

    /**
     * Hands out drops: into {@code target} when it takes them, and what is left appears above {@code pos}, hopping toward
     * {@code player} when there is one. With a null target everything appears at once.
     */
    public static void deliver(ServerLevel level, BlockPos pos, @Nullable Container target, @Nullable Player player, List<Drop> drops) {
        for (Drop d : drops) {
            for (ItemStack stack : split(d)) {
                ItemStack left = insert(target, stack);
                if (!left.isEmpty()) {
                    spawn(level, pos, left, player);
                }
            }
        }
    }

    /** The container under {@code pos} (a chest, hopper, drive...), or null. */
    public static Container below(ServerLevel level, BlockPos pos) {
        return HopperBlockEntity.getContainerAt(level, pos.below());
    }

    /** An item entity on top of the block: it hops toward the player when there is one, and falls just under the block otherwise. */
    public static void spawn(ServerLevel level, BlockPos pos, ItemStack stack, @Nullable Player player) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.05;
        double z = pos.getZ() + 0.5;
        Vec3 velocity = Vec3.ZERO;
        if (player != null) {
            Vec3 to = new Vec3(player.getX() - x, 0, player.getZ() - z);
            if (to.lengthSqr() > 1.0E-4) {
                to = to.normalize().scale(0.18);
            }
            velocity = new Vec3(to.x, 0.2, to.z);
        } else {
            y = pos.getY() - 0.1;
        }
        ItemEntity entity = new ItemEntity(level, x, y, z, stack, velocity.x, velocity.y, velocity.z);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    /** Gives a stack to a player; what does not fit is dropped at their feet. */
    public static void giveBack(@Nullable Player player, ItemStack stack) {
        if (!stack.isEmpty() && player != null) {
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
            }
        }
    }

    /** Takes one item from the hand stack unless the player is in creative mode. */
    public static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
