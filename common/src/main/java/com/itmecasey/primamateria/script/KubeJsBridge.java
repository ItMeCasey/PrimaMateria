package com.itmecasey.primamateria.script;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * KubeJS support without a compile-time dependency on any KubeJS version (there is no KubeJS build for Minecraft 26.2 or 26.3 yet):
 * scripts reach the {@link PrimaMateriaApi} through KubeJS's own {@code Java.loadClass}. When KubeJS is installed, a small startup
 * script is written once to {@code kubejs/startup_scripts/primamateria.js}; it puts the API in {@code global.PrimaMateria}. An
 * existing file is never overwritten. Recipes, drop tables, mesh tiers, heat sources, melts and compost values need no bridge:
 * they are datapack JSON and recipe types, so KubeJS edits them directly.
 */
public final class KubeJsBridge {
    private static final Logger LOG = Logger.getLogger("primamateria");

    static final String FILE = "primamateria.js";

    static final String SCRIPT = "// Written by Prima Materia. An existing file is never overwritten; delete it to get a fresh copy.\n"
            + "// Use global.PrimaMateria in any script type: global.PrimaMateria.on('sifted', e => { ... }).\n"
            + "// Recipes and data files are plain datapack JSON and recipe types, so ServerEvents.recipes edits them directly.\n"
            + "global.PrimaMateria = Java.loadClass('com.itmecasey.primamateria.script.PrimaMateriaApi').INSTANCE\n"
            + "global.PrimaMateria.clearListeners()\n";

    private KubeJsBridge() {
    }

    /** True when a KubeJS class is visible. */
    public static boolean present() {
        for (String name : new String[] {"dev.latvian.mods.kubejs.KubeJS", "dev.latvian.mods.kubejs.KubeJSPlugin"}) {
            try {
                Class.forName(name, false, KubeJsBridge.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                // try the next name
            }
        }
        return false;
    }

    /** Writes the helper into the game folder (the process working folder) when KubeJS is there. */
    public static boolean install() {
        return install(Path.of("").toAbsolutePath());
    }

    /** Writes the helper script into {@code gameDir/kubejs} when KubeJS is installed or its folder already exists. */
    public static boolean install(Path gameDir) {
        if (!present() && !Files.isDirectory(gameDir.resolve("kubejs"))) {
            return false;
        }
        return writeHelper(gameDir);
    }

    static boolean writeHelper(Path gameDir) {
        Path file = gameDir.resolve("kubejs").resolve("startup_scripts").resolve(FILE);
        try {
            if (Files.exists(file) && !Files.isRegularFile(file)) {
                return false;
            }
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, SCRIPT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                LOG.info("Prima Materia: wrote " + file + " so KubeJS scripts get the PrimaMateria global");
            }
        } catch (FileAlreadyExistsException e) {
            return Files.isRegularFile(file);
        } catch (IOException e) {
            LOG.warning("Prima Materia: could not write the KubeJS helper script: " + e);
            return false;
        }
        return true;
    }
}
