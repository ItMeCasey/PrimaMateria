package com.itmecasey.primamateria;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Every block state property of the mod. Frozen: workstream A's blocks use these, workstream B's blockstate files name them and
 * workstream C's renderers read them.
 */
public final class StateProps {
    /** Sieve and auto hammer: true while being shaken (the renderer draws the moving parts, the block model only the frame). */
    public static final BooleanProperty MOVING = BooleanProperty.create("moving");
    /** Sieve: 0 = no mesh, 1 to 8 = the fitted mesh's slot from its mesh data file. */
    public static final IntegerProperty MESH = IntegerProperty.create("mesh", 0, 8);
    /** Sieve: layer height in quarters, used when animations are off. */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 4);
    /** Furnace drive: burning. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Powered drive: charge shown on the strip in five steps. */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 5);
    /** Drives and the casting tray: the side the front (or the fed notch) faces. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private StateProps() {
    }
}
