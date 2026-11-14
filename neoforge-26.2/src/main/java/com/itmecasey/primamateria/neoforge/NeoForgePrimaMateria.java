package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.Dev;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.PrimaMateria;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point. Registers the content; workstream A adds capabilities, networking and reload listeners here. */
@Mod(PrimaMateria.MODID)
public final class NeoForgePrimaMateria {
    public NeoForgePrimaMateria(IEventBus modBus) {
        PrimaMateria.init(FMLPaths.CONFIGDIR::get);
        modBus.addListener(NeoForgePrimaMateria::register);
        Dev.call("com.itmecasey.primamateria.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.SOUND_EVENT, helper -> ModContent.registerSounds((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> com.itmecasey.primamateria.ModAnim.registerBlockEntityTypes((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.PARTICLE_TYPE, helper -> com.itmecasey.primamateria.ModAnim.registerParticles((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id(Ids.CREATIVE_TAB), ModContent.creativeTab()));
    }
}
