package com.itmecasey.primamateria.crucible;

import com.itmecasey.primamateria.view.TrayView;

/**
 * One casting tray: empty, being poured into for a moment, cooling, then holding its cast item until it is taken. Pure Java.
 */
public final class TrayState {
    /** Ticks the pour lasts before the cooling time starts. */
    public static final int FILL_TICKS = 10;

    /** A finished cast. */
    public record Cast(String fluid, String result, int count) {
    }

    private TrayView.Stage stage = TrayView.Stage.EMPTY;
    private String fluid = "";
    private String result = "";
    private int count;
    private int cool;
    private int ticks;

    public TrayView.Stage stage() {
        return stage;
    }

    public String fluid() {
        return fluid;
    }

    public String result() {
        return result;
    }

    public int count() {
        return count;
    }

    public boolean canAccept() {
        return stage == TrayView.Stage.EMPTY;
    }

    /** Starts a cast: the tray is poured into and then cools for {@code coolTicks}. */
    public boolean fill(String fluidId, String resultItem, int resultCount, int coolTicks) {
        if (!canAccept() || resultItem == null || resultItem.isEmpty() || resultCount < 1) {
            return false;
        }
        stage = TrayView.Stage.FILLING;
        fluid = fluidId;
        result = resultItem;
        count = resultCount;
        cool = Math.max(1, coolTicks);
        ticks = 0;
        return true;
    }

    /** One tick; returns the cast when cooling just finished. */
    public Cast tick() {
        if (stage == TrayView.Stage.FILLING) {
            if (++ticks >= FILL_TICKS) {
                stage = TrayView.Stage.COOLING;
                ticks = 0;
            }
            return null;
        }
        if (stage == TrayView.Stage.COOLING && ++ticks >= cool) {
            stage = TrayView.Stage.DONE;
            return new Cast(fluid, result, count);
        }
        return null;
    }

    /** 0 to 1 within the current stage. */
    public float progress() {
        return switch (stage) {
            case FILLING -> ticks / (float) FILL_TICKS;
            case COOLING -> ticks / (float) cool;
            case DONE -> 1F;
            default -> 0F;
        };
    }

    public boolean done() {
        return stage == TrayView.Stage.DONE;
    }

    /** Takes the cast item out and empties the tray. Returns null when there is nothing to take. */
    public String takeResult() {
        if (!done()) {
            return null;
        }
        String item = result;
        reset();
        return item;
    }

    /** Takes up to {@code n} of the cast items out (a hopper takes one at a time). Returns how many came out; empties the tray at zero. */
    public int removeCount(int n) {
        if (!done() || n < 1) {
            return 0;
        }
        int out = Math.min(n, count);
        count -= out;
        if (count == 0) {
            reset();
        }
        return out;
    }

    /** How many items {@link #takeResult} gives (valid while done). */
    public int takeCount() {
        return count;
    }

    public void reset() {
        stage = TrayView.Stage.EMPTY;
        fluid = "";
        result = "";
        count = 0;
        cool = 0;
        ticks = 0;
    }

    public int cool() {
        return cool;
    }

    public int ticks() {
        return ticks;
    }

    /** Restores a saved state; damaged values give an empty tray. */
    public void restore(int stageOrdinal, String fluidId, String resultItem, int resultCount, int coolTicks, int tickCount) {
        reset();
        TrayView.Stage[] all = TrayView.Stage.values();
        if (stageOrdinal <= 0 || stageOrdinal >= all.length || resultItem == null || resultItem.isEmpty() || resultCount < 1) {
            return;
        }
        stage = all[stageOrdinal];
        fluid = fluidId == null ? "" : fluidId;
        result = resultItem;
        count = resultCount;
        cool = Math.max(1, coolTicks);
        ticks = Math.max(0, tickCount);
    }
}
