package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.ModAnim;
import com.itmecasey.primamateria.blockentity.SievePreviewBlockEntity;
import com.itmecasey.primamateria.client.SieveRenderer;
import com.itmecasey.primamateria.client.SiftParticles;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup: the sieve renderer and the sifting particles. The events only fire on a client. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void install(FMLJavaModLoadingContext context) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeClient::renderers);
        RegisterParticleProvidersEvent.BUS.addListener(ForgeClient::particles);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModAnim.SIEVE_PREVIEW, SieveRenderer<SievePreviewBlockEntity>::new);
        event.registerBlockEntityRenderer(ModAnim.MACHINE_PREVIEW, com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.MachinePreviewBlockEntity>::new);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModAnim.particle("fines"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.fines(level, x, y, z, sprites));
        event.registerSpriteSet(ModAnim.particle("pour"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.fines(level, x, y, z, sprites));
        event.registerSpriteSet(ModAnim.particle("sift_dust"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.dust(level, x, y, z, sprites));
    }
}
