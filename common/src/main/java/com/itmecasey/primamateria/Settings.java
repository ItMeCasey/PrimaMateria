package com.itmecasey.primamateria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every tunable value in the mod: the single list of keys, defaults, ranges and descriptions. The loader layers build their config
 * specs from {@link #definitions()} (NeoForge's {@code ModConfigSpec}, Forge's {@code ForgeConfigSpec}, files
 * {@code config/primamateria-common.toml}) and push every value they load into
 * {@link #apply}, so the rest of the code and the tests read plain {@code Settings.getInt(...)} and friends. This class reads and
 * writes no config file; the old {@code primamateria.properties} is imported once by {@link LegacyImport}.
 */
public final class Settings {
    /**
     * One setting. {@code fallback} is a Boolean, Integer, Double or String; {@code min} and {@code max} bound a number;
     * {@code choices} lists the allowed values of a text setting.
     */
    public record Def(String key, Object fallback, String comment, Number min, Number max, List<String> choices) {
        /** Client settings are read on the client only; they sit in the {@code [client]} table of the same file. */
        public boolean client() {
            return key.startsWith("client.");
        }
    }

    private static final Map<String, Def> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void flag(String key, boolean fallback, String comment) {
        DEFS.put(key, new Def(key, fallback, comment, null, null, List.of()));
    }

    private static void whole(String key, int fallback, int min, int max, String comment) {
        DEFS.put(key, new Def(key, fallback, comment, min, max, List.of()));
    }

    private static void real(String key, double fallback, double min, double max, String comment) {
        DEFS.put(key, new Def(key, fallback, comment, min, max, List.of()));
    }

    private static void choice(String key, String fallback, List<String> choices, String comment) {
        DEFS.put(key, new Def(key, fallback, comment, null, null, choices));
    }

    static {
        whole("sieve.shakes_per_block", 8, 1, 1000, "Shakes needed to sift one block with a mesh of speed 1.0.");
        flag("sieve.link", true, "Sieves next to each other are shaken and refilled together.");
        whole("sieve.link_radius", 1, 1, 3, "How far linking reaches in blocks around a sieve (1 is a 3 by 3 square).");
        real("sieve.drop_multiplier", 1.0, 0.0, 100.0, "Multiplies every sifting chance.");
        real("hammer.bonus_per_tier", 0.10, 0.0, 1.0, "Chance of one extra result per hammer tier above 2.");
        whole("drive.windup_max_turns", 48, 1, 10000, "Turns a windup drive stores.");
        whole("drive.windup_ticks_per_shake", 8, 1, 200, "Ticks between shakes of a windup drive.");
        whole("drive.furnace_ticks_per_shake", 4, 1, 200, "Ticks between shakes of a furnace drive.");
        whole("drive.powered_ticks_per_shake", 2, 1, 200, "Ticks between shakes of a powered drive.");
        whole("drive.powered_fe_per_shake", 40, 0, 1000000, "Energy (FE) a powered drive uses per shake.");
        whole("drive.powered_capacity", 10000, 1, 100000000, "Energy (FE) a powered drive stores.");
        whole("auto_hammer.strokes_per_block", 6, 1, 100, "Drive shakes an auto hammer needs to crush one block.");
        whole("barrel.dew_ticks_per_mb", 4, 0, 1200, "Ticks for a barrel under open sky to gain 1 mB of water. 0 switches dew off.");
        whole("barrel.rain_multiplier", 8, 1, 100, "How much faster a barrel fills in rain.");
        real("barrel.time_multiplier", 1.0, 0.01, 100.0, "Multiplies compost, soak, steep and grow times.");
        whole("crucible.mb_per_tick_per_heat", 2, 1, 100, "mB a crucible melts per tick for each point of heat below it.");
        choice("start.island", "preset_only", List.of("preset_only", "always", "never"),
                "When the start island is placed: preset_only (Void Island worlds), always, never.");
        choice("client.animations", "full", List.of("full", "reduced", "off"), "Animation level. Client side.");
        flag("client.particles", true, "Show sifting dust and falling fines. Client side.");
        whole("client.animation_distance", 48, 8, 256, "Blocks within which moving sieves are animated; half of it is the full detail range. Client side.");
        whole("client.sounds_per_tick", 4, 0, 64, "Most sieve shake sounds played in one tick. Client side.");
    }

    private Settings() {
    }

    /** Every setting, in file order. The loader layers build their config specs from this list. */
    public static List<Def> definitions() {
        return new ArrayList<>(DEFS.values());
    }

    /** Every setting key, in file order. */
    public static Set<String> keys() {
        return DEFS.keySet();
    }

    /** Restores every default. */
    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Def e = DEFS.get(key);
        if (e == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, String.valueOf(e.fallback()));
    }

    /** Sets one value: what a loaded config pushes in, and what tests use. */
    public static synchronized void apply(String key, Object value) {
        raw(key);
        VALUES.put(key, String.valueOf(value));
    }

    /** Overrides one setting (tests only). */
    public static void set(String key, Object value) {
        apply(key, value);
    }

    public static String getString(String key) {
        return raw(key).trim();
    }

    public static int getInt(String key) {
        try {
            return (int) Math.round(Double.parseDouble(raw(key).trim()));
        } catch (NumberFormatException ex) {
            return (int) Math.round(Double.parseDouble(String.valueOf(DEFS.get(key).fallback())));
        }
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(raw(key).trim());
        } catch (NumberFormatException ex) {
            return Double.parseDouble(String.valueOf(DEFS.get(key).fallback()));
        }
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }
}
