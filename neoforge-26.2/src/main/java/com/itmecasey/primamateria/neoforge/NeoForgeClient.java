package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.PrimaMateria;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/** Client-only setup. Never loaded on a dedicated server. Workstream C adds renderers, particles and color handlers here. */
@EventBusSubscriber(modid = PrimaMateria.MODID, value = Dist.CLIENT)
public final class NeoForgeClient {
    private NeoForgeClient() {
    }
}
