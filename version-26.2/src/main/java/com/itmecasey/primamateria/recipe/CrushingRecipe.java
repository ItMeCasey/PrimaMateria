package com.itmecasey.primamateria.recipe;

import com.itmecasey.primamateria.crush.CrushMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
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
 * {@code primamateria:crushing}: what a block breaks into under a hammer (and in an auto hammer).
 *
 * <pre>
 * { "type": "primamateria:crushing", "input": "minecraft:cobblestone", "hammer_tier": 1,
 *   "results": [ { "item": "minecraft:gravel" }, { "item": "minecraft:sand", "count": [1, 2], "chance": 0.5 } ] }
 * </pre>
 *
 * {@code hammer_tier} is the lowest hammer tier that can crush the block (1 wood to 6 netherite, default 1). Each result has an
 * item id (kept as text, so another mod's item is skipped when missing), {@code count} (a number or {@code [min, max]},
 * default 1) and {@code chance} (default 1). When several recipes match a block the one with the fewest input items wins, so a
 * recipe for one block beats a recipe for a tag.
 */
public record CrushingRecipe(Ingredient input, int hammerTier, List<Entry> results) implements Recipe<NoInput> {

    /** One result of the recipe. */
    public record Entry(Identifier item, int min, int max, double chance) {
        static final MapCodec<Entry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ItemRef.CODEC.fieldOf("item").forGetter(Entry::item),
                SiftingRecipe.COUNT.optionalFieldOf("count", new int[] {1, 1}).forGetter(e -> new int[] {e.min, e.max}),
                Codec.doubleRange(0.0, 1.0).optionalFieldOf("chance", 1.0).forGetter(Entry::chance))
                .apply(i, (item, count, chance) -> new Entry(item, count[0], count[1], chance)));

        CrushMath.Result toResult() {
            String resolved = ItemRef.resolveText(item);
            return new CrushMath.Result(resolved != null ? resolved : item.toString(), min, max, chance);
        }
    }

    public static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("input").forGetter(CrushingRecipe::input),
            Codec.intRange(1, 6).optionalFieldOf("hammer_tier", 1).forGetter(CrushingRecipe::hammerTier),
            Entry.CODEC.codec().listOf(1, 16).fieldOf("results").forGetter(CrushingRecipe::results))
            .apply(i, CrushingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.input);
                buf.writeVarInt(r.hammerTier);
                buf.writeVarInt(r.results.size());
                for (Entry e : r.results) {
                    buf.writeUtf(e.item.toString());
                    buf.writeVarInt(e.min);
                    buf.writeVarInt(e.max);
                    buf.writeDouble(e.chance);
                }
            },
            buf -> {
                Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                int tier = buf.readVarInt();
                int n = buf.readVarInt();
                List<Entry> list = new ArrayList<>();
                for (int k = 0; k < n; k++) {
                    list.add(new Entry(Identifier.parse(buf.readUtf()), buf.readVarInt(), buf.readVarInt(), buf.readDouble()));
                }
                return new CrushingRecipe(input, tier, list);
            });

    public static final RecipeSerializer<CrushingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<CrushingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:crushing";
        }
    };

    public CrushingRecipe {
        results = List.copyOf(results);
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
