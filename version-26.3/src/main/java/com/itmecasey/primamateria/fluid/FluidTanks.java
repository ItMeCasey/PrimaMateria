package com.itmecasey.primamateria.fluid;

import com.itmecasey.primamateria.blockentity.BarrelBlockEntity;
import com.itmecasey.primamateria.blockentity.CastingTrayBlockEntity;
import com.itmecasey.primamateria.blockentity.CrucibleBlockEntity;
import com.itmecasey.primamateria.machine.CrucibleLogic;
import com.itmecasey.primamateria.recipe.CrucibleRecipes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid tank of each machine as a {@link MachineTank}. Loader code wraps these in the loader's fluid handler; nothing here
 * depends on a loader. The tanks are cheap to build: ask for a new one per call.
 */
public final class FluidTanks {
    /** How many times a fluid change was reported to a machine since the game started (the self test counts them). */
    public static final java.util.concurrent.atomic.AtomicInteger CHANGES = new java.util.concurrent.atomic.AtomicInteger();

    private FluidTanks() {
    }

    /** The tank of a barrel, crucible or casting tray, or null for any other block entity. */
    @Nullable
    public static MachineTank of(BlockEntity be) {
        if (be instanceof BarrelBlockEntity barrel) {
            return new BarrelTank(barrel.state());
        }
        if (be instanceof CrucibleBlockEntity crucible) {
            return new CrucibleTank(crucible.state(), fluid -> accepted(crucible.getLevel(), fluid));
        }
        if (be instanceof CastingTrayBlockEntity tray) {
            return new TrayTank(tray.state(), fluid -> plan(tray.getLevel(), fluid));
        }
        return null;
    }

    /** Tells a machine that its fluid changed: it is saved and the clients are updated. */
    public static void changed(BlockEntity be) {
        CHANGES.incrementAndGet();
        if (be instanceof BarrelBlockEntity barrel) {
            barrel.changed();
        } else if (be instanceof CrucibleBlockEntity crucible) {
            crucible.changed();
        } else if (be instanceof CastingTrayBlockEntity tray) {
            tray.changed();
        }
    }

    /** What a crucible takes: lava, the molten metals, and any fluid (of any mod) that a casting recipe turns into an item. */
    private static boolean accepted(@Nullable net.minecraft.world.level.Level level, String fluid) {
        return level instanceof ServerLevel server ? CrucibleLogic.accepts(server, fluid) : CrucibleLogic.hot(fluid);
    }

    private static CrucibleRecipes.Cast cast(@Nullable net.minecraft.world.level.Level level, String fluid) {
        return level instanceof ServerLevel server ? CrucibleRecipes.castFor(server.recipeAccess(), fluid) : null;
    }

    private static TrayTank.Plan plan(@Nullable net.minecraft.world.level.Level level, String fluid) {
        CrucibleRecipes.Cast cast = cast(level, fluid);
        return cast == null ? null : new TrayTank.Plan(cast.amount(), cast.result(), 1, cast.time());
    }
}
