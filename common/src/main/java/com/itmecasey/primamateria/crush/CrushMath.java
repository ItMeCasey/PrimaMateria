package com.itmecasey.primamateria.crush;

import com.itmecasey.primamateria.sift.Drop;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Pure rolling of what one crushed block gives. The caller supplies the results of the recipe, the hammer tier and the random source. */
public final class CrushMath {
    /** One possible result of a crushing recipe: full item id, count range and chance. */
    public record Result(String item, int min, int max, double chance) {
        public Result {
            if (min < 1 || max < min) {
                throw new IllegalArgumentException("bad count range " + min + " to " + max);
            }
        }
    }

    /** Hammer tiers above this one add a chance of one extra item (config {@code hammer.bonus_per_tier} for each tier above it). */
    public static final int BONUS_FROM_TIER = 2;

    private CrushMath() {
    }

    /** Chance of one extra item: {@code bonusPerTier} for each tier above {@link #BONUS_FROM_TIER}, at most 1. */
    public static double bonusChance(int hammerTier, double bonusPerTier) {
        return Math.max(0.0, Math.min(1.0, (hammerTier - BONUS_FROM_TIER) * bonusPerTier));
    }

    /**
     * Rolls the results of one block. Same items are merged. The bonus (see {@link #bonusChance}) adds one to the first result that
     * dropped, so a hammer never creates an item the recipe did not roll.
     */
    public static List<Drop> roll(List<Result> results, int hammerTier, double bonusPerTier, RandomGenerator rng) {
        Map<String, Integer> found = new LinkedHashMap<>();
        for (Result r : results) {
            if (rng.nextDouble() < r.chance()) {
                int count = r.min() == r.max() ? r.min() : r.min() + rng.nextInt(r.max() - r.min() + 1);
                found.merge(r.item(), count, Integer::sum);
            }
        }
        if (!found.isEmpty() && rng.nextDouble() < bonusChance(hammerTier, bonusPerTier)) {
            String first = found.keySet().iterator().next();
            found.merge(first, 1, Integer::sum);
        }
        List<Drop> out = new ArrayList<>();
        found.forEach((item, count) -> out.add(new Drop(item, count)));
        return out;
    }
}
