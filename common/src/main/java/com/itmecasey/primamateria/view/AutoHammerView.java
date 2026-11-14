package com.itmecasey.primamateria.view;

/**
 * What the renderer needs to draw one auto hammer. Filled by workstream A, read by workstream C.
 *
 * @param hammerId      the fitted hammer item, or empty
 * @param inputBlock    the block under the stamps, or empty
 * @param queued        blocks waiting, 0 to {@link #INPUT_SLOTS}
 * @param progress      0 to 1 for the block under the stamps
 * @param drive         what is driving it
 * @param lastShakeTick game time of the last stroke
 */
public record AutoHammerView(String hammerId, String inputBlock, int queued, float progress, DriveKind drive, long lastShakeTick) {
    public static final int INPUT_SLOTS = 16;
    public static final AutoHammerView EMPTY = new AutoHammerView("", "", 0, 0F, DriveKind.NONE, 0L);
}
