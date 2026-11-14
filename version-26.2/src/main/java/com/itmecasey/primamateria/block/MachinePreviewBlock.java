package com.itmecasey.primamateria.block;

import com.itmecasey.primamateria.blockentity.MachinePreviewBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A shell for the barrels, crucible, casting tray, drives and auto hammer so their renderer can be drawn and tested: the
 * selection shape, a facing, and a block entity that holds a view record. No gameplay. Workstream A's real blocks replace it; the
 * renderer only needs a block entity that implements {@code MachineViewSource}.
 */
public class MachinePreviewBlock extends Block implements EntityBlock {
    private static final MapCodec<MachinePreviewBlock> CODEC = simpleCodec(p -> new MachinePreviewBlock(p, net.minecraft.world.phys.shapes.Shapes.block()));
    private final VoxelShape shape;

    public MachinePreviewBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachinePreviewBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
