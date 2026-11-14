package com.itmecasey.primamateria.compat;

import com.itmecasey.primamateria.view.ViewAccess;
import com.itmecasey.primamateria.view.ViewProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Lets the script bridges read the state of blocks: finds the block entity (a {@link ViewProvider}) at a position. */
public final class GameAccess {
    private GameAccess() {
    }

    /** Called once by {@code ModJsBridge.install()} at startup. */
    public static void install() {
        ViewAccess.install((level, x, y, z) -> view(level, new BlockPos(x, y, z)));
    }

    /** The view record of the block entity at a position, or null. */
    public static Object view(Object level, BlockPos pos) {
        if (level instanceof Level l && l.isLoaded(pos)) {
            BlockEntity be = l.getBlockEntity(pos);
            if (be instanceof ViewProvider provider) {
                return provider.view();
            }
        }
        return null;
    }

    /** The view record of a block entity, or null when it is not one of ours. */
    public static Object view(BlockEntity entity) {
        return entity instanceof ViewProvider provider ? provider.view() : null;
    }
}
