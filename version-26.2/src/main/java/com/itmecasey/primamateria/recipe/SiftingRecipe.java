package com.itmecasey.primamateria.recipe;

import com.itmecasey.primamateria.sift.DropEntry;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * {@code primamateria:sifting}: one possible drop of one sifted block.
 *
 * <pre>
 * { "type": "primamateria:sifting", "input": "minecraft:gravel", "result": "primamateria:iron_dust",
 *   "count": [1, 2], "chance": 0.16, "mesh_min": 3, "mesh_max": 0 }
 * </pre>
 *
 * {@code count} is a number or {@code [min, max]} (default 1); {@code mesh_min} defaults to 1 and {@code mesh_max} to 0 (no limit).
 * The result is an item id kept as text, so a recipe for another mod's item loads whether or not that mod is installed and is
 * skipped when the item is missing.
 */
public record SiftingRecipe(Ingredient input, Identifier result, int min, int max, double chance, int meshMin, int meshMax)
        implements Recipe<NoInput> {

    /** {@code 3} or {@code [1, 2]} as {min, max}. */
    static final Codec<int[]> COUNT = Codec.either(Codec.INT, Codec.INT.listOf()).flatXmap(
            e -> e.map(n -> DataResult.success(new int[] {n, n}), list -> list.size() == 2 && list.get(0) <= list.get(1)
                    ? DataResult.success(new int[] {list.get(0), list.get(1)}) : DataResult.<int[]>error(() -> "count needs [min, max] with min <= max")),
            a -> DataResult.success(a[0] == a[1] ? Either.<Integer, List<Integer>>left(a[0]) : Either.<Integer, List<Integer>>right(List.of(a[0], a[1]))));

    public static final MapCodec<SiftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("input").forGetter(SiftingRecipe::input),
            ItemRef.CODEC.fieldOf("result").forGetter(SiftingRecipe::result),
            COUNT.optionalFieldOf("count", new int[] {1, 1}).forGetter(r -> new int[] {r.min, r.max}),
            Codec.doubleRange(0.0, 1.0).fieldOf("chance").forGetter(SiftingRecipe::chance),
            Codec.intRange(1, 100).optionalFieldOf("mesh_min", 1).forGetter(SiftingRecipe::meshMin),
            Codec.intRange(0, 100).optionalFieldOf("mesh_max", 0).forGetter(SiftingRecipe::meshMax))
            .apply(i, (input, result, count, chance, meshMin, meshMax) -> new SiftingRecipe(input, result, count[0], count[1], chance, meshMin, meshMax)));

    public static final StreamCodec<RegistryFriendlyByteBuf, SiftingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.input);
                buf.writeUtf(r.result.toString());
                buf.writeVarInt(r.min);
                buf.writeVarInt(r.max);
                buf.writeDouble(r.chance);
                buf.writeVarInt(r.meshMin);
                buf.writeVarInt(r.meshMax);
            },
            buf -> new SiftingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), Identifier.parse(buf.readUtf()), buf.readVarInt(),
                    buf.readVarInt(), buf.readDouble(), buf.readVarInt(), buf.readVarInt()));

    public static final RecipeSerializer<SiftingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<SiftingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:sifting";
        }
    };

    private String resolved() {
        String item = ItemRef.resolveText(result);
        return item != null ? item : result.toString();
    }

    public DropEntry toEntry() {
        return new DropEntry(resolved(), min, max, chance, meshMin, meshMax);
    }

    @Override
    public boolean matches(NoInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(NoInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<NoInput>> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<? extends Recipe<NoInput>> getType() {
        return TYPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
