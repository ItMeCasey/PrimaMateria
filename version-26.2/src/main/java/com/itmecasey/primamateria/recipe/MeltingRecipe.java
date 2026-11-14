package com.itmecasey.primamateria.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * {@code primamateria:melting}: a solid item that melts in a crucible into some fluid.
 *
 * <pre>
 * { "type": "primamateria:melting", "input": "primamateria:iron_dust", "result": "primamateria:molten_iron", "amount": 20 }
 * </pre>
 *
 * {@code result} is a fluid id (lava, a molten metal, or any fluid id a pack names) and {@code amount} is in mB (1 to 4000).
 */
public record MeltingRecipe(Ingredient input, Identifier result, int amount) implements Recipe<NoInput> {
    public static final MapCodec<MeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("input").forGetter(MeltingRecipe::input),
            Identifier.CODEC.fieldOf("result").forGetter(MeltingRecipe::result),
            Codec.intRange(1, 4000).fieldOf("amount").forGetter(MeltingRecipe::amount))
            .apply(i, MeltingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.input);
                buf.writeUtf(r.result.toString());
                buf.writeVarInt(r.amount);
            },
            buf -> new MeltingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), Identifier.parse(buf.readUtf()), buf.readVarInt()));

    public static final RecipeSerializer<MeltingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<MeltingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:melting";
        }
    };

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
