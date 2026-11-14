package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.Dev;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.PrimaMateria;
import com.itmecasey.primamateria.data.PrimaReload;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.blockentity.DriveBlockEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** NeoForge entry point. Registers the content; workstream A adds capabilities, networking and reload listeners here. */
@Mod(PrimaMateria.MODID)
public final class NeoForgePrimaMateria {
    public NeoForgePrimaMateria(IEventBus modBus) {
        PrimaMateria.init(FMLPaths.CONFIGDIR::get);
        modBus.addListener(NeoForgePrimaMateria::register);
        modBus.addListener(NeoForgePrimaMateria::capabilities);
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> e.addListener(ModContent.id("data"), new PrimaReload()));
        Dev.call("com.itmecasey.primamateria.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        // the powered drive takes energy from every side
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModContent.POWERED_DRIVE_BE, (be, side) -> new DriveEnergy(be));
    }

    /** The powered drive's energy as other mods see it: it can be filled and never drained. */
    private static final class DriveEnergy extends SnapshotJournal<Integer> implements EnergyHandler {
        private final DriveBlockEntity be;

        DriveEnergy(DriveBlockEntity be) {
            this.be = be;
        }

        @Override
        protected Integer createSnapshot() {
            return be.state().energy();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            be.state().restore(0, 0, 0, snapshot, be.state().cooldown());
        }

        @Override
        protected void onRootCommit(Integer original) {
            be.setChanged();
        }

        @Override
        public long getAmountAsLong() {
            return be.state().energy();
        }

        @Override
        public long getCapacityAsLong() {
            return be.state().capacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int taken = be.receiveEnergy(amount, true);
            if (taken > 0) {
                updateSnapshots(transaction);
                be.receiveEnergy(taken, false);
            }
            return taken;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
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
