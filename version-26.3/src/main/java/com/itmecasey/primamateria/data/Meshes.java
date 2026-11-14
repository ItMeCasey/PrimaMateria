package com.itmecasey.primamateria.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The mesh tiers loaded from {@code data/<namespace>/primamateria/mesh/<id>.json}. Any item can be a mesh: a file names the item,
 * its tier (1 is the coarsest), its speed (shakes needed are divided by it), the texture of the mesh at rest and the block state
 * slot (1 to 8) that picks the resting model. Replaced after every datapack reload.
 */
public final class Meshes {
    /** One mesh tier. {@code item} is the full item id. */
    public record Mesh(String item, int tier, double speed, String texture, int slot) {
    }

    private static volatile Map<String, Mesh> byItem = Map.of();

    /** The data folder {@code primamateria/mesh}. */
    public static final DataFolders.Folder FOLDER = new DataFolders.Folder() {
        @Override
        public String path() {
            return "primamateria/mesh";
        }

        @Override
        public void apply(Map<net.minecraft.resources.Identifier, com.google.gson.JsonObject> files) {
            Map<String, Mesh> meshes = new java.util.HashMap<>();
            for (Map.Entry<net.minecraft.resources.Identifier, com.google.gson.JsonObject> e : files.entrySet()) {
                String[] error = new String[1];
                Mesh mesh = parse(e.getValue(), error);
                if (mesh == null) {
                    org.slf4j.LoggerFactory.getLogger("PrimaMateria").warn("Mesh file {} skipped: {}", e.getKey(), error[0]);
                } else {
                    meshes.put(mesh.item(), mesh);
                }
            }
            set(meshes);
            org.slf4j.LoggerFactory.getLogger("PrimaMateria").info("Loaded {} mesh tiers", meshes.size());
        }
    };

    private Meshes() {
    }

    public static void set(Map<String, Mesh> meshes) {
        byItem = Map.copyOf(meshes);
    }

    /** The mesh data of an item id such as {@code primamateria:iron_mesh}, or null when the item is not a mesh. */
    public static Mesh get(String itemId) {
        return itemId == null ? null : byItem.get(itemId);
    }

    public static Map<String, Mesh> all() {
        return byItem;
    }

    /** Parses one mesh file; returns null (with a reason in {@code error[0]}) when it is unusable. */
    public static Mesh parse(com.google.gson.JsonObject json, String[] error) {
        try {
            if (!json.has("item") || !json.has("tier")) {
                error[0] = "needs item and tier";
                return null;
            }
            String item = json.get("item").getAsString();
            int tier = json.get("tier").getAsInt();
            double speed = json.has("speed") ? json.get("speed").getAsDouble() : 1.0;
            String texture = json.has("texture") ? json.get("texture").getAsString() : "";
            int slot = json.has("slot") ? json.get("slot").getAsInt() : 0;
            if (tier < 1 || speed <= 0 || slot < 0 || slot > 8) {
                error[0] = "tier must be 1 or more, speed above 0, slot 0 to 8";
                return null;
            }
            return new Mesh(item, tier, speed, texture, slot);
        } catch (RuntimeException e) {
            error[0] = e.toString();
            return null;
        }
    }

    /** Test helper: the registry of this class without a datapack. */
    static void clear() {
        byItem = new ConcurrentHashMap<>();
    }
}
