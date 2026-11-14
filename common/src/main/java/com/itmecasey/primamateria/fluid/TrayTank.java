package com.itmecasey.primamateria.fluid;

import com.itmecasey.primamateria.crucible.TrayState;
import java.util.function.Function;

/**
 * A casting tray's fluid input. The tray holds no fluid: it takes exactly one cast's worth (usually 100 mB) of a fluid that has a
 * casting recipe, while it is empty, and starts that cast. It never gives fluid back.
 */
public final class TrayTank implements MachineTank {
    /** What a tray takes per cast when the fluid is not known. */
    public static final int CAST_CAPACITY = 100;

    /** One cast: the mB it costs, the item it makes and how many, and the cooling time in ticks. */
    public record Plan(int cost, String result, int count, int coolTicks) {
    }

    private record Snap(int stage, String fluid, String result, int count, int cool, int ticks) {
    }

    private final TrayState state;
    private final Function<String, Plan> plans;

    public TrayTank(TrayState state, Function<String, Plan> plans) {
        this.state = state;
        this.plans = plans;
    }

    @Override
    public String fluid() {
        return "";
    }

    @Override
    public int amount() {
        return 0;
    }

    @Override
    public int capacity(String fluid) {
        Plan plan = fluid == null || fluid.isEmpty() ? null : plans.apply(fluid);
        return plan == null ? CAST_CAPACITY : plan.cost();
    }

    @Override
    public boolean accepts(String fluid) {
        return fluid != null && !fluid.isEmpty() && state.canAccept() && plans.apply(fluid) != null;
    }

    @Override
    public int insert(String fluid, int amount, boolean simulate) {
        if (!accepts(fluid)) {
            return 0;
        }
        Plan plan = plans.apply(fluid);
        if (plan.cost() <= 0 || amount < plan.cost()) {
            return 0;
        }
        if (!simulate && !state.fill(fluid, plan.result(), plan.count(), plan.coolTicks())) {
            return 0;
        }
        return plan.cost();
    }

    @Override
    public int extract(String fluid, int max, boolean simulate) {
        return 0;
    }

    @Override
    public Object snapshot() {
        return new Snap(state.stage().ordinal(), state.fluid(), state.result(), state.count(), state.cool(), state.ticks());
    }

    @Override
    public void restore(Object snapshot) {
        Snap s = (Snap) snapshot;
        state.restore(s.stage(), s.fluid(), s.result(), s.count(), s.cool(), s.ticks());
    }
}
