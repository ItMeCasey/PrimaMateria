package com.itmecasey.primamateria;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Every tunable value in the mod. The file {@code config/primamateria.properties} holds one {@code key=value} line per setting;
 * a missing key uses its default, and the file is rewritten with every key and a comment after loading.
 */
public final class Settings {
    private record Entry(String fallback, String comment) {
    }

    private static final Map<String, Entry> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    private static void def(String key, Object fallback, String comment) {
        DEFS.put(key, new Entry(String.valueOf(fallback), comment));
    }

    static {
        def("sieve.shakes_per_block", 8, "Shakes needed for a load of 1 block with a mesh of speed 1.0.");
        def("sieve.extra_block_factor", 0.5, "Extra work for each further block in a load, as a share of the first block.");
        def("sieve.max_load", 4, "Most blocks in one load (1 to 4).");
        def("sieve.drop_multiplier", 1.0, "Multiplies every sifting chance.");
        def("hammer.bonus_per_tier", 0.10, "Chance of one extra result per hammer tier above 2.");
        def("drive.windup_max_turns", 48, "Turns a windup drive stores.");
        def("drive.windup_ticks_per_shake", 8, "Ticks between shakes of a windup drive.");
        def("drive.furnace_ticks_per_shake", 4, "Ticks between shakes of a furnace drive.");
        def("drive.powered_ticks_per_shake", 2, "Ticks between shakes of a powered drive.");
        def("drive.powered_fe_per_shake", 40, "Energy (FE) a powered drive uses per shake.");
        def("drive.powered_capacity", 10000, "Energy (FE) a powered drive stores.");
        def("auto_hammer.strokes_per_block", 6, "Drive shakes an auto hammer needs to crush one block.");
        def("barrel.dew_ticks_per_mb", 4, "Ticks for a barrel under open sky to gain 1 mB of water. 0 switches dew off.");
        def("barrel.rain_multiplier", 8, "How much faster a barrel fills in rain.");
        def("barrel.time_multiplier", 1.0, "Multiplies compost, soak, steep and grow times.");
        def("crucible.mb_per_tick_per_heat", 2, "mB a crucible melts per tick for each point of heat below it.");
        def("start.island", "preset_only", "When the start island is placed: preset_only (Void Island worlds), always, never.");
        def("client.animations", "full", "Animation level: full, reduced or off. Client side.");
        def("client.particles", true, "Show sifting dust and falling fines. Client side.");
    }

    /** Every setting key, in file order. */
    public static Set<String> keys() {
        return DEFS.keySet();
    }

    private Settings() {
    }

    /** Restores every default. */
    public static synchronized void reset() {
        VALUES.clear();
    }

    private static synchronized String raw(String key) {
        Entry e = DEFS.get(key);
        if (e == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return VALUES.getOrDefault(key, e.fallback());
    }

    /** Overrides one setting (tests only). */
    public static synchronized void set(String key, Object value) {
        raw(key);
        VALUES.put(key, String.valueOf(value));
    }

    public static String getString(String key) {
        return raw(key).trim();
    }

    public static int getInt(String key) {
        try {
            return (int) Math.round(Double.parseDouble(raw(key).trim()));
        } catch (NumberFormatException ex) {
            return (int) Math.round(Double.parseDouble(DEFS.get(key).fallback()));
        }
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(raw(key).trim());
        } catch (NumberFormatException ex) {
            return Double.parseDouble(DEFS.get(key).fallback());
        }
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(raw(key).trim());
    }

    /** Reads overrides from a properties text (also used by tests). */
    public static synchronized void load(Reader reader) throws IOException {
        Properties p = new Properties();
        p.load(reader);
        VALUES.clear();
        for (String key : p.stringPropertyNames()) {
            if (DEFS.containsKey(key)) {
                VALUES.put(key, p.getProperty(key));
            }
        }
    }

    /** Loads the file if there is one, then rewrites it with every key so players can see what they can change. */
    public static synchronized void loadFile(Path file) {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    load(r);
                }
            }
            write(file);
        } catch (IOException | RuntimeException ex) {
            System.err.println("[PrimaMateria] Could not read " + file + ", using defaults: " + ex);
            VALUES.clear();
        }
    }

    private static void write(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("# Prima Materia settings. Edit a value and restart the game or server. Delete a line to restore its default.\n");
            for (Map.Entry<String, Entry> e : DEFS.entrySet()) {
                w.write("\n# " + e.getValue().comment() + "\n");
                w.write(e.getKey() + "=" + VALUES.getOrDefault(e.getKey(), e.getValue().fallback()) + "\n");
            }
        }
    }
}
