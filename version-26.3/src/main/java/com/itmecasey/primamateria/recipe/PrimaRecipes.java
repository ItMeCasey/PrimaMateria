package com.itmecasey.primamateria.recipe;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.sift.DropEntry;
import com.itmecasey.primamateria.sift.ResidueEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/** Registration of the mod's recipe types and the lookup of sifting recipes by sifted item. */
public final class PrimaRecipes {
    /** What the recipes say about one sifted item. */
    public record Sifts(List<DropEntry> drops, List<ResidueEntry> residue) {
        public boolean siftable() {
            return !drops.isEmpty();
        }
    }

    private static final Map<String, Sifts> INDEX = new ConcurrentHashMap<>();

    private PrimaRecipes() {
    }

    public static void registerTypes(BiConsumer<String, RecipeType<?>> out) {
        out.accept("sifting", SiftingRecipe.TYPE);
        out.accept("sifting_residue", SiftingResidueRecipe.TYPE);
    }

    public static void registerSerializers(BiConsumer<String, RecipeSerializer<?>> out) {
        out.accept("sifting", SiftingRecipe.SERIALIZER);
        out.accept("sifting_residue", SiftingResidueRecipe.SERIALIZER);
    }

    /** Forgets the lookup built from the old recipes (called after every datapack reload). */
    public static void clearIndex() {
        INDEX.clear();
    }

    /** Every sifting entry that applies to a stack of this item. Results naming a missing item are left out. */
    public static Sifts siftsFor(RecipeManager recipes, ItemStack stack) {
        if (stack.isEmpty()) {
            return new Sifts(List.of(), List.of());
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return INDEX.computeIfAbsent(key, k -> build(recipes, stack));
    }

    private static Sifts build(RecipeManager recipes, ItemStack stack) {
        List<DropEntry> drops = new ArrayList<>();
        List<ResidueEntry> residue = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            Object value = holder.value();
            if (value instanceof SiftingRecipe r && r.input().test(stack) && itemExists(r.result())) {
                drops.add(r.toEntry());
            } else if (value instanceof SiftingResidueRecipe r && r.input().test(stack) && itemExists(r.result())) {
                residue.add(r.toEntry());
            }
        }
        return new Sifts(List.copyOf(drops), List.copyOf(residue));
    }

    private static boolean itemExists(Identifier id) {
        return BuiltInRegistries.ITEM.containsKey(id);
    }

    /** Used by registration code that needs the mod's namespace. */
    static Identifier id(String path) {
        return ModContent.id(path);
    }
}
