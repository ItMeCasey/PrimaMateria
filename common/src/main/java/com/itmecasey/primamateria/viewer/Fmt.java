package com.itmecasey.primamateria.viewer;

import java.util.Locale;

/** Number formats for card values: always a value and a unit, never a sentence. */
public final class Fmt {
    private Fmt() {
    }

    /** {@code 16%}, {@code 4.5%}, {@code 0.35%}; a sure drop is {@code 100%}. */
    public static String chance(double chance) {
        double pct = chance * 100.0;
        if (pct >= 99.995) {
            return "100%";
        }
        if (pct >= 10) {
            return Math.round(pct) + "%";
        }
        if (pct >= 1) {
            return trim(String.format(Locale.ROOT, "%.1f", pct)) + "%";
        }
        return trim(String.format(Locale.ROOT, "%.2f", pct)) + "%";
    }

    /** {@code 1-2}, or empty when the count is a single number (the stack size already shows it). */
    public static String range(int min, int max) {
        return min == max ? "" : min + "-" + max;
    }

    /** Ticks as seconds: {@code 20 s}, {@code 0.5 s}. */
    public static String seconds(int ticks) {
        double s = ticks / 20.0;
        return (s == Math.floor(s) ? String.valueOf((long) s) : trim(String.format(Locale.ROOT, "%.1f", s))) + " s";
    }

    public static String mb(int amount) {
        return amount + " mB";
    }

    /** A mesh tier or a run of tiers: {@code T3} or {@code T3-5}. */
    public static String tiers(int from, int to) {
        return from == to ? "T" + from : "T" + from + "-" + to;
    }

    private static String trim(String s) {
        if (s.indexOf('.') < 0) {
            return s;
        }
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
