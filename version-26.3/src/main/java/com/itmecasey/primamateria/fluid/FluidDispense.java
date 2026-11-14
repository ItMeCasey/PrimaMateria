package com.itmecasey.primamateria.fluid;

import com.itmecasey.primamateria.Ids;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Lets a dispenser place our buckets. The game's own lava bucket behavior works for any bucket item (it empties whatever fluid the
 * bucket holds in front of the dispenser and leaves an empty bucket), so our four buckets reuse it. Picking fluid up with an empty
 * bucket needs nothing: the game's empty bucket behavior takes any fluid block.
 */
public final class FluidDispense {
    private FluidDispense() {
    }

    /** Called once from each loader's common setup. */
    public static void register() {
        DispenseItemBehavior bucket = DispenserBlock.DISPENSER_REGISTRY.get(Items.LAVA_BUCKET);
        if (bucket == null) {
            return;
        }
        for (String id : Ids.FLUIDS) {
            DispenserBlock.registerBehavior(ModFluids.bucket(id), bucket);
        }
    }
}
