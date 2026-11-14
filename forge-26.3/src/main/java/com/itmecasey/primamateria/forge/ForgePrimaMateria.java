package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.Dev;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.PrimaMateria;
import com.itmecasey.primamateria.fluid.ModFluids;
import com.itmecasey.primamateria.data.PrimaReload;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.world.StartIslandPlacer;
import com.itmecasey.primamateria.hammer.HammerLogic;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import com.itmecasey.primamateria.data.LookupSync;
import net.minecraftforge.event.level.BlockEvent;
import com.itmecasey.primamateria.blockentity.DriveBlockEntity;
import com.itmecasey.primamateria.view.DriveKind;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Forge entry point. Registers the content; workstream A adds capabilities, networking and reload listeners here. */
@Mod(PrimaMateria.MODID)
public final class ForgePrimaMateria {
    static SimpleChannel channel;

    public ForgePrimaMateria(FMLJavaModLoadingContext context) {
        PrimaMateria.init(FMLPaths.CONFIGDIR::get);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgePrimaMateria::register);
        net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent.getBus(context.getModBusGroup())
                .addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(ForgePrimaMateria::registerDispensers));
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            ForgeClient.install(context);
            ForgeFluidClient.install();
        }
        com.itmecasey.primamateria.fluid.ItemFluids.install(new ForgeItemFluids());
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgePrimaMateria::attach);
        AttachCapabilitiesEvent.ItemStacks.BUS.addListener(ForgePrimaMateria::attachBuckets);
        channel = ChannelBuilder.named(net.minecraft.resources.Identifier.fromNamespaceAndPath(com.itmecasey.primamateria.PrimaMateria.MODID, "lookup"))
                .networkProtocolVersion(1)
                .optional()
                .simpleChannel();
        channel.messageBuilder(LookupMessage.class)
                .codec(LookupMessage.CODEC)
                .consumerMainThread(LookupMessage::handle)
                .add();
        // players of a dedicated server get the recipe lookup when they join and after every reload, before tags and recipes
        OnDatapackSyncEvent.BUS.addListener((OnDatapackSyncEvent e) -> {
            for (ServerPlayer player : e.getPlayers()) {
                LookupSync.send(player, frame -> channel.send(new LookupMessage(frame), PacketDistributor.PLAYER.with(player)));
            }
        });
        AddReloadListenerEvent.BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new PrimaReload()));
        ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> StartIslandPlacer.onServerStarted(e.getServer()));
        // A hammer crushes the block it breaks: HammerLogic does the work and the original break is canceled (true cancels).
        BlockEvent.BreakEvent.BUS.addListener((BlockEvent.BreakEvent e) ->
                e.getPlayer() instanceof ServerPlayer player && e.getLevel() instanceof ServerLevel level
                        && HammerLogic.onBreak(player, level, e.getPos(), e.getState()));
        Dev.call("com.itmecasey.primamateria.forge.dev.ForgeDev", "install", context);
    }

    /** The powered drive takes energy from every side. */
    /** Forge's own bucket wrapper is the fluid handler of every bucket item (this Forge build attaches none by itself). */
    private static void attachBuckets(AttachCapabilitiesEvent.ItemStacks event) {
        if (event.getObject().getItem() instanceof net.minecraft.world.item.BucketItem) {
            event.addCapability(ModContent.id("fluid_bucket"), new net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper(event.getObject()));
        }
    }

    /**
     * Forge's {@code DispenseFluidContainer} is the loader's dispenser behavior for fluid containers, but it finds the container's
     * fluid through {@code FluidUtil.getFluidHandler(ItemStack)}, which this Forge build stubs out ("TEMP FIX"), and then swallows
     * the bucket. So on Forge the dispensers use the game's own bucket behavior, which it registers for every bucket.
     */
    private static void registerDispensers() {
        net.minecraft.core.dispenser.DispenseItemBehavior game = net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.get(net.minecraft.world.item.Items.LAVA_BUCKET);
        if (game != null) {
            com.itmecasey.primamateria.fluid.FluidDispense.register(game);
        }
    }

    private static void attach(AttachCapabilitiesEvent.BlockEntities event) {
        BlockEntity be = event.getObject();
        if (be instanceof com.itmecasey.primamateria.blockentity.BarrelBlockEntity
                || be instanceof com.itmecasey.primamateria.blockentity.CrucibleBlockEntity
                || be instanceof com.itmecasey.primamateria.blockentity.CastingTrayBlockEntity) {
            // the barrels, the crucible and the casting tray offer their fluid to pipes and tanks
            LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluid = LazyOptional.of(() -> MachineFluidHandler.of(be));
            event.addCapability(ModContent.id("fluid"), new ICapabilityProvider() {
                @Override
                public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                    return cap == ForgeCapabilities.FLUID_HANDLER ? fluid.cast() : LazyOptional.empty();
                }
            });
            event.addListener(fluid::invalidate);
            return;
        }
        // Forge attaches capabilities while the block entity is still being constructed, before it knows its kind, so which
        // drives answer is decided when the capability is asked for.
        if (!(be instanceof DriveBlockEntity drive)) {
            return;
        }
        LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> new DriveEnergy(drive));
        event.addCapability(ModContent.id("energy"), new ICapabilityProvider() {
            @Override
            public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY && drive.kind() == DriveKind.POWERED ? energy.cast() : LazyOptional.empty();
            }
        });
        event.addListener(energy::invalidate);
    }

    /** The powered drive's energy as other mods see it: it can be filled and never drained. */
    private record DriveEnergy(DriveBlockEntity be) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return be.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return be.state().energy();
        }

        @Override
        public int getMaxEnergyStored() {
            return be.state().capacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    private static void register(RegisterEvent event) {
        event.register(net.minecraftforge.registries.ForgeRegistries.Keys.FLUID_TYPES,
                helper -> ForgeFluids.registerTypes((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.FLUID, helper -> ModFluids.registerFluids((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK, helper -> ModFluids.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModContent.registerBlockEntities((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.RECIPE_TYPE, helper -> PrimaRecipes.registerTypes((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.RECIPE_SERIALIZER, helper -> PrimaRecipes.registerSerializers((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.SOUND_EVENT, helper -> ModContent.registerSounds((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> com.itmecasey.primamateria.ModAnim.registerBlockEntityTypes((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.PARTICLE_TYPE, helper -> com.itmecasey.primamateria.ModAnim.registerParticles((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id(Ids.CREATIVE_TAB), ModContent.creativeTab()));
    }
}
