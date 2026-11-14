package com.itmecasey.primamateria.blockentity;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.drive.DriveState;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.machine.Drivable;
import com.itmecasey.primamateria.util.Transfer;
import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.DriveView;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * One drive: a windup, furnace or powered drive. It sits under a {@link Drivable} machine and shakes it. It is also a container:
 * slots 0 to 8 are the output buffer the machine above delivers into (hoppers and pipes empty it from the bottom and the sides),
 * and on the furnace drive slot 9 is the fuel slot (fed by hand or by a hopper from the sides).
 */
public class DriveBlockEntity extends BlockEntity implements com.itmecasey.primamateria.view.ViewProvider, com.itmecasey.primamateria.anim.MachineViewSource, WorldlyContainer {
    public static final int BUFFER = DriveView.BUFFER_SLOTS;
    public static final int FUEL_SLOT = BUFFER;
    /** Fewest ticks between two turns of the crank by hand: a held use repeats every 4 ticks. */
    public static final int WIND_GUARD_TICKS = 3;
    /** While nothing is above to work, the drive looks again only every this many ticks. */
    private static final int IDLE_CHECK_TICKS = 5;

    private final DriveKind kind;
    private final DriveState state;
    private final ItemStack[] slots;
    private long lastShakeTick = Long.MIN_VALUE / 2;
    private long lastWindTick = Long.MIN_VALUE / 2;
    private boolean running;
    private boolean blocked;
    private int idleWait;

    public DriveBlockEntity(DriveKind kind, net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.kind = kind;
        int ticks = switch (kind) {
            case WINDUP -> Settings.getInt("drive.windup_ticks_per_shake");
            case FURNACE -> Settings.getInt("drive.furnace_ticks_per_shake");
            default -> Settings.getInt("drive.powered_ticks_per_shake");
        };
        this.state = new DriveState(kind, ticks, Settings.getInt("drive.windup_max_turns"), Settings.getInt("drive.powered_capacity"),
                Settings.getInt("drive.powered_fe_per_shake"));
        this.slots = new ItemStack[kind == DriveKind.FURNACE ? BUFFER + 1 : BUFFER];
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
    }

    public DriveKind kind() {
        return kind;
    }

    public DriveState state() {
        return state;
    }

    /** The picture the renderer and the viewers need. */
    public DriveView view() {
        return new DriveView(kind, state.charge(), running, blocked, lastShakeTick);
    }

    // ---- working ----

    private Drivable machineAbove(Level level) {
        return level.getBlockEntity(worldPosition.above()) instanceof Drivable machine ? machine : null;
    }

    /** Empty slots of the output buffer. */
    public int freeSlots() {
        int free = 0;
        for (int i = 0; i < BUFFER; i++) {
            if (slots[i].isEmpty()) {
                free++;
            }
        }
        return free;
    }

    /** Server tick, called by the block's ticker. */
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, DriveBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickOnce(serverLevel);
        }
    }

    /**
     * One game tick of the drive (public so a test can run it in a loop without game time passing). A drive with nothing to work
     * looks again only every few ticks; one that is running or burning is checked every tick.
     */
    public void tickOnce(ServerLevel level) {
        if (!running && !state.burning() && idleWait > 0) {
            idleWait--;
            return;
        }
        Drivable machine = machineAbove(level);
        boolean redstone = level.hasNeighborSignal(worldPosition);
        blocked = DriveState.blocked(freeSlots());
        boolean work = machine != null && !redstone && !blocked && machine.hasWork(level);
        if (kind == DriveKind.FURNACE && work && !state.burning()) {
            igniteFromFuel(level);
        }
        boolean wasRunning = running;
        running = work && state.hasPower();
        if (!running && !state.burning() && !work) {
            idleWait = IDLE_CHECK_TICKS;
        }
        if (state.tick(work)) {
            lastShakeTick = level.getGameTime();
            machine.drive(level, 0);
            setChanged();
            refreshBlockState(level);
            syncToClients(level);
        } else if (wasRunning != running) {
            syncToClients(level);
        }
        if (kind == DriveKind.FURNACE) {
            boolean lit = state.burning();
            BlockState current = getBlockState();
            if (current.getValue(StateProps.LIT) != lit) {
                level.setBlock(worldPosition, current.setValue(StateProps.LIT, lit), Block.UPDATE_ALL);
            }
        }
    }

    private void igniteFromFuel(ServerLevel level) {
        ItemStack fuel = slots[FUEL_SLOT];
        if (fuel.isEmpty()) {
            return;
        }
        int ticks = burnTime(level, fuel);
        if (ticks > 0 && state.ignite(ticks)) {
            fuel.shrink(1);
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.2F);
        }
    }

    /** Copies the energy step to the block state (powered drive). */
    private void refreshBlockState(ServerLevel level) {
        if (kind == DriveKind.POWERED) {
            BlockState current = getBlockState();
            int step = state.chargeStep();
            if (current.getValue(StateProps.CHARGE) != step) {
                level.setBlock(worldPosition, current.setValue(StateProps.CHARGE, step), Block.UPDATE_ALL);
            }
        }
    }

    private void syncToClients(ServerLevel level) {
        BlockState now = getBlockState();
        level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
    }

    // ---- winding, fuel, energy ----

    /**
     * One turn of the crank by a player (or {@code null} in a test). Returns whether a turn was stored. At most one turn per
     * {@link #WIND_GUARD_TICKS} game ticks, so holding the use button winds at the rate the game repeats it.
     */
    public boolean wind(ServerLevel level, @Nullable Player player, boolean ignoreGuard) {
        long now = level.getGameTime();
        if (!ignoreGuard && now - lastWindTick < WIND_GUARD_TICKS) {
            return false;
        }
        if (state.wind(1) == 0) {
            return false;
        }
        lastWindTick = now;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 0.8F + 0.6F * state.charge());
        GameEvents.DRIVE_WOUND.fire(new GameEvents.DriveWound(
                new GameEvents.Where(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), player), state.turns()));
        syncToClients(level);
        return true;
    }

    /** Energy from a cable or another mod (the capability of each loader calls this). Returns how much was taken. */
    public int receiveEnergy(int amount, boolean simulate) {
        int taken = state.receive(amount, simulate);
        if (taken > 0 && !simulate) {
            setChanged();
            if (level instanceof ServerLevel serverLevel) {
                refreshBlockState(serverLevel);
            }
        }
        return taken;
    }

    /**
     * How many ticks an item burns. This is the one place that differs between Minecraft versions (26.2 has a fuel table on the
     * level, 26.3 a cooking fuel component on the item), so the sync script replaces the body for 26.3.
     */
    private static int burnTime(Level level, ItemStack stack) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return 0;
        }
        net.minecraft.world.level.storage.loot.LootContext context = new net.minecraft.world.level.storage.loot.LootContext.Builder(
                new net.minecraft.world.level.storage.loot.LootParams.Builder(serverLevel)
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY))
                .create(java.util.Optional.empty());
        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(stack,
                net.minecraft.core.component.DataComponents.COOKING_FUEL,
                net.minecraft.world.item.component.CookingFuel::burnTime, context, 0);
    }

    /** True when the stack can be burned here: it has a burn time and leaves nothing behind (a lava bucket would). */
    private boolean isFuel(ItemStack stack) {
        return level != null && !stack.isEmpty() && !stack.is(Items.LAVA_BUCKET) && burnTime(level, stack) > 0;
    }

    /** Puts as much of the stack in the fuel slot as fits; returns how many were taken. */
    public int insertFuel(ItemStack stack) {
        if (kind != DriveKind.FURNACE || !isFuel(stack)) {
            return 0;
        }
        ItemStack fuel = slots[FUEL_SLOT];
        if (fuel.isEmpty()) {
            int n = Math.min(stack.getCount(), stack.getMaxStackSize());
            slots[FUEL_SLOT] = stack.copyWithCount(n);
            setChanged();
            return n;
        }
        if (!ItemStack.isSameItemSameComponents(fuel, stack)) {
            return 0;
        }
        int n = Math.min(stack.getCount(), fuel.getMaxStackSize() - fuel.getCount());
        if (n > 0) {
            fuel.grow(n);
            setChanged();
        }
        return Math.max(0, n);
    }

    /** What a player takes out of the output buffer: every stack in it. */
    public List<ItemStack> takeBuffer() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < BUFFER; i++) {
            if (!slots[i].isEmpty()) {
                out.add(slots[i]);
                slots[i] = ItemStack.EMPTY;
            }
        }
        if (!out.isEmpty()) {
            setChanged();
        }
        return out;
    }

    /** Gives the buffer to a player. */
    public void handBufferTo(Player player) {
        for (ItemStack stack : takeBuffer()) {
            Transfer.giveBack(player, stack);
        }
    }

    /** What breaking the drive drops besides the drive itself: the buffer and the fuel. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState blockState) {
        super.preRemoveSideEffects(pos, blockState);
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < slots.length; i++) {
                if (!slots[i].isEmpty()) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, slots[i]);
                    slots[i] = ItemStack.EMPTY;
                }
            }
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Turns", state.turns());
        output.putInt("BurnLeft", state.burnLeft());
        output.putInt("BurnTotal", state.burnTotal());
        output.putInt("Energy", state.energy());
        output.putInt("Cooldown", state.cooldown());
        output.putLong("LastShake", lastShakeTick);
        ValueOutput.ValueOutputList list = output.childrenList("Slots");
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].isEmpty()) {
                ValueOutput entry = list.addChild();
                entry.putInt("Slot", i);
                entry.store("Item", ItemStack.CODEC, slots[i]);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        state.restore(input.getIntOr("Turns", 0), input.getIntOr("BurnLeft", 0), input.getIntOr("BurnTotal", 0), input.getIntOr("Energy", 0),
                input.getIntOr("Cooldown", 0));
        lastShakeTick = input.getLongOr("LastShake", Long.MIN_VALUE / 2);
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        for (ValueInput entry : input.childrenListOrEmpty("Slots")) {
            int slot = entry.getIntOr("Slot", -1);
            if (slot >= 0 && slot < slots.length) {
                slots[slot] = entry.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- container: the output buffer and the fuel slot ----

    @Override
    public int getContainerSize() {
        return slots.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < slots.length ? slots[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= slots.length || slots[slot].isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = slots[slot].split(amount);
        setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= slots.length) {
            return ItemStack.EMPTY;
        }
        ItemStack out = slots[slot];
        slots[slot] = ItemStack.EMPTY;
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < slots.length) {
            slots[slot] = stack;
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= slots.length) {
            return false;
        }
        return slot < BUFFER || isFuel(stack);
    }

    @Override
    public void clearContent() {
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    /**
     * The top face is where the machine above delivers its results: buffer slots only. A side feeds fuel into the furnace drive's
     * fuel slot and empties the buffer; the bottom only empties the buffer.
     */
    @Override
    public int[] getSlotsForFace(Direction face) {
        if (face == Direction.UP) {
            return range(0, BUFFER);
        }
        if (face == Direction.DOWN || kind != DriveKind.FURNACE) {
            return range(0, BUFFER);
        }
        return range(0, BUFFER + 1);
    }

    private static int[] range(int from, int to) {
        int[] out = new int[to - from];
        for (int i = 0; i < out.length; i++) {
            out[i] = from + i;
        }
        return out;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (slot < BUFFER) {
            return face == Direction.UP;
        }
        return face != Direction.UP && face != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return slot < BUFFER && face != Direction.UP;
    }

    // ---- for tests ----

    /** Direct access to the fuel slot and buffer for the self test. */
    public ItemStack[] slotsForTest() {
        return slots;
    }

    public boolean running() {
        return running;
    }

    public boolean blocked() {
        return blocked;
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
