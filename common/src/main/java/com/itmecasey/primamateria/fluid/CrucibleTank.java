package com.itmecasey.primamateria.fluid;

import com.itmecasey.primamateria.crucible.CrucibleState;
import java.util.function.Predicate;

/**
 * A crucible's fluid: 4000 mB of one kind. What it takes is decided by {@code allowed} (lava, the molten metals, and any fluid
 * with a casting recipe), the same check a bucket gets. A pipe may drain it at any time.
 */
public final class CrucibleTank implements MachineTank {
    private record Snap(String fluid, int amount) {
    }

    private final CrucibleState state;
    private final Predicate<String> allowed;

    public CrucibleTank(CrucibleState state, Predicate<String> allowed) {
        this.state = state;
        this.allowed = allowed;
    }

    @Override
    public String fluid() {
        return state.amount() > 0 ? state.fluid() : "";
    }

    @Override
    public int amount() {
        return state.amount();
    }

    @Override
    public int capacity(String fluid) {
        return CrucibleState.CAPACITY;
    }

    @Override
    public boolean accepts(String fluid) {
        return fluid != null && !fluid.isEmpty() && allowed.test(fluid) && (state.amount() == 0 || state.fluid().equals(fluid));
    }

    @Override
    public int insert(String fluid, int amount, boolean simulate) {
        if (amount <= 0 || !accepts(fluid)) {
            return 0;
        }
        int accepted = Math.min(amount, CrucibleState.CAPACITY - state.amount());
        if (accepted <= 0 || !state.canFill(fluid, accepted)) {
            return 0;
        }
        if (!simulate) {
            state.fill(fluid, accepted);
        }
        return accepted;
    }

    @Override
    public int extract(String fluid, int max, boolean simulate) {
        String held = fluid();
        if (held.isEmpty() || max <= 0 || (fluid != null && !fluid.isEmpty() && !fluid.equals(held))) {
            return 0;
        }
        int taken = Math.min(max, state.amount());
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
