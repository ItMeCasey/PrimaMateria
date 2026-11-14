package com.itmecasey.primamateria.recipe;

import com.itmecasey.primamateria.crucible.CrucibleState;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/** Lookup of melting and casting recipes, cached until the next datapack reload (see {@link PrimaRecipes#clearIndex}). */
public final class CrucibleRecipes {
    /** What a fluid casts into: the result item id, the fluid amount in mB and the cooling time in ticks. */
    public record Cast(String result, int amount, int time) {
    }

    private static final Map<String, Optional<CrucibleState.Melt>> MELTS = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Cast>> CASTS = new ConcurrentHashMap<>();

    private CrucibleRecipes() {
    }

    public static void clear() {
        MELTS.clear();
        CASTS.clear();
    }

    /** What melting one of this item gives, or null when no melting recipe matches. */
    public static CrucibleState.Melt meltFor(RecipeManager recipes, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return MELTS.computeIfAbsent(key, k -> buildMelt(recipes, stack)).orElse(null);
    }

    private static Optional<CrucibleState.Melt> buildMelt(RecipeManager recipes, ItemStack stack) {
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (holder.value() instanceof MeltingRecipe r && r.input().test(stack)) {
                return Optional.of(new CrucibleState.Melt(r.result().toString(), r.amount()));
            }
        }
        return Optional.empty();
    }

    /** What a casting tray makes of this fluid, or null when no casting recipe matches (or its result item does not exist). */
    public static Cast castFor(RecipeManager recipes, String fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return null;
        }
        return CASTS.computeIfAbsent(fluid, k -> buildCast(recipes, k)).orElse(null);
    }

    private static Optional<Cast> buildCast(RecipeManager recipes, String fluid) {
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (holder.value() instanceof CastingRecipe r && r.fluid().toString().equals(fluid) && BuiltInRegistries.ITEM.containsKey(r.result())) {
                return Optional.of(new Cast(r.result().toString(), r.amount(), r.time()));
            }
        }
        return Optional.empty();
    }
}
