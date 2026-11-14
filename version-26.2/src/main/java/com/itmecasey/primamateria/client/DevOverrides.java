package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.Dev;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;

/**
 * Dev runs only: lets the screenshot tour show a made-up view (a sift in progress, a barrel with crystals) on a real block
 * without touching the server. The renderers ask here first; in a normal run {@link Dev#ENABLED} is false and nothing is looked up.
 */
public final class DevOverrides {
    private static final Map<Long, Supplier<Object>> VIEWS = new HashMap<>();

    private DevOverrides() {
    }

    public static void put(BlockPos pos, Supplier<Object> view) {
        VIEWS.put(pos.asLong(), view);
    }

    public static void clear() {
        VIEWS.clear();
    }

    /** The made-up view for this position, or null. */
    public static Object get(BlockPos pos) {
        if (!Dev.ENABLED || VIEWS.isEmpty()) {
            return null;
        }
        Supplier<Object> s = VIEWS.get(pos.asLong());
        return s == null ? null : s.get();
    }
}
