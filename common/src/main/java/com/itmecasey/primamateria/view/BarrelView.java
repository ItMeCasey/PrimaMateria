package com.itmecasey.primamateria.view;

import java.util.List;

/**
 * What the renderer needs to draw one barrel. Filled by workstream A, read by workstream C.
 *
 * @param heatproof     true for the clay barrel
 * @param contents      what is inside
 * @param contentId     fluid id for {@link Contents#FLUID}, empty otherwise
 * @param amount        mB of fluid or units of compost, 0 to 1000
 * @param color         tint of the surface as 0xRRGGBB
 * @param stage         what the barrel is doing
 * @param stageProgress 0 to 1 within the stage
 * @param soakItem      the item or block in the soak slot, or empty
 * @param result        the finished block or item waiting to be taken, or empty
 * @param crystals      seeds growing, at most {@link #GROW_SLOTS}
 */
public record BarrelView(boolean heatproof, Contents contents, String contentId, int amount, int color, Stage stage,
                         float stageProgress, String soakItem, String result, List<Crystal> crystals) {
    public static final int CAPACITY = 1000;
    public static final int GROW_SLOTS = 4;
    public static final BarrelView EMPTY = new BarrelView(false, Contents.EMPTY, "", 0, 0xFFFFFF, Stage.IDLE, 0F, "", "", List.of());

    public enum Contents {
        EMPTY, FLUID, COMPOST
    }

    public enum Stage {
        IDLE, COMPOSTING, MATURING, SOAKING, MIXING, STEEPING, GROWING, DONE
    }

    /**
     * One growing seed.
     *
     * @param texture  crystal texture from the growing recipe
     * @param progress 0 to 1
     * @param slot     0 to {@link #GROW_SLOTS} - 1
     */
    public record Crystal(String texture, float progress, int slot) {
    }

    public BarrelView {
        crystals = List.copyOf(crystals);
    }
}
