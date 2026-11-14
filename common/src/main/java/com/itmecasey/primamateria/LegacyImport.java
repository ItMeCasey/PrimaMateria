package com.itmecasey.primamateria;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * The one-time import of the old {@code config/primamateria.properties} of 2026.11.1 into {@code config/primamateria-common.toml}.
 * It runs only when the toml did not exist. Steps, in order: parse the old file (a corrupt or partly unreadable file is left exactly
 * where it is and the defaults are used); write the toml; re-read it and check every migrated value; write
 * {@code primamateria.properties.back} (a header line plus the exact original bytes; never over an existing backup, then
 * {@code .back2}, {@code .back3}); verify the backup; and only then remove the original. Any failing step leaves the original alone.
 * The toml itself is written by the loader's config system through the {@link Target}.
 */
public final class LegacyImport {
    /** Keys that were renamed since 2026.11.1: old name to new name. None so far. */
    private static final Map<String, String> RENAMED = Map.of();

    /** The loader side: write the values into the toml, and re-read the saved file to check them. */
    public interface Target {
        /** Sets these values (key to Boolean, Integer, Double or String) in the loaded config and saves it. */
        boolean write(Map<String, Object> values);

        /** Re-reads the saved toml and returns whether every value is in it. */
        boolean verify(Map<String, Object> values);
    }

    /** What reading the old file gave. */
    public record Parsed(boolean readable, boolean corrupt, byte[] original, Map<String, Object> values, List<String> invalid,
                         List<String> unknown) {
    }

    /** What the import did. */
    public enum Outcome { NOTHING_TO_IMPORT, MIGRATED, LEFT_IN_PLACE }

    private LegacyImport() {
    }

    /** Reads the old file strictly and checks every known key against its definition. Never changes anything. */
    public static Parsed parse(Path file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            return new Parsed(false, false, new byte[0], Map.of(), List.of(), List.of());
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new Parsed(true, true, bytes, Map.of(), List.of(), List.of());
        }
        if (text.indexOf('\0') >= 0) {
            return new Parsed(true, true, bytes, Map.of(), List.of(), List.of());
        }
        Properties p = new Properties();
        try {
            p.load(new StringReader(text));
        } catch (IOException | IllegalArgumentException e) {
            return new Parsed(true, true, bytes, Map.of(), List.of(), List.of());
        }
        Map<String, Object> values = new LinkedHashMap<>();
        List<String> invalid = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        Map<String, Settings.Def> defs = new LinkedHashMap<>();
        for (Settings.Def d : Settings.definitions()) {
            defs.put(d.key(), d);
        }
        for (String oldKey : new java.util.TreeSet<>(p.stringPropertyNames())) {
            String key = RENAMED.getOrDefault(oldKey, oldKey);
            Settings.Def d = defs.get(key);
            if (d == null) {
                unknown.add(oldKey);
                continue;
            }
            String raw = p.getProperty(oldKey).trim();
            Object value = convert(d, raw);
            if (value == null) {
                invalid.add(oldKey + "=" + raw);
            } else {
                values.put(key, value);
            }
        }
        return new Parsed(true, false, bytes, values, invalid, unknown);
    }

    /** The typed value of a setting from its text, or null when it is not valid for that setting. */
    static Object convert(Settings.Def d, String raw) {
        try {
            if (d.fallback() instanceof Boolean) {
                if (raw.equalsIgnoreCase("true")) {
                    return Boolean.TRUE;
                }
                return raw.equalsIgnoreCase("false") ? Boolean.FALSE : null;
            }
            if (d.fallback() instanceof Integer) {
                double n = Double.parseDouble(raw);
                if (Double.isNaN(n) || Double.isInfinite(n)) {
                    return null;
                }
                int whole = (int) Math.round(n);
                return whole < d.min().intValue() || whole > d.max().intValue() ? null : (Object) whole;
            }
            if (d.fallback() instanceof Double) {
                double n = Double.parseDouble(raw);
                return Double.isNaN(n) || n < d.min().doubleValue() || n > d.max().doubleValue() ? null : (Object) n;
            }
            return d.choices().contains(raw) ? raw : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The next free backup name: {@code .back}, then {@code .back2}, {@code .back3}. */
    public static Path nextBackup(Path legacy) {
        Path candidate = legacy.resolveSibling(legacy.getFileName() + ".back");
        for (int n = 2; Files.exists(candidate); n++) {
            candidate = legacy.resolveSibling(legacy.getFileName() + ".back" + n);
        }
        return candidate;
    }

    /** The first line of a backup. */
    public static String header(String version, String date, Path toml) {
        return "# Migrated by Prima Materia " + version + " on " + date + " to config/" + toml.getFileName()
                + "; this file is no longer read. Kept as a backup.\n";
    }

    /**
     * Runs the whole import for one old file.
     *
     * @param legacy the old {@code primamateria.properties}
     * @param toml   the new toml (named in the backup header and the log line)
     */
    public static Outcome migrate(Path legacy, Path toml, Target target, String version, String date, Consumer<String> log) {
        if (legacy == null || !Files.isRegularFile(legacy)) {
            return Outcome.NOTHING_TO_IMPORT;
        }
        Parsed parsed = parse(legacy);
        if (!parsed.readable() || parsed.corrupt()) {
            log.accept("Could not migrate " + legacy + ": " + (parsed.corrupt() ? "the file is corrupt" : "the file cannot be read")
                    + ". It stays where it is and the defaults are used.");
            return Outcome.LEFT_IN_PLACE;
        }
        for (String s : parsed.invalid()) {
            log.accept("Old setting " + s + " is not valid; the default is used.");
        }
        for (String s : parsed.unknown()) {
            log.accept("Old setting " + s + " is not a Prima Materia setting; ignored.");
        }
        if (!target.write(parsed.values()) || !target.verify(parsed.values())) {
            log.accept("Could not write and verify " + toml + " with the old values. " + legacy + " stays where it is.");
            return Outcome.LEFT_IN_PLACE;
        }
        Path back = nextBackup(legacy);
        byte[] head = header(version, date, toml).getBytes(StandardCharsets.UTF_8);
        byte[] expected = new byte[head.length + parsed.original().length];
        System.arraycopy(head, 0, expected, 0, head.length);
        System.arraycopy(parsed.original(), 0, expected, head.length, parsed.original().length);
        try {
            Files.write(back, expected, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            if (!Arrays.equals(Files.readAllBytes(back), expected)) {
                log.accept("The backup " + back + " does not hold the original; " + legacy + " stays where it is.");
                return Outcome.LEFT_IN_PLACE;
            }
            Files.delete(legacy);
        } catch (IOException | RuntimeException e) {
            log.accept("Could not move " + legacy + " to " + back + ": " + e + ". It stays where it is.");
            return Outcome.LEFT_IN_PLACE;
        }
        log.accept("Migrated " + parsed.values().size() + " settings: " + legacy + " moved to " + back + ", values in " + toml + ".");
        return Outcome.MIGRATED;
    }
}
