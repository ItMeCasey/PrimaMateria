package com.itmecasey.primamateria.client;

/** Dev-run timing of our renderer code (extraction and submit, per sieve). Only counted when the dev switch is on. */
public final class SieveStats {
    public static long nanos;
    public static long calls;
    public static long drawn;

    private SieveStats() {
    }

    public static void reset() {
        nanos = 0;
        calls = 0;
        drawn = 0;
    }
}
