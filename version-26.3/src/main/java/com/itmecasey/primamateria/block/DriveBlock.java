package com.itmecasey.primamateria.block;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.ModShapes;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.blockentity.DriveBlockEntity;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.util.Transfer;
import com.itmecasey.primamateria.view.DriveKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
 * The windup, furnace and powered drives (one class, three kinds). A drive sits under a sieve or an auto hammer and shakes it.
 * Windup: hold use to crank. Furnace: use fuel on it, or feed the side from a hopper. Powered: feed it energy. An empty hand takes
 * what is in the output buffer (on the windup drive, sneak with an empty hand, because an empty hand cranks).
 */
public class DriveBlock extends BaseEntityBlock {
    private final DriveKind kind;

    public DriveBlock(Properties properties, DriveKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(StateProps.FACING, Direction.NORTH).setValue(StateProps.LIT, false)
                .setValue(StateProps.CHARGE, 0));
    }

    public DriveKind kind() {
        return kind;
    }

    /** Every drive carries all three properties; each kind uses the ones that mean something for it. */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(StateProps.FACING, StateProps.LIT, StateProps.CHARGE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(StateProps.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (kind) {
            case WINDUP -> ModShapes.WINDUP_DRIVE;
            case FURNACE -> ModShapes.FURNACE_DRIVE;
            default -> ModShapes.POWERED_DRIVE;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DriveBlockEntity(kind, ModContent.driveType(kind), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModContent.driveType(kind), DriveBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (kind != DriveKind.FURNACE) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof DriveBlockEntity be) {
            int taken = be.insertFuel(stack);
            if (taken <= 0) {
                return InteractionResult.PASS;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(taken);
            }
            return InteractionResult.SUCCESS;
        }
        // the client cannot tell fuel from anything else without the fuel map, and a swing on a miss is harmless
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof DriveBlockEntity be)) {
            return InteractionResult.PASS;
        }
        boolean crank = kind == DriveKind.WINDUP && !player.isShiftKeyDown();
        if (crank) {
            be.wind(serverLevel, player, false);
            return InteractionResult.SUCCESS;
        }
        if (!be.isEmpty() && be.freeSlots() < DriveBlockEntity.BUFFER) {
            be.handBufferTo(player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator: how many of the nine buffer slots are in use, 0 to 15. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof DriveBlockEntity be) {
            int used = DriveBlockEntity.BUFFER - be.freeSlots();
            return Math.min(15, (int) Math.ceil(used * 15.0 / DriveBlockEntity.BUFFER));
        }
        return 0;
    }
}
