package com.itmecasey.primamateria;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The selection and collision shape of every model block. Workstream B owns this file and replaces each full cube with boxes that
 * follow the model; workstream A's blocks only read the constants.
 */
public final class ModShapes {
    public static final VoxelShape SIEVE = Shapes.block();
    public static final VoxelShape WOODEN_BARREL = Shapes.block();
    public static final VoxelShape CLAY_BARREL = Shapes.block();
    public static final VoxelShape CRUCIBLE = Shapes.block();
    public static final VoxelShape CASTING_TRAY = Shapes.block();
    public static final VoxelShape WINDUP_DRIVE = Shapes.block();
    public static final VoxelShape FURNACE_DRIVE = Shapes.block();
    public static final VoxelShape POWERED_DRIVE = Shapes.block();
    public static final VoxelShape AUTO_HAMMER = Shapes.block();

    private ModShapes() {
    }
}
