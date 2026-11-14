package com.itmecasey.primamateria.blockentity;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.data.Meshes;
import com.itmecasey.primamateria.sieve.SieveLogic;
import com.itmecasey.primamateria.sift.SiftMath;
import com.itmecasey.primamateria.sift.SiftState;
import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.SieveView;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The data of one sieve: its mesh, the block being sifted, the drops rolled for it and the shake count. It is also a one-slot
 * container so hoppers can drop a block in from above and the side (the slot always reads empty; a block is taken the moment it
 * is offered). Drops leave through whatever container is below, or hop onto the ground.
 */
public class SieveBlockEntity extends BlockEntity implements WorldlyContainer {
    private final SiftState state = new SiftState();

    public SieveBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModContent.SIEVE_BE, pos, blockState);
    }

    public SiftState state() {
        return state;
    }

    /** The picture the renderer and the viewers need. */
    public SieveView view() {
        Meshes.Mesh mesh = Meshes.get(state.meshId());
        return new SieveView(state.meshId(), mesh == null ? "" : mesh.texture(), state.block(), state.progress(), state.shakes(),
                state.lastShakeTick(), DriveKind.HAND, false, state.seed(), SiftMath.finds(state.drops(), state.seed()));
    }

    /** The resting model slot of the fitted mesh (0 when there is none). */
    private int meshSlot() {
        Meshes.Mesh mesh = Meshes.get(state.meshId());
        return mesh == null ? 0 : mesh.slot();
    }

    /** Layer height in quarters for the no-animation fallback: 4 when just filled, 1 when almost done, 0 when empty. */
    private int fill() {
        return state.filled() ? Math.max(1, 4 - (int) Math.floor(state.progress() * 4F)) : 0;
    }

    /** Saves, copies the mesh slot and layer height to the block state, and sends the new data to nearby players. */
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState old = getBlockState();
            BlockState now = old.setValue(StateProps.MESH, meshSlot()).setValue(StateProps.FILL, fill());
            if (now != old) {
                level.setBlock(worldPosition, now, Block.UPDATE_ALL);
            }
            level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
        }
    }

    /** Switches the moving-parts flag of the block state (the renderer draws the tray only while it is set). */
    public void setMoving(boolean moving) {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState old = getBlockState();
        if (old.getValue(StateProps.MOVING) != moving) {
            level.setBlock(worldPosition, old.setValue(StateProps.MOVING, moving), Block.UPDATE_ALL);
        }
    }

    /** Server tick, only while the block state says the sieve is moving: clears the flag once it has been still for a while. */
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, SieveBlockEntity be) {
        if (level.getGameTime() - be.state.lastShakeTick() > SiftMath.MOVING_HOLD_TICKS) {
            be.setMoving(false);
        }
    }

    /** What breaking the sieve drops besides the sieve itself: the mesh and the unsifted block. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        super.preRemoveSideEffects(pos, blockState);
        if (level instanceof ServerLevel serverLevel) {
            List<ItemStack> stacks = new ArrayList<>();
            if (state.hasMesh()) {
                stacks.add(SieveLogic.stackOf(state.meshId(), 1));
            }
            if (state.filled()) {
                stacks.add(SieveLogic.stackOf(state.block(), 1));
            }
            for (ItemStack stack : stacks) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
            state.clearBlock();
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Mesh", state.meshId());
        output.putInt("MeshTier", state.meshTier());
        output.putDouble("MeshSpeed", state.meshSpeed());
        output.putString("Block", state.block());
        output.putInt("Shakes", state.shakes());
        output.putInt("Needed", state.needed());
        output.putLong("Seed", state.seed());
        output.putString("Drops", SiftState.encode(state.drops()));
        output.putString("Residue", SiftState.encode(state.residue()));
        output.putLong("LastShake", state.lastShakeTick());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        state.restore(input.getStringOr("Mesh", ""), input.getIntOr("MeshTier", 0), input.getDoubleOr("MeshSpeed", 1.0),
                input.getStringOr("Block", ""), input.getIntOr("Shakes", 0), input.getIntOr("Needed", 1), input.getLongOr("Seed", 0L),
                input.getStringOr("Drops", ""), input.getStringOr("Residue", ""), input.getLongOr("LastShake", Long.MIN_VALUE / 2));
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

    /** A hopper offers a block: it goes straight into the tray. */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (level instanceof ServerLevel serverLevel && canPlaceItem(slot, stack)) {
            SieveLogic.insert(serverLevel, this, stack, null);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getCount() == 1 && level instanceof ServerLevel serverLevel && state.canAccept()
                && SieveLogic.siftable(serverLevel, stack);
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
}
