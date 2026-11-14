package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.ModAnim;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.anim.SieveAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * Sounds and particles for the events the animator reports. Shake sounds are limited per tick (nearest sieves would be best, the
 * ones that come first in the frame are used), pitch varies by 5 percent, and shake volume falls as the layer thins.
 */
final class SieveFx {
    static final int COARSE = 0;
    static final int FINE = 1;
    static final int SOIL = 2;
    private static final String[] SHAKE_SOUNDS = {"sieve.shake.coarse", "sieve.shake.fine", "sieve.shake.soil"};
    private static long budgetTick = Long.MIN_VALUE;
    private static int budgetUsed;

    private SieveFx() {
    }

    /** Fills the material class, map color and tint of a cache from the loaded block. */
    static void classify(SieveCache c, String blockId) {
        c.loadBlock = blockId;
        c.layerTexture = null;
        c.tint = 0xFFFFFF;
        c.soundClass = SOIL;
        c.dry = false;
        c.mapColor = 0x8a8a8a;
        if (blockId.isEmpty()) {
            return;
        }
        Identifier id = Identifier.tryParse(blockId);
        if (id == null) {
            return;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        SoundType type = block.defaultBlockState().getSoundType();
        if (type == SoundType.GRAVEL || type == SoundType.STONE || type == SoundType.DEEPSLATE || type == SoundType.NETHERRACK) {
            c.soundClass = COARSE;
            c.dry = true;
        } else if (type == SoundType.SAND || type == SoundType.SNOW || type == SoundType.WOOL) {
            c.soundClass = FINE;
            c.dry = true;
        }
        c.mapColor = block.defaultMapColor().col;
        String path = id.getPath();
        if (path.equals("grass_block")) {
            c.tint = 0x79C05A;
        }
        c.layerTexture = SieveModels.layerTexture(blockId);
    }

    static void handle(Level level, BlockPos pos, SieveCache c, int mode, double distSq) {
        SieveAnimator a = c.animator;
        int ev = a.events;
        if (ev == 0) {
            return;
        }
        RandomSource random = level.getRandom();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.7;
        double cz = pos.getZ() + 0.5;
        if ((ev & SieveAnimator.E_LOAD) != 0) {
            play(level, "sieve.load", cx, cy, cz, 0.7F, 1F, random);
            if (puffs(mode, distSq)) {
                for (int i = 0; i < 5 && c.dry; i++) {
                    dust(c, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.9, pos.getZ() + 0.3 + random.nextDouble() * 0.4);
                }
            }
        }
        if ((ev & SieveAnimator.E_SHAKE) != 0) {
            long now = level.getGameTime();
            if (now != budgetTick) {
                budgetTick = now;
                budgetUsed = 0;
            }
            if (budgetUsed < ClientConfig.soundsPerTick()) {
                budgetUsed++;
                play(level, SHAKE_SOUNDS[c.soundClass], cx, cy, cz, a.shakeVolume, 1F, random);
            }
            if (puffs(mode, distSq)) {
                int n = 1 + (int) Math.min(2, a.layerQuarters());
                for (int i = 0; i < n; i++) {
                    fines(c, pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.55, pos.getZ() + 0.35 + random.nextDouble() * 0.3);
                }
                if (c.dry) {
                    dust(c, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5);
                }
            }
        }
        if ((ev & SieveAnimator.E_REVEAL) != 0) {
            float pitch = (float) Math.pow(2.0, Math.max(0, a.revealedCount - 1) / 12.0);
            play(level, "sieve.reveal", cx, cy, cz, 0.5F, pitch, random);
        }
        if ((ev & SieveAnimator.E_FINISH) != 0) {
            play(level, "sieve.finish", cx, cy, cz, 0.8F, 1F, random);
        }
        if ((ev & SieveAnimator.E_MESH) != 0) {
            play(level, "sieve.mesh", cx, cy, cz, 0.7F, 1F, random);
        }
    }

    private static boolean puffs(int mode, double distSq) {
        return mode == ClientConfig.FULL && ClientConfig.particles() && distSq < 24.0 * 24.0;
    }

    private static void play(Level level, String name, double x, double y, double z, float volume, float pitch, RandomSource random) {
        SoundEvent event = ModContent.sound(name);
        if (event != null) {
            level.playLocalSound(x, y, z, event, SoundSource.BLOCKS, volume, pitch * (0.95F + random.nextFloat() * 0.1F), false);
        }
    }

    private static void fines(SieveCache c, double x, double y, double z) {
        if (!SiftParticles.hasRoom()) {
            return;
        }
        Particle p = Minecraft.getInstance().particleEngine.createParticle(ModAnim.particle("fines"), x, y, z, 0, 0, 0);
        tint(p, c.mapColor, 1F);
    }

    private static void dust(SieveCache c, double x, double y, double z) {
        if (!SiftParticles.hasRoom()) {
            return;
        }
        Particle p = Minecraft.getInstance().particleEngine.createParticle(ModAnim.particle("sift_dust"), x, y, z, 0, 0.01, 0);
        tint(p, c.mapColor, 1.25F);
    }

    private static void tint(Particle p, int rgb, float lighten) {
        if (p instanceof SingleQuadParticle q) {
            float r = Math.min(1F, ((rgb >> 16) & 255) / 255F * lighten);
            float g = Math.min(1F, ((rgb >> 8) & 255) / 255F * lighten);
            float b = Math.min(1F, (rgb & 255) / 255F * lighten);
            q.setColor(r, g, b);
        }
    }
}
