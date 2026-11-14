package com.itmecasey.primamateria.blockentity;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.crucible.CrucibleState;
import com.itmecasey.primamateria.data.HeatSources;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.fluid.FluidLook;
import com.itmecasey.primamateria.recipe.CrucibleRecipes;
import com.itmecasey.primamateria.util.Transfer;
import com.itmecasey.primamateria.view.CrucibleView;
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
 * One crucible: the fluid it holds, the solids waiting to melt, and the heat of the block below. It melts the solids, and pours
 * its fluid into casting trays against its four sides. It is also a one-slot container so hoppers can drop solids in (the slot
 * always reads empty; an item is taken the moment it is offered).
 */
public class CrucibleBlockEntity extends BlockEntity implements com.itmecasey.primamateria.view.ViewProvider, WorldlyContainer {
    private final CrucibleState state = new CrucibleState();
    private int heat;
    private boolean dirty;

    public CrucibleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModContent.CRUCIBLE_BE, pos, blockState);
    }

    public CrucibleState state() {
        return state;
    }

    public int heat() {
        return heat;
    }

    /** The picture the renderer and the viewers need. */
    public CrucibleView view() {
        int pour = 0;
        if (level != null) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (level.getBlockEntity(worldPosition.relative(d)) instanceof CastingTrayBlockEntity tray
                        && tray.state().stage() == TrayView.Stage.FILLING) {
                    pour |= 1 << d.get2DDataValue();
                }
            }
        }
        String first = state.solids().isEmpty() ? "" : state.solids().get(0);
        return new CrucibleView(state.fluid(), state.amount(), FluidLook.color(state.fluid()), FluidLook.glow(state.fluid()),
                state.solidCount(), first, heat, pour);
    }

    /** Saves and sends the new data to nearby players. */
    public void changed() {
        dirty = false;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState now = getBlockState();
            level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, CrucibleBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tick(serverLevel);
        }
    }

    /** One server tick: heat, melting, pouring into trays. Public so tests can run it directly. */
    public void tick(ServerLevel serverLevel) {
        if (state.empty()) {
            heat = 0;
            return;
        }
        heat = HeatSources.heatAt(serverLevel, worldPosition.below());
        if (heat > 0 && state.solidCount() > 0) {
            int before = state.amount();
            for (CrucibleState.Done done : state.tick(heat, Settings.getInt("crucible.mb_per_tick_per_heat"), item -> meltOf(serverLevel, item))) {
                GameEvents.MELTED.fire(new GameEvents.Melted(
                        new GameEvents.Where(serverLevel, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), null),
                        done.input(), done.fluid(), done.amount()));
                serverLevel.playSound(null, worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5F, 1.0F);
                changed();
            }
            if (state.amount() != before) {
                dirty = true;
            }
        }
        pourIntoTrays(serverLevel);
        if (dirty && serverLevel.getGameTime() % 5L == 0L) {
            changed();
        }
    }

    private CrucibleState.Melt meltOf(ServerLevel serverLevel, String item) {
        ItemStack stack = Transfer.stackOf(item, 1);
        return stack.isEmpty() ? null : CrucibleRecipes.meltFor(serverLevel.recipeAccess(), stack);
    }

    /** Pours into every empty tray against a side while the crucible holds enough of a fluid that has a casting recipe. */
    private void pourIntoTrays(ServerLevel serverLevel) {
        if (state.amount() <= 0) {
            return;
        }
        CrucibleRecipes.Cast cast = CrucibleRecipes.castFor(serverLevel.recipeAccess(), state.fluid());
        if (cast == null) {
            return;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (state.amount() < cast.amount()) {
                return;
            }
            if (serverLevel.getBlockEntity(worldPosition.relative(d)) instanceof CastingTrayBlockEntity tray && tray.state().canAccept()) {
                String fluid = state.fluid();
                if (state.drain(cast.amount()) && tray.pour(fluid, cast, 1)) {
                    serverLevel.playSound(null, worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.4F, 1.3F);
                    changed();
                }
            }
        }
    }

    /** What breaking the crucible drops besides itself: the solids inside (the fluid is lost). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        super.preRemoveSideEffects(pos, blockState);
        if (level instanceof ServerLevel serverLevel) {
            for (String item : state.solids()) {
                ItemStack stack = Transfer.stackOf(item, 1);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
            state.restore("", 0, "", -1, 0, "");
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Fluid", state.fluid());
        output.putInt("Amount", state.amount());
        output.putString("Solids", state.encodeSolids());
        output.putInt("MeltLeft", state.meltLeft());
        output.putInt("MeltTotal", state.meltTotal());
        output.putString("MeltFluid", state.meltFluid());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        state.restore(input.getStringOr("Fluid", ""), input.getIntOr("Amount", 0), input.getStringOr("Solids", ""),
                input.getIntOr("MeltLeft", -1), input.getIntOr("MeltTotal", 0), input.getStringOr("MeltFluid", ""));
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

    /** A hopper offers a solid: it goes straight into the buffer. */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (canPlaceItem(slot, stack) && state.addSolid(Transfer.itemId(stack))) {
            changed();
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getCount() == 1 && level instanceof ServerLevel serverLevel && state.canAddSolid()
                && CrucibleRecipes.meltFor(serverLevel.recipeAccess(), stack) != null;
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
