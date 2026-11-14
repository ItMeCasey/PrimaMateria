package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.Dev;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.PrimaMateria;
import com.itmecasey.primamateria.data.PrimaReload;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.world.StartIslandPlacer;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge entry point. Registers the content; workstream A adds capabilities, networking and reload listeners here. */
@Mod(PrimaMateria.MODID)
public final class ForgePrimaMateria {
    public ForgePrimaMateria(FMLJavaModLoadingContext context) {
        PrimaMateria.init(FMLPaths.CONFIGDIR::get);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgePrimaMateria::register);
        ForgeClient.install(context);
        AddReloadListenerEvent.BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new PrimaReload()));
        ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> StartIslandPlacer.onServerStarted(e.getServer()));
        Dev.call("com.itmecasey.primamateria.forge.dev.ForgeDev", "install", context);
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
