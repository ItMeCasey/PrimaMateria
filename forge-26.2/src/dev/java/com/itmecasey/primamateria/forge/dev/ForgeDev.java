package com.itmecasey.primamateria.forge.dev;

import com.itmecasey.primamateria.blockentity.DriveBlockEntity;
import com.itmecasey.primamateria.dev.DriveSelfTest;
import com.itmecasey.primamateria.dev.SelfTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import com.itmecasey.primamateria.dev.ShotTour;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Development hooks for Forge: the server self test and the checks of Forge's own capability system. Only built with -PdevTools. */
public final class ForgeDev {
    private ForgeDev() {
    }

    public static void install(Object context) {
        if (SelfTest.requested()) {
            SelfTest.loaderChecks = ForgeDev::checks;
            ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
        if (FMLEnvironment.dist.isClient() && System.getenv("PRIMAMATERIA_SHOTS") != null) {
            TickEvent.ClientTickEvent.Post.BUS.addListener((TickEvent.ClientTickEvent.Post e) -> ShotTour.tick());
        }
        if (FMLEnvironment.dist.isClient() && com.itmecasey.primamateria.dev.IntegrationTour.requested()) {
            TickEvent.ClientTickEvent.Post.BUS.addListener((TickEvent.ClientTickEvent.Post e) -> com.itmecasey.primamateria.dev.IntegrationTour.tick());
        }
    }

    /** The powered drive takes energy through Forge's energy capability, never gives any back. */
    private static void checks(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos pos = DriveSelfTest.poweredPos(level);
        BlockEntity be = level.getBlockEntity(pos);
        IEnergyStorage storage = be == null ? null : be.getCapability(ForgeCapabilities.ENERGY, Direction.NORTH).orElse(null);
        SelfTest.verdict("forge_energy_capability_on_the_powered_drive", storage != null && be instanceof DriveBlockEntity, String.valueOf(storage));
        if (storage == null || !(be instanceof DriveBlockEntity drive)) {
            return;
        }
        drive.state().restore(0, 0, 0, 0, 0);
        SelfTest.verdict("forge_energy_can_receive_not_extract", storage.canReceive() && !storage.canExtract(), "receive only");
        int in = storage.receiveEnergy(300, false);
        SelfTest.verdict("forge_energy_insert_stores", in == 300 && drive.state().energy() == 300, "energy " + drive.state().energy());
        int simulated = storage.receiveEnergy(100, true);
        SelfTest.verdict("forge_energy_simulate_changes_nothing", simulated == 100 && drive.state().energy() == 300, "energy " + drive.state().energy());
        SelfTest.verdict("forge_energy_cannot_be_drained", storage.extractEnergy(100, false) == 0 && drive.state().energy() == 300, "energy " + drive.state().energy());
        SelfTest.verdict("forge_energy_amount_and_capacity", storage.getEnergyStored() == 300 && storage.getMaxEnergyStored() == 10000,
                storage.getEnergyStored() + " of " + storage.getMaxEnergyStored());
    }
}
