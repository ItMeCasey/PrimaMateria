package com.itmecasey.primamateria.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.Level;

/**
 * Base of the mod's machine recipes (barrel, crucible...): they are real recipe types so datapacks, KubeJS and ModJS handle
 * them like any recipe, but they are never matched against a crafting grid and never shown in the recipe book.
 */
public interface PrimaRecipe extends Recipe<NoInput> {
    @Override
    default boolean matches(NoInput input, Level level) {
        return false;
    }

    @Override
    default ItemStack assemble(NoInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default String group() {
        return "";
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
