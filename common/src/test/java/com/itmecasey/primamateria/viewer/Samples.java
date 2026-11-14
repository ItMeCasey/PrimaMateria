package com.itmecasey.primamateria.viewer;

import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.data.RecipeLookup.Snapshot;
import java.util.List;

/** A small world of recipes for the tests of the viewers and scripts. */
public final class Samples {
    private Samples() {
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
                List.of(new RecipeLookup.Melting("melt_iron", "primamateria:iron_dust", "primamateria:molten_iron", 20),
                        new RecipeLookup.Melting("melt_stone", "minecraft:stone", "minecraft:lava", 250)),
                List.of(new RecipeLookup.Casting("cast_iron", "primamateria:molten_iron", 100, "minecraft:iron_ingot", 60)),
                meshes,
                List.of(new RecipeLookup.Heat("h_campfire", "minecraft:campfire", "lit=true", 2)),
                List.of(new RecipeLookup.Compost("cp_leaves", "#minecraft:leaves", 20, 0.0)));
    }
}
