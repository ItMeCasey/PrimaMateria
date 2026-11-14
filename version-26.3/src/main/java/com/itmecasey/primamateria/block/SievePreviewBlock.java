package com.itmecasey.primamateria.block;

import com.itmecasey.primamateria.ModShapes;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.blockentity.SievePreviewBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The sieve as workstream C needs it to draw and test the animation: the frozen state properties, the selection shape and a
 * block entity that holds a {@code SieveView}. It has no gameplay. Workstream A's real sieve block takes this place; the renderer
 * only needs a block entity that implements {@code SieveViewSource}.
 */
public class SievePreviewBlock extends Block implements EntityBlock {
    public SievePreviewBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(StateProps.MOVING, false).setValue(StateProps.MESH, 0)
                .setValue(StateProps.FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(StateProps.MOVING, StateProps.MESH, StateProps.FILL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SievePreviewBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModShapes.SIEVE;
    }
}
