package com.itmecasey.primamateria.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.event.GameEvents;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** The KubeJS helper script, and proof that the mod works with no script mod present. */
class KubeJsAndOptionalTest {
    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        PrimaMateriaApi.INSTANCE.reset();
    }

    @Test
    void helperIsWrittenOnceIntoTheKubeJsFolder() throws IOException {
        assertTrue(KubeJsBridge.writeHelper(dir));
        String script = Files.readString(dir.resolve("kubejs/startup_scripts/primamateria.js"));
        assertTrue(script.contains("global.PrimaMateria = Java.loadClass('com.itmecasey.primamateria.script.PrimaMateriaApi').INSTANCE"));
        assertTrue(script.contains("clearListeners()"));
    }

    @Test
    void anExistingHelperIsKept() throws IOException {
        Path file = dir.resolve("kubejs/startup_scripts/primamateria.js");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "// mine");
        assertTrue(KubeJsBridge.writeHelper(dir));
        assertEquals("// mine", Files.readString(file));
    }

    @Test
    void aBlockedPathIsReportedNotThrown() throws IOException {
        Files.writeString(dir.resolve("kubejs"), "a file where the folder should be");
        assertFalse(KubeJsBridge.writeHelper(dir));
    }

    @Test
    void withoutKubeJsNothingIsCreated() {
        assertFalse(KubeJsBridge.install(dir));
        assertFalse(Files.exists(dir.resolve("kubejs")));
    }

    @Test
    void anExistingKubeJsFolderGetsTheHelper() throws IOException {
        Files.createDirectories(dir.resolve("kubejs"));
        assertTrue(KubeJsBridge.install(dir));
        assertTrue(Files.exists(dir.resolve("kubejs/startup_scripts/primamateria.js")));
    }

    /** Runs the generated helper the way KubeJS does (its Java.loadClass) and then uses the global. */
    @Test
    void theHelperGivesScriptsTheSameApi() {
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            cx.evaluateString(scope, "var global = {}; var Java = { loadClass: function (n) { return eval('Packages.' + n); } };",
                    "shim.js", 1, null);
            cx.evaluateString(scope, KubeJsBridge.SCRIPT, "primamateria.js", 1, null);
            cx.evaluateString(scope, "global.PrimaMateria.on('sifted', e => e.addDrop('minecraft:stick', 3));"
                    + "global.PrimaMateria.sifting.add({ input: 'minecraft:sand', result: 'minecraft:stick' });", "user.js", 1, null);
            GameEvents.Sifted event = new GameEvents.Sifted(new GameEvents.Where("w", 0, 0, 0, null), "minecraft:sand", "", 0, List.of());
            GameEvents.SIFTED.fire(event);
            assertEquals(1, event.drops.size());
            assertEquals(3, event.drops.get(0).count());
            assertTrue(ScriptContent.hasChanges());
            // loading the helper again (a script reload) clears what the old scripts registered
            cx.evaluateString(scope, KubeJsBridge.SCRIPT, "primamateria.js", 1, null);
            GameEvents.Sifted again = new GameEvents.Sifted(new GameEvents.Where("w", 0, 0, 0, null), "minecraft:sand", "", 0, List.of());
            GameEvents.SIFTED.fire(again);
            assertTrue(again.drops.isEmpty());
            assertFalse(ScriptContent.hasChanges());
        } finally {
            Context.exit();
        }
    }

    /** A class loader that behaves as if ModJS were not installed: the mod's own classes load, the ModJS ones do not. */
    private static final class NoModJsLoader extends ClassLoader {
        NoModJsLoader() {
            super(KubeJsAndOptionalTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("com.itmecasey.modjs.")) {
                throw new ClassNotFoundException(name);
            }
            if (name.startsWith("com.itmecasey.primamateria.script.")) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (in == null) {
                                throw new ClassNotFoundException(name);
                            }
                            byte[] bytes = in.readAllBytes();
                            c = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }
                    }
                    return c;
                }
            }
            return super.loadClass(name, resolve);
        }
    }

    @Test
    void theModRunsTheSameWithoutAnyScriptMod() throws Exception {
        ClassLoader loader = new NoModJsLoader();
        Class<?> bridge = loader.loadClass("com.itmecasey.primamateria.script.ModJsBridge");
        Class<?> api = loader.loadClass("com.itmecasey.primamateria.script.PrimaMateriaApi");
        Object instance = api.getField("INSTANCE").get(null);
        // no ModJS: the binding is not installed and nothing throws
        assertEquals(Boolean.FALSE, bridge.getMethod("install", api).invoke(null, instance));
        // the entry point PrimaMateria.init calls must also be quiet
        bridge.getMethod("install").invoke(null);
        // the game events still work with no listeners: a fired event just returns
        assertEquals(0, new GameEvents.Sifted(new GameEvents.Where("w", 0, 0, 0, null), "a", "", 0, List.of()).drops.size());
        GameEvents.SIFTED.fire(new GameEvents.Sifted(new GameEvents.Where("w", 0, 0, 0, null), "a", "", 0, List.of()));
    }
}
