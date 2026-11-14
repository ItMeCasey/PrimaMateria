package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.fluid.FluidIds;
import com.itmecasey.primamateria.fluid.FluidTanks;
import com.itmecasey.primamateria.fluid.MachineTank;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The fluid of a barrel, crucible or casting tray as NeoForge's fluid handler: one slot, rolled back with the transaction. The
 * machine keeps its fluid as an id and an amount; this only translates, and every committed change goes through the machine's own
 * {@code changed()} so the world saves it and the clients see it.
 */
final class MachineFluidHandler extends SnapshotJournal<Object> implements ResourceHandler<FluidResource> {
    private final BlockEntity be;
    private final MachineTank tank;

    MachineFluidHandler(BlockEntity be, MachineTank tank) {
        this.be = be;
        this.tank = tank;
    }

    /** The handler of one of our machines, or null for any other block entity. */
    static MachineFluidHandler of(BlockEntity be) {
        MachineTank tank = FluidTanks.of(be);
        return tank == null ? null : new MachineFluidHandler(be, tank);
    }

    @Override
    protected Object createSnapshot() {
        return tank.snapshot();
    }

    @Override
    protected void revertToSnapshot(Object snapshot) {
        tank.restore(snapshot);
    }

    @Override
    protected void onRootCommit(Object original) {
        FluidTanks.changed(be);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        return FluidIds.fluidOf(tank.fluid()).map(FluidResource::of).orElse(FluidResource.EMPTY);
    }

    @Override
    public long getAmountAsLong(int index) {
        return tank.amount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return tank.capacity(FluidIds.idOf(resource.getFluid()));
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && tank.accepts(FluidIds.idOf(resource.getFluid()));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        String id = FluidIds.idOf(resource.getFluid());
        int accepted = tank.insert(id, amount, true);
        if (accepted > 0) {
            updateSnapshots(transaction);
            accepted = tank.insert(id, accepted, false);
        }
        return accepted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) {
            return 0;
        }
        String id = FluidIds.idOf(resource.getFluid());
        int taken = tank.extract(id, amount, true);
        if (taken > 0) {
            updateSnapshots(transaction);
            taken = tank.extract(id, taken, false);
        }
        return taken;
    }
}
