package com.itmecasey.primamateria.fluid;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Fluids and buckets by registry id. The machines keep their fluid as a plain id; this turns it into the game's fluid and back, for
 * any fluid of any mod, and finds the bucket that carries it. Flowing forms count as their source.
 */
public final class FluidIds {
    private FluidIds() {
    }

    /** The registry id of the source form of {@code fluid}, or an empty string for empty or unregistered fluids. */
    public static String idOf(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return "";
        }
        Fluid source = fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
        Identifier key = BuiltInRegistries.FLUID.getKey(source);
        return key == null || source == Fluids.EMPTY ? "" : key.toString();
    }

    /** The source fluid registered under {@code id}, or empty when there is none (a removed mod). */
    public static Optional<Fluid> fluidOf(String id) {
        Identifier parsed = id == null ? null : Identifier.tryParse(id);
        if (parsed == null || !BuiltInRegistries.FLUID.containsKey(parsed)) {
            return Optional.empty();
        }
        Fluid fluid = BuiltInRegistries.FLUID.getValue(parsed);
        return fluid == Fluids.EMPTY ? Optional.empty() : Optional.of(fluid);
    }

    /** The fluid a full container in this stack holds (a bucket or tank item of any mod), by the loader's item fluid handler. */
    public static Optional<String> carriedBy(ItemStack stack) {
        return ItemFluids.carriedBy(stack);
    }
}
