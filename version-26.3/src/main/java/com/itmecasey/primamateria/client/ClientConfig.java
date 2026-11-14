package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.Settings;

/** The client settings the renderer reads, refreshed every 20 reads so a frame never parses strings. */
public final class ClientConfig {
    public static final int FULL = 0;
    public static final int REDUCED = 1;
    public static final int OFF = 2;

    private static int mode;
    private static boolean particles = true;
    private static float distance = 48F;
    private static int soundsPerTick = 4;
    private static int countdown;

    private ClientConfig() {
    }

    private static void refresh() {
        if (countdown-- > 0) {
            return;
        }
        countdown = 20;
        String m = Settings.getString("client.animations").toLowerCase(java.util.Locale.ROOT);
        mode = m.equals("off") ? OFF : m.equals("reduced") ? REDUCED : FULL;
        particles = Settings.getBool("client.particles");
        distance = (float) Math.max(8.0, Settings.getDouble("client.animation_distance"));
        soundsPerTick = Math.max(0, Settings.getInt("client.sounds_per_tick"));
    }

    /** 0 full, 1 reduced, 2 off. */
    public static int mode() {
        refresh();
        return mode;
    }

    public static boolean particles() {
        refresh();
        return particles;
    }

    /** Beyond this many blocks a moving sieve is drawn in its rest pose with a flat layer. */
    public static float distance() {
        refresh();
        return distance;
    }

    public static int soundsPerTick() {
        refresh();
        return soundsPerTick;
    }
}
