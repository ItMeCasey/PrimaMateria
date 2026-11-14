package com.itmecasey.primamateria.script;

import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.view.AutoHammerView;
import com.itmecasey.primamateria.view.BarrelView;
import com.itmecasey.primamateria.view.CrucibleView;
import com.itmecasey.primamateria.view.DriveView;
import com.itmecasey.primamateria.view.SieveView;
import com.itmecasey.primamateria.view.TrayView;
import com.itmecasey.primamateria.view.ViewAccess;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * The scripting surface of Prima Materia, the global {@code PrimaMateria} of ModJS and (through the helper script) KubeJS. Everything
 * is plain strings, numbers and maps. Server side only.
 *
 * <pre>{@code
 * PrimaMateria.on('sifted', e => { if (e.input() == 'minecraft:gravel') e.addDrop('minecraft:flint', 1); });
 * PrimaMateria.sifting.add({ input: 'minecraft:sand', result: 'minecraft:gold_nugget', chance: 0.02, mesh_min: 4 });
 * }</pre>
 */
public final class PrimaMateriaApi {
    private static final Logger LOG = Logger.getLogger("primamateria");

    /** The one shared object both bridges hand to scripts. */
    public static final PrimaMateriaApi INSTANCE = new PrimaMateriaApi();

    /** A content family: {@code add}, {@code remove}, {@code replace}. */
    public static final class Content {
        private final String family;

        Content(String family) {
            this.family = family;
        }

        /** Adds an entry; the fields are those of the recipe JSON. Returns false when the object is not valid (logged). */
        public boolean add(Map<String, Object> data) {
            return guard(() -> ScriptContent.add(family, data));
        }

        /** Removes every entry whose fields equal the filter's, for example {@code {input: 'minecraft:gravel'}}. */
        public boolean remove(Map<String, Object> filter) {
            return guard(() -> ScriptContent.remove(family, filter));
        }

        /** Removes the matches of the filter and adds the new entry. */
        public boolean replace(Map<String, Object> filter, Map<String, Object> data) {
            return guard(() -> ScriptContent.replace(family, filter, data));
        }
    }

    /** Read-only access to {@code primamateria.properties}. */
    public static final class Config {
        /** A setting as text; "" for an unknown key. */
        public String get(String key) {
            try {
                return Settings.getString(key);
            } catch (RuntimeException e) {
                return "";
            }
        }

        public double number(String key) {
            try {
                return Settings.getDouble(key);
            } catch (RuntimeException e) {
                return 0;
            }
        }

        public boolean bool(String key) {
            try {
                return Settings.getBool(key);
            } catch (RuntimeException e) {
                return false;
            }
        }
    }

    public final Content crushing = new Content("crushing");
    public final Content sifting = new Content("sifting");
    public final Content residue = new Content("residue");
    public final Content composting = new Content("composting");
    public final Content soaking = new Content("soaking");
    public final Content mixing = new Content("mixing");
    public final Content steeping = new Content("steeping");
    public final Content growing = new Content("growing");
    public final Content melting = new Content("melting");
    public final Content casting = new Content("casting");
    public final Content meshes = new Content("meshes");
    public final Content heat = new Content("heat");
    public final Content compost = new Content("compost");
    public final Config config = new Config();

    private final List<Runnable> removers = new ArrayList<>();

    private PrimaMateriaApi() {
    }

    private static boolean guard(java.util.function.BooleanSupplier call) {
        try {
            return call.getAsBoolean();
        } catch (RuntimeException e) {
            LOG.warning("Prima Materia: a script call failed: " + e);
            return false;
        }
    }

    /** Wraps a script function so an error in it is one log line, never a crash. */
    private <E> Consumer<E> safe(String name, Consumer<ScriptEvent> listener, java.util.function.Function<E, ScriptEvent> wrap) {
        return event -> {
            try {
                listener.accept(wrap.apply(event));
            } catch (RuntimeException | LinkageError e) {
                LOG.warning("Prima Materia: a script listener for '" + name + "' failed: " + e);
            }
        };
    }

    /**
     * Listens to a game event: sifted, crushed, barrelFinished, melted, cast or driveWound. A listener can change
     * {@code e.drops()} (sifted, crushed) and {@code e.cancel()} a sifted load. Returns false for an unknown name.
     */
    public synchronized boolean on(String name, Consumer<ScriptEvent> listener) {
        if (name == null || listener == null) {
            return false;
        }
        switch (name) {
            case "sifted" -> add(GameEvents.SIFTED, safe(name, listener, ScriptEvent::of));
            case "crushed" -> add(GameEvents.CRUSHED, safe(name, listener, ScriptEvent::of));
            case "barrelFinished" -> add(GameEvents.BARREL_FINISHED, safe(name, listener, ScriptEvent::of));
            case "melted" -> add(GameEvents.MELTED, safe(name, listener, ScriptEvent::of));
            case "cast" -> add(GameEvents.CAST, safe(name, listener, ScriptEvent::of));
            case "driveWound" -> add(GameEvents.DRIVE_WOUND, safe(name, listener, ScriptEvent::of));
            default -> {
                LOG.warning("Prima Materia: there is no event called '" + name + "'");
                return false;
            }
        }
        return true;
    }

    private <E> void add(GameEvents.Channel<E> channel, Consumer<E> listener) {
        channel.listen(listener);
        removers.add(() -> channel.remove(listener));
    }

    /**
     * Drops every listener and every recipe or data change a script made. Call it first in a script that is loaded again on
     * {@code /reload}, so changes do not stack.
     */
    public synchronized void reset() {
        for (Runnable r : removers) {
            r.run();
        }
        removers.clear();
        ScriptContent.clear();
    }

    /** Same as {@link #reset()}: the name the KubeJS helper uses. */
    public void clearListeners() {
        reset();
    }

    // ---- state (read only; null when the block is something else) ----

    public SieveView sieveAt(Object level, int x, int y, int z) {
        return ViewAccess.at(level, x, y, z, SieveView.class);
    }

    public BarrelView barrelAt(Object level, int x, int y, int z) {
        return ViewAccess.at(level, x, y, z, BarrelView.class);
    }

    public CrucibleView crucibleAt(Object level, int x, int y, int z) {
        return ViewAccess.at(level, x, y, z, CrucibleView.class);
    }

    public TrayView trayAt(Object level, int x, int y, int z) {
        return ViewAccess.at(level, x, y, z, TrayView.class);
    }

    public DriveView driveAt(Object level, int x, int y, int z) {
        return ViewAccess.at(level, x, y, z, DriveView.class);
    }

    public AutoHammerView autoHammerAt(Object level, int x, int y, int z) {
        return ViewAccess.at(level, x, y, z, AutoHammerView.class);
    }

    /** The same lookups with a block position object (anything with getX, getY and getZ). */
    public SieveView sieveAt(Object level, Object pos) {
        int[] p = coords(pos);
        return p == null ? null : sieveAt(level, p[0], p[1], p[2]);
    }

    public BarrelView barrelAt(Object level, Object pos) {
        int[] p = coords(pos);
        return p == null ? null : barrelAt(level, p[0], p[1], p[2]);
    }

    public CrucibleView crucibleAt(Object level, Object pos) {
        int[] p = coords(pos);
        return p == null ? null : crucibleAt(level, p[0], p[1], p[2]);
    }

    public TrayView trayAt(Object level, Object pos) {
        int[] p = coords(pos);
        return p == null ? null : trayAt(level, p[0], p[1], p[2]);
    }

    public DriveView driveAt(Object level, Object pos) {
        int[] p = coords(pos);
        return p == null ? null : driveAt(level, p[0], p[1], p[2]);
    }

    public AutoHammerView autoHammerAt(Object level, Object pos) {
        int[] p = coords(pos);
        return p == null ? null : autoHammerAt(level, p[0], p[1], p[2]);
    }

    private static int[] coords(Object pos) {
        if (pos == null) {
            return null;
        }
        try {
            Method gx = pos.getClass().getMethod("getX");
            Method gy = pos.getClass().getMethod("getY");
            Method gz = pos.getClass().getMethod("getZ");
            return new int[] {((Number) gx.invoke(pos)).intValue(), ((Number) gy.invoke(pos)).intValue(),
                    ((Number) gz.invoke(pos)).intValue()};
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
