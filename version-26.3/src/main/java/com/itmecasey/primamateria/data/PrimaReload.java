package com.itmecasey.primamateria.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itmecasey.primamateria.PrimaMateria;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Reads the mod's own data folders ({@link DataFolders}) after every datapack reload and drops the recipe lookup built from the
 * old recipes. The loader entry classes add one instance to the server reload listeners; it runs after the vanilla recipe manager.
 * A file that is not a JSON object, cannot be read, or is {@code {"remove": true}} (the way a datapack removes a file it
 * replaces) gives no entry.
 */
public final class PrimaReload extends SimplePreparableReloadListener<Map<String, Map<Identifier, JsonObject>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaMateria.NAME);

    @Override
    protected Map<String, Map<Identifier, JsonObject>> prepare(ResourceManager resources, ProfilerFiller profiler) {
        Map<String, Map<Identifier, JsonObject>> all = new HashMap<>();
        for (DataFolders.Folder folder : DataFolders.all()) {
            FileToIdConverter files = new FileToIdConverter(folder.path(), ".json");
            Map<Identifier, JsonObject> parsed = new LinkedHashMap<>();
            for (Map.Entry<Identifier, Resource> entry : files.listMatchingResources(resources).entrySet()) {
                Identifier id = files.fileToId(entry.getKey());
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonObject()) {
                        LOGGER.warn("{} file {} is not a JSON object, skipped", folder.path(), id);
                        continue;
                    }
                    JsonObject json = element.getAsJsonObject();
                    if (json.has("remove") && json.get("remove").getAsBoolean()) {
                        continue;
                    }
                    parsed.put(id, json);
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("Could not read {} file {}: {}", folder.path(), id, e.toString());
                }
            }
            all.put(folder.path(), parsed);
        }
        return all;
    }

    @Override
    protected void apply(Map<String, Map<Identifier, JsonObject>> all, ResourceManager resources, ProfilerFiller profiler) {
        for (DataFolders.Folder folder : DataFolders.all()) {
            try {
                folder.apply(all.getOrDefault(folder.path(), Map.of()));
            } catch (RuntimeException e) {
                LOGGER.error("Could not apply {}: {}", folder.path(), e.toString());
            }
        }
        PrimaRecipes.clearIndex();
    }
}
