package com.itmecasey.primamateria.recipe;

import com.itmecasey.primamateria.sift.ResidueEntry;
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
 * {@code primamateria:sifting_residue}: the finer material a sifted block can leave behind (gravel leaves sand).
 *
 * <pre>
 * { "type": "primamateria:sifting_residue", "input": "minecraft:gravel", "result": "minecraft:sand", "chance": 0.15 }
 * </pre>
 */
public record SiftingResidueRecipe(Ingredient input, Identifier result, double chance) implements Recipe<NoInput> {
    public static final MapCodec<SiftingResidueRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("input").forGetter(SiftingResidueRecipe::input),
            Identifier.CODEC.fieldOf("result").forGetter(SiftingResidueRecipe::result),
            Codec.doubleRange(0.0, 1.0).fieldOf("chance").forGetter(SiftingResidueRecipe::chance))
            .apply(i, SiftingResidueRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SiftingResidueRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.input);
                buf.writeUtf(r.result.toString());
                buf.writeDouble(r.chance);
            },
            buf -> new SiftingResidueRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), Identifier.parse(buf.readUtf()), buf.readDouble()));

    public static final RecipeSerializer<SiftingResidueRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<SiftingResidueRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:sifting_residue";
        }
    };

    public ResidueEntry toEntry() {
        return new ResidueEntry(result.toString(), chance);
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
