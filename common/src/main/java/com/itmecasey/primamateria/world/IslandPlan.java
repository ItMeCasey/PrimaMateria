package com.itmecasey.primamateria.world;

import java.util.ArrayList;
import java.util.List;

/**
 * The start island as a list of blocks relative to the center of its top layer. Pure data: the version code maps each kind to a
 * game block and places it. A 7 by 7 island, three blocks deep (grass on top, two layers of dirt), with one oak tree standing in
 * a corner and nothing else.
 */
public final class IslandPlan {
    public enum Kind {
        GRASS, DIRT, LOG, LEAVES
    }

    /** One block: offset from the center of the top layer, and what it is. */
    public record Part(int dx, int dy, int dz, Kind kind) {
    }

    /** The island reaches this many blocks from its center on each side (a 7 by 7 square). */
    public static final int RADIUS = 3;
    /** Height of the top layer (the grass) in the world. */
    public static final int TOP_Y = 64;
    /** The tree trunk stands at this offset from the center, in both x and z. */
    public static final int TREE_OFFSET = 2;
    public static final int TRUNK_HEIGHT = 4;

    private static final List<Part> PARTS = build();

    private IslandPlan() {
    }

    /** Every block of the island and its tree, bottom to top. */
    public static List<Part> parts() {
        return PARTS;
    }

    /** Where a player appears, relative to the center of the top layer: one block above the grass at the center. */
    public static int[] spawnOffset() {
        return new int[] {0, 1, 0};
    }

    private static List<Part> build() {
        List<Part> out = new ArrayList<>();
        for (int dy = -2; dy <= 0; dy++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    out.add(new Part(dx, dy, dz, dy == 0 ? Kind.GRASS : Kind.DIRT));
                }
            }
        }
        int tx = TREE_OFFSET;
        int tz = TREE_OFFSET;
        for (int dy = 1; dy <= TRUNK_HEIGHT; dy++) {
            out.add(new Part(tx, dy, tz, Kind.LOG));
        }
        // crown: two wide layers around the top of the trunk, then a small cap
        for (int dy = TRUNK_HEIGHT - 1; dy <= TRUNK_HEIGHT; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    boolean trunk = dx == 0 && dz == 0 && dy <= TRUNK_HEIGHT;
                    if (!corner && !trunk) {
                        out.add(new Part(tx + dx, dy, tz + dz, Kind.LEAVES));
                    }
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                out.add(new Part(tx + dx, TRUNK_HEIGHT + 1, tz + dz, Kind.LEAVES));
            }
        }
        int cap = TRUNK_HEIGHT + 2;
        out.add(new Part(tx, cap, tz, Kind.LEAVES));
        out.add(new Part(tx + 1, cap, tz, Kind.LEAVES));
        out.add(new Part(tx - 1, cap, tz, Kind.LEAVES));
        out.add(new Part(tx, cap, tz + 1, Kind.LEAVES));
        out.add(new Part(tx, cap, tz - 1, Kind.LEAVES));
        return List.copyOf(out);
    }
}
