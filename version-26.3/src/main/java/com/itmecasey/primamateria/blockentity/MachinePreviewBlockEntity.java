package com.itmecasey.primamateria.blockentity;

import com.itmecasey.primamateria.ModAnim;
import com.itmecasey.primamateria.anim.MachineViewSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds a view record for the machine renderer; the dev tour sets it on the client. Saves nothing. */
public class MachinePreviewBlockEntity extends BlockEntity implements MachineViewSource {
    private Object view;
    private Object cache;

    public MachinePreviewBlockEntity(BlockPos pos, BlockState state) {
        super(ModAnim.MACHINE_PREVIEW, pos, state);
    }

    @Override
    public Object machineView() {
        return view;
    }

    public void show(Object view) {
        this.view = view;
    }

    @Override
    public Object renderCache() {
        return cache;
    }

    @Override
    public void renderCache(Object cache) {
        this.cache = cache;
    }
}
