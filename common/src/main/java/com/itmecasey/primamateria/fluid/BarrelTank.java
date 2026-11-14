package com.itmecasey.primamateria.fluid;

import com.itmecasey.primamateria.barrel.BarrelState;

/**
 * A barrel's fluid. Fills follow the same rules a bucket does: no other kind while one is inside, nothing while compost is in,
 * hot fluids only in a heatproof barrel. Drains follow the bucket's rule too: nothing while a soak, a steep or a crystal runs, so
 * a pipe can never break a running recipe.
 */
public final class BarrelTank implements MachineTank {
    private record Snap(String fluid, int amount) {
    }

    private final BarrelState state;

    public BarrelTank(BarrelState state) {
        this.state = state;
    }

    @Override
    public String fluid() {
        return state.fluid();
    }

    @Override
    public int amount() {
        return state.amount();
    }

    @Override
    public int capacity(String fluid) {
        return BarrelState.CAPACITY;
    }

    @Override
    public boolean accepts(String fluid) {
        return state.canFill(fluid);
    }

    @Override
    public int insert(String fluid, int amount, boolean simulate) {
        if (amount <= 0 || !state.canFill(fluid)) {
            return 0;
        }
        int accepted = Math.min(amount, BarrelState.CAPACITY - state.amount());
        if (accepted <= 0) {
            return 0;
        }
        return simulate ? accepted : state.fill(fluid, accepted);
    }

    @Override
    public int extract(String fluid, int max, boolean simulate) {
        String held = state.fluid();
        if (held.isEmpty() || max <= 0 || (fluid != null && !fluid.isEmpty() && !fluid.equals(held))) {
            return 0;
        }
        int taken = Math.min(max, state.amount());
        if (!state.canDrain(taken)) {
            return 0;
        }
        if (!simulate) {
            state.drain(taken);
        }
        return taken;
    }

    @Override
    public Object snapshot() {
        return new Snap(state.fluid(), state.amount());
    }

    @Override
    public void restore(Object snapshot) {
        Snap s = (Snap) snapshot;
        state.restoreFluid(s.fluid(), s.amount());
    }
}
