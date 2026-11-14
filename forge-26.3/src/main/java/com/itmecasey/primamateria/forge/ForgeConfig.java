package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.LegacyImport;
import com.itmecasey.primamateria.Settings;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The loader's own config system for every setting: a ForgeConfigSpec built from {@code Settings.definitions()} (the single list of
 * keys, defaults, ranges and descriptions) and registered as {@code config/primamateria-common.toml}, with the client settings in
 * its {@code [client]} table. Every value the loader loads or reloads is pushed into {@code Settings}, which the rest of the mod
 * reads. When the toml did not exist and a {@code primamateria.properties} of 2026.11.1 does, {@link LegacyImport} moves the
 * settings across once (and the old file to a {@code .back}).
 */
public final class ForgeConfig {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("PrimaMateria");
    private static final String FILE = "primamateria-common.toml";
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();
    private static ForgeConfigSpec spec;
    private static Path legacyFile;
    private static Path tomlFile;
    private static boolean importPending;

    private ForgeConfig() {
    }

    /** The loader's config registration: a config type, its spec and the file name. */
    public interface Registrar {
        void register(ModConfig.Type type, ForgeConfigSpec spec, String fileName);
    }

    /** Builds the spec and registers it. Call from the mod constructor with the config folder. */
    public static void register(Registrar registrar, Path configDir) {
        legacyFile = configDir.resolve("primamateria.properties");
        tomlFile = configDir.resolve(FILE);
        importPending = java.nio.file.Files.notExists(tomlFile) && java.nio.file.Files.isRegularFile(legacyFile);
        spec = build();
        registrar.register(ModConfig.Type.COMMON, spec, FILE);
    }

    private static ForgeConfigSpec build() {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        String section = null;
        for (Settings.Def d : Settings.definitions()) {
            int dot = d.key().indexOf('.');
            String sec = d.key().substring(0, dot);
            String name = d.key().substring(dot + 1);
            if (!sec.equals(section)) {
                if (section != null) {
                    b.pop();
                }
                b.push(sec);
                section = sec;
            }
            VALUES.put(d.key(), define(b, d, name));
        }
        if (section != null) {
            b.pop();
        }
        return b.build();
    }

    private static ForgeConfigSpec.ConfigValue<?> define(ForgeConfigSpec.Builder b, Settings.Def d, String name) {
        if (d.fallback() instanceof Boolean flag) {
            return b.comment(d.comment()).define(name, flag.booleanValue());
        }
        if (d.fallback() instanceof Integer whole) {
            return b.comment(d.comment()).defineInRange(name, whole.intValue(), d.min().intValue(), d.max().intValue());
        }
        if (d.fallback() instanceof Double real) {
            return b.comment(d.comment()).defineInRange(name, real.doubleValue(), d.min().doubleValue(), d.max().doubleValue());
        }
        return b.comment(d.comment() + " One of " + String.join(", ", d.choices()) + ".")
                .defineInList(name, String.valueOf(d.fallback()), new java.util.ArrayList<>(d.choices()));
    }

    /** The loader fires this when the config file is loaded (first time) or changed on disk. */
    public static void onConfig(ModConfig config, boolean loading) {
        if (config.getSpec() != spec) {
            return;
        }
        if (loading && importPending) {
            importPending = false;
            String version = net.minecraftforge.fml.ModList.getModContainerById("primamateria").map(c -> c.getModInfo().getVersion().toString()).orElse("?");
            LegacyImport.migrate(legacyFile, tomlFile, new Writer(), version, java.time.LocalDate.now().toString(), LOGGER::info);
        }
        for (Settings.Def d : Settings.definitions()) {
            Settings.apply(d.key(), VALUES.get(d.key()).get());
        }
    }

    /** Writes imported values through the loader's spec and re-reads the saved toml with the loader's own toml library. */
    private static final class Writer implements LegacyImport.Target {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public boolean write(Map<String, Object> values) {
            try {
                for (Map.Entry<String, Object> e : values.entrySet()) {
                    ((ForgeConfigSpec.ConfigValue) VALUES.get(e.getKey())).set(e.getValue());
                }
                spec.save();
                return true;
            } catch (RuntimeException ex) {
                LOGGER.warn("Could not write the migrated settings: {}", ex.toString());
                return false;
            }
        }

        @Override
        public boolean verify(Map<String, Object> values) {
            try (com.electronwill.nightconfig.core.file.FileConfig saved = com.electronwill.nightconfig.core.file.FileConfig.of(tomlFile)) {
                saved.load();
                for (Map.Entry<String, Object> e : values.entrySet()) {
                    Object got = saved.get(e.getKey());
                    boolean same = got instanceof Number a && e.getValue() instanceof Number b ? a.doubleValue() == b.doubleValue()
                            : e.getValue().equals(got);
                    if (!same) {
                        LOGGER.warn("Migrated setting {} reads back as {} instead of {}", e.getKey(), got, e.getValue());
                        return false;
                    }
                }
                return true;
            } catch (RuntimeException ex) {
                LOGGER.warn("Could not re-read {}: {}", tomlFile, ex.toString());
                return false;
            }
        }
    }
}
