package com.itmecasey.primamateria.world;

/** The {@code start.island} setting: when the start island is placed. */
public enum StartMode {
    /** Only in a world made with the Void Island preset (the default). */
    PRESET_ONLY,
    /** In every world that is loaded without an island, void or not. */
    ALWAYS,
    /** Never. */
    NEVER;

    /** Reads the setting text; anything unknown means the default, {@link #PRESET_ONLY}. */
    public static StartMode parse(String text) {
        if (text != null) {
            switch (text.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "always":
                    return ALWAYS;
                case "never":
                    return NEVER;
                default:
                    break;
            }
        }
        return PRESET_ONLY;
    }

    /** True when the island should be placed now. */
    public boolean shouldPlace(boolean worldUsesVoidPreset, boolean alreadyPlaced) {
        if (alreadyPlaced) {
            return false;
        }
        return switch (this) {
            case PRESET_ONLY -> worldUsesVoidPreset;
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }
}
