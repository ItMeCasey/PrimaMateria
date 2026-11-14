package com.itmecasey.primamateria.neoforge.dev;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.barrel.BarrelState;
import com.itmecasey.primamateria.blockentity.BarrelBlockEntity;
import com.itmecasey.primamateria.blockentity.CastingTrayBlockEntity;
import com.itmecasey.primamateria.blockentity.CrucibleBlockEntity;
import com.itmecasey.primamateria.dev.FluidSelfTest;
import com.itmecasey.primamateria.fluid.FluidTanks;
import com.itmecasey.primamateria.fluid.ModFluids;
import com.itmecasey.primamateria.view.TrayView;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
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
        fluidChecks(server);
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

    // ---- fluids: the fluid types and the fluid handler of the machines ----

    private static void fluidChecks(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (String id : Ids.FLUIDS) {
            Identifier key = ModContent.id(id);
            FluidType type = NeoForgeRegistries.FLUID_TYPES.getValue(key);
            Fluid source = ModFluids.source(id);
            Fluid flowing = ModFluids.flowing(id);
            SelfTest.verdict("neoforge_fluid_type_registered_" + id, type != null && NeoForgeRegistries.FLUID_TYPES.containsKey(key)
                    && source.getFluidType() == type && flowing.getFluidType() == type, String.valueOf(NeoForgeRegistries.FLUID_TYPES.getKey(type)));
            boolean molten = ModFluids.isMolten(id);
            SelfTest.verdict("neoforge_fluid_type_matches_its_kind_" + id, molten
                            ? type.getTemperature() >= 1300 && type.getDensity() >= 3000 && type.getViscosity() >= 6000 && type.getLightLevel() >= 12
                                    && !type.canSwim(null) && !type.canConvertToSource(source.defaultFluidState(), level, BlockPos.ZERO)
                            : type.getTemperature() < 400 && type.getDensity() > 1000 && type.getViscosity() <= 1000 && type.getLightLevel() == 0
                                    && type.canSwim(null) && type.canExtinguish(null) && type.getIsWaterLike(),
                    type.getTemperature() + " K, density " + type.getDensity() + ", viscosity " + type.getViscosity() + ", light " + type.getLightLevel());
            SelfTest.verdict("neoforge_fluid_type_sounds_" + id, type.getSound(SoundActions.BUCKET_FILL) != null && type.getSound(SoundActions.BUCKET_EMPTY) != null,
                    "bucket sounds");
        }

        FluidSelfTest.Machines m = FluidSelfTest.machines(level);
        BlockPos[] spots = {m.crucible(), m.wooden(), m.clay(), m.tray()};
        boolean all = true;
        for (BlockPos spot : spots) {
            all &= level.getCapability(Capabilities.Fluid.BLOCK, spot, Direction.NORTH) != null;
        }
        SelfTest.verdict("neoforge_fluid_capability_on_crucible_both_barrels_and_tray", all, "four machines");
        SelfTest.verdict("neoforge_no_fluid_capability_elsewhere",
                level.getCapability(Capabilities.Fluid.BLOCK, m.crucible().offset(0, 0, 8), Direction.NORTH) == null, "air has none");

        ResourceHandler<FluidResource> crucible = level.getCapability(Capabilities.Fluid.BLOCK, m.crucible(), Direction.NORTH);
        CrucibleBlockEntity be = (CrucibleBlockEntity) level.getBlockEntity(m.crucible());
        be.state().restoreFluid("", 0);
        FluidResource copper = FluidResource.of(ModFluids.source("molten_copper"));
        // a pipe-style fill and drain, in many small transactions
        int changes = FluidTanks.CHANGES.get();
        int in = 0;
        for (int n = 0; n < 14; n++) {
            try (Transaction tx = Transaction.openRoot()) {
                in += crucible.insert(copper, 300, tx);
                tx.commit();
            }
        }
        SelfTest.verdict("neoforge_pipe_fills_the_crucible", in == 4000 && be.state().amount() == 4000 && crucible.getAmountAsInt(0) == 4000
                && crucible.getCapacityAsInt(0, copper) == 4000 && crucible.getResource(0).equals(copper), in + " mB in");
        SelfTest.verdict("neoforge_every_committed_insert_calls_changed", FluidTanks.CHANGES.get() - changes == 14, (FluidTanks.CHANGES.get() - changes) + " calls");
        net.minecraft.nbt.CompoundTag saved = be.saveWithoutMetadata(level.registryAccess());
        SelfTest.verdict("neoforge_inserted_fluid_is_saved", saved.getIntOr("Amount", -1) == 4000 && saved.getStringOr("Fluid", "").equals("primamateria:molten_copper"),
                saved.getIntOr("Amount", -1) + " mB " + saved.getStringOr("Fluid", ""));
        int before = FluidTanks.CHANGES.get();
        int extra;
        try (Transaction tx = Transaction.openRoot()) {
            extra = crucible.insert(copper, 100, tx);
            tx.commit();
        }
        SelfTest.verdict("neoforge_full_crucible_takes_no_more", extra == 0 && be.state().amount() == 4000, "overfill refused");
        try (Transaction tx = Transaction.openRoot()) {
            crucible.extract(copper, 1500, tx);
        }
        SelfTest.verdict("neoforge_rolled_back_extract_changes_nothing", be.state().amount() == 4000 && FluidTanks.CHANGES.get() == before, "still 4000, no change reported");
        int out = 0;
        for (int n = 0; n < 8; n++) {
            try (Transaction tx = Transaction.openRoot()) {
                out += crucible.extract(copper, 700, tx);
                tx.commit();
            }
        }
        SelfTest.verdict("neoforge_pipe_drains_the_crucible", out == 4000 && be.state().amount() == 0 && be.state().fluid().isEmpty(), out + " mB out");
        int rolled;
        try (Transaction tx = Transaction.openRoot()) {
            rolled = crucible.insert(copper, 500, tx);
            SelfTest.verdict("neoforge_open_transaction_reports_the_insert", rolled == 500, "500 mB inside the transaction");
        }
        SelfTest.verdict("neoforge_rolled_back_insert_changes_nothing", be.state().amount() == 0 && be.state().fluid().isEmpty(), "still empty");
        SelfTest.verdict("neoforge_crucible_refuses_water_and_brine", !crucible.isValid(0, FluidResource.of(Fluids.WATER))
                        && !crucible.isValid(0, FluidResource.of(ModFluids.source("brine"))) && crucible.isValid(0, FluidResource.of(Fluids.LAVA)),
                "water and brine no, lava yes");
        try (Transaction tx = Transaction.openRoot()) {
            SelfTest.verdict("neoforge_crucible_insert_of_water_is_zero", crucible.insert(FluidResource.of(Fluids.WATER), 1000, tx) == 0, "refused");
        }

        // the wooden barrel: no lava or molten metal, one kind at a time, one bucket at most
        ResourceHandler<FluidResource> wooden = level.getCapability(Capabilities.Fluid.BLOCK, m.wooden(), Direction.NORTH);
        ResourceHandler<FluidResource> clay = level.getCapability(Capabilities.Fluid.BLOCK, m.clay(), Direction.NORTH);
        BarrelBlockEntity woodenBe = (BarrelBlockEntity) level.getBlockEntity(m.wooden());
        BarrelBlockEntity clayBe = (BarrelBlockEntity) level.getBlockEntity(m.clay());
        woodenBe.state().restoreFluid("", 0);
        clayBe.state().restoreFluid("", 0);
        FluidResource lava = FluidResource.of(Fluids.LAVA);
        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidResource brine = FluidResource.of(ModFluids.source("brine"));
        int lavaIn;
        try (Transaction tx = Transaction.openRoot()) {
            lavaIn = wooden.insert(lava, 1000, tx) + wooden.insert(copper, 1000, tx);
            tx.commit();
        }
        SelfTest.verdict("neoforge_wooden_barrel_refuses_lava_and_molten_metal", lavaIn == 0 && woodenBe.state().amount() == 0 && !wooden.isValid(0, lava),
                "nothing went in");
        int clayIn;
        try (Transaction tx = Transaction.openRoot()) {
            clayIn = clay.insert(lava, 1000, tx);
            tx.commit();
        }
        SelfTest.verdict("neoforge_clay_barrel_takes_lava", clayIn == 1000 && clayBe.state().fluid().equals("minecraft:lava"), clayIn + " mB");
        int wet;
        try (Transaction tx = Transaction.openRoot()) {
            wet = wooden.insert(water, 600, tx);
            tx.commit();
        }
        int other;
        try (Transaction tx = Transaction.openRoot()) {
            other = wooden.insert(brine, 100, tx);
            tx.commit();
        }
        int over;
        try (Transaction tx = Transaction.openRoot()) {
            over = wooden.insert(water, 600, tx);
            tx.commit();
        }
        SelfTest.verdict("neoforge_barrel_holds_one_kind_and_one_bucket", wet == 600 && other == 0 && over == 400 && woodenBe.state().amount() == 1000
                && woodenBe.state().fluid().equals("minecraft:water"), wet + ", " + other + ", " + over);
        int simulated;
        try (Transaction tx = Transaction.openRoot()) {
            simulated = wooden.extract(water, 300, tx);
        }
        SelfTest.verdict("neoforge_simulated_drain_leaves_the_barrel", simulated == 300 && woodenBe.state().amount() == 1000, simulated + " mB offered, " + woodenBe.state().amount() + " left");
        // a running soak keeps the water in
        BarrelState.SoakRule rule = new BarrelState.SoakRule("fluid_test_soak", "minecraft:water", 100, "minecraft:clay", 400);
        boolean soaking = woodenBe.state().startSoak(rule, "minecraft:dirt", 1.0);
        int drained;
        try (Transaction tx = Transaction.openRoot()) {
            drained = wooden.extract(water, 500, tx);
            tx.commit();
        }
        SelfTest.verdict("neoforge_pipe_cannot_drain_a_working_barrel", soaking && drained == 0, "soak running, drained " + drained);
        woodenBe.state().restoreFluid("", 0);
        clayBe.state().restoreFluid("", 0);

        // the tray takes one cast
        ResourceHandler<FluidResource> tray = level.getCapability(Capabilities.Fluid.BLOCK, m.tray(), Direction.NORTH);
        CastingTrayBlockEntity trayBe = (CastingTrayBlockEntity) level.getBlockEntity(m.tray());
        int cast;
        try (Transaction tx = Transaction.openRoot()) {
            cast = tray.insert(copper, 1000, tx);
            tx.commit();
        }
        SelfTest.verdict("neoforge_tray_takes_one_cast_of_100", cast == 100 && trayBe.state().stage() == TrayView.Stage.FILLING
                && tray.getCapacityAsInt(0, copper) == 100, cast + " mB");
        int again;
        int back;
        try (Transaction tx = Transaction.openRoot()) {
            again = tray.insert(copper, 1000, tx);
            back = tray.extract(copper, 1000, tx);
        }
        SelfTest.verdict("neoforge_busy_tray_takes_and_gives_nothing", again == 0 && back == 0, again + " in, " + back + " out");
        trayBe.state().reset();
    }
}
