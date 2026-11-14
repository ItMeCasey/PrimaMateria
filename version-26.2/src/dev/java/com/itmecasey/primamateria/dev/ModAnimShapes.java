package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.ModShapes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Checks the selection shapes: every box lies inside the block and none is empty. */
final class ModAnimShapes {
    private ModAnimShapes() {
    }

    static boolean sieveWithinBlock() {
        for (VoxelShape shape : new VoxelShape[] {ModShapes.SIEVE, ModShapes.WOODEN_BARREL, ModShapes.CLAY_BARREL, ModShapes.CRUCIBLE,
                ModShapes.AUTO_HAMMER, ModShapes.POWERED_DRIVE, ModShapes.CASTING_TRAY, ModShapes.WINDUP_DRIVE, ModShapes.FURNACE_DRIVE}) {
            if (shape.isEmpty()) {
                return false;
            }
            AABB b = shape.bounds();
            if (b.minX < 0 || b.minY < 0 || b.minZ < 0 || b.maxX > 1 || b.maxY > 1 || b.maxZ > 1) {
                return false;
            }
        }
        return true;
    }
}
