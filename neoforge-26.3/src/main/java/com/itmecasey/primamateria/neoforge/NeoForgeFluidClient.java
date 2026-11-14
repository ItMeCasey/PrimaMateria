package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.PrimaMateria;
import com.itmecasey.primamateria.fluid.ModFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

/**
 * Draws the four fluids from their own animated textures (still and flowing; brine also has an overlay for the faces behind glass).
 * NeoForge 26 draws fluids from fluid models. Client only: never loaded on a dedicated server.
 */
@EventBusSubscriber(modid = PrimaMateria.MODID, value = Dist.CLIENT)
public final class NeoForgeFluidClient {
    private NeoForgeFluidClient() {
    }

    @SubscribeEvent
    public static void models(RegisterFluidModelsEvent event) {
        for (String id : Ids.FLUIDS) {
            Material still = new Material(ModContent.id("block/" + id + "_still"));
            Material flowing = new Material(ModContent.id("block/" + id + "_flow"));
            Material overlay = ModFluids.isMolten(id) ? null : new Material(ModContent.id("block/" + id + "_overlay"));
            FluidModel.Unbaked model = new FluidModel.Unbaked(still, flowing, overlay, FluidTintSources.constant(0xFFFFFFFF));
            event.register(model, ModFluids.source(id), ModFluids.flowing(id));
        }
    }
}
