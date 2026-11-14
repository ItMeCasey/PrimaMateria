package com.itmecasey.primamateria.sift;

import java.util.ArrayList;
import java.util.List;

/**
 * The state of one sieve: the fitted mesh, the block being sifted, the drops rolled for it and how far the shaking has got.
 * Pure Java; the block entity owns one and saves it as plain strings.
 */
public final class SiftState {
    /** Result of {@link #shake()}. */
    public enum Shake {
        /** Nothing in the sieve, or no mesh. */
        NONE,
        /** The block is partly sifted. */
        PROGRESS,
        /** The block is done: take {@link #drops()} and {@link #residue()}, then call {@link #clearBlock()}. */
        DONE
    }

    private String meshId = "";
    private int meshTier;
    private double meshSpeed = 1.0;
    private String block = "";
    private int shakes;
    private int needed = 1;
    private long seed;
    private List<Drop> drops = List.of();
    private List<Drop> residue = List.of();
    private long lastShakeTick = Long.MIN_VALUE / 2;

    public String meshId() {
        return meshId;
    }

    public int meshTier() {
        return meshTier;
    }

    public double meshSpeed() {
        return meshSpeed;
    }

    public boolean hasMesh() {
        return !meshId.isEmpty();
    }

    /** Fits a mesh (or removes it with an empty id). Changing the mesh while a block is in does not change that block's drops. */
    public void setMesh(String id, int tier, double speed) {
        this.meshId = id == null ? "" : id;
        this.meshTier = this.meshId.isEmpty() ? 0 : tier;
        this.meshSpeed = this.meshId.isEmpty() ? 1.0 : speed;
    }

    public boolean filled() {
        return !block.isEmpty();
    }

    public String block() {
        return block;
    }

    public long seed() {
        return seed;
    }

    public List<Drop> drops() {
        return drops;
    }

    public List<Drop> residue() {
        return residue;
    }

    public long lastShakeTick() {
        return lastShakeTick;
    }

    public int shakes() {
        return shakes;
    }

    public int needed() {
        return needed;
    }

    /** 0 when the block goes in, 1 when it is done; 0 when empty. */
    public float progress() {
        return filled() ? Math.min(1F, shakes / (float) needed) : 0F;
    }

    public boolean canAccept() {
        return hasMesh() && !filled();
    }

    /** Puts one block in. Needs a mesh and an empty sieve. */
    public boolean start(String blockId, DropRoller.Rolled rolled, int baseShakes, long newSeed) {
        if (!canAccept() || blockId == null || blockId.isEmpty()) {
            return false;
        }
        this.block = blockId;
        this.shakes = 0;
        this.needed = SiftMath.shakesNeeded(baseShakes, meshSpeed);
        this.seed = newSeed;
        this.drops = rolled.drops();
        this.residue = rolled.residue();
        return true;
    }

    /** True when {@link #shake} would count at this game time (a hand cannot shake faster than every few ticks). */
    public boolean ready(long gameTime, int minTicks) {
        return filled() && gameTime - lastShakeTick >= minTicks;
    }

    /** One shake. */
    public Shake shake(long gameTime) {
        if (!filled()) {
            return Shake.NONE;
        }
        lastShakeTick = gameTime;
        shakes++;
        return shakes >= needed ? Shake.DONE : Shake.PROGRESS;
    }

    /** Empties the sieve of its block (after it finished or was taken out). The mesh stays. */
    public void clearBlock() {
        block = "";
        shakes = 0;
        needed = 1;
        drops = List.of();
        residue = List.of();
    }

    // ---- saving as plain strings ----

    public static String encode(List<Drop> list) {
        StringBuilder sb = new StringBuilder();
        for (Drop d : list) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(d.item()).append('|').append(d.count());
        }
        return sb.toString();
    }

    public static List<Drop> decode(String text) {
        List<Drop> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        for (String part : text.split(";")) {
            int bar = part.lastIndexOf('|');
            if (bar <= 0) {
                continue;
            }
            try {
                int count = Integer.parseInt(part.substring(bar + 1));
                if (count >= 1) {
                    out.add(new Drop(part.substring(0, bar), count));
                }
            } catch (NumberFormatException e) {
                // A damaged entry is skipped.
            }
        }
        return out;
    }

    /** Restores a saved state. Unknown or damaged values fall back to an empty sieve with the mesh kept. */
    public void restore(String meshId, int meshTier, double meshSpeed, String block, int shakes, int needed, long seed,
                        String drops, String residue, long lastShakeTick) {
        setMesh(meshId, meshTier, meshSpeed);
        this.block = block == null ? "" : block;
        this.shakes = Math.max(0, shakes);
        this.needed = Math.max(1, needed);
        this.seed = seed;
        this.drops = List.copyOf(decode(drops));
        this.residue = List.copyOf(decode(residue));
        this.lastShakeTick = lastShakeTick;
        if (this.block.isEmpty()) {
            clearBlock();
        }
    }
}
