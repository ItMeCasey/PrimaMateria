package com.itmecasey.primamateria.data;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The mod's own datapack folders, {@code data/<namespace>/<path>/<name>.json}, that are not recipes (recipes use real recipe
 * types). Each feature adds one {@link Folder} here: it names its folder and receives every parsed file after each reload, in
 * one call, with files from all namespaces and datapacks already merged by the game (a replaced file arrives once).
 */
public final class DataFolders {
    /** One data folder, for example {@code primamateria/mesh} or {@code primamateria/heat}. */
    public interface Folder {
        /** The folder under {@code data/<namespace>/}. */
        String path();

        /** Called on the server thread after every datapack reload with the parsed files (empty map when there are none). */
        void apply(Map<Identifier, JsonObject> files);
    }

    private DataFolders() {
    }

    /** Every folder. Add new ones at the end, one per line. */
    public static List<Folder> all() {
        return List.of(
                Meshes.FOLDER,
                CompostValues.FOLDER
        );
    }
}
