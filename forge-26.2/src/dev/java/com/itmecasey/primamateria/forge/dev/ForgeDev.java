package com.itmecasey.primamateria.forge.dev;

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
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
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
        fluidChecks(server);
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

    // ---- fluids: the fluid types and the fluid handler of the machines ----

    private static IFluidHandler handler(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).orElse(null);
    }

    private static void fluidChecks(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (String id : Ids.FLUIDS) {
            Identifier key = ModContent.id(id);
            FluidType type = ForgeRegistries.FLUID_TYPES.get().getValue(key);
            Fluid source = ModFluids.source(id);
            Fluid flowing = ModFluids.flowing(id);
            SelfTest.verdict("forge_fluid_type_registered_" + id, type != null && ForgeRegistries.FLUID_TYPES.get().containsKey(key)
                    && source.getFluidType() == type && flowing.getFluidType() == type, String.valueOf(ForgeRegistries.FLUID_TYPES.get().getKey(type)));
            boolean molten = ModFluids.isMolten(id);
            SelfTest.verdict("forge_fluid_type_matches_its_kind_" + id, molten
                            ? type.getTemperature() >= 1300 && type.getDensity() >= 3000 && type.getViscosity() >= 6000 && type.getLightLevel() >= 12
                                    && !type.canSwim(null) && !type.canConvertToSource(source.defaultFluidState(), level, BlockPos.ZERO)
                            : type.getTemperature() < 400 && type.getDensity() > 1000 && type.getViscosity() <= 1000 && type.getLightLevel() == 0
                                    && type.canSwim(null) && type.canExtinguish(null),
                    type.getTemperature() + " K, density " + type.getDensity() + ", viscosity " + type.getViscosity() + ", light " + type.getLightLevel());
            SelfTest.verdict("forge_fluid_type_sounds_" + id, type.getSound(SoundActions.BUCKET_FILL) != null && type.getSound(SoundActions.BUCKET_EMPTY) != null,
                    "bucket sounds");
        }

        FluidSelfTest.Machines m = FluidSelfTest.machines(level);
        boolean all = handler(level, m.crucible()) != null && handler(level, m.wooden()) != null && handler(level, m.clay()) != null
                && handler(level, m.tray()) != null;
        SelfTest.verdict("forge_fluid_capability_on_crucible_both_barrels_and_tray", all, "four machines");
        SelfTest.verdict("forge_no_fluid_capability_elsewhere", handler(level, m.crucible().offset(0, 0, 8)) == null
                && handler(level, DriveSelfTest.poweredPos(level)) == null, "air and the drive have none");

        IFluidHandler crucible = handler(level, m.crucible());
        CrucibleBlockEntity be = (CrucibleBlockEntity) level.getBlockEntity(m.crucible());
        be.state().restoreFluid("", 0);
        FluidStack copper = new FluidStack(ModFluids.source("molten_copper"), 300);
        // a pipe-style fill and drain, in many small steps
        int changes = FluidTanks.CHANGES.get();
        int in = 0;
        for (int n = 0; n < 14; n++) {
            in += crucible.fill(copper, IFluidHandler.FluidAction.EXECUTE);
        }
        SelfTest.verdict("forge_pipe_fills_the_crucible", in == 4000 && be.state().amount() == 4000 && crucible.getFluidInTank(0).getAmount() == 4000
                && crucible.getTankCapacity(0) == 4000 && crucible.getFluidInTank(0).getFluid() == ModFluids.source("molten_copper"), in + " mB in");
        SelfTest.verdict("forge_every_executed_fill_calls_changed", FluidTanks.CHANGES.get() - changes == 14, (FluidTanks.CHANGES.get() - changes) + " calls");
        net.minecraft.nbt.CompoundTag saved = be.saveWithoutMetadata(level.registryAccess());
        SelfTest.verdict("forge_inserted_fluid_is_saved", saved.getIntOr("Amount", -1) == 4000 && saved.getStringOr("Fluid", "").equals("primamateria:molten_copper"),
                saved.getIntOr("Amount", -1) + " mB " + saved.getStringOr("Fluid", ""));
        int before = FluidTanks.CHANGES.get();
        SelfTest.verdict("forge_full_crucible_takes_no_more", crucible.fill(copper, IFluidHandler.FluidAction.EXECUTE) == 0
                && crucible.fill(copper, IFluidHandler.FluidAction.SIMULATE) == 0, "overfill refused");
        FluidStack simulated = crucible.drain(1500, IFluidHandler.FluidAction.SIMULATE);
        SelfTest.verdict("forge_simulated_drain_changes_nothing", simulated.getAmount() == 1500 && be.state().amount() == 4000 && FluidTanks.CHANGES.get() == before,
                "1500 offered, 4000 left, no change reported");
        int out = 0;
        for (int n = 0; n < 8; n++) {
            out += crucible.drain(new FluidStack(ModFluids.source("molten_copper"), 700), IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
        SelfTest.verdict("forge_pipe_drains_the_crucible", out == 4000 && be.state().amount() == 0 && be.state().fluid().isEmpty(), out + " mB out");
        SelfTest.verdict("forge_simulated_fill_changes_nothing", crucible.fill(copper, IFluidHandler.FluidAction.SIMULATE) == 300 && be.state().amount() == 0
                && FluidTanks.CHANGES.get() - before == 6, "300 offered, still empty, six drains were reported");
        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        FluidStack lava = new FluidStack(Fluids.LAVA, 1000);
        FluidStack brine = new FluidStack(ModFluids.source("brine"), 1000);
        SelfTest.verdict("forge_crucible_refuses_water_and_brine", !crucible.isFluidValid(0, water) && !crucible.isFluidValid(0, brine) && crucible.isFluidValid(0, lava)
                && crucible.fill(water, IFluidHandler.FluidAction.EXECUTE) == 0, "water and brine no, lava yes");

        // the wooden barrel: no lava or molten metal, one kind at a time, one bucket at most
        IFluidHandler wooden = handler(level, m.wooden());
        IFluidHandler clay = handler(level, m.clay());
        BarrelBlockEntity woodenBe = (BarrelBlockEntity) level.getBlockEntity(m.wooden());
        BarrelBlockEntity clayBe = (BarrelBlockEntity) level.getBlockEntity(m.clay());
        woodenBe.state().restoreFluid("", 0);
        clayBe.state().restoreFluid("", 0);
        int lavaIn = wooden.fill(lava, IFluidHandler.FluidAction.EXECUTE) + wooden.fill(new FluidStack(ModFluids.source("molten_iron"), 1000), IFluidHandler.FluidAction.EXECUTE);
        SelfTest.verdict("forge_wooden_barrel_refuses_lava_and_molten_metal", lavaIn == 0 && woodenBe.state().amount() == 0 && !wooden.isFluidValid(0, lava), "nothing went in");
        int clayIn = clay.fill(lava, IFluidHandler.FluidAction.EXECUTE);
        SelfTest.verdict("forge_clay_barrel_takes_lava", clayIn == 1000 && clayBe.state().fluid().equals("minecraft:lava"), clayIn + " mB");
        int wet = wooden.fill(new FluidStack(Fluids.WATER, 600), IFluidHandler.FluidAction.EXECUTE);
        int other = wooden.fill(new FluidStack(ModFluids.source("brine"), 100), IFluidHandler.FluidAction.EXECUTE);
        int over = wooden.fill(new FluidStack(Fluids.WATER, 600), IFluidHandler.FluidAction.EXECUTE);
        SelfTest.verdict("forge_barrel_holds_one_kind_and_one_bucket", wet == 600 && other == 0 && over == 400 && woodenBe.state().amount() == 1000
                && woodenBe.state().fluid().equals("minecraft:water"), wet + ", " + other + ", " + over);
        SelfTest.verdict("forge_wrong_kind_drain_gets_nothing", wooden.drain(new FluidStack(ModFluids.source("brine"), 100), IFluidHandler.FluidAction.EXECUTE).isEmpty()
                && woodenBe.state().amount() == 1000, "brine asked, water held");
        // a running soak keeps the water in
        BarrelState.SoakRule rule = new BarrelState.SoakRule("fluid_test_soak", "minecraft:water", 100, "minecraft:clay", 400);
        boolean soaking = woodenBe.state().startSoak(rule, "minecraft:dirt", 1.0);
        int drained = wooden.drain(500, IFluidHandler.FluidAction.EXECUTE).getAmount();
        SelfTest.verdict("forge_pipe_cannot_drain_a_working_barrel", soaking && drained == 0, "soak running, drained " + drained);
        woodenBe.state().restoreFluid("", 0);
        clayBe.state().restoreFluid("", 0);

        // the tray takes one cast
        IFluidHandler tray = handler(level, m.tray());
        CastingTrayBlockEntity trayBe = (CastingTrayBlockEntity) level.getBlockEntity(m.tray());
        FluidStack bucketOfCopper = new FluidStack(ModFluids.source("molten_copper"), 1000);
        int simulatedCast = tray.fill(bucketOfCopper, IFluidHandler.FluidAction.SIMULATE);
        boolean idle = trayBe.state().canAccept();
        int cast = tray.fill(bucketOfCopper, IFluidHandler.FluidAction.EXECUTE);
        SelfTest.verdict("forge_tray_takes_one_cast_of_100", simulatedCast == 100 && idle && cast == 100 && trayBe.state().stage() == TrayView.Stage.FILLING
                && tray.getTankCapacity(0) == 100, cast + " mB");
        SelfTest.verdict("forge_busy_tray_takes_and_gives_nothing", tray.fill(bucketOfCopper, IFluidHandler.FluidAction.EXECUTE) == 0
                && tray.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "busy");
        trayBe.state().reset();
    }
}
