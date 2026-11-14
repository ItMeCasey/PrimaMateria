package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.fluid.FluidIds;
import com.itmecasey.primamateria.fluid.ItemFluids;
import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The machines' view of the item in a hand, through NeoForge's own item fluid handler ({@code Capabilities.Fluid.ITEM}): a bucket,
 * a tank item or any other fluid container of any mod fills and empties the barrels, the crucible and the casting tray the way it
 * does any other fluid handler. Nothing here knows a bucket by name.
 */
final class NeoForgeItemFluids implements ItemFluids.Access {
    @Override
    public Optional<ItemFluids.Held> contents(ItemStack stack) {
        FluidStack fluid = FluidUtil.getFirstStackContained(stack.copyWithCount(1));
        if (fluid.isEmpty()) {
            return Optional.empty();
        }
        String id = FluidIds.idOf(fluid.getFluid());
        return id.isEmpty() ? Optional.empty() : Optional.of(new ItemFluids.Held(id, fluid.getAmount()));
    }

    private static ResourceHandler<FluidResource> handler(ItemAccess access) {
        return access.oneByOne().getCapability(Capabilities.Fluid.ITEM);
    }

    @Override
    public boolean canFill(ItemStack stack, String fluid, int amount) {
        ItemStack copy = stack.copyWithCount(1);
        Optional<Fluid> f = FluidIds.fluidOf(fluid);
        if (copy.isEmpty() || f.isEmpty()) {
            return false;
        }
        ResourceHandler<FluidResource> handler = handler(ItemAccess.forStack(copy));
        if (handler == null) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            return handler.insert(FluidResource.of(f.get()), amount, transaction) >= amount;
        }
    }

    @Override
    public boolean fillHeld(Player player, InteractionHand hand, String fluid, int amount) {
        Optional<Fluid> f = FluidIds.fluidOf(fluid);
        if (f.isEmpty() || player.getItemInHand(hand).isEmpty()) {
            return false;
        }
        ResourceHandler<FluidResource> handler = handler(ItemAccess.forPlayerInteraction(player, hand));
        if (handler == null) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.insert(FluidResource.of(f.get()), amount, transaction) < amount) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    @Override
    public boolean drainHeld(Player player, InteractionHand hand, int amount) {
        ItemStack held = player.getItemInHand(hand);
        FluidStack inside = FluidUtil.getFirstStackContained(held);
        if (held.isEmpty() || inside.isEmpty()) {
            return false;
        }
        ResourceHandler<FluidResource> handler = handler(ItemAccess.forPlayerInteraction(player, hand));
        if (handler == null) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(FluidResource.of(inside), amount, transaction) < amount) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }
}
