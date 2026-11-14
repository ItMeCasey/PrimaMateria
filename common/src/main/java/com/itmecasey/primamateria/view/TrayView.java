package com.itmecasey.primamateria.view;

/**
 * What the renderer needs to draw one casting tray. Filled by workstream A, read by workstream C.
 *
 * @param fluidId  the fluid in the tray, or empty
 * @param color    tint of the fluid as 0xRRGGBB
 * @param stage    what the tray is doing
 * @param progress 0 to 1 within the stage
 * @param result   the cast item (shown while cooling and when done), or empty
 */
public record TrayView(String fluidId, int color, Stage stage, float progress, String result) {
    public static final int AMOUNT = 100;
    public static final TrayView EMPTY = new TrayView("", 0xFFFFFF, Stage.EMPTY, 0F, "");

    public enum Stage {
        EMPTY, FILLING, COOLING, DONE
    }
}
