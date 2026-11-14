package com.itmecasey.primamateria;

import com.itmecasey.primamateria.blockentity.SievePreviewBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * What workstream C adds to the registries: the particle types and the block entity type that the sieve renderer is bound to.
 * {@link ModContent} stays as scaffolded. The sieve block entity type here belongs to the preview block; when workstream A's real
 * sieve block entity exists, the renderer is bound to its type instead (it only needs {@code SieveViewSource}).
 */
public final class ModAnim {
    public static final Map<String, SimpleParticleType> PARTICLES = new LinkedHashMap<>();
    public static final BlockEntityType<SievePreviewBlockEntity> SIEVE_PREVIEW;

    static {
        for (String name : Ids.PARTICLES) {
            PARTICLES.put(name, new SimpleParticleType(false));
        }
        SIEVE_PREVIEW = new BlockEntityType<>(SievePreviewBlockEntity::new, java.util.Set.<Block>of(ModContent.block(Ids.SIEVE)));
    }

    private ModAnim() {
    }

    public static SimpleParticleType particle(String name) {
        return PARTICLES.get(name);
    }

    public static void registerParticles(BiConsumer<String, ParticleType<?>> out) {
        PARTICLES.forEach(out);
    }

    public static void registerBlockEntityTypes(BiConsumer<String, BlockEntityType<?>> out) {
        out.accept("sieve_preview", SIEVE_PREVIEW);
    }
}
