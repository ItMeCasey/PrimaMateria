package com.itmecasey.primamateria.block;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.ModShapes;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.blockentity.AutoHammerBlockEntity;
import com.itmecasey.primamateria.item.HammerItem;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.util.Transfer;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
 * The auto hammer. Use a hammer on it to fit it, use a crushable block on it to queue one, sneak with an empty hand to take the
 * hammer out. It works only when a drive under it works it. No screen.
 */
public class AutoHammerBlock extends BaseEntityBlock {
    private static final MapCodec<AutoHammerBlock> CODEC = simpleCodec(AutoHammerBlock::new);

    public AutoHammerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(StateProps.MOVING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(StateProps.MOVING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModShapes.AUTO_HAMMER;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoHammerBlockEntity(pos, state);
    }

    /** Only a machine that is working needs a tick (to clear its moving flag). */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || !state.getValue(StateProps.MOVING) ? null
                : createTickerHelper(type, ModContent.AUTO_HAMMER_BE, AutoHammerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AutoHammerBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (HammerItem.tierOf(stack) > 0) {
            ItemStack one = stack.copyWithCount(1);
            Transfer.consume(player, stack);
            Transfer.giveBack(player, be.swapHammer(one));
            return InteractionResult.SUCCESS;
        }
        if (be.state().canAccept() && PrimaRecipes.crushFor(serverLevel.recipeAccess(), stack) != null) {
            if (be.state().offer(Transfer.itemId(stack))) {
                Transfer.consume(player, stack);
                be.changed();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel)) {
            return player.isShiftKeyDown() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof AutoHammerBlockEntity be && !be.hammer().isEmpty()) {
            Transfer.giveBack(player, be.swapHammer(ItemStack.EMPTY));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
