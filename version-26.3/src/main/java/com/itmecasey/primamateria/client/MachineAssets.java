package com.itmecasey.primamateria.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Texture lookups for the machine renderer: fluid surfaces, compost, crystals, and the share of an animated strip that is one frame. */
final class MachineAssets {
    static final Identifier WATER = file("minecraft", "block/water_still");
    static final Identifier LAVA = file("minecraft", "block/lava_still");
    static final Identifier COMPOST = file("minecraft", "block/composter_compost");
    static final Identifier MELT = file("minecraft", "block/white_concrete_powder");
    static final Identifier BRASS = file("minecraft", "block/gold_block");
    static final Identifier IRON = file("minecraft", "block/iron_block");
    static final Identifier WOOD = file("minecraft", "block/oak_planks");
    static final Identifier AMETHYST = file("minecraft", "block/amethyst_block");
    static final Identifier CRYSTAL_FALLBACK = file("minecraft", "block/amethyst_cluster");

    private static final Map<String, Identifier> FLUIDS = new HashMap<>();
    private static final Map<String, Identifier> CRYSTALS = new HashMap<>();
    private static final Map<Identifier, Float> FRAMES = new HashMap<>();

    private MachineAssets() {
    }

    static Identifier file(String ns, String path) {
        return Identifier.fromNamespaceAndPath(ns, "textures/" + path + ".png");
    }

    static synchronized void invalidate() {
        FLUIDS.clear();
        CRYSTALS.clear();
        FRAMES.clear();
    }

    private static boolean exists(Identifier f) {
        return Minecraft.getInstance().getResourceManager().getResource(f).isPresent();
    }

    /** The still texture of a fluid: {@code <ns>:textures/block/<path>_still.png} when a pack has it, else water (tinted by the caller). */
    static synchronized Identifier fluid(String id) {
        return FLUIDS.computeIfAbsent(id, k -> {
            if (k.equals("minecraft:lava")) {
                return LAVA;
            }
            Identifier parsed = Identifier.tryParse(k);
            if (parsed != null) {
                Identifier f = file(parsed.getNamespace(), "block/" + parsed.getPath() + "_still");
                if (exists(f)) {
                    return f;
                }
            }
            return WATER;
        });
    }

    /** A crystal texture id such as {@code primamateria:block/crystal_diamond}; a vanilla cluster when the file is not there. */
    static synchronized Identifier crystal(String id) {
        return CRYSTALS.computeIfAbsent(id, k -> {
            Identifier parsed = Identifier.tryParse(k);
            if (parsed != null) {
                Identifier f = file(parsed.getNamespace(), parsed.getPath());
                if (exists(f)) {
                    return f;
                }
            }
            return CRYSTAL_FALLBACK;
        });
    }

    /** Height over width of the image: an animated strip of n frames gives 1/n of the texture per frame. */
    static synchronized float frame(Identifier f) {
        return FRAMES.computeIfAbsent(f, k -> {
            try (java.io.InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(k).open()) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
                return img.getWidth() >= img.getHeight() ? 1F : (float) img.getWidth() / img.getHeight();
            } catch (Exception e) {
                return 1F;
            }
        });
    }
}
