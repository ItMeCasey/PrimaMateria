package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.data.RecipeLookup.Snapshot;
import java.util.List;

/**
 * Development only (built with -PdevTools): a small world of recipes so the recipe viewers and the scripts have something to show
 * before the gameplay workstream fills {@code RecipeLookup} from the datapack. Installed at startup only when the game is started
 * with the environment variable {@code PRIMAMATERIA_DEMO}; it never replaces a snapshot that already has recipes.
 */
public final class DemoRecipes {
    private DemoRecipes() {
    }

    /** Called by name from the script bridge's install() in dev runs. */
    public static void install() {
        if (System.getenv("PRIMAMATERIA_DEMO") != null && RecipeLookup.get().sifting().isEmpty()) {
            RecipeLookup.set(snapshot());
        }
    }

    public static Snapshot snapshot() {
        List<RecipeLookup.Mesh> meshes = List.of(
                new RecipeLookup.Mesh("m1", "primamateria:cloth_mesh", 1, 0.75, "primamateria:block/mesh_cloth", 1),
                new RecipeLookup.Mesh("m2", "primamateria:copper_mesh", 2, 1.0, "primamateria:block/mesh_copper", 2),
                new RecipeLookup.Mesh("m3", "primamateria:iron_mesh", 3, 1.25, "primamateria:block/mesh_iron", 3),
                new RecipeLookup.Mesh("m4", "primamateria:diamond_mesh", 4, 1.5, "primamateria:block/mesh_diamond", 4),
                new RecipeLookup.Mesh("m5", "primamateria:netherite_mesh", 5, 2.0, "primamateria:block/mesh_netherite", 5));
        List<RecipeLookup.Sifting> sifting = List.of(
                new RecipeLookup.Sifting("g_flint", "minecraft:gravel", "minecraft:flint", 1, 2, 0.5, 1, 0),
                new RecipeLookup.Sifting("g_iron", "minecraft:gravel", "primamateria:iron_dust", 1, 2, 0.16, 3, 0),
                new RecipeLookup.Sifting("g_gold", "minecraft:gravel", "primamateria:gold_dust", 1, 1, 0.0035, 4, 5),
                new RecipeLookup.Sifting("d_chip", "minecraft:dirt", "primamateria:rock_chip", 1, 1, 0.35, 1, 0));
        List<RecipeLookup.Residue> residue = List.of(new RecipeLookup.Residue("g_sand", "minecraft:gravel", "minecraft:sand", 0.15));
        List<RecipeLookup.Crushing> crushing = List.of(new RecipeLookup.Crushing("c_leaves", "#minecraft:leaves", 1,
                List.of(new RecipeLookup.Chance("primamateria:plant_fiber", 1, 1, 1.0),
                        new RecipeLookup.Chance("primamateria:plant_fiber", 1, 2, 0.35))));
        return new Snapshot(crushing, sifting, residue,
                List.of(new RecipeLookup.Composting("comp", "minecraft:dirt", 400)),
                List.of(new RecipeLookup.Soaking("soak", "minecraft:water", 250, "primamateria:stone_meal", "minecraft:clay", 100)),
                List.of(new RecipeLookup.Mixing("mix", "minecraft:lava", "minecraft:water", 1000, "minecraft:obsidian", true)),
                List.of(new RecipeLookup.Steeping("steep", "minecraft:water", "primamateria:salt_dust", 4, "primamateria:brine", 600)),
                List.of(new RecipeLookup.Growing("grow", "primamateria:brine", 250, "primamateria:diamond_seed", "minecraft:diamond", 1,
                        6000, "primamateria:block/crystal_diamond")),
                List.of(new RecipeLookup.Melting("melt_iron", "primamateria:iron_dust", "primamateria:molten_iron", true, 20),
                        new RecipeLookup.Melting("melt_stone", "minecraft:stone", "minecraft:lava", false, 250)),
                List.of(new RecipeLookup.Casting("cast_iron", "primamateria:molten_iron", 100, "minecraft:iron_ingot", 60)),
                meshes,
                List.of(new RecipeLookup.Heat("h_campfire", "minecraft:campfire", "lit=true", 2)),
                List.of(new RecipeLookup.Melt("primamateria:molten_iron", 0xD8D8D8, 8, "melt.primamateria.molten_iron")),
                List.of(new RecipeLookup.Compost("cp_leaves", "#minecraft:leaves", 20, 0.0)));
    }
}
