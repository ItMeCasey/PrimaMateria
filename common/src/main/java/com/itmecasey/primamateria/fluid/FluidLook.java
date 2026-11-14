package com.itmecasey.primamateria.fluid;

import java.util.Map;

/** How a fluid looks inside a crucible or tray: the surface tint (0xRRGGBB) and the light it gives (0 to 15). */
public final class FluidLook {
    private static final Map<String, Integer> COLOR = Map.of(
            "minecraft:lava", 0xFF6A00,
            "primamateria:molten_copper", 0xD8742A,
            "primamateria:molten_iron", 0xE6C9A8,
            "primamateria:molten_gold", 0xF5C623,
            "primamateria:brine", 0x45979C,
            "minecraft:water", 0x3F76E4);
    private static final Map<String, Integer> GLOW = Map.of(
            "minecraft:lava", 15,
            "primamateria:molten_copper", 12,
            "primamateria:molten_iron", 13,
            "primamateria:molten_gold", 13);

    private FluidLook() {
    }

    public static int color(String fluid) {
        return COLOR.getOrDefault(fluid, 0xFFFFFF);
    }

    public static int glow(String fluid) {
        return GLOW.getOrDefault(fluid, 0);
    }
}
