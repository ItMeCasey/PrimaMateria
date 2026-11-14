package com.itmecasey.primamateria.fluid;

import com.itmecasey.primamateria.Ids;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.level.block.DispenserBlock;

/** Registers the loader's own fluid container behavior (its {@code DispenseFluidContainer}) for each of our buckets. */
public final class FluidDispense {
    private FluidDispense() {
    }

    /** Called once from each loader's common setup with the loader's behavior. */
    public static void register(DispenseItemBehavior loaderBehavior) {
        for (String id : Ids.FLUIDS) {
            DispenserBlock.registerBehavior(ModFluids.bucket(id), loaderBehavior);
        }
    }
}
