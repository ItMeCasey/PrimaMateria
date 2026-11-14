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
    public static final BlockEntityType<com.itmecasey.primamateria.blockentity.MachinePreviewBlockEntity> MACHINE_PREVIEW;
    public static final java.util.List<String> MACHINE_BLOCKS = java.util.List.of(Ids.WOODEN_BARREL, Ids.CLAY_BARREL, Ids.CRUCIBLE,
            Ids.CASTING_TRAY, Ids.WINDUP_DRIVE, Ids.FURNACE_DRIVE, Ids.POWERED_DRIVE, Ids.AUTO_HAMMER);

    static {
        for (String name : Ids.PARTICLES) {
            PARTICLES.put(name, new SimpleParticleType(false));
        }
        SIEVE_PREVIEW = new BlockEntityType<>(SievePreviewBlockEntity::new, java.util.Set.<Block>of(ModContent.block(Ids.SIEVE)));
    }

    static {
        java.util.Set<Block> blocks = new java.util.HashSet<>();
        for (String name : MACHINE_BLOCKS) {
            blocks.add(ModContent.block(name));
        }
        MACHINE_PREVIEW = new BlockEntityType<>(com.itmecasey.primamateria.blockentity.MachinePreviewBlockEntity::new, blocks);
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
        out.accept("machine_preview", MACHINE_PREVIEW);
    }
}
