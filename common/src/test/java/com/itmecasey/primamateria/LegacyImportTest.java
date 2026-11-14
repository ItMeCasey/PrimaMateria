package com.itmecasey.primamateria;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The one-time import of the 2026.11.1 settings file into the toml, with the real file of that release as the fixture. */
class LegacyImportTest {
    @TempDir
    Path dir;

    /** A toml the test controls: records what was written and can be told to fail. */
    private static final class FakeToml implements LegacyImport.Target {
        Map<String, Object> written;
        boolean failWrite;
        boolean failVerify;

        @Override
        public boolean write(Map<String, Object> values) {
            written = values;
            return !failWrite;
        }

        @Override
        public boolean verify(Map<String, Object> values) {
            return !failVerify && values.equals(written);
        }
    }

    private byte[] release() throws IOException {
        // the real settings file written by the 2026.11.1 release; each loader module runs its tests in its own folder
        for (String base : new String[] {"../common", "common", "."}) {
            Path file = Path.of(base, "src/test/resources/config/primamateria-2026.11.1.properties");
            if (Files.isRegularFile(file)) {
                return Files.readAllBytes(file);
            }
        }
        throw new IOException("fixture primamateria-2026.11.1.properties not found");
    }

    private Path old(byte[] content) throws IOException {
        Path file = dir.resolve("primamateria.properties");
        Files.write(file, content);
        return file;
    }

    private LegacyImport.Outcome run(Path legacy, FakeToml toml, List<String> log) {
        return LegacyImport.migrate(legacy, dir.resolve("primamateria-common.toml"), toml, "2026.11.2", "2026-11-14", log::add);
    }

    @Test
    void theReleaseFileMigratesAndMovesToABackup() throws IOException {
        byte[] original = release();
        // the player changed a few values in the release's file
        String edited = new String(original, StandardCharsets.UTF_8).replace("sieve.link_radius=1", "sieve.link_radius=2")
                .replace("barrel.time_multiplier=1.0", "barrel.time_multiplier=0.5").replace("client.animations=full", "client.animations=reduced")
                .replace("sieve.link=true", "sieve.link=false");
        byte[] bytes = edited.getBytes(StandardCharsets.UTF_8);
        Path legacy = old(bytes);
        FakeToml toml = new FakeToml();
        List<String> log = new ArrayList<>();
        assertEquals(LegacyImport.Outcome.MIGRATED, run(legacy, toml, log));
        // exact values, typed, not defaults
        assertEquals(2, toml.written.get("sieve.link_radius"));
        assertEquals(0.5, toml.written.get("barrel.time_multiplier"));
        assertEquals("reduced", toml.written.get("client.animations"));
        assertEquals(false, toml.written.get("sieve.link"));
        assertEquals(Settings.keys().size(), toml.written.size());
        // the original is gone, the backup is the header line plus the exact original bytes
        assertFalse(Files.exists(legacy));
        Path back = dir.resolve("primamateria.properties.back");
        byte[] head = LegacyImport.header("2026.11.2", "2026-11-14", dir.resolve("primamateria-common.toml")).getBytes(StandardCharsets.UTF_8);
        byte[] expected = new byte[head.length + bytes.length];
        System.arraycopy(head, 0, expected, 0, head.length);
        System.arraycopy(bytes, 0, expected, head.length, bytes.length);
        assertArrayEquals(expected, Files.readAllBytes(back));
        assertTrue(Files.readString(back).startsWith("# Migrated by Prima Materia 2026.11.2 on 2026-11-14 to config/primamateria-common.toml; this file is no longer read. Kept as a backup.\n"));
        assertTrue(log.stream().anyMatch(l -> l.contains("moved to") && l.contains("primamateria.properties.back") && l.contains("primamateria-common.toml")));
        // a second launch finds no old file and does nothing
        assertEquals(LegacyImport.Outcome.NOTHING_TO_IMPORT, run(legacy, new FakeToml(), new ArrayList<>()));
        assertTrue(Files.exists(back));
    }

    @Test
    void theUntouchedReleaseFileGivesTheDefaults() throws IOException {
        LegacyImport.Parsed parsed = LegacyImport.parse(old(release()));
        assertTrue(parsed.readable() && !parsed.corrupt());
        assertTrue(parsed.invalid().isEmpty() && parsed.unknown().isEmpty());
        for (Settings.Def d : Settings.definitions()) {
            Object v = parsed.values().get(d.key());
            assertTrue(v instanceof Number n && d.fallback() instanceof Number f ? n.doubleValue() == f.doubleValue() : d.fallback().equals(v), d.key());
        }
    }

    @Test
    void aPartialFileKeepsWhatIsValidAndLogsTheRest() throws IOException {
        Path legacy = old("sieve.link_radius=3\nsieve.shakes_per_block=banana\nbarrel.rain_multiplier=500\nstart.island=sometimes\nold.removed=1\nclient.particles=false\n"
                .getBytes(StandardCharsets.UTF_8));
        FakeToml toml = new FakeToml();
        List<String> log = new ArrayList<>();
        assertEquals(LegacyImport.Outcome.MIGRATED, run(legacy, toml, log));
        assertEquals(Map.of("sieve.link_radius", 3, "client.particles", false), toml.written);
        assertTrue(log.stream().anyMatch(l -> l.contains("sieve.shakes_per_block=banana") && l.contains("default")));
        assertTrue(log.stream().anyMatch(l -> l.contains("barrel.rain_multiplier=500")));
        assertTrue(log.stream().anyMatch(l -> l.contains("start.island=sometimes")));
        assertTrue(log.stream().anyMatch(l -> l.contains("old.removed") && l.contains("ignored")));
    }

    @Test
    void anEmptyFileMigratesNothingAndIsStillBackedUp() throws IOException {
        Path legacy = old(new byte[0]);
        FakeToml toml = new FakeToml();
        assertEquals(LegacyImport.Outcome.MIGRATED, run(legacy, toml, new ArrayList<>()));
        assertTrue(toml.written.isEmpty());
        assertFalse(Files.exists(legacy));
        assertEquals(LegacyImport.header("2026.11.2", "2026-11-14", dir.resolve("primamateria-common.toml")), Files.readString(dir.resolve("primamateria.properties.back")));
    }

    @Test
    void aMissingFileDoesNothing() {
        assertEquals(LegacyImport.Outcome.NOTHING_TO_IMPORT, run(dir.resolve("primamateria.properties"), new FakeToml(), new ArrayList<>()));
        assertEquals(LegacyImport.Outcome.NOTHING_TO_IMPORT, run(null, new FakeToml(), new ArrayList<>()));
    }

    @Test
    void aCorruptFileStaysExactlyWhereItIs() throws IOException {
        byte[][] corrupt = {
                {(byte) 0xC3, (byte) 0x28, 's', 'i', 'e', 'v', 'e', '=', '1'},          // invalid UTF-8
                "sieve.link_radius=\\u12\n".getBytes(StandardCharsets.UTF_8),           // a broken unicode escape
                {'a', '=', 'b', 0, 0, 1}};                                             // binary
        for (byte[] bytes : corrupt) {
            Path legacy = old(bytes);
            FakeToml toml = new FakeToml();
            List<String> log = new ArrayList<>();
            assertEquals(LegacyImport.Outcome.LEFT_IN_PLACE, run(legacy, toml, log));
            assertArrayEquals(bytes, Files.readAllBytes(legacy));
            assertFalse(Files.exists(dir.resolve("primamateria.properties.back")));
            assertTrue(toml.written == null);
            assertTrue(log.stream().anyMatch(l -> l.contains("corrupt") && l.contains("stays where it is")));
        }
    }

    @Test
    void aFailedWriteOrCheckKeepsTheOriginalAndMakesNoBackup() throws IOException {
        byte[] bytes = release();
        for (int mode = 0; mode < 2; mode++) {
            Path legacy = old(bytes);
            FakeToml toml = new FakeToml();
            toml.failWrite = mode == 0;
            toml.failVerify = mode == 1;
            List<String> log = new ArrayList<>();
            assertEquals(LegacyImport.Outcome.LEFT_IN_PLACE, run(legacy, toml, log));
            assertArrayEquals(bytes, Files.readAllBytes(legacy));
            assertFalse(Files.exists(dir.resolve("primamateria.properties.back")));
            assertTrue(log.stream().anyMatch(l -> l.contains("stays where it is")));
        }
    }

    @Test
    void anExistingBackupIsNeverOverwritten() throws IOException {
        Files.writeString(dir.resolve("primamateria.properties.back"), "first backup");
        Files.writeString(dir.resolve("primamateria.properties.back2"), "second backup");
        byte[] bytes = "sieve.link_radius=2\n".getBytes(StandardCharsets.UTF_8);
        Path legacy = old(bytes);
        assertEquals(LegacyImport.Outcome.MIGRATED, run(legacy, new FakeToml(), new ArrayList<>()));
        assertEquals("first backup", Files.readString(dir.resolve("primamateria.properties.back")));
        assertEquals("second backup", Files.readString(dir.resolve("primamateria.properties.back2")));
        assertTrue(Files.readString(dir.resolve("primamateria.properties.back3")).endsWith("sieve.link_radius=2\n"));
        assertFalse(Files.exists(legacy));
    }

    @Test
    void settingDefinitionsFeedTheLoaderConfigSpec() {
        for (Settings.Def d : Settings.definitions()) {
            assertTrue(!d.comment().isBlank() && d.key().contains("."), d.key());
            if (d.fallback() instanceof Integer n) {
                assertTrue(d.min().intValue() <= n && n <= d.max().intValue(), d.key() + " default inside its range");
            } else if (d.fallback() instanceof Double x) {
                assertTrue(d.min().doubleValue() <= x && x <= d.max().doubleValue(), d.key() + " default inside its range");
            } else if (d.fallback() instanceof String s) {
                assertTrue(d.choices().contains(s), d.key() + " default is one of its choices");
            }
            assertEquals(d.key().startsWith("client."), d.client());
        }
    }
}
