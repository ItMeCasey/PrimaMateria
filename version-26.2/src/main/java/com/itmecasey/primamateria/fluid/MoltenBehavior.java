package com.itmecasey.primamateria.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * What makes a molten metal act like lava on top of the loader's own flowing fluid class: it sets fire to flammable neighbors,
 * turns to stone where it meets water from above, pops and crackles. The loader fluid classes call these from their overrides, so
 * the rules are written once.
 */
public final class MoltenBehavior {
    private MoltenBehavior() {
    }

    /** Lava's random tick: sets flammable blocks near the fluid alight. */
    public static void randomTick(ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.canSpreadFireAround(pos)) {
            return;
        }
        int passes = random.nextInt(3);
        if (passes > 0) {
            BlockPos test = pos;
            for (int pass = 0; pass < passes; pass++) {
                test = test.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                if (!level.isLoaded(test)) {
                    return;
                }
                BlockState state = level.getBlockState(test);
                if (state.isAir()) {
                    if (hasFlammableNeighbours(level, test)) {
                        level.setBlockAndUpdate(test, BaseFireBlock.getState(level, test));
                        return;
                    }
                } else if (!state.getCollisionShape(level, test).isEmpty()) {
                    return;
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                BlockPos test = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                if (!level.isLoaded(test)) {
                    return;
                }
                if (level.isEmptyBlock(test.above()) && isFlammable(level, test, Direction.UP)) {
                    level.setBlockAndUpdate(test.above(), BaseFireBlock.getState(level, test.above()));
                }
            }
        }
    }

    private static boolean hasFlammableNeighbours(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (isFlammable(level, pos.relative(direction), direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFlammable(LevelReader level, BlockPos pos, Direction face) {
        if (level.isInsideBuildHeight(pos.getY()) && !level.hasChunkAt(pos)) {
            return false;
        }
        return level.getBlockState(pos).ignitedByLava();
    }

    /** Lava's pops and crackles above the surface (client side). */
    public static void animateTick(Level level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && !level.getBlockState(above).isSolidRender()) {
            if (random.nextInt(100) == 0) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + 1.0;
                double z = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
                level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.AMBIENT, 0.2F + random.nextFloat() * 0.2F,
                        0.9F + random.nextFloat() * 0.15F, false);
            }
            if (random.nextInt(200) == 0) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.AMBIENT,
                        0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }
        }
    }

    /** The fizz of lava meeting water or breaking a block. */
    public static void fizz(LevelAccessor level, BlockPos pos) {
        level.levelEvent(1501, pos, 0);
    }

    /**
     * Where a molten metal flows down into water it turns the liquid block there to stone. Returns whether it handled the spread.
     */
    public static boolean spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction) {
        if (direction == Direction.DOWN) {
            FluidState fluidState = level.getFluidState(pos);
            if (fluidState.is(FluidTags.WATER)) {
                if (state.getBlock() instanceof LiquidBlock) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                }
                fizz(level, pos);
                return true;
            }
        }
        return false;
    }
}
