package com.itmecasey.primamateria.neoforge.dev;

import com.itmecasey.primamateria.dev.IntegrationTour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Development hook for NeoForge clients: runs the integration tour. Only built with -PdevTools; found by name. */
public final class IntegrationDev {
    private IntegrationDev() {
    }

    public static void install() {
        if (FMLEnvironment.getDist() == Dist.CLIENT && IntegrationTour.requested()) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> IntegrationTour.tick());
        }
    }
}
