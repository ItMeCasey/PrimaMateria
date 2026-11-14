package com.itmecasey.primamateria.fluid;

import java.util.Map;

/** How a fluid looks inside a crucible or tray: the surface tint (0xRRGGBB) and the light it gives (0 to 15). */
public final class FluidLook {
    private static final Map<String, Integer> COLOR = new java.util.HashMap<>();
    private static final Map<String, Integer> GLOW = new java.util.HashMap<>();

    static {
        COLOR.put("minecraft:lava", 0xFF6A00);
        COLOR.put("primamateria:brine", 0x45979C);
        COLOR.put("minecraft:water", 0x3F76E4);
        GLOW.put("minecraft:lava", 15);
        for (Metals.Metal metal : Metals.ALL) {
            COLOR.put("primamateria:" + metal.fluid(), metal.color());
            GLOW.put("primamateria:" + metal.fluid(), metal.glow());
        }
    }

    private FluidLook() {
    }

    public static int color(String fluid) {
        return COLOR.getOrDefault(fluid, 0xFFFFFF);
    }

    public static int glow(String fluid) {
        return GLOW.getOrDefault(fluid, 0);
    }
}
