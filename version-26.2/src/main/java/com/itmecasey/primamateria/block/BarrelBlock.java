package com.itmecasey.primamateria.block;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.ModShapes;
import com.itmecasey.primamateria.barrel.BarrelLogic;
import com.itmecasey.primamateria.barrel.BarrelState;
import com.itmecasey.primamateria.blockentity.BarrelBlockEntity;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The wooden and the clay barrel (one class, a flag tells them apart). Everything is done in the world: a bucket fills or
 * empties it, an item is put in by using it, an empty hand takes the finished result. The clay barrel is heatproof: it takes
 * lava and molten metals and does the mixing recipes that need them.
 */
public class BarrelBlock extends BaseEntityBlock {
    private static final MapCodec<BarrelBlock> CODEC = simpleCodec(properties -> new BarrelBlock(properties, false));

    private final boolean heatproof;

    public BarrelBlock(Properties properties, boolean heatproof) {
        super(properties);
        this.heatproof = heatproof;
    }

    public boolean heatproof() {
        return heatproof;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return heatproof ? ModShapes.CLAY_BARREL : ModShapes.WOODEN_BARREL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModContent.BARREL_BE, BarrelBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel) {
            return BarrelLogic.interact(serverLevel, pos, player, hand, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            return BarrelLogic.interact(serverLevel, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator: how full the barrel is (fluid or compost), 0 to 15. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity be) {
            int filled = be.state().compost() > 0 ? be.state().compost() : be.state().amount();
            return filled <= 0 ? 0 : Math.min(15, 1 + filled * 14 / BarrelState.CAPACITY);
        }
        return 0;
    }
}
