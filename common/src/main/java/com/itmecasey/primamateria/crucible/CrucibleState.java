package com.itmecasey.primamateria.crucible;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The contents of one crucible: one fluid (an id and an amount in mB, never more than {@link #CAPACITY}) and a buffer of up to
 * {@link #SOLID_SLOTS} solid items waiting to melt. Pure Java; the block entity owns one and saves it as plain strings.
 *
 * <p>Melting works on the first meltable solid: each tick the heat of the block below turns {@code heat * rate} mB of it into
 * fluid, until its whole amount has melted and it is used up. A solid whose fluid does not match what the crucible holds, or that
 * would overfill it, waits while the others (if they fit) go first.
 */
public final class CrucibleState {
    public static final int CAPACITY = 4000;
    public static final int SOLID_SLOTS = 4;

    /** What melting one solid gives: a fluid id and an amount in mB. */
    public record Melt(String fluid, int amount) {
    }

    /** One solid that finished melting. */
    public record Done(String input, String fluid, int amount) {
    }

    private String fluid = "";
    private int amount;
    private final List<String> solids = new ArrayList<>();
    /** mB of the first solid still to melt, or -1 when it has not started. */
    private int meltLeft = -1;
    private int meltTotal;
    private String meltFluid = "";

    public String fluid() {
        return fluid;
    }

    public int amount() {
        return amount;
    }

    public List<String> solids() {
        return List.copyOf(solids);
    }

    public int solidCount() {
        return solids.size();
    }

    public boolean empty() {
        return amount == 0 && solids.isEmpty();
    }

    /** Share of the first solid that has melted, 0 to 1. */
    public float meltProgress() {
        return meltLeft < 0 || meltTotal <= 0 ? 0F : 1F - meltLeft / (float) meltTotal;
    }

    // ---- solids ----

    public boolean canAddSolid() {
        return solids.size() < SOLID_SLOTS;
    }

    public boolean addSolid(String item) {
        if (item == null || item.isEmpty() || !canAddSolid()) {
            return false;
        }
        solids.add(item);
        return true;
    }

    /**
     * Takes the last solid back out. The solid that is partly melted cannot be taken (its progress would be lost).
     * Returns the item id, or null when there is nothing to take.
     */
    public String removeLastSolid() {
        if (solids.isEmpty() || (solids.size() == 1 && meltLeft >= 0)) {
            return null;
        }
        return solids.remove(solids.size() - 1);
    }

    // ---- fluid ----

    /** True when {@code mb} of this fluid fit in: the crucible is empty of fluid or holds the same one, and there is room. */
    public boolean canFill(String fluidId, int mb) {
        return mb > 0 && (amount == 0 || fluid.equals(fluidId)) && amount + mb <= CAPACITY;
    }

    /** Adds the fluid only if all of it fits (a bucket is all or nothing). Returns whether it did. */
    public boolean fill(String fluidId, int mb) {
        if (!canFill(fluidId, mb)) {
            return false;
        }
        fluid = fluidId;
        amount += mb;
        return true;
    }

    /** Takes {@code mb} out only if that much is there. Returns whether it did. */
    public boolean drain(int mb) {
        if (mb <= 0 || amount < mb) {
            return false;
        }
        amount -= mb;
        if (amount == 0) {
            fluid = "";
        }
        return true;
    }

    // ---- melting ----

    /**
     * One tick of melting. {@code lookup} gives the melt of a solid item id (null when it cannot melt). Returns the solids that
     * finished this tick (at most one).
     */
    public List<Done> tick(int heat, int mbPerTickPerHeat, Function<String, Melt> lookup) {
        if (heat <= 0 || mbPerTickPerHeat <= 0 || solids.isEmpty()) {
            return List.of();
        }
        if (meltLeft < 0 && !startNext(lookup)) {
            return List.of();
        }
        if (amount > 0 && !fluid.equals(meltFluid)) {
            return List.of();
        }
        int step = Math.min(heat * mbPerTickPerHeat, meltLeft);
        step = Math.min(step, CAPACITY - amount);
        if (step <= 0) {
            return List.of();
        }
        fluid = meltFluid;
        amount += step;
        meltLeft -= step;
        if (meltLeft > 0) {
            return List.of();
        }
        String input = solids.remove(0);
        int total = meltTotal;
        String done = meltFluid;
        meltLeft = -1;
        meltTotal = 0;
        meltFluid = "";
        return List.of(new Done(input, done, total));
    }

    /** Moves the first solid that can melt now to the front and starts it. */
    private boolean startNext(Function<String, Melt> lookup) {
        for (int i = 0; i < solids.size(); i++) {
            Melt m = lookup.apply(solids.get(i));
            if (m == null || m.amount() <= 0) {
                continue;
            }
            if (amount > 0 && !fluid.equals(m.fluid())) {
                continue;
            }
            if (amount + 1 > CAPACITY) {
                continue;
            }
            if (i > 0) {
                solids.add(0, solids.remove(i));
            }
            meltFluid = m.fluid();
            meltTotal = m.amount();
            meltLeft = m.amount();
            return true;
        }
        return false;
    }

    // ---- saving as plain values ----

    public String encodeSolids() {
        return String.join(";", solids);
    }

    public int meltLeft() {
        return meltLeft;
    }

    public int meltTotal() {
        return meltTotal;
    }

    public String meltFluid() {
        return meltFluid;
    }

    /** Restores a saved state; damaged values fall back to an empty crucible. */
    public void restore(String fluidId, int mb, String solidsText, int meltLeft, int meltTotal, String meltFluid) {
        this.fluid = fluidId == null ? "" : fluidId;
        this.amount = this.fluid.isEmpty() ? 0 : Math.max(0, Math.min(CAPACITY, mb));
        if (this.amount == 0) {
            this.fluid = "";
        }
        solids.clear();
        if (solidsText != null && !solidsText.isEmpty()) {
            for (String s : solidsText.split(";")) {
                if (!s.isEmpty() && solids.size() < SOLID_SLOTS) {
                    solids.add(s);
                }
            }
        }
        boolean running = meltLeft >= 0 && meltTotal > 0 && meltLeft <= meltTotal && meltFluid != null && !meltFluid.isEmpty()
                && !solids.isEmpty();
        this.meltLeft = running ? meltLeft : -1;
        this.meltTotal = running ? meltTotal : 0;
        this.meltFluid = running ? meltFluid : "";
    }
}
