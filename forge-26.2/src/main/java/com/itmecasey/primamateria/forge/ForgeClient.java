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
        // the recipe lookup a dedicated server sent is dropped when the player leaves it
        net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> com.itmecasey.primamateria.data.LookupSync.onDisconnect());
        net.minecraftforge.client.event.RegisterColorHandlersEvent.Block.BUS.addListener(ForgeClient::tints);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModAnim.SIEVE_PREVIEW, SieveRenderer<SievePreviewBlockEntity>::new);
        event.registerBlockEntityRenderer(com.itmecasey.primamateria.ModContent.SIEVE_BE,
                SieveRenderer<com.itmecasey.primamateria.blockentity.SieveBlockEntity>::new);
        event.registerBlockEntityRenderer(com.itmecasey.primamateria.ModContent.BARREL_BE,
                com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.BarrelBlockEntity>::new);
        event.registerBlockEntityRenderer(com.itmecasey.primamateria.ModContent.CRUCIBLE_BE,
                com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.CrucibleBlockEntity>::new);
        event.registerBlockEntityRenderer(com.itmecasey.primamateria.ModContent.TRAY_BE,
                com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.CastingTrayBlockEntity>::new);
        event.registerBlockEntityRenderer(com.itmecasey.primamateria.ModContent.WINDUP_DRIVE_BE,
                com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.DriveBlockEntity>::new);
        event.registerBlockEntityRenderer(com.itmecasey.primamateria.ModContent.FURNACE_DRIVE_BE,
                com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.DriveBlockEntity>::new);
        event.registerBlockEntityRenderer(com.itmecasey.primamateria.ModContent.POWERED_DRIVE_BE,
                com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.DriveBlockEntity>::new);
        event.registerBlockEntityRenderer(com.itmecasey.primamateria.ModContent.AUTO_HAMMER_BE,
                com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.AutoHammerBlockEntity>::new);
        event.registerBlockEntityRenderer(ModAnim.MACHINE_PREVIEW, com.itmecasey.primamateria.client.MachineRenderer<com.itmecasey.primamateria.blockentity.MachinePreviewBlockEntity>::new);
    }

    private static void tints(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(java.util.List.of(new com.itmecasey.primamateria.client.FillTint()), com.itmecasey.primamateria.ModContent.block(com.itmecasey.primamateria.Ids.SIEVE));
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModAnim.particle("fines"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.fines(level, x, y, z, sprites));
        event.registerSpriteSet(ModAnim.particle("pour"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.fines(level, x, y, z, sprites));
        event.registerSpriteSet(ModAnim.particle("sift_dust"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.dust(level, x, y, z, sprites));
    }
}
