package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.crucible.CrucibleState;
import com.itmecasey.primamateria.fluid.Metals;
import com.itmecasey.primamateria.fluid.ModFluids;
import com.itmecasey.primamateria.recipe.CrucibleRecipes;
import com.itmecasey.primamateria.recipe.ItemRef;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Server checks of the ore compat: recipes that name other mods' materials by convention tag load and do nothing while no mod
 * fills the tag, resolve to a real item once something does (a vanilla tag stands in here), and every metal has its fluid.
 */
final class CompatSelfTest {
    private CompatSelfTest() {
    }

    static void run(ServerLevel level) {
        var recipes = level.recipeAccess();
        // a tag with items resolves to the lowest id, minecraft first; a tag nobody fills resolves to nothing
        Identifier logs = ItemRef.resolve(ItemRef.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, new com.google.gson.JsonPrimitive("#minecraft:logs")).result().orElseThrow());
        SelfTest.verdict("tag_result_resolves_to_an_item_of_the_tag", logs != null && logs.getNamespace().equals("minecraft") && logs.getPath().endsWith("log"), String.valueOf(logs));
        Identifier none = ItemRef.resolve(ItemRef.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, new com.google.gson.JsonPrimitive("#c:ingots/not_a_metal")).result().orElseThrow());
        SelfTest.verdict("tag_result_nobody_fills_is_nothing", none == null, String.valueOf(none));
        SelfTest.verdict("tag_result_text_round_trips", ItemRef.text(ItemRef.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                new com.google.gson.JsonPrimitive("#c:ingots/tin")).result().orElseThrow()).equals("#c:ingots/tin"), "#c:ingots/tin");

        // vanilla materials through the convention tags: iron ore crushes to raw iron, an iron nugget melts, iron casts to the vanilla ingot
        PrimaRecipes.Crush ore = PrimaRecipes.crushFor(recipes, new ItemStack(Items.IRON_ORE));
        SelfTest.verdict("compat_crushes_a_vanilla_ore_by_tag", ore != null && ore.results().size() == 1
                && ore.results().get(0).item().equals("minecraft:raw_iron"), String.valueOf(ore));
        CrucibleState.Melt nugget = CrucibleRecipes.meltFor(recipes, new ItemStack(Items.IRON_NUGGET));
        SelfTest.verdict("compat_melts_a_nugget_by_tag", nugget != null && nugget.fluid().equals("primamateria:molten_iron") && nugget.amount() == 11,
                String.valueOf(nugget));
        CrucibleRecipes.Cast iron = CrucibleRecipes.castFor(recipes, "primamateria:molten_iron");
        SelfTest.verdict("compat_casts_iron_to_the_vanilla_ingot", iron != null && iron.result().equals("minecraft:iron_ingot"), String.valueOf(iron));
        CrucibleState.Melt netherite = CrucibleRecipes.meltFor(recipes, new ItemStack(Items.NETHERITE_INGOT));
        SelfTest.verdict("compat_melts_netherite", netherite != null && netherite.fluid().equals("primamateria:molten_netherite"), String.valueOf(netherite));

        // without the other mods nothing of theirs happens: no tin ingot exists, so the tin fluid casts nothing and tin drops are not offered
        SelfTest.verdict("compat_without_the_mod_casts_nothing", CrucibleRecipes.castFor(recipes, "primamateria:molten_tin") == null
                || BuiltInRegistries.ITEM.containsKey(Identifier.parse(CrucibleRecipes.castFor(recipes, "primamateria:molten_tin").result())), "tin");
        PrimaRecipes.Sifts gravel = PrimaRecipes.siftsFor(recipes, new ItemStack(Items.GRAVEL));
        boolean broken = gravel.drops().stream().anyMatch(d -> d.item().startsWith("primamateria:tag/") || d.item().startsWith("#"));
        SelfTest.verdict("compat_sifting_offers_only_real_items", !broken && !gravel.drops().isEmpty(), gravel.drops().size() + " gravel drops");

        // every metal has a real fluid, a flowing form, a liquid block and a bucket
        int missing = 0;
        StringBuilder names = new StringBuilder();
        for (Metals.Metal metal : Metals.ALL) {
            String id = metal.fluid();
            if (ModFluids.source(id) == null || ModFluids.flowing(id) == null || ModFluids.block(id) == null
                    || !BuiltInRegistries.ITEM.containsKey(Identifier.parse("primamateria:" + id + "_bucket"))) {
                missing++;
                names.append(id).append(' ');
            }
        }
        SelfTest.verdict("every_metal_has_a_fluid_block_and_bucket", missing == 0 && Metals.ALL.size() >= 40 && Ids.FLUIDS.size() == Metals.ALL.size() + 1,
                Metals.ALL.size() + " metals, missing: " + names);
    }
}
