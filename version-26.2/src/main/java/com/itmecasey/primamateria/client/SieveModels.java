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
    private static final Map<Identifier, Integer> AVERAGES = new HashMap<>();
    /** Neutral sprite for blocks with no usable texture. */
    static final Identifier FALLBACK = Identifier.fromNamespaceAndPath("primamateria", "textures/block/stone_meal.png");

    private SieveModels() {
    }

    public static synchronized void invalidate() {
        sieve = null;
        failed = false;
        generation++;
        BLOCK_TEXTURES.clear();
        TINTED.clear();
        AVERAGES.clear();
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

    /** Blocks whose top quad carries a tint index (grass and the like). */
    private static final java.util.Set<String> TINTED = new java.util.HashSet<>();

    static synchronized boolean tinted(String blockId) {
        return TINTED.contains(blockId);
    }

    /** The top face sprite of the block's baked model as a texture file, or null (no model, no top quad, animated). */
    private static Identifier fromModel(String blockId, Identifier id) {
        try {
            net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(id);
            if (block == net.minecraft.world.level.block.Blocks.AIR) {
                return null;
            }
            var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block.defaultBlockState());
            java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts = new java.util.ArrayList<>();
            model.collectParts(net.minecraft.util.RandomSource.create(0L), parts);
            for (var part : parts) {
                for (var quad : part.getQuads(net.minecraft.core.Direction.UP)) {
                    var info = quad.materialInfo();
                    if ((info.flags() & net.minecraft.client.resources.model.geometry.BakedQuad.FLAG_ANIMATED) != 0) {
                        return null;
                    }
                    Identifier name = info.sprite().contents().name();
                    Identifier file = Identifier.fromNamespaceAndPath(name.getNamespace(), "textures/" + name.getPath() + ".png");
                    if (Minecraft.getInstance().getResourceManager().getResource(file).isEmpty()) {
                        return null;
                    }
                    if (info.tintIndex() >= 0) {
                        TINTED.add(blockId);
                    }
                    return file;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.info("Could not read the model of {}: {}", blockId, e.toString());
        }
        return null;
    }

    /** The texture drawn on the layer for a loaded block: the top face of its own baked model, else the nearest file by name. */
    static synchronized Identifier layerTexture(String blockId) {
        return BLOCK_TEXTURES.computeIfAbsent(blockId, k -> {
            Identifier modelTexture = fromModel(k, Identifier.parse(k));
            if (modelTexture != null) {
                return modelTexture;
            }
            Identifier parsed = Identifier.parse(k);
            String path = parsed.getPath();
            for (String candidate : new String[] {path + "_top", path, path + "_side", path.replace("_block", "")}) {
                Identifier file = Identifier.fromNamespaceAndPath(parsed.getNamespace(), "textures/block/" + candidate + ".png");
                var res = Minecraft.getInstance().getResourceManager().getResource(file);
                boolean animated = Minecraft.getInstance().getResourceManager()
                        .getResource(Identifier.fromNamespaceAndPath(parsed.getNamespace(), file.getPath() + ".mcmeta")).isPresent();
                if (res.isPresent() && !animated) {
                    return file;
                }
            }
            LOGGER.info("No usable top texture for {}, the sieve layer uses a neutral sprite", k);
            return FALLBACK;
        });
    }

    /** Average color (rgb) of the opaque pixels of the first square frame of a texture; a gray when it cannot be read. */
    static synchronized int averageColor(Identifier file) {
        return AVERAGES.computeIfAbsent(file, f -> {
            try (java.io.InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(f).open()) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
                int size = Math.min(img.getWidth(), img.getHeight());
                long r = 0;
                long g = 0;
                long b = 0;
                long n = 0;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        int p = img.getRGB(x, y);
                        if (((p >>> 24) & 255) > 128) {
                            r += (p >> 16) & 255;
                            g += (p >> 8) & 255;
                            b += p & 255;
                            n++;
                        }
                    }
                }
                return n == 0 ? 0x8A8A8A : (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            } catch (Exception e) {
                LOGGER.info("Could not read {} for its color, using gray", f);
                return 0x8A8A8A;
            }
        });
    }
}
