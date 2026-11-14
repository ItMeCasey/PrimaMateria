package com.itmecasey.primamateria.view;

import java.util.List;

/**
 * What the renderer needs to draw one sieve. Workstream A fills it from the block entity (and syncs it to clients); workstream C
 * reads it. Ids are full strings such as {@code primamateria:iron_mesh}; an empty string means none.
 *
 * @param meshId        the fitted mesh item, or empty
 * @param meshTexture   texture of the fitted mesh (from its mesh data file), or empty
 * @param block         the block being sifted, or empty when the sieve is empty
 * @param progress      0 when the block goes in, 1 when it is done
 * @param shakeCount    shakes done on this block
 * @param lastShakeTick game time of the last shake
 * @param drive         what is shaking the sieve
 * @param blocked       the drive below cannot hand out results
 * @param seed          per-block random seed, for find rotation and the diagonal shakes
 * @param finds         drops already rolled for this block, in reveal order
 */
public record SieveView(String meshId, String meshTexture, String block, float progress, int shakeCount,
                        long lastShakeTick, DriveKind drive, boolean blocked, long seed, List<Find> finds) {
    public static final SieveView EMPTY = new SieveView("", "", "", 0F, 0, 0L, DriveKind.NONE, false, 0L, List.of());

    /** Spots on the mesh a find can lie on (a 3 by 2 grid). */
    public static final int FIND_SPOTS = 6;

    /** True when a block is in the sieve. */
    public boolean filled() {
        return !block.isEmpty();
    }

    /**
     * One rolled drop shown on the mesh.
     *
     * @param item     full item id
     * @param count    how many
     * @param revealAt progress (0 to 1) at which the layer has sunk far enough to show it
     * @param spot     0 to {@link #FIND_SPOTS} - 1
     */
    public record Find(String item, int count, float revealAt, int spot) {
    }

    public SieveView {
        finds = List.copyOf(finds);
    }
}
