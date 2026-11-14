package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.fluid.ModFluids;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;

/**
 * Draws the four fluids from their own animated textures (still and flowing; brine also has an overlay for the faces behind glass).
 * Forge 26 draws fluids from fluid models. Client only: never loaded on a dedicated server.
 */
final class ForgeFluidClient {
    private ForgeFluidClient() {
    }

    static void install() {
        ModelEvent.BakeFluidModels.BUS.addListener(ForgeFluidClient::models);
    }

    private static void models(ModelEvent.BakeFluidModels event) {
        // no tint: the textures carry their own color
        BlockTintSource none = new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return 0xFFFFFFFF;
            }
        };
        for (String id : Ids.FLUIDS) {
            Material still = new Material(ModContent.id("block/" + id + "_still"));
            Material flowing = new Material(ModContent.id("block/" + id + "_flow"));
            Material overlay = ModFluids.isMolten(id) ? null : new Material(ModContent.id("block/" + id + "_overlay"));
            FluidModel baked = new FluidModel.Unbaked(still, flowing, overlay, none).bake(event.materials(), () -> "primamateria " + id);
            event.register(ModFluids.source(id), baked);
            event.register(ModFluids.flowing(id), baked);
        }
    }
}
