package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.fluid.FluidIds;
import com.itmecasey.primamateria.fluid.ItemFluids;
import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * The machines' view of the item in a hand, through Forge's own item fluid handler ({@code ForgeCapabilities.FLUID_HANDLER_ITEM}):
 * a bucket, a tank item or any other fluid container of any mod fills and empties the barrels, the crucible and the casting tray
 * the way it does any other fluid handler. Nothing here knows a bucket by name. (This Forge build returns no item fluid handler
 * for any item, vanilla buckets included, so with none the game's own bucket handling answers.)
 */
final class ForgeItemFluids implements ItemFluids.Access {
    private static Optional<IFluidHandlerItem> handler(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve();
    }

    private static final ItemFluids.Access GAME = ItemFluids.vanilla();

    @Override
    public Optional<ItemFluids.Held> contents(ItemStack stack) {
        if (handler(stack.copyWithCount(1)).isEmpty()) {
            return GAME.contents(stack);
        }
        return handler(stack.copyWithCount(1)).flatMap(h -> {
            FluidStack fluid = h.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (fluid.isEmpty()) {
                return Optional.<ItemFluids.Held>empty();
            }
            String id = FluidIds.idOf(fluid.getFluid());
            return id.isEmpty() ? Optional.<ItemFluids.Held>empty() : Optional.of(new ItemFluids.Held(id, fluid.getAmount()));
        });
    }

    @Override
    public boolean canFill(ItemStack stack, String fluid, int amount) {
        if (handler(stack.copyWithCount(1)).isEmpty()) {
            return GAME.canFill(stack, fluid, amount);
        }
        Optional<Fluid> f = FluidIds.fluidOf(fluid);
        return f.isPresent() && handler(stack.copyWithCount(1))
                .map(h -> h.fill(new FluidStack(f.get(), amount), IFluidHandler.FluidAction.SIMULATE) >= amount).orElse(false);
    }

    @Override
    public boolean fillHeld(Player player, InteractionHand hand, String fluid, int amount) {
        Optional<Fluid> f = FluidIds.fluidOf(fluid);
        ItemStack held = player.getItemInHand(hand);
        if (handler(held.copyWithCount(1)).isEmpty()) {
            return GAME.fillHeld(player, hand, fluid, amount);
        }
        if (f.isEmpty()) {
            return false;
        }
        return handler(held.copyWithCount(1)).map(h -> {
            if (h.fill(new FluidStack(f.get(), amount), IFluidHandler.FluidAction.EXECUTE) < amount) {
                return false;
            }
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, h.getContainer()));
            return true;
        }).orElse(false);
    }

    @Override
    public boolean drainHeld(Player player, InteractionHand hand, int amount) {
        ItemStack held = player.getItemInHand(hand);
        if (handler(held.copyWithCount(1)).isEmpty()) {
            return GAME.drainHeld(player, hand, amount);
        }
        return handler(held.copyWithCount(1)).map(h -> {
            if (h.drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount() < amount) {
                return false;
            }
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, h.getContainer()));
            return true;
        }).orElse(false);
    }
}
