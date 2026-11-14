package com.itmecasey.primamateria.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Our recipes are looked up by their own machines, never matched against a crafting grid, so their input has no slots. */
public record NoInput() implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }
}
