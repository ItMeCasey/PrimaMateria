package com.itmecasey.primamateria.view;

/**
 * What the renderer needs to draw one drive. Filled by workstream A, read by workstream C.
 *
 * @param kind          windup, furnace or flux
 * @param charge        stored turns, burn time or energy, as 0 to 1 of the maximum
 * @param running       shaking the machine above right now
 * @param blocked       the output buffer is full
 * @param lastShakeTick game time of the last shake
 */
public record DriveView(DriveKind kind, float charge, boolean running, boolean blocked, long lastShakeTick) {
    public static final int BUFFER_SLOTS = 9;
    public static final DriveView EMPTY = new DriveView(DriveKind.NONE, 0F, false, false, 0L);
}
