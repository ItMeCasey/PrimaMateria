package com.itmecasey.primamateria.fluid;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;

/** The liquid block of one of our fluids. Vanilla's own liquid block does everything; this only makes its constructor reachable. */
public class PrimaLiquidBlock extends LiquidBlock {
    public PrimaLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }
}
