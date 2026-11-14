package com.itmecasey.primamateria.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.itmecasey.primamateria.script.ScriptContent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds {@link RecipeLookup}'s snapshot after every datapack reload: the recipes of our types (encoded back to the JSON shape
 * and read by the same code scripts use) plus the meshes, heat sources and compost values from their data files, then the
 * script changes on top. Recipe viewers, SMWTI, Jade and the script bridge read the result.
 */
public final class LookupBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");

    private LookupBuilder() {
    }

    /** recipe type path to the family name scripts and the lookup use */
    private static String family(String path) {
        return switch (path) {
            case "sifting_residue" -> "residue";
            case "sifting", "crushing", "composting", "soaking", "mixing", "steeping", "growing", "melting", "casting" -> path;
            default -> null;
        };
    }

    public static void rebuild(Map<String, Map<Identifier, JsonObject>> folders) {
        Map<String, List<Map.Entry<String, Map<String, Object>>>> data = new LinkedHashMap<>();
        Map<Identifier, JsonObject> recipes = folders.getOrDefault("recipe", Map.of());
        for (Map.Entry<Identifier, JsonObject> entry : recipes.entrySet()) {
            String type = entry.getValue().get("type").getAsString();
            String family = family(type.substring("primamateria:".length()));
            if (family != null) {
                data.computeIfAbsent(family, k -> new ArrayList<>()).add(Map.entry(entry.getKey().toString(), plain(entry.getValue())));
            }
        }
        add(data, "meshes", folders.get("primamateria/mesh"));
        add(data, "heat", folders.get("primamateria/heat"));
        add(data, "compost", folders.get("primamateria/compost"));
        RecipeLookup.Snapshot snapshot = ScriptContent.apply(ScriptContent.fromData(data));
        RecipeLookup.set(snapshot);
        LOGGER.info("Recipe lookup: {} sifting, {} crushing, {} barrel, {} crucible recipes, {} meshes",
                snapshot.sifting().size(), snapshot.crushing().size(),
                snapshot.soaking().size() + snapshot.mixing().size() + snapshot.steeping().size() + snapshot.growing().size() + snapshot.composting().size(),
                snapshot.melting().size() + snapshot.casting().size(), snapshot.meshes().size());
    }

    private static void add(Map<String, List<Map.Entry<String, Map<String, Object>>>> data, String family, Map<Identifier, JsonObject> files) {
        if (files == null) {
            return;
        }
        List<Map.Entry<String, Map<String, Object>>> list = new ArrayList<>();
        for (Map.Entry<Identifier, JsonObject> e : files.entrySet()) {
            Map<String, Object> m = plain(e.getValue());
            Object state = m.get("state");
            if (state instanceof Map<?, ?> sm) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<?, ?> s : sm.entrySet()) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(s.getKey()).append('=').append(s.getValue());
                }
                m.put("state", sb.toString());
            }
            list.add(Map.entry(e.getKey().toString(), m));
        }
        data.put(family, list);
    }

    /** JSON to plain maps, lists, numbers (as doubles), booleans and text. An ingredient list shows its first entry. */
    static Map<String, Object> plain(JsonObject o) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            Object v = value(e.getValue());
            if ("input".equals(e.getKey()) && v instanceof List<?> l && !l.isEmpty()) {
                v = l.get(0);
            }
            out.put(e.getKey(), v);
        }
        return out;
    }

    private static Object value(JsonElement e) {
        if (e.isJsonObject()) {
            return plain(e.getAsJsonObject());
        }
        if (e.isJsonArray()) {
            List<Object> l = new ArrayList<>();
            JsonArray a = e.getAsJsonArray();
            for (JsonElement x : a) {
                l.add(value(x));
            }
            return l;
        }
        if (e.isJsonNull()) {
            return null;
        }
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isBoolean()) {
            return p.getAsBoolean();
        }
        if (p.isNumber()) {
            return p.getAsDouble();
        }
        return p.getAsString();
    }
}
