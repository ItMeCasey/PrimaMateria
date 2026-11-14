package com.itmecasey.primamateria.blockentity;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.crush.AutoHammerState;
import com.itmecasey.primamateria.hammer.HammerLogic;
import com.itmecasey.primamateria.item.HammerItem;
import com.itmecasey.primamateria.machine.Drivable;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.sift.Drop;
import com.itmecasey.primamateria.sift.SiftMath;
import com.itmecasey.primamateria.util.Transfer;
import com.itmecasey.primamateria.view.AutoHammerView;
import com.itmecasey.primamateria.view.DriveKind;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * An auto hammer: holds one hammer and a queue of up to 16 blocks, and when a drive under it works it, crushes the oldest block
 * in {@code auto_hammer.strokes_per_block} strokes. The results go into the container below (the drive). A hopper can offer blocks
 * from above and the sides; the slot always reads empty, a block is taken the moment it is offered.
 */
public class AutoHammerBlockEntity extends BlockEntity implements com.itmecasey.primamateria.view.ViewProvider, com.itmecasey.primamateria.anim.MachineViewSource, WorldlyContainer, Drivable {
    private final AutoHammerState state = new AutoHammerState();
    private ItemStack hammer = ItemStack.EMPTY;

    public AutoHammerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModContent.AUTO_HAMMER_BE, pos, blockState);
    }

    public AutoHammerState state() {
        return state;
    }

    public ItemStack hammer() {
        return hammer;
    }

    /** Fits a hammer (or removes it with an empty stack) and returns the one that was fitted before. */
    public ItemStack swapHammer(ItemStack next) {
        ItemStack old = hammer;
        hammer = next;
        changed();
        return old;
    }

    /** The picture the renderer and the viewers need. */
    public AutoHammerView view() {
        return new AutoHammerView(hammer.isEmpty() ? "" : Transfer.itemId(hammer), state.next(), state.queued(), state.progress(),
                DriveKind.NONE, state.lastStrokeTick());
    }

    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState now = getBlockState();
            level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
        }
    }

    private void setMoving(boolean moving) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState old = getBlockState();
        if (old.getValue(StateProps.MOVING) != moving) {
            level.setBlock(worldPosition, old.setValue(StateProps.MOVING, moving), Block.UPDATE_ALL);
        }
    }

    /** Server tick, only while the block state says the machine is moving: clears the flag after it has been still for a while. */
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, AutoHammerBlockEntity be) {
        if (level.getGameTime() - be.state.lastStrokeTick() > SiftMath.MOVING_HOLD_TICKS) {
            be.setMoving(false);
        }
    }

    // ---- drives ----

    private boolean tierAllows(ServerLevel serverLevel, String blockId) {
        PrimaRecipes.Crush crush = PrimaRecipes.crushFor(serverLevel.recipeAccess(), Transfer.stackOf(blockId, 1));
        return crush != null && HammerItem.tierOf(hammer) >= crush.hammerTier();
    }

    /** The output below must have a few free slots (or there is no container and results fall to the ground). */
    private static boolean outputHasRoom(ServerLevel serverLevel, BlockPos pos) {
        Container below = Transfer.below(serverLevel, pos);
        if (below == null) {
            return true;
        }
        int free = 0;
        for (int i = 0; i < below.getContainerSize(); i++) {
            if (below.getItem(i).isEmpty()) {
                free++;
            }
        }
        return free >= 3;
    }

    @Override
    public boolean hasWork(ServerLevel serverLevel) {
        return !hammer.isEmpty() && state.hasBlock() && tierAllows(serverLevel, state.next()) && outputHasRoom(serverLevel, worldPosition);
    }

    @Override
    public boolean drive(ServerLevel serverLevel, int minTicks) {
        long now = serverLevel.getGameTime();
        if (!hasWork(serverLevel) || !state.ready(now, minTicks)) {
            return false;
        }
        String finished = state.stroke(now, Settings.getInt("auto_hammer.strokes_per_block"));
        setMoving(true);
        serverLevel.playSound(null, worldPosition, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.5F, 0.8F + serverLevel.getRandom().nextFloat() * 0.3F);
        if (finished != null) {
            List<Drop> drops = HammerLogic.crush(serverLevel, worldPosition, Transfer.stackOf(finished, 1), Transfer.itemId(hammer),
                    HammerItem.tierOf(hammer), true, null);
            if (drops != null) {
                Transfer.deliver(serverLevel, worldPosition, Transfer.below(serverLevel, worldPosition), null, drops);
            }
            hammer.hurtAndBreak(1, serverLevel, null, item -> { });
            if (hammer.isEmpty()) {
                hammer = ItemStack.EMPTY;
            }
        }
        changed();
        return true;
    }

    /** What breaking the machine drops besides itself: the hammer and the queued blocks. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        super.preRemoveSideEffects(pos, blockState);
        if (level instanceof ServerLevel serverLevel) {
            if (!hammer.isEmpty()) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, hammer);
                hammer = ItemStack.EMPTY;
            }
            for (String id : state.contents()) {
                ItemStack stack = Transfer.stackOf(id, 1);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
            state.clear();
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Hammer", ItemStack.OPTIONAL_CODEC, hammer);
        output.putString("Queue", state.encodeQueue());
        output.putString("Current", state.current());
        output.putInt("Strokes", state.strokes());
        output.putInt("Needed", state.needed());
        output.putLong("LastStroke", state.lastStrokeTick());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hammer = input.read("Hammer", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        state.restore(input.getStringOr("Queue", ""), input.getStringOr("Current", ""), input.getIntOr("Strokes", 0),
                input.getIntOr("Needed", 1), input.getLongOr("LastStroke", Long.MIN_VALUE / 2));
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
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    /** A hopper offers a block: it joins the queue. */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (level instanceof ServerLevel && canPlaceItem(slot, stack) && state.offer(Transfer.itemId(stack))) {
            changed();
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getCount() == 1 && state.canAccept() && level instanceof ServerLevel serverLevel
                && PrimaRecipes.crushFor(serverLevel.recipeAccess(), stack) != null;
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
        return face == Direction.DOWN ? new int[0] : new int[] {0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return face != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return false;
    }

    // ---- renderer access ----

    private Object machineViewCache;
    private long machineViewTick = Long.MIN_VALUE;
    private Object renderCacheField;

    /** The view record the machine renderer reads: rebuilt at most once per game tick, never per frame. */
    public Object machineView() {
        long now = level == null ? 0L : level.getGameTime();
        if (machineViewCache == null || now != machineViewTick) {
            machineViewCache = view();
            machineViewTick = now;
        }
        return machineViewCache;
    }

    public Object renderCache() {
        return renderCacheField;
    }

    public void renderCache(Object cache) {
        this.renderCacheField = cache;
    }
}
