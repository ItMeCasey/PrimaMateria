package com.itmecasey.primamateria.forge;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only setup; the event only fires on a client. Workstream C adds renderers, particles and color handlers here. */
final class ForgeClient {
    private ForgeClient() {
    }

    static void install(FMLJavaModLoadingContext context) {
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(ForgeClient::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
    }
}
