package com.itmecasey.primamateria.sift;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import com.itmecasey.primamateria.view.SieveView;

/** Small pure helpers of the sieve: work per block, link area, comparator signal and where finds show on the mesh. */
public final class SiftMath {
    /** Shakes between hand shakes: a held use button repeats every 4 ticks, so a hand shakes a sieve about every 4 ticks. */
    public static final int HAND_MIN_TICKS = 3;
    /** Ticks after the last shake until a sieve stops drawing its moving parts. */
    public static final int MOVING_HOLD_TICKS = 20;
    /** Lowest and highest reveal point of a find, as a share of the block's progress. */
    public static final float REVEAL_FIRST = 0.15F;
    public static final float REVEAL_LAST = 0.90F;

    private SiftMath() {
    }

    /** Shakes needed to sift one block: the base count divided by the mesh speed, rounded up, never less than 1. */
    public static int shakesNeeded(int baseShakes, double meshSpeed) {
        double speed = meshSpeed <= 0 ? 1.0 : meshSpeed;
        return Math.max(1, (int) Math.ceil(Math.max(1, baseShakes) / speed - 1e-9));
    }

    /** Comparator output: 0 empty, 1 just filled, 15 almost done. */
    public static int comparatorSignal(boolean filled, float progress) {
        if (!filled) {
            return 0;
        }
        return Math.min(15, 1 + (int) Math.floor(Math.max(0F, Math.min(1F, progress)) * 14F));
    }

    /**
     * The other sieves a shake reaches: every position in the square of the given radius around a sieve at the same height,
     * nearest first (ties by x then z). Radius is clamped to 1 to 3. The sieve itself is not in the list.
     */
    public static List<int[]> linkOffsets(int radius) {
        int r = Math.max(1, Math.min(3, radius));
        List<int[]> out = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx != 0 || dz != 0) {
                    out.add(new int[] {dx, dz});
                }
            }
        }
        out.sort(Comparator.<int[]>comparingInt(o -> o[0] * o[0] + o[1] * o[1]).thenComparingInt(o -> o[0]).thenComparingInt(o -> o[1]));
        return out;
    }

    /**
     * Where each drop shows on the mesh and when. Reveal points are spread evenly between {@link #REVEAL_FIRST} and
     * {@link #REVEAL_LAST} and the order is shuffled by the seed, so the same seed always gives the same picture. Spots are
     * distinct while there are fewer drops than {@link SieveView#FIND_SPOTS}; past that they repeat.
     */
    public static List<SieveView.Find> finds(List<Drop> drops, long seed) {
        int n = drops.size();
        List<SieveView.Find> out = new ArrayList<>();
        if (n == 0) {
            return out;
        }
        RandomGenerator rng = new Random(seed);
        List<Integer> spots = new ArrayList<>();
        for (int i = 0; i < SieveView.FIND_SPOTS; i++) {
            spots.add(i);
        }
        java.util.Collections.shuffle(spots, new Random(seed ^ 0x5DEECE66DL));
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        java.util.Collections.shuffle(order, new Random(rng.nextLong()));
        for (int k = 0; k < n; k++) {
            Drop d = drops.get(order.get(k));
            float reveal = n == 1 ? (REVEAL_FIRST + REVEAL_LAST) / 2F : REVEAL_FIRST + (REVEAL_LAST - REVEAL_FIRST) * k / (n - 1);
            out.add(new SieveView.Find(d.item(), d.count(), reveal, spots.get(k % SieveView.FIND_SPOTS)));
        }
        return out;
    }
}
