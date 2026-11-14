package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.anim.AnimJson;
import com.itmecasey.primamateria.anim.AnimModel;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads {@code primamateria_anim/sieve.json} from the resource packs and the block textures the renderer needs. Loaded lazily on
 * first use and again after a resource reload ({@link #invalidate()} is called by the loaders' reload hook).
 */
public final class SieveModels {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");
    private static final Identifier SIEVE = Identifier.fromNamespaceAndPath("primamateria", "primamateria_anim/sieve.json");
    private static AnimModel sieve;
    private static boolean failed;
    private static int generation;
    private static final Map<String, Identifier> BLOCK_TEXTURES = new HashMap<>();
    private static final Map<String, Identifier> TEXTURE_IDS = new HashMap<>();

    private SieveModels() {
    }

    public static synchronized void invalidate() {
        sieve = null;
        failed = false;
        generation++;
        BLOCK_TEXTURES.clear();
        TEXTURE_IDS.clear();
    }

    static int generation() {
        return generation;
    }

    /** The sieve animation model, or null when the file is missing or broken (logged once; the renderer then stays idle). */
    static synchronized AnimModel sieve() {
        if (sieve == null && !failed) {
            try {
                Resource res = Minecraft.getInstance().getResourceManager().getResourceOrThrow(SIEVE);
                try (BufferedReader reader = res.openAsReader()) {
                    sieve = AnimJson.parse(reader);
                }
                sieve.requireBones("tray", "mesh", "layer");
            } catch (Exception e) {
                failed = true;
                LOGGER.error("Could not load the sieve animation, animations are off for sieves", e);
            }
        }
        return sieve;
    }

    /** {@code minecraft:block/oak_planks} to {@code minecraft:textures/block/oak_planks.png}. */
    static synchronized Identifier textureFile(String id) {
        return TEXTURE_IDS.computeIfAbsent(id, k -> {
            Identifier parsed = Identifier.parse(k);
            return Identifier.fromNamespaceAndPath(parsed.getNamespace(), "textures/" + parsed.getPath() + ".png");
        });
    }

    /** The texture drawn on the layer for a loaded block: its own top texture, or the nearest file the packs have. */
    static synchronized Identifier layerTexture(String blockId) {
        return BLOCK_TEXTURES.computeIfAbsent(blockId, k -> {
            Identifier parsed = Identifier.parse(k);
            String path = parsed.getPath();
            for (String candidate : new String[] {path + "_top", path, path + "_side", path.replace("_block", "")}) {
                Identifier file = Identifier.fromNamespaceAndPath(parsed.getNamespace(), "textures/block/" + candidate + ".png");
                if (Minecraft.getInstance().getResourceManager().getResource(file).isPresent()) {
                    return file;
                }
            }
            return Identifier.fromNamespaceAndPath("minecraft", "textures/block/dirt.png");
        });
    }
}
