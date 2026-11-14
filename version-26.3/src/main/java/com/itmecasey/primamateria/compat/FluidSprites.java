package com.itmecasey.primamateria.compat;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Draws a fluid's still texture (its first frame) in a recipe viewer slot that holds no item, for viewers whose slots only take
 * items. Works for the mod's own fluids and for water and lava; the texture is read as a plain resource, so no loader class is used.
 */
public final class FluidSprites {
    private static final Map<Identifier, int[]> SIZES = new HashMap<>();

    private FluidSprites() {
    }

    /** The texture file of a fluid id's still face, or null. */
    private static Identifier texture(String fluidId) {
        Identifier id = Identifier.tryParse(fluidId);
        if (id == null) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/block/" + id.getPath() + "_still.png");
    }

    private static int tint(String fluidId) {
        return fluidId.equals("minecraft:water") ? 0xFF3F76E4 : 0xFFFFFFFF;
    }

    private static int[] size(Identifier tex) {
        return SIZES.computeIfAbsent(tex, t -> {
            try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(t).open(); NativeImage image = NativeImage.read(in)) {
                return new int[] {image.getWidth(), image.getHeight()};
            } catch (java.io.IOException | RuntimeException e) {
                return new int[] {16, 16};
            }
        });
    }

    /** Draws the fluid in the 16 by 16 area at x, y. Returns whether a texture was drawn. */
    public static boolean draw(GuiGraphicsExtractor g, String fluidId, int x, int y) {
        Identifier tex = texture(fluidId);
        if (tex == null) {
            return false;
        }
        int[] s = size(tex);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0F, 0F, 16, 16, s[0], 16, s[0], s[1], tint(fluidId));
        return true;
    }

    /** The slot tooltip of a fluid: its name and the amount in mB. */
    public static java.util.List<Component> tooltip(String fluidId, int mb) {
        return java.util.List.of(Component.literal(Stacks.name(fluidId)), Component.literal(mb + " mB"));
    }
}
