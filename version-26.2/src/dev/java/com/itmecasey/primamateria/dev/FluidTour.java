package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Development-only camera tour of the four fluids, run by {@link ShotTour} when {@code PRIMAMATERIA_SHOTS=fluids}. For each fluid it
 * builds a pool (a source block inside a stone-brick ring, glass on the near side) and a stone block with a source on top that spills
 * down its sides, waits for the flow to settle, and takes a frame of each with the fluid's bucket in hand and all four buckets in the
 * hotbar; then frames from inside a brine pool and a pool of molten gold. Frames are named {@code fluids_<id>_still},
 * {@code fluids_<id>_flowing}, {@code fluids_brine_inside} and {@code fluids_molten_gold_inside}.
 */
final class FluidTour {
    private FluidTour() {
    }

    static void plan(int y) {
        int count = Ids.FLUIDS.size();
        ShotTour.then(0, () -> {
            ShotTour.server("gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule spawn_monsters false",
                    "gamerule fire_spread_radius_around_player 0", "gamerule fire_damage false", "difficulty peaceful", "time set noon", "weather clear",
                    "kill @e[type=!player,type=!item]", "clear @p");
        });
        ShotTour.then(4, () -> {
            ShotTour.server("fill -24 " + (y - 1) + " -12 40 " + (y - 1) + " 40 minecraft:smooth_stone",
                    "fill -24 " + y + " -12 40 " + (y + 12) + " 40 minecraft:air");
            for (int i = 0; i < count; i++) {
                ShotTour.server("item replace entity @p hotbar." + i + " with primamateria:" + Ids.FLUIDS.get(i) + "_bucket");
            }
        });
        // pools and spills, all four at once so the flow settles together
        for (int i = 0; i < count; i++) {
            int x0 = -20 + 9 * i;
            String block = "primamateria:" + Ids.FLUIDS.get(i);
            ShotTour.then(2, () -> ShotTour.server(
                    "fill " + x0 + " " + y + " 0 " + (x0 + 4) + " " + y + " 4 minecraft:stone_bricks",
                    "fill " + (x0 + 1) + " " + y + " 1 " + (x0 + 3) + " " + y + " 3 " + block,
                    "fill " + x0 + " " + y + " 0 " + (x0 + 4) + " " + y + " 0 minecraft:glass",
                    "fill " + x0 + " " + y + " 12 " + (x0 + 2) + " " + (y + 1) + " 14 minecraft:stone_bricks",
                    "setblock " + (x0 + 1) + " " + (y + 2) + " 13 " + block));
        }
        for (int i = 0; i < count; i++) {
            int index = i;
            int x0 = -20 + 9 * i;
            String id = Ids.FLUIDS.get(i);
            if (i == 0) {
                ShotTour.then(500, () -> {
                });
            } else {
                ShotTour.then(2, () -> {
                });
            }
            ShotTour.then(2, () -> {
                ShotTour.server("item replace entity @p weapon.mainhand with primamateria:" + id + "_bucket");
                ShotTour.moveTo(new Vec3(x0 + 2.5, y + 2.7, -3.6), new Vec3(x0 + 2.5, y + 0.4, 2.5));
            });
            ShotTour.shoot("fluids_" + id + "_still", 25);
            ShotTour.then(2, () -> ShotTour.moveTo(new Vec3(x0 + 1.5, y + 3.0, 7.4), new Vec3(x0 + 1.5, y + 0.8, 13.0)));
            ShotTour.shoot("fluids_" + id + "_flowing", 25);
            if (index == 0 || index == 3) {
                int tx = 40 + index * 3;
                String tank = "primamateria:" + id;
                ShotTour.then(2, () -> ShotTour.server("fill " + tx + " " + y + " 20 " + (tx + 6) + " " + (y + 6) + " 26 minecraft:stone_bricks",
                        "fill " + (tx + 1) + " " + (y + 1) + " 21 " + (tx + 5) + " " + (y + 5) + " 25 " + tank));
                ShotTour.then(6, () -> ShotTour.moveTo(new Vec3(tx + 3.5, y + 3.0, 23.5), new Vec3(tx + 3.5, y + 3.0, 26.5)));
                ShotTour.shoot("fluids_" + id + "_inside", 30);
            }
        }
        ShotTour.then(20, () -> ShotTour.quit(Minecraft.getInstance()));
    }
}
