package com.itmecasey.primamateria.neoforge.dev;

import com.itmecasey.primamateria.blockentity.DriveBlockEntity;
import com.itmecasey.primamateria.dev.DriveSelfTest;
import com.itmecasey.primamateria.dev.SelfTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import com.itmecasey.primamateria.dev.ShotTour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Development hooks for NeoForge: the server self test and the checks of NeoForge's own capability system. Only built with -PdevTools. */
public final class NeoForgeDev {
    private NeoForgeDev() {
    }

    public static void install(IEventBus modBus) {
        if (SelfTest.requested()) {
            SelfTest.loaderChecks = NeoForgeDev::checks;
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> SelfTest.run(e.getServer()));
        }
        if (FMLEnvironment.getDist() == Dist.CLIENT && System.getenv("PRIMAMATERIA_SHOTS") != null) {
            NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ShotTour.tick());
        }
    }

    /** The powered drive takes energy through NeoForge's energy capability: insert commits, a rolled back insert changes nothing. */
    private static void checks(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos pos = DriveSelfTest.poweredPos(level);
        DriveBlockEntity drive = (DriveBlockEntity) level.getBlockEntity(pos);
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, Direction.NORTH);
        SelfTest.verdict("neoforge_energy_capability_on_the_powered_drive", handler != null && drive != null, String.valueOf(handler));
        if (handler == null || drive == null) {
            return;
        }
        drive.state().restore(0, 0, 0, 0, 0);
        int in;
        try (Transaction tx = Transaction.openRoot()) {
            in = handler.insert(300, tx);
            tx.commit();
        }
        SelfTest.verdict("neoforge_energy_insert_commits", in == 300 && drive.state().energy() == 300, "energy " + drive.state().energy());
        try (Transaction tx = Transaction.openRoot()) {
            handler.insert(100, tx);
        }
        SelfTest.verdict("neoforge_energy_insert_rolls_back", drive.state().energy() == 300, "energy " + drive.state().energy());
        int out;
        try (Transaction tx = Transaction.openRoot()) {
            out = handler.extract(100, tx);
            tx.commit();
        }
        SelfTest.verdict("neoforge_energy_cannot_be_drained", out == 0 && drive.state().energy() == 300, "energy " + drive.state().energy());
        SelfTest.verdict("neoforge_energy_amount_and_capacity", handler.getAmountAsLong() == 300 && handler.getCapacityAsLong() == 10000,
                handler.getAmountAsLong() + " of " + handler.getCapacityAsLong());
    }
}
