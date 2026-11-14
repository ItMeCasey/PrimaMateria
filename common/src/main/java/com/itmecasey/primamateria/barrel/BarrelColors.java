package com.itmecasey.primamateria.barrel;

/** Surface colors of what a barrel holds, as 0xRRGGBB. Vanilla water and lava keep their game colors; ours are tinted. */
public final class BarrelColors {
    public static final int COMPOST_FRESH = 0x5E7A2A;
    public static final int COMPOST_RIPE = 0x4A3322;

    private BarrelColors() {
    }

    public static int fluid(String id) {
        return switch (id) {
            case "" -> 0xFFFFFF;
            case "minecraft:water" -> 0x3F76E4;
            case "minecraft:lava" -> 0xFF6A00;
            case "primamateria:brine" -> 0x45979C;
            default -> id.startsWith("primamateria:molten_") ? com.itmecasey.primamateria.fluid.FluidLook.color(id) : 0x808080 | (id.hashCode() & 0x7F7F7F);
        };
    }

    /** Linear blend of two colors; {@code t} 0 gives {@code a}, 1 gives {@code b}. */
    public static int mix(int a, int b, float t) {
        float k = Math.max(0F, Math.min(1F, t));
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }
}
