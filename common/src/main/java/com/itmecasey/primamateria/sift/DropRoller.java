package com.itmecasey.primamateria.sift;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Rolls what one sifted block gives. Pure: the caller supplies the entries and the random source. */
public final class DropRoller {
    /** What one block gave: the drops that show on the mesh and the residue that falls straight through. */
    public record Rolled(List<Drop> drops, List<Drop> residue) {
        public Rolled {
            drops = List.copyOf(drops);
            residue = List.copyOf(residue);
        }
    }

    private DropRoller() {
    }

    /**
     * @param entries    every drop the block can give (any mesh tier)
     * @param residues   the residue entries of the block
     * @param meshTier   tier of the fitted mesh, 1 or more
     * @param multiplier scales every chance (config {@code sieve.drop_multiplier}); a result above 1 counts as 1
     */
    public static Rolled roll(List<DropEntry> entries, List<ResidueEntry> residues, int meshTier, double multiplier, RandomGenerator rng) {
        Map<String, Integer> found = new LinkedHashMap<>();
        for (DropEntry e : entries) {
            if (!e.allows(meshTier)) {
                continue;
            }
            if (rng.nextDouble() < Math.min(1.0, e.chance() * multiplier)) {
                int count = e.min() == e.max() ? e.min() : e.min() + rng.nextInt(e.max() - e.min() + 1);
                found.merge(e.item(), count, Integer::sum);
            }
        }
        List<Drop> drops = new ArrayList<>();
        found.forEach((item, count) -> drops.add(new Drop(item, count)));
        Map<String, Integer> left = new LinkedHashMap<>();
        for (ResidueEntry r : residues) {
            if (rng.nextDouble() < Math.min(1.0, r.chance() * multiplier)) {
                left.merge(r.item(), 1, Integer::sum);
            }
        }
        List<Drop> residue = new ArrayList<>();
        left.forEach((item, count) -> residue.add(new Drop(item, count)));
        return new Rolled(drops, residue);
    }
}
