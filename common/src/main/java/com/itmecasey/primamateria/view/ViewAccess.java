package com.itmecasey.primamateria.view;

/**
 * Reads the view record of the block at a position without game classes in this package. The game side installs a resolver once
 * (see {@code compat/GameAccess}); scripts and anything else in {@code common} call {@link #at}.
 */
public final class ViewAccess {
    /** Looks up the {@link ViewProvider} block entity at a position of a level and returns its view, or null. */
    @FunctionalInterface
    public interface Resolver {
        Object viewAt(Object level, int x, int y, int z);
    }

    private static volatile Resolver resolver;

    private ViewAccess() {
    }

    public static void install(Resolver value) {
        resolver = value;
    }

    public static boolean installed() {
        return resolver != null;
    }

    /** The view at a position when it is of the wanted type, else null. Never throws. */
    public static <T> T at(Object level, int x, int y, int z, Class<T> type) {
        Resolver r = resolver;
        if (r == null || level == null) {
            return null;
        }
        try {
            Object v = r.viewAt(level, x, y, z);
            return type.isInstance(v) ? type.cast(v) : null;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}
