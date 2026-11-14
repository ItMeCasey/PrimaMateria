package com.itmecasey.primamateria.blockentity;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.barrel.BarrelLogic;
import com.itmecasey.primamateria.barrel.BarrelState;
import com.itmecasey.primamateria.block.BarrelBlock;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.sift.Drop;
import com.itmecasey.primamateria.util.Transfer;
import com.itmecasey.primamateria.view.BarrelView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The data of one barrel (wooden or clay): its fluid or compost, what it is working on and what waits to be taken. It is also a
 * two-slot container for hoppers: slot 0 takes one item from above or the side (the slot always reads empty; the item is used the
 * moment it is offered), slot 1 shows the first finished result and can be taken from below.
 */
public class BarrelBlockEntity extends BlockEntity implements WorldlyContainer {
    private final BarrelState state;
    private long lastTick = -1;
    private boolean dirty;

    public BarrelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModContent.BARREL_BE, pos, blockState);
        this.state = new BarrelState(blockState.getBlock() instanceof BarrelBlock b && b.heatproof());
    }

    public BarrelState state() {
        return state;
    }

    /** The picture the renderer and the viewers need. */
    public BarrelView view() {
        Drop result = state.peekResult();
        boolean compost = state.contents() == BarrelView.Contents.COMPOST;
        return new BarrelView(state.heatproof(), state.contents(), state.fluid(), compost ? state.compost() : state.amount(),
                state.color(), state.stage(), state.stageProgress(), state.soakItem(), result == null ? "" : result.item(),
                state.crystalViews());
    }

    /** Saves and sends the new data to nearby players. */
    public void changed() {
        setChanged();
        dirty = false;
        if (level != null && !level.isClientSide()) {
            BlockState now = getBlockState();
            level.sendBlockUpdated(worldPosition, now, now, 3);
            level.updateNeighbourForOutputSignal(worldPosition, now.getBlock());
        }
    }

    /** A change that need not reach players at once (dew, a hopper taking a result): sent with the next periodic update. */
    private void lazyChanged() {
        setChanged();
        dirty = true;
    }

    /** Fires the finished-recipe event for each recipe that just finished. */
    public void fire(ServerLevel serverLevel, java.util.List<BarrelState.Finished> done) {
        for (BarrelState.Finished f : done) {
            BlockPos p = getBlockPos();
            GameEvents.BARREL_FINISHED.fire(new GameEvents.BarrelFinished(new GameEvents.Where(serverLevel, p.getX(), p.getY(), p.getZ(), null),
                    f.family(), f.recipeId(), f.result(), f.count()));
        }
    }

    /** Server tick. A working barrel ticks every tick; an idle one only looks for dew every 8 ticks. */
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, BarrelBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = level.getGameTime();
        boolean busy = be.state.busy();
        if (!busy && (now & 7L) != 0L) {
            return;
        }
        int dt = be.lastTick < 0 ? 1 : (int) Math.max(1, Math.min(now - be.lastTick, 1000));
        be.lastTick = now;
        boolean changed = false;
        if (busy) {
            java.util.List<BarrelState.Finished> done = be.state.tick(dt);
            if (!done.isEmpty()) {
                be.fire(serverLevel, done);
                changed = true;
            }
        }
        be.dewTick(serverLevel, dt);
        if (changed) {
            be.changed();
        } else if (be.dirty && (now % 20L == 0L || (now & 7L) == 0L && !busy)) {
            be.changed();
        } else if (busy && now % 20L == 0L) {
            be.changed();
        }
    }

    /** Rain and dew for {@code dt} ticks: a barrel with open sky above it gains water. Returns the mB that came in. */
    public int dewTick(ServerLevel serverLevel, int dt) {
        int perMb = Settings.getInt("barrel.dew_ticks_per_mb");
        BlockPos above = worldPosition.above();
        if (perMb <= 0 || state.amount() >= BarrelState.CAPACITY || !openSky(serverLevel, above)) {
            return 0;
        }
        int added = state.dew(dt, perMb, serverLevel.isRainingAt(above), Settings.getInt("barrel.rain_multiplier"));
        if (added > 0) {
            lazyChanged();
        }
        return added;
    }

    /** True when nothing stops rain and dew from reaching {@code above} (a roof, a ceiling, a dimension without sky). */
    private static boolean openSky(ServerLevel serverLevel, BlockPos above) {
        return serverLevel.dimensionType().hasSkyLight()
                && serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, above.getX(), above.getZ()) <= above.getY();
    }

    /** What breaking the barrel drops: the finished results (the contents are lost, like a broken cauldron). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        super.preRemoveSideEffects(pos, blockState);
        if (level instanceof ServerLevel serverLevel) {
            Drop d;
            while ((d = state.takeResult()) != null) {
                for (ItemStack stack : Transfer.split(d)) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Barrel", state.save());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        state.load(input.getStringOr("Barrel", ""));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- hoppers ----

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return !state.hasResult();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot != 1) {
            return ItemStack.EMPTY;
        }
        Drop d = state.peekResult();
        if (d == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = Transfer.stackOf(d.item(), 1);
        if (stack.isEmpty()) {
            return stack;
        }
        stack.setCount(Math.min(d.count(), stack.getMaxStackSize()));
        return stack;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 1) {
            return ItemStack.EMPTY;
        }
        Drop d = state.takeResult(amount);
        if (d == null) {
            return ItemStack.EMPTY;
        }
        lazyChanged();
        return Transfer.stackOf(d.item(), d.count());
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 64);
    }

    /** A hopper offers an item: it is used at once. */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0 && level instanceof ServerLevel serverLevel) {
            BarrelLogic.offer(serverLevel, this, stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getCount() == 1 && level instanceof ServerLevel serverLevel && BarrelLogic.accepts(serverLevel, this, stack);
    }

    @Override
    public void clearContent() {
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return face == Direction.DOWN ? new int[] {1} : new int[] {0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return slot == 0 && face != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return slot == 1 && face == Direction.DOWN;
    }
}
