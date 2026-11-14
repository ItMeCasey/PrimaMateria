package com.itmecasey.primamateria.drive;

import com.itmecasey.primamateria.view.DriveKind;

/**
 * The rules of one drive, without any game classes: how it is powered, when it shakes the machine above, and what it has left.
 * The block entity feeds it the facts of each game tick and acts on the answer.
 *
 * <ul>
 *   <li>{@link DriveKind#WINDUP}: stores turns (wound by hand), spends one per shake.</li>
 *   <li>{@link DriveKind#FURNACE}: burns fuel time while it runs; a shake needs the fire to be lit.</li>
 *   <li>{@link DriveKind#POWERED}: stores energy (FE), spends a fixed amount per shake.</li>
 * </ul>
 */
public final class DriveState {
    /** A drive stops when its output buffer has fewer empty slots than this. */
    public static final int MIN_FREE_SLOTS = 3;

    private final DriveKind kind;
    private final int ticksPerShake;
    private final int maxTurns;
    private final int capacity;
    private final int energyPerShake;

    private int turns;
    private int burnLeft;
    private int burnTotal;
    private int energy;
    private int cooldown;

    /**
     * @param kind           which drive this is
     * @param ticksPerShake  game ticks between two shakes while it runs (at least 1)
     * @param maxTurns       turns a windup drive stores
     * @param capacity       energy a powered drive stores
     * @param energyPerShake energy a powered drive spends per shake
     */
    public DriveState(DriveKind kind, int ticksPerShake, int maxTurns, int capacity, int energyPerShake) {
        this.kind = kind;
        this.ticksPerShake = Math.max(1, ticksPerShake);
        this.maxTurns = Math.max(1, maxTurns);
        this.capacity = Math.max(1, capacity);
        this.energyPerShake = Math.max(1, energyPerShake);
        this.cooldown = 0;
    }

    public DriveKind kind() {
        return kind;
    }

    /** True when the output buffer, with this many empty slots, is too full for the drive to run. */
    public static boolean blocked(int emptySlots) {
        return emptySlots < MIN_FREE_SLOTS;
    }

    // ---- windup ----

    public int turns() {
        return turns;
    }

    public int maxTurns() {
        return maxTurns;
    }

    /** Winds up to {@code amount} turns; returns how many were stored. Only a windup drive stores turns. */
    public int wind(int amount) {
        if (kind != DriveKind.WINDUP || amount <= 0) {
            return 0;
        }
        int stored = Math.min(amount, maxTurns - turns);
        turns += stored;
        return stored;
    }

    // ---- furnace ----

    public int burnLeft() {
        return burnLeft;
    }

    public int burnTotal() {
        return burnTotal;
    }

    public boolean burning() {
        return burnLeft > 0;
    }

    /** Lights a new fuel item that burns for {@code ticks}. Only a furnace drive burns, and only when the last fuel is used up. */
    public boolean ignite(int ticks) {
        if (kind != DriveKind.FURNACE || ticks <= 0 || burnLeft > 0) {
            return false;
        }
        burnLeft = ticks;
        burnTotal = ticks;
        return true;
    }

    // ---- powered ----

    public int energy() {
        return energy;
    }

    public int capacity() {
        return capacity;
    }

    /** Takes up to {@code amount} FE; with {@code simulate} nothing is stored. Returns how much was (or would be) taken. */
    public int receive(int amount, boolean simulate) {
        if (kind != DriveKind.POWERED || amount <= 0) {
            return 0;
        }
        int taken = Math.min(amount, capacity - energy);
        if (!simulate) {
            energy += taken;
        }
        return taken;
    }

    // ---- running ----

    /** True when there is power for the next shake (the furnace counts as ready only while its fire is lit). */
    public boolean hasPower() {
        return switch (kind) {
            case WINDUP -> turns > 0;
            case FURNACE -> burnLeft > 0;
            case POWERED -> energy >= energyPerShake;
            default -> false;
        };
    }

    /**
     * One game tick. {@code running} says whether the drive has work, room for the output and no redstone signal; a drive that is not
     * running stands still (no burning, no waiting). Returns true when the machine above should be shaken now.
     */
    public boolean tick(boolean running) {
        if (!running) {
            return false;
        }
        if (kind == DriveKind.FURNACE) {
            if (burnLeft <= 0) {
                return false;
            }
            burnLeft--;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (cooldown > 0) {
            return false;
        }
        switch (kind) {
            case WINDUP -> {
                if (turns <= 0) {
                    return false;
                }
                turns--;
            }
            case FURNACE -> {
                // the fire burned this tick above; the shake itself costs nothing more
            }
            case POWERED -> {
                if (energy < energyPerShake) {
                    return false;
                }
                energy -= energyPerShake;
            }
            default -> {
                return false;
            }
        }
        cooldown = ticksPerShake;
        return true;
    }

    /** How full the drive is, 0 to 1: turns, fuel left in the fire, or energy. */
    public float charge() {
        return switch (kind) {
            case WINDUP -> turns / (float) maxTurns;
            case FURNACE -> burnTotal <= 0 ? 0F : burnLeft / (float) burnTotal;
            case POWERED -> energy / (float) capacity;
            default -> 0F;
        };
    }

    /** The five-step reading of a powered drive's energy for the block state: 0 only when empty, 5 only when full. */
    public int chargeStep() {
        if (kind != DriveKind.POWERED || energy <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(5, (int) Math.ceil(energy * 5.0 / capacity - 1e-9)));
    }

    // ---- saving ----

    /** Restores saved values; anything out of range is clamped. */
    public void restore(int turns, int burnLeft, int burnTotal, int energy, int cooldown) {
        this.turns = kind == DriveKind.WINDUP ? Math.max(0, Math.min(maxTurns, turns)) : 0;
        this.burnTotal = kind == DriveKind.FURNACE ? Math.max(0, burnTotal) : 0;
        this.burnLeft = kind == DriveKind.FURNACE ? Math.max(0, Math.min(this.burnTotal, burnLeft)) : 0;
        this.energy = kind == DriveKind.POWERED ? Math.max(0, Math.min(capacity, energy)) : 0;
        this.cooldown = Math.max(0, Math.min(ticksPerShake, cooldown));
    }

    public int cooldown() {
        return cooldown;
    }
}
