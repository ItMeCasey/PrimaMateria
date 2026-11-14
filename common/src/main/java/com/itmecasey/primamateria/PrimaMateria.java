package com.itmecasey.primamateria;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.Supplier;

/** Entry point shared by every loader layer. The layers call {@link #init} from their mod constructor. */
public final class PrimaMateria {
    public static final String MODID = Ids.MODID;
    public static final String NAME = "Prima Materia";

    private PrimaMateria() {
    }

    public static void init(Supplier<Path> configDir) {
        // the settings come from the loader's config system (NeoForgeConfig, ForgeConfig); nothing is read here
        // The script bridge is optional and lives in its own package; it is found by name so this file never changes for it.
        callIfPresent("com.itmecasey.primamateria.script.ModJsBridge", "install");
    }

    /** Calls a public static no-argument method by name when the class is part of this build. Never throws. */
    static void callIfPresent(String className, String method) {
        try {
            Method m = Class.forName(className).getMethod(method);
            m.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Not built yet or not part of this jar: nothing to do.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            System.err.println("[PrimaMateria] " + className + "." + method + " failed: " + e);
        }
    }
}
