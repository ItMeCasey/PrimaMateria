package com.itmecasey.primamateria.script;

import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.data.RecipeLookup.Snapshot;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Recipe and data changes made by scripts. A script says "add this sifting drop", "remove everything that matches this filter" or
 * "replace it"; the calls are kept in script order and {@link #apply} runs them over a {@link Snapshot} after each datapack reload
 * (workstream A calls it once, before {@code RecipeLookup.set}). The field names are the ones of the recipe JSON. A change that is
 * not valid is logged on one plain line and skipped, so a script can never break a reload.
 */
public final class ScriptContent {
    private static final Logger LOG = Logger.getLogger("primamateria");

    /** One kind of content: its name in scripts, how to read it from a snapshot and how to make one from a script object. */
    record Family<R>(String name, Function<Snapshot, List<R>> read, BiFunction<String, Map<String, Object>, R> make) {
    }

    private enum Kind {
        ADD, REMOVE, REPLACE
    }

    private record Op(Kind kind, String family, Map<String, Object> filter, Map<String, Object> data, int serial) {
    }

    static final Map<String, Family<?>> FAMILIES = new LinkedHashMap<>();
    private static final List<Op> OPS = new CopyOnWriteArrayList<>();
    private static int serial;

    private static <R> void family(String name, Function<Snapshot, List<R>> read, BiFunction<String, Map<String, Object>, R> make) {
        FAMILIES.put(name, new Family<>(name, read, make));
    }

    static {
        family("crushing", Snapshot::crushing, (id, m) -> new RecipeLookup.Crushing(id, Fields.str(m, "input"),
                Fields.integer(m, "hammer_tier", 1), Fields.chances(m, "results")));
        family("sifting", Snapshot::sifting, (id, m) -> {
            int[] count = Fields.range(m, "count", 1, 1);
            return new RecipeLookup.Sifting(id, Fields.str(m, "input"), Fields.str(m, "result"), count[0], count[1],
                    Fields.number(m, "chance", 1.0), Fields.integer(m, "mesh_min", 1), Fields.integer(m, "mesh_max", 0));
        });
        family("residue", Snapshot::residue, (id, m) -> new RecipeLookup.Residue(id, Fields.str(m, "input"),
                Fields.str(m, "result"), Fields.number(m, "chance", 1.0)));
        family("composting", Snapshot::composting, (id, m) -> new RecipeLookup.Composting(id, Fields.str(m, "result"),
                Fields.integer(m, "time", 400)));
        family("soaking", Snapshot::soaking, (id, m) -> new RecipeLookup.Soaking(id, Fields.str(m, "fluid"),
                Fields.integer(m, "amount", 250), Fields.str(m, "input"), Fields.str(m, "result"), Fields.integer(m, "time", 100)));
        family("mixing", Snapshot::mixing, (id, m) -> new RecipeLookup.Mixing(id, Fields.str(m, "base_fluid"),
                Fields.str(m, "added_fluid"), Fields.integer(m, "amount", 1000), Fields.str(m, "result"),
                Fields.bool(m, "needs_heatproof", false)));
        family("steeping", Snapshot::steeping, (id, m) -> new RecipeLookup.Steeping(id, Fields.str(m, "fluid"),
                Fields.str(m, "input"), Fields.integer(m, "count", 1), Fields.str(m, "result_fluid"), Fields.integer(m, "time", 600)));
        family("growing", Snapshot::growing, (id, m) -> new RecipeLookup.Growing(id, Fields.str(m, "fluid"),
                Fields.integer(m, "amount", 250), Fields.str(m, "seed"), Fields.str(m, "result"), Fields.integer(m, "count", 1),
                Fields.integer(m, "time", 6000), Fields.optional(m, "crystal")));
        family("melting", Snapshot::melting, (id, m) -> {
            Object result = m.get("result");
            String target;
            boolean melt;
            if (result instanceof Map<?, ?> r) {
                Map<String, Object> rm = Fields.copy(r);
                melt = rm.containsKey("melt");
                target = Fields.str(rm, melt ? "melt" : "fluid");
            } else {
                target = Fields.str(m, "result");
                melt = !Fields.bool(m, "is_fluid", false);
            }
            return new RecipeLookup.Melting(id, Fields.str(m, "input"), target, melt, Fields.integer(m, "amount", 20));
        });
        family("casting", Snapshot::casting, (id, m) -> new RecipeLookup.Casting(id, Fields.str(m, "melt"),
                Fields.integer(m, "amount", 100), Fields.str(m, "result"), Fields.integer(m, "time", 60)));
        family("meshes", Snapshot::meshes, (id, m) -> new RecipeLookup.Mesh(id, Fields.str(m, "item"),
                Fields.integer(m, "tier", 1), Fields.number(m, "speed", 1.0), Fields.optional(m, "texture"),
                Fields.integer(m, "slot", 1)));
        family("heat", Snapshot::heat, (id, m) -> new RecipeLookup.Heat(id, Fields.idOrTag(m, "block"),
                Fields.optional(m, "state"), Fields.integer(m, "heat", 1)));
        family("melts", Snapshot::melts, (id, m) -> new RecipeLookup.Melt(Fields.optional(m, "id").isEmpty() ? id : Fields.str(m, "id"),
                Fields.color(m, "color"), Fields.integer(m, "glow", 0), Fields.optional(m, "name")));
        family("compost", Snapshot::compost, (id, m) -> new RecipeLookup.Compost(id, Fields.idOrTag(m, "item"),
                Fields.integer(m, "units", 100), Fields.number(m, "mature_boost", 0.0)));
    }

    private ScriptContent() {
    }

    /** True when the name is a content family a script can change. */
    public static boolean known(String family) {
        return FAMILIES.containsKey(family);
    }

    /** Adds one entry. Returns false (and logs once) when the object is not valid. */
    public static boolean add(String family, Map<String, Object> data) {
        if (!check(family, data)) {
            return false;
        }
        OPS.add(new Op(Kind.ADD, family, Map.of(), Fields.copy(data), ++serial));
        return true;
    }

    /** Removes every entry whose fields equal the filter's (an empty filter removes nothing). */
    public static boolean remove(String family, Map<String, Object> filter) {
        if (!FAMILIES.containsKey(family)) {
            LOG.warning("Prima Materia: scripts cannot change '" + family + "'");
            return false;
        }
        if (filter == null || filter.isEmpty()) {
            LOG.warning("Prima Materia: remove on '" + family + "' needs a filter");
            return false;
        }
        OPS.add(new Op(Kind.REMOVE, family, Fields.copy(filter), Map.of(), ++serial));
        return true;
    }

    /** Removes every match of the filter and adds the new entry in its place. */
    public static boolean replace(String family, Map<String, Object> filter, Map<String, Object> data) {
        if (!check(family, data)) {
            return false;
        }
        if (filter == null || filter.isEmpty()) {
            LOG.warning("Prima Materia: replace on '" + family + "' needs a filter");
            return false;
        }
        OPS.add(new Op(Kind.REPLACE, family, Fields.copy(filter), Fields.copy(data), ++serial));
        return true;
    }

    /** Forgets every script change (a script reload, tests). */
    public static void clear() {
        OPS.clear();
    }

    public static boolean hasChanges() {
        return !OPS.isEmpty();
    }

    private static boolean check(String family, Map<String, Object> data) {
        Family<?> f = FAMILIES.get(family);
        if (f == null) {
            LOG.warning("Prima Materia: scripts cannot change '" + family + "'");
            return false;
        }
        try {
            f.make().apply("script:probe", data == null ? Map.of() : Fields.copy(data));
            return true;
        } catch (RuntimeException e) {
            LOG.warning("Prima Materia: a script's " + family + " entry is not valid: " + e.getMessage());
            return false;
        }
    }

    /** The snapshot with every script change applied, in script order. Returns the same snapshot when there are none. */
    public static Snapshot apply(Snapshot base) {
        if (OPS.isEmpty()) {
            return base;
        }
        Map<String, List<Object>> lists = new LinkedHashMap<>();
        for (Family<?> f : FAMILIES.values()) {
            lists.put(f.name(), new ArrayList<>(f.read().apply(base)));
        }
        for (Op op : OPS) {
            List<Object> list = lists.get(op.family());
            Family<?> f = FAMILIES.get(op.family());
            if (op.kind() != Kind.ADD) {
                list.removeIf(r -> matches(r, op.filter()));
            }
            if (op.kind() != Kind.REMOVE) {
                try {
                    list.add(f.make().apply("primamateria:script/" + op.family() + "/" + op.serial(), op.data()));
                } catch (RuntimeException e) {
                    LOG.warning("Prima Materia: a script's " + op.family() + " entry was skipped: " + e.getMessage());
                }
            }
        }
        return new Snapshot(cast(lists.get("crushing")), cast(lists.get("sifting")), cast(lists.get("residue")),
                cast(lists.get("composting")), cast(lists.get("soaking")), cast(lists.get("mixing")), cast(lists.get("steeping")),
                cast(lists.get("growing")), cast(lists.get("melting")), cast(lists.get("casting")), cast(lists.get("meshes")),
                cast(lists.get("heat")), cast(lists.get("melts")), cast(lists.get("compost")));
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> cast(List<Object> list) {
        return (List<T>) (List<?>) list;
    }

    /** A record as field name (snake_case, as in the JSON) to value. */
    static Map<String, Object> toMap(Object record) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (RecordComponent c : record.getClass().getRecordComponents()) {
            try {
                out.put(snake(c.getName()), c.getAccessor().invoke(record));
            } catch (ReflectiveOperationException e) {
                // a component we cannot read just cannot be filtered on
            }
        }
        return out;
    }

    static boolean matches(Object record, Map<String, Object> filter) {
        Map<String, Object> fields = toMap(record);
        for (Map.Entry<String, Object> e : filter.entrySet()) {
            String key = e.getKey();
            if (key.equals("tag")) {
                key = fields.containsKey("block") ? "block" : "item";
            }
            if (!fields.containsKey(key) || !same(fields.get(key), e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(Object field, Object wanted) {
        if (field instanceof Number a && wanted instanceof Number b) {
            return Math.abs(a.doubleValue() - b.doubleValue()) < 1e-9;
        }
        return String.valueOf(field).equals(String.valueOf(wanted));
    }

    private static String snake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char ch : camel.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                sb.append('_').append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }
}
