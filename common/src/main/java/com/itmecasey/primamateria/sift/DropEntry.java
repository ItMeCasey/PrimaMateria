package com.itmecasey.primamateria.sift;

/**
 * One possible drop of one sifted block, as read from a sifting recipe.
 *
 * @param item     full item id of the result
 * @param min      fewest items when it drops
 * @param max      most items when it drops
 * @param chance   chance per block sifted, 0 to 1
 * @param meshMin  lowest mesh tier that can give it (1 or more)
 * @param meshMax  highest mesh tier that can give it, 0 for no limit
 */
public record DropEntry(String item, int min, int max, double chance, int meshMin, int meshMax) {
    public DropEntry {
        if (min < 1 || max < min) {
            throw new IllegalArgumentException("bad count range " + min + " to " + max);
        }
        if (meshMin < 1) {
            throw new IllegalArgumentException("meshMin must be at least 1");
        }
    }

    /** True when a mesh of this tier can give the drop. */
    public boolean allows(int meshTier) {
        return meshTier >= meshMin && (meshMax <= 0 || meshTier <= meshMax);
    }
}
