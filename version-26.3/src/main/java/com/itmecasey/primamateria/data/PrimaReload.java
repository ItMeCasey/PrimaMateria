package com.itmecasey.primamateria.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itmecasey.primamateria.PrimaMateria;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the mod's own data folders after every datapack reload and drops the recipe index that was built from the old recipes.
 * The loader entry classes add one instance to the server reload listeners; it runs after the vanilla recipe manager.
 */
public final class PrimaReload extends SimplePreparableReloadListener<Map<String, Meshes.Mesh>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaMateria.NAME);
    private static final FileToIdConverter MESH_FILES = new FileToIdConverter("primamateria/mesh", ".json");

    @Override
    protected Map<String, Meshes.Mesh> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<String, Meshes.Mesh> meshes = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : MESH_FILES.listMatchingResources(resources).entrySet()) {
            Identifier id = MESH_FILES.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    LOGGER.warn("Mesh file {} is not a JSON object, skipped", id);
                    continue;
                }
                JsonObject json = element.getAsJsonObject();
                if (json.has("remove") && json.get("remove").getAsBoolean()) {
                    continue; // a datapack removes a mesh by replacing its file with {"remove": true}
                }
                String[] error = new String[1];
                Meshes.Mesh mesh = Meshes.parse(json, error);
                if (mesh == null) {
                    LOGGER.warn("Mesh file {} skipped: {}", id, error[0]);
                } else {
                    meshes.put(mesh.item(), mesh);
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Could not read mesh file {}: {}", id, e.toString());
            }
        }
        return meshes;
    }

    @Override
    protected void apply(Map<String, Meshes.Mesh> meshes, ResourceManager resources, ProfilerFiller profiler) {
        Meshes.set(meshes);
        PrimaRecipes.clearIndex();
        LOGGER.info("Loaded {} mesh tiers", meshes.size());
    }
}
