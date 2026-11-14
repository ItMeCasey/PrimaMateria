package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.fluid.FluidLook;
import com.itmecasey.primamateria.fluid.ModFluids;
import com.itmecasey.primamateria.fluid.MoltenBehavior;
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
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * The loader side of the four fluids on NeoForge: their fluid types and fluid classes. Brine is water's own fluid class and the molten
 * metals are lava's, each with its own fluid type, so they behave as water and lava do and everything else is shared in
 * {@link ModFluids}. The fluid type of each is created once and registered by {@link NeoForgePrimaMateria}.
 */
public final class NeoForgeFluids implements ModFluids.Factory {
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
        return new net.minecraft.world.item.BucketItem(ModFluids.source(id), properties);
    }

    /** The fluid type of one of our fluids (made on first use). */
    static synchronized FluidType type(String id) {
        return TYPES.computeIfAbsent(id, NeoForgeFluids::makeType);
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
            p.fallDistanceModifier(0F).canExtinguish(true).canConvertToSource(true).supportsBoating(true).isWaterLike(true)
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

    // ---- the fluids: the loader's own flowing fluid class, with its bucket and block in the Properties ----

    private static Item bucket(String id) {
        return ModFluids.bucket(id);
    }

    private static net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties props(String id) {
        net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties p = new net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties(() -> type(id), () -> ModFluids.source(id), () -> ModFluids.flowing(id))
                .bucket(() -> bucket(id)).block(() -> ModFluids.block(id));
        if (ModFluids.isMolten(id)) {
            // lava's numbers: slow, short, heavy
            p.slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(30).explosionResistance(100F);
        }
        return p;
    }

    /** Brine: the loader's flowing fluid with water's numbers (slope 4, drop 1, tick 5, its type makes it act as water). */
    private static final class BrineSource extends net.neoforged.neoforge.fluids.BaseFlowingFluid.Source {
        BrineSource(String id) {
            super(props(id));
        }
    }

    private static final class BrineFlowing extends net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing {
        BrineFlowing(String id) {
            super(props(id));
        }
    }

    /** A molten metal: the loader's flowing fluid plus lava's fire and stone rules (see {@code MoltenBehavior}). */
    private static final class MoltenSource extends net.neoforged.neoforge.fluids.BaseFlowingFluid.Source {
        MoltenSource(String id) {
            super(props(id));
        }

        @Override
        public void animateTick(net.minecraft.world.level.Level level, BlockPos pos, FluidState state, net.minecraft.util.RandomSource random) {
            MoltenBehavior.animateTick(level, pos, random);
        }

        @Override
        public void randomTick(ServerLevel level, BlockPos pos, FluidState state, net.minecraft.util.RandomSource random) {
            MoltenBehavior.randomTick(level, pos, random);
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void spreadTo(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state, net.minecraft.core.Direction direction, FluidState target) {
            if (!MoltenBehavior.spreadTo(level, pos, state, direction)) {
                super.spreadTo(level, pos, state, direction, target);
            }
        }

        @Override
        protected void entityInside(net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.Entity entity,
                                    net.minecraft.world.entity.InsideBlockEffectApplier applier) {
            applier.apply(net.minecraft.world.entity.InsideBlockEffectType.CLEAR_FREEZE);
            applier.apply(net.minecraft.world.entity.InsideBlockEffectType.LAVA_IGNITE);
            applier.runAfter(net.minecraft.world.entity.InsideBlockEffectType.LAVA_IGNITE, net.minecraft.world.entity.Entity::lavaHurt);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Fluid other,
                                            net.minecraft.core.Direction direction) {
            return state.getHeight(level, pos) >= 0.44444445F && other.is(net.minecraft.tags.FluidTags.WATER);
        }
    }

    private static final class MoltenFlowing extends net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing {
        MoltenFlowing(String id) {
            super(props(id));
        }

        @Override
        public void animateTick(net.minecraft.world.level.Level level, BlockPos pos, FluidState state, net.minecraft.util.RandomSource random) {
            MoltenBehavior.animateTick(level, pos, random);
        }

        @Override
        public void randomTick(ServerLevel level, BlockPos pos, FluidState state, net.minecraft.util.RandomSource random) {
            MoltenBehavior.randomTick(level, pos, random);
        }

        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        protected void spreadTo(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state, net.minecraft.core.Direction direction, FluidState target) {
            if (!MoltenBehavior.spreadTo(level, pos, state, direction)) {
                super.spreadTo(level, pos, state, direction, target);
            }
        }

        @Override
        protected void entityInside(net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.Entity entity,
                                    net.minecraft.world.entity.InsideBlockEffectApplier applier) {
            applier.apply(net.minecraft.world.entity.InsideBlockEffectType.CLEAR_FREEZE);
            applier.apply(net.minecraft.world.entity.InsideBlockEffectType.LAVA_IGNITE);
            applier.runAfter(net.minecraft.world.entity.InsideBlockEffectType.LAVA_IGNITE, net.minecraft.world.entity.Entity::lavaHurt);
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Fluid other,
                                            net.minecraft.core.Direction direction) {
            return state.getHeight(level, pos) >= 0.44444445F && other.is(net.minecraft.tags.FluidTags.WATER);
        }
    }
}
