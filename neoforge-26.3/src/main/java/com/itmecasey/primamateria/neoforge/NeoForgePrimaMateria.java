package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.Dev;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.PrimaMateria;
import com.itmecasey.primamateria.data.PrimaReload;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.hammer.HammerLogic;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point. Registers the content; workstream A adds capabilities, networking and reload listeners here. */
@Mod(PrimaMateria.MODID)
public final class NeoForgePrimaMateria {
    public NeoForgePrimaMateria(IEventBus modBus) {
        PrimaMateria.init(FMLPaths.CONFIGDIR::get);
        modBus.addListener(NeoForgePrimaMateria::register);
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> e.addListener(ModContent.id("data"), new PrimaReload()));
        // A hammer crushes the block it breaks: HammerLogic does the work and the original break is canceled.
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent e) -> {
            if (e.getPlayer() instanceof ServerPlayer player && e.getLevel() instanceof ServerLevel level
                    && HammerLogic.onBreak(player, level, e.getPos(), e.getState())) {
                e.setCanceled(true);
            }
        });
        Dev.call("com.itmecasey.primamateria.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModContent.registerBlockEntities((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.RECIPE_TYPE, helper -> PrimaRecipes.registerTypes((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.RECIPE_SERIALIZER, helper -> PrimaRecipes.registerSerializers((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.SOUND_EVENT, helper -> ModContent.registerSounds((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id(Ids.CREATIVE_TAB), ModContent.creativeTab()));
    }
}
