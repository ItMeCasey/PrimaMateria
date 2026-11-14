package com.itmecasey.primamateria.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/** Lookup of the barrel's recipes by item, cached until the next datapack reload (see {@link PrimaRecipes#clearIndex}). */
public final class BarrelRecipes {
    /** A recipe with its id (the recipe file's id). */
    public record Held<T>(String id, T recipe) {
    }

    /** The barrel recipes an item can take part in. */
    public record ForItem(List<Held<SoakingRecipe>> soaking, List<Held<SteepingRecipe>> steeping, List<Held<GrowingRecipe>> growing) {
        public boolean any() {
            return !soaking.isEmpty() || !steeping.isEmpty() || !growing.isEmpty();
        }
    }

    private static final Map<String, ForItem> BY_ITEM = new ConcurrentHashMap<>();
    private static volatile List<Held<MixingRecipe>> mixing;
    private static volatile Held<CompostingRecipe> composting;
    private static volatile boolean compostingLoaded;

    private BarrelRecipes() {
    }

    public static void clear() {
        BY_ITEM.clear();
        mixing = null;
        composting = null;
        compostingLoaded = false;
    }

    private static String idOf(RecipeHolder<?> holder) {
        return holder.id().identifier().toString();
    }

    private static boolean itemExists(Identifier id) {
        return BuiltInRegistries.ITEM.containsKey(id);
    }

    /** Soaking, steeping and growing recipes whose ingredient matches the item. Recipes with a missing result item are left out. */
    public static ForItem forItem(RecipeManager recipes, ItemStack stack) {
        if (stack.isEmpty()) {
            return new ForItem(List.of(), List.of(), List.of());
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return BY_ITEM.computeIfAbsent(key, k -> {
            List<Held<SoakingRecipe>> soaking = new ArrayList<>();
            List<Held<SteepingRecipe>> steeping = new ArrayList<>();
            List<Held<GrowingRecipe>> growing = new ArrayList<>();
            for (RecipeHolder<?> holder : recipes.getRecipes()) {
                Object value = holder.value();
                if (value instanceof SoakingRecipe r && r.input().test(stack) && itemExists(r.result())) {
                    soaking.add(new Held<>(idOf(holder), r));
                } else if (value instanceof SteepingRecipe r && r.input().test(stack)) {
                    steeping.add(new Held<>(idOf(holder), r));
                } else if (value instanceof GrowingRecipe r && r.seed().test(stack) && itemExists(r.result())) {
                    growing.add(new Held<>(idOf(holder), r));
                }
            }
            return new ForItem(List.copyOf(soaking), List.copyOf(steeping), List.copyOf(growing));
        });
    }

    public static List<Held<MixingRecipe>> mixing(RecipeManager recipes) {
        List<Held<MixingRecipe>> list = mixing;
        if (list == null) {
            list = new ArrayList<>();
            for (RecipeHolder<?> holder : recipes.getRecipes()) {
                if (holder.value() instanceof MixingRecipe r && itemExists(r.result())) {
                    list.add(new Held<>(idOf(holder), r));
                }
            }
            mixing = list = List.copyOf(list);
        }
        return list;
    }

    /** The composting recipe in use (the first one found), or null when there is none. */
    public static Held<CompostingRecipe> composting(RecipeManager recipes) {
        if (!compostingLoaded) {
            Held<CompostingRecipe> found = null;
            for (RecipeHolder<?> holder : recipes.getRecipes()) {
                if (holder.value() instanceof CompostingRecipe r && itemExists(r.result())
                        && (found == null || idOf(holder).compareTo(found.id()) < 0)) {
                    found = new Held<>(idOf(holder), r);
                }
            }
            composting = found;
            compostingLoaded = true;
        }
        return composting;
    }
}
