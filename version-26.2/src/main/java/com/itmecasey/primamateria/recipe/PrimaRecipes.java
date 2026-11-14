package com.itmecasey.primamateria.recipe;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.crush.CrushMath;
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

    /** What the recipes say about crushing one block: the lowest hammer tier and the results. */
    public record Crush(int hammerTier, List<CrushMath.Result> results) {
    }

    private static final Map<String, Sifts> INDEX = new ConcurrentHashMap<>();
    private static final Map<String, java.util.Optional<Crush>> CRUSH_INDEX = new ConcurrentHashMap<>();

    private PrimaRecipes() {
    }

    public static void registerTypes(BiConsumer<String, RecipeType<?>> out) {
        out.accept("sifting", SiftingRecipe.TYPE);
        out.accept("sifting_residue", SiftingResidueRecipe.TYPE);
        out.accept("soaking", SoakingRecipe.TYPE);
        out.accept("mixing", MixingRecipe.TYPE);
        out.accept("steeping", SteepingRecipe.TYPE);
        out.accept("growing", GrowingRecipe.TYPE);
        out.accept("composting", CompostingRecipe.TYPE);
        out.accept("crushing", CrushingRecipe.TYPE);
        out.accept("melting", MeltingRecipe.TYPE);
        out.accept("casting", CastingRecipe.TYPE);
    }

    public static void registerSerializers(BiConsumer<String, RecipeSerializer<?>> out) {
        out.accept("sifting", SiftingRecipe.SERIALIZER);
        out.accept("sifting_residue", SiftingResidueRecipe.SERIALIZER);
        out.accept("soaking", SoakingRecipe.SERIALIZER);
        out.accept("mixing", MixingRecipe.SERIALIZER);
        out.accept("steeping", SteepingRecipe.SERIALIZER);
        out.accept("growing", GrowingRecipe.SERIALIZER);
        out.accept("composting", CompostingRecipe.SERIALIZER);
        out.accept("crushing", CrushingRecipe.SERIALIZER);
        out.accept("melting", MeltingRecipe.SERIALIZER);
        out.accept("casting", CastingRecipe.SERIALIZER);
    }

    /** Forgets the lookup built from the old recipes (called after every datapack reload). */
    public static void clearIndex() {
        INDEX.clear();
        BarrelRecipes.clear();
        CRUSH_INDEX.clear();
        CrucibleRecipes.clear();
    }

    /** Every sifting entry that applies to a stack of this item. Results naming a missing item are left out. */
    public static Sifts siftsFor(RecipeManager recipes, ItemStack stack) {
        if (stack.isEmpty()) {
            return new Sifts(List.of(), List.of());
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return INDEX.computeIfAbsent(key, k -> build(recipes, stack));
    }

    /**
     * The crushing recipe for a stack of this item, or null. When several recipes match, the one with the fewest input items wins
     * (a recipe for one block beats a recipe for a tag); results naming a missing item are left out.
     */
    public static Crush crushFor(RecipeManager recipes, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return CRUSH_INDEX.computeIfAbsent(key, k -> java.util.Optional.ofNullable(buildCrush(recipes, stack))).orElse(null);
    }

    private static Crush buildCrush(RecipeManager recipes, ItemStack stack) {
        CrushingRecipe best = null;
        long bestSize = Long.MAX_VALUE;
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            if (holder.value() instanceof CrushingRecipe r && r.input().test(stack)) {
                long size = r.input().items().count();
                if (size < bestSize) {
                    best = r;
                    bestSize = size;
                }
            }
        }
        if (best == null) {
            return null;
        }
        List<CrushMath.Result> results = new ArrayList<>();
        for (CrushingRecipe.Entry e : best.results()) {
            if (itemExists(e.item())) {
                results.add(e.toResult());
            }
        }
        return results.isEmpty() ? null : new Crush(best.hammerTier(), List.copyOf(results));
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
