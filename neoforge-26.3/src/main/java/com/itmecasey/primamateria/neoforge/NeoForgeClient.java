package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.ModAnim;
import com.itmecasey.primamateria.PrimaMateria;
import com.itmecasey.primamateria.blockentity.SievePreviewBlockEntity;
import com.itmecasey.primamateria.client.SieveRenderer;
import com.itmecasey.primamateria.client.SiftParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Client-only setup: the sieve renderer and the sifting particles. Never loaded on a dedicated server. */
@EventBusSubscriber(modid = PrimaMateria.MODID, value = Dist.CLIENT)
public final class NeoForgeClient {
    private NeoForgeClient() {
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
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

    /** The recipe lookup a dedicated server sent is dropped when the player leaves it. */
    @SubscribeEvent
    public static void loggedOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        com.itmecasey.primamateria.data.LookupSync.onDisconnect();
    }

    @SubscribeEvent
    public static void tints(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(java.util.List.of(new com.itmecasey.primamateria.client.FillTint()), com.itmecasey.primamateria.ModContent.block(com.itmecasey.primamateria.Ids.SIEVE));
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModAnim.particle("fines"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.fines(level, x, y, z, sprites));
        event.registerSpriteSet(ModAnim.particle("pour"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.fines(level, x, y, z, sprites));
        event.registerSpriteSet(ModAnim.particle("sift_dust"), sprites -> (o, level, x, y, z, xa, ya, za, random) -> SiftParticles.dust(level, x, y, z, sprites));
    }
}
