package com.itmecasey.primamateria.block;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.ModShapes;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.blockentity.SieveBlockEntity;
import com.itmecasey.primamateria.sieve.SieveLogic;
import com.itmecasey.primamateria.sift.SiftMath;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The sieve. Fit a mesh by using the mesh on it; put a siftable block in by using the block on it; hold use to shake. Everything
 * is done in the world: there is no screen.
 */
public class SieveBlock extends BaseEntityBlock {
    private static final MapCodec<SieveBlock> CODEC = simpleCodec(SieveBlock::new);

    public SieveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(StateProps.MESH, 0).setValue(StateProps.MOVING, false).setValue(StateProps.FILL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(StateProps.MESH, StateProps.MOVING, StateProps.FILL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModShapes.SIEVE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SieveBlockEntity(pos, state);
    }

    /** Only a sieve that is shaking needs a tick (to clear its moving flag); a resting sieve costs nothing. */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || !state.getValue(StateProps.MOVING) ? null
                : createTickerHelper(type, ModContent.SIEVE_BE, SieveBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel) {
            return SieveLogic.interact(serverLevel, pos, player, hand, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            return SieveLogic.interact(serverLevel, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        boolean worthASwing = state.getValue(StateProps.FILL) > 0 || (player.isShiftKeyDown() && state.getValue(StateProps.MESH) > 0);
        return worthASwing ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SieveBlockEntity be
                ? SiftMath.comparatorSignal(be.state().filled(), be.state().progress()) : 0;
    }
}
