package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.anim.SieveViewSource;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tints the sieve's still layer (the {@code fill} models, used when animations are off and while the sieve rests) with the loaded
 * block's own color: the average of its top texture, like the fines particles. Gray when nothing is known.
 */
public final class FillTint implements BlockTintSource {
    @Override
    public int color(BlockState state) {
        return 0xFFB0B0B0;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SieveViewSource source) {
            String block = source.sieveView().loadBlock();
            if (!block.isEmpty()) {
                return 0xFF000000 | SieveModels.averageColor(SieveModels.layerTexture(block));
            }
        }
        return color(state);
    }
}
