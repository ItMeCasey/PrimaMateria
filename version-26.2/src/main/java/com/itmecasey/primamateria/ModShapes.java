package com.itmecasey.primamateria;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The selection and collision shape of every model block. Workstream B owns this file; the boxes follow the models in
 * {@code branding/models} (sieve, barrels, crucible, auto hammer and powered drive; the other three stay full blocks until their
 * models are made). Workstream A's blocks only read the constants.
 */
public final class ModShapes {
    /** Four posts with feet, the rail ring and the hanging tray. */
    public static final VoxelShape SIEVE = Shapes.or(
            Block.box(0.5, 0, 0.5, 3.5, 13, 3.5), Block.box(12.5, 0, 0.5, 15.5, 13, 3.5),
            Block.box(0.5, 0, 12.5, 3.5, 13, 15.5), Block.box(12.5, 0, 12.5, 15.5, 13, 15.5),
            Block.box(1, 10, 1, 15, 13, 15), Block.box(3.5, 13, 3.5, 12.5, 15.5, 12.5));
    public static final VoxelShape WOODEN_BARREL = Block.box(0.5, 0, 0.5, 15.5, 16, 15.5);
    public static final VoxelShape CLAY_BARREL = Block.box(1, 0, 1, 15, 16, 15);
    /** Three feet, the belly and the flared rim. */
    public static final VoxelShape CRUCIBLE = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13), Block.box(2, 2, 2, 14, 14, 14), Block.box(0.5, 14, 0.5, 15.5, 15.5, 15.5));
    public static final VoxelShape CASTING_TRAY = Shapes.block();
    public static final VoxelShape WINDUP_DRIVE = Shapes.block();
    public static final VoxelShape FURNACE_DRIVE = Shapes.block();
    /** Base, case, the cams on top and the coil at the back (the front faces north). */
    public static final VoxelShape POWERED_DRIVE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16), Block.box(1.5, 3, 1.5, 14.5, 12, 14.5), Block.box(4, 12, 5, 12, 14.5, 11),
            Block.box(4, 4, 14.5, 12, 11, 16));
    public static final VoxelShape AUTO_HAMMER = Block.box(0.5, 0, 0.5, 15.5, 15, 15.5);

    private ModShapes() {
    }
}
