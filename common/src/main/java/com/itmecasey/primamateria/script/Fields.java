package com.itmecasey.primamateria.script;

import com.itmecasey.primamateria.data.RecipeLookup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads typed values out of the loosely typed maps a script hands over (numbers are doubles, arrays are lists). */
final class Fields {
    private Fields() {
    }

    static Map<String, Object> copy(Map<?, ?> in) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : in.entrySet()) {
            out.put(String.valueOf(e.getKey()), e.getValue());
        }
        return out;
    }

    /** A required text value (an id). */
    static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            throw new IllegalArgumentException("'" + key + "' is missing");
        }
        return String.valueOf(v).trim();
    }

    /** The id under {@code key}, or {@code #tag} when the object has a {@code tag} field. */
    static String idOrTag(Map<String, Object> m, String key) {
        if (m.containsKey("tag")) {
            String t = str(m, "tag");
            return t.startsWith("#") ? t : "#" + t;
        }
        return str(m, key);
    }

    static String optional(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    static int integer(Map<String, Object> m, String key, int fallback) {
        Object v = m.get(key);
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number n) {
            return (int) Math.round(n.doubleValue());
        }
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(v).trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + key + "' is not a number");
        }
    }

    static double number(Map<String, Object> m, String key, double fallback) {
        Object v = m.get(key);
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + key + "' is not a number");
        }
    }

    static boolean bool(Map<String, Object> m, String key, boolean fallback) {
        Object v = m.get(key);
        return v == null ? fallback : Boolean.parseBoolean(String.valueOf(v));
    }

    /** {@code count}: a number, or a list of two numbers (min, max). */
    static int[] range(Map<String, Object> m, String key, int min, int max) {
        Object v = m.get(key);
        if (v == null) {
            return new int[] {min, max};
        }
        if (v instanceof List<?> l && l.size() == 2 && l.get(0) instanceof Number a && l.get(1) instanceof Number b) {
            int lo = (int) Math.round(a.doubleValue());
            int hi = (int) Math.round(b.doubleValue());
            return new int[] {Math.min(lo, hi), Math.max(lo, hi)};
        }
        if (v instanceof Number n) {
            int c = (int) Math.round(n.doubleValue());
            return new int[] {c, c};
        }
        throw new IllegalArgumentException("'" + key + "' must be a number or [min, max]");
    }

    /** {@code #rrggbb}, {@code 0xrrggbb} or a plain number. */
    static int color(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            return 0xFFFFFF;
        }
        if (v instanceof Number n) {
            return n.intValue() & 0xFFFFFF;
        }
        String s = String.valueOf(v).trim();
        try {
            if (s.startsWith("#")) {
                return Integer.parseInt(s.substring(1), 16) & 0xFFFFFF;
            }
            if (s.startsWith("0x") || s.startsWith("0X")) {
                return Integer.parseInt(s.substring(2), 16) & 0xFFFFFF;
            }
            return Integer.parseInt(s) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + key + "' is not a color");
        }
    }

    /** {@code results}: a list of {@code {item, chance?, count?}} objects (a bare id counts as a sure drop). */
    static List<RecipeLookup.Chance> chances(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (!(v instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException("'" + key + "' must be a list of results");
        }
        List<RecipeLookup.Chance> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> raw) {
                Map<String, Object> r = copy(raw);
                int[] count = range(r, "count", 1, 1);
                out.add(new RecipeLookup.Chance(str(r, "item"), count[0], count[1], number(r, "chance", 1.0)));
            } else {
                out.add(new RecipeLookup.Chance(String.valueOf(o), 1, 1, 1.0));
            }
        }
        return out;
    }
}
