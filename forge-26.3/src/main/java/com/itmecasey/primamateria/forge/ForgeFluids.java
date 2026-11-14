package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.fluid.FluidLook;
import com.itmecasey.primamateria.fluid.ModFluids;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

/**
 * The loader side of the four fluids on Forge: their fluid types and fluid classes. Brine is water's own fluid class and the molten
 * metals are lava's, each with its own fluid type, so they behave as water and lava do and everything else is shared in
 * {@link ModFluids}. The fluid type of each is created once and registered by {@link ForgePrimaMateria}.
 */
public final class ForgeFluids implements ModFluids.Factory {
    private static final Map<String, FluidType> TYPES = new LinkedHashMap<>();

    @Override
    public Fluid create(String id, boolean flowing) {
        if (ModFluids.isMolten(id)) {
            return flowing ? new MoltenFlowing(id) : new MoltenSource(id);
        }
        return flowing ? new BrineFlowing(id) : new BrineSource(id);
    }

    @Override
    public Item bucket(String id, Item.Properties properties) {
        // Forge looks a fluid up in its registry when a bucket is given the fluid itself; the supplier is read later, once it is registered
        return new net.minecraft.world.item.BucketItem(() -> ModFluids.source(id), properties);
    }

    /** The fluid type of one of our fluids (made on first use). */
    static synchronized FluidType type(String id) {
        return TYPES.computeIfAbsent(id, ForgeFluids::makeType);
    }

    /** Registers the fluid types. */
    static void registerTypes(BiConsumer<String, FluidType> out) {
        for (String id : Ids.FLUIDS) {
            out.accept(id, type(id));
        }
    }

    private static FluidType makeType(String id) {
        FluidType.Properties p = FluidType.Properties.create().descriptionId("block.primamateria." + id);
        if (!ModFluids.isMolten(id)) {
            // brine acts as water: it swims, it puts fires out, it carries boats; it does not water crops
            p.fallDistanceModifier(0F).canExtinguish(true).canConvertToSource(true).supportsBoating(true)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                    .density(1100).viscosity(1000).temperature(295);
            return new FluidType(p) {
                @Override
                public boolean canConvertToSource(FluidState state, LevelReader reader, BlockPos pos) {
                    if (reader instanceof ServerLevel level) {
                        return level.getGameRules().get(GameRules.WATER_SOURCE_CONVERSION);
                    }
                    return super.canConvertToSource(state, reader, pos);
                }
            };
        }
        // the molten metals are as hot and as heavy as lava, and they never make new sources
        p.canSwim(false).canDrown(false).pathType(PathType.LAVA).adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
                .lightLevel(FluidLook.glow("primamateria:" + id));
        int temperature = com.itmecasey.primamateria.fluid.Metals.ALL.stream().filter(m -> m.fluid().equals(id))
                .map(com.itmecasey.primamateria.fluid.Metals.Metal::temperature).findFirst().orElse(1337);
        p.temperature(temperature).density(3400).viscosity(6500);
        return new FluidType(p);
    }

    // ---- brine: water's fluid class ----

    private static Item bucket(String id) {
        return ModFluids.bucket(id);
    }

    private static final class BrineSource extends WaterFluid.Source {
        private final String id;

        BrineSource(String id) {
            this.id = id;
        }

        @Override
        public FluidType getFluidType() {
            return type(id);
        }

        @Override
        public Fluid getSource() {
            return ModFluids.source(id);
        }

        @Override
        public Fluid getFlowing() {
            return ModFluids.flowing(id);
        }

        @Override
        public Item getBucket() {
            return bucket(id);
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return ModFluids.block(id).defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == getSource() || fluid == getFlowing();
        }
    }

    private static final class BrineFlowing extends WaterFluid.Flowing {
        private final String id;

        BrineFlowing(String id) {
            this.id = id;
        }

        @Override
        public FluidType getFluidType() {
            return type(id);
        }

        @Override
        public Fluid getSource() {
            return ModFluids.source(id);
        }

        @Override
        public Fluid getFlowing() {
            return ModFluids.flowing(id);
        }

        @Override
        public Item getBucket() {
            return bucket(id);
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return ModFluids.block(id).defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == getSource() || fluid == getFlowing();
        }
    }

    // ---- molten metals: lava's fluid class ----

    private static final class MoltenSource extends LavaFluid.Source {
        private final String id;

        MoltenSource(String id) {
            this.id = id;
        }

        @Override
        public FluidType getFluidType() {
            return type(id);
        }

        @Override
        public Fluid getSource() {
            return ModFluids.source(id);
        }

        @Override
        public Fluid getFlowing() {
            return ModFluids.flowing(id);
        }

        @Override
        public Item getBucket() {
            return bucket(id);
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return ModFluids.block(id).defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == getSource() || fluid == getFlowing();
        }

        @Override
        protected boolean canConvertToSource(ServerLevel level) {
            return false;
        }
    }

    private static final class MoltenFlowing extends LavaFluid.Flowing {
        private final String id;

        MoltenFlowing(String id) {
            this.id = id;
        }

        @Override
        public FluidType getFluidType() {
            return type(id);
        }

        @Override
        public Fluid getSource() {
            return ModFluids.source(id);
        }

        @Override
        public Fluid getFlowing() {
            return ModFluids.flowing(id);
        }

        @Override
        public Item getBucket() {
            return bucket(id);
        }

        @Override
        public BlockState createLegacyBlock(FluidState state) {
            return ModFluids.block(id).defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        }

        @Override
        public boolean isSame(Fluid fluid) {
            return fluid == getSource() || fluid == getFlowing();
        }

        @Override
        protected boolean canConvertToSource(ServerLevel level) {
            return false;
        }
    }
}
