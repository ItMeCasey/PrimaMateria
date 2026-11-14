package com.itmecasey.primamateria.blockentity;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.crucible.TrayState;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.fluid.FluidLook;
import com.itmecasey.primamateria.recipe.CrucibleRecipes;
import com.itmecasey.primamateria.util.Transfer;
import com.itmecasey.primamateria.view.TrayView;
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
 * One casting tray. A crucible against its side (or a bucket in a hand) pours into it, it cools, and then it holds the cast
 * item until a hand or a hopper takes it. It is a one-slot container that only gives items out.
 */
public class CastingTrayBlockEntity extends BlockEntity implements WorldlyContainer {
    private final TrayState state = new TrayState();

    public CastingTrayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModContent.TRAY_BE, pos, blockState);
    }

    public TrayState state() {
        return state;
    }

    /** The picture the renderer and the viewers need. */
    public TrayView view() {
        return new TrayView(state.fluid(), FluidLook.color(state.fluid()), state.stage(), state.progress(), state.result());
    }

    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState now = getBlockState();
            level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
        }
    }

    /** Starts a cast of {@code count} items (a crucible pours one, a bucket pours as many as it holds). */
    public boolean pour(String fluid, CrucibleRecipes.Cast cast, int count) {
        boolean ok = state.fill(fluid, cast.result(), count, cast.time());
        if (ok) {
            changed();
        }
        return ok;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, CastingTrayBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tick(serverLevel);
        }
    }

    /** One server tick of cooling. Public so tests can run it directly. */
    public void tick(ServerLevel serverLevel) {
        if (state.stage() == TrayView.Stage.EMPTY || state.done()) {
            return;
        }
        TrayView.Stage before = state.stage();
        TrayState.Cast cast = state.tick();
        if (cast != null) {
            GameEvents.CAST.fire(new GameEvents.Cast(
                    new GameEvents.Where(serverLevel, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), null),
                    cast.fluid(), cast.result(), cast.count()));
            serverLevel.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
            changed();
        } else if (state.stage() != before) {
            changed();
        }
    }

    /** What breaking the tray drops besides itself: the finished cast. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        super.preRemoveSideEffects(pos, blockState);
        if (level instanceof ServerLevel serverLevel && state.done()) {
            ItemStack stack = Transfer.stackOf(state.result(), state.count());
            if (!stack.isEmpty()) {
                Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
            state.reset();
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Stage", state.stage().ordinal());
        output.putString("Fluid", state.fluid());
        output.putString("Result", state.result());
        output.putInt("Count", state.count());
        output.putInt("Cool", state.cool());
        output.putInt("Ticks", state.ticks());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        state.restore(input.getIntOr("Stage", 0), input.getStringOr("Fluid", ""), input.getStringOr("Result", ""), input.getIntOr("Count", 0),
                input.getIntOr("Cool", 1), input.getIntOr("Ticks", 0));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- hoppers: items come out, nothing goes in ----

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return !state.done();
    }

    @Override
    public ItemStack getItem(int slot) {
        return state.done() ? Transfer.stackOf(state.result(), state.count()) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!state.done()) {
            return ItemStack.EMPTY;
        }
        String item = state.result();
        int taken = state.removeCount(amount);
        if (taken > 0) {
            changed();
        }
        return Transfer.stackOf(item, taken);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!state.done()) {
            return ItemStack.EMPTY;
        }
        String item = state.result();
        int taken = state.removeCount(state.count());
        return Transfer.stackOf(item, taken);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
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
        return new int[] {0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return state.done();
    }
}
