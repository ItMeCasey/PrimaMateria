package com.itmecasey.primamateria.view;

/**
 * What the renderer needs to draw one crucible. Filled by workstream A, read by workstream C.
 *
 * @param contentId  fluid id (lava or a molten metal), or empty
 * @param amount     mB held, 0 to {@link #CAPACITY}
 * @param color      tint of the surface as 0xRRGGBB
 * @param glow       light level of the surface, 0 to 15
 * @param solids     solid items waiting to melt, 0 to {@link #SOLID_SLOTS}
 * @param solidItem  the item on top of the solids, or empty
 * @param heat       heat from the block below, 0 when there is none
 * @param pourSides  bit set of horizontal sides currently pouring into a casting tray (bit = Direction 2D index)
 */
public record CrucibleView(String contentId, int amount, int color, int glow, int solids, String solidItem,
                           int heat, int pourSides) {
    public static final int CAPACITY = 4000;
    public static final int SOLID_SLOTS = 4;
    public static final CrucibleView EMPTY = new CrucibleView("", 0, 0xFFFFFF, 0, 0, "", 0, 0);
}
