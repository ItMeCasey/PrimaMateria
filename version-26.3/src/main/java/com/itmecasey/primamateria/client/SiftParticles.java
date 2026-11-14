package com.itmecasey.primamateria.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

/** The two sifting particles: fines (a speck that falls and dies on the first block) and dust (a soft puff that rises). */
public final class SiftParticles {
    /** Most of our particles alive on this client at once; further spawns are skipped, which drops the newest first. */
    public static final int MAX_ALIVE = 150;
    private static int alive;

    private SiftParticles() {
    }

    public static boolean hasRoom() {
        return alive < MAX_ALIVE;
    }

    public static final class Fines extends SingleQuadParticle {
        Fines(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
            this.gravity = 1.0F;
            this.friction = 0.98F;
            this.lifetime = 24 + level.getRandom().nextInt(10);
            this.quadSize = level.getRandom().nextBoolean() ? 0.025F : 0.0375F;
            alive++;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.onGround) {
                this.remove();
            }
        }

        @Override
        public void remove() {
            if (!this.removed) {
                alive--;
            }
            super.remove();
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    public static final class Dust extends SimpleAnimatedParticle {
        Dust(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites, -0.01F);
            this.lifetime = 10 + level.getRandom().nextInt(8);
            this.quadSize = 0.08F;
            this.friction = 0.9F;
            alive++;
        }

        @Override
        public void remove() {
            if (!this.removed) {
                alive--;
            }
            super.remove();
        }
    }

    /** A drop of a stream pouring from a crucible lip into a casting tray: bright, tinted by the fluid, falls and dies on the tray. */
    public static final class Pour extends SingleQuadParticle {
        Pour(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
            this.gravity = 1.0F;
            this.friction = 0.98F;
            this.lifetime = 12;
            this.quadSize = 0.05F;
            alive++;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.onGround) {
                this.remove();
            }
        }

        @Override
        public int getLightCoords(float partialTick) {
            return 15728880;
        }

        @Override
        public void remove() {
            if (!this.removed) {
                alive--;
            }
            super.remove();
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    public static Pour pour(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        return new Pour(level, x, y, z, sprites);
    }

    public static Fines fines(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        return new Fines(level, x, y, z, sprites);
    }

    public static Dust dust(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        return new Dust(level, x, y, z, sprites);
    }
}
