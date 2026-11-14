package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.fluid.FluidIds;
import com.itmecasey.primamateria.fluid.FluidTanks;
import com.itmecasey.primamateria.fluid.MachineTank;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The fluid of a barrel, crucible or casting tray as Forge's fluid handler: one tank. The machine keeps its fluid as an id and an
 * amount; this only translates, and every executed change goes through the machine's own {@code changed()} so the world saves it and
 * the clients see it. Simulated calls change nothing.
 */
final class MachineFluidHandler implements IFluidHandler {
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
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int index) {
        Fluid fluid = FluidIds.fluidOf(tank.fluid()).orElse(null);
        return fluid == null || tank.amount() <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, tank.amount());
    }

    @Override
    public int getTankCapacity(int index) {
        return tank.capacity(tank.fluid());
    }

    @Override
    public boolean isFluidValid(int index, @NotNull FluidStack stack) {
        return !stack.isEmpty() && tank.accepts(FluidIds.idOf(stack.getFluid()));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        String id = FluidIds.idOf(resource.getFluid());
        int accepted = tank.insert(id, resource.getAmount(), true);
        if (accepted > 0 && action.execute()) {
            accepted = tank.insert(id, accepted, false);
            FluidTanks.changed(be);
        }
        return accepted;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return take(FluidIds.idOf(resource.getFluid()), resource.getAmount(), action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return take("", maxDrain, action);
    }

    private FluidStack take(String id, int max, FluidAction action) {
        String held = tank.fluid();
        int taken = tank.extract(id, max, true);
        if (taken <= 0) {
            return FluidStack.EMPTY;
        }
        if (action.execute()) {
            taken = tank.extract(id, taken, false);
            FluidTanks.changed(be);
        }
        Fluid fluid = FluidIds.fluidOf(held).orElse(null);
        return fluid == null || taken <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, taken);
    }
}
