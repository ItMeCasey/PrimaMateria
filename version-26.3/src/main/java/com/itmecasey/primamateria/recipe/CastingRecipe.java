package com.itmecasey.primamateria.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * {@code primamateria:casting}: a casting tray turns an amount of fluid into an item after it has cooled.
 *
 * <pre>
 * { "type": "primamateria:casting", "fluid": "primamateria:molten_iron", "amount": 100, "result": "minecraft:iron_ingot", "time": 60 }
 * </pre>
 *
 * {@code amount} is in mB (1 to 4000) and {@code time} in ticks (at least 1). The result is an item id kept as text, so a recipe
 * for another mod's item loads whether or not that mod is installed and is skipped when the item is missing.
 */
public record CastingRecipe(Identifier fluid, int amount, Identifier result, int time) implements Recipe<NoInput> {
    public static final MapCodec<CastingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("fluid").forGetter(CastingRecipe::fluid),
            Codec.intRange(1, 4000).fieldOf("amount").forGetter(CastingRecipe::amount),
            Identifier.CODEC.fieldOf("result").forGetter(CastingRecipe::result),
            Codec.intRange(1, 72000).fieldOf("time").forGetter(CastingRecipe::time))
            .apply(i, CastingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.fluid.toString());
                buf.writeVarInt(r.amount);
                buf.writeUtf(r.result.toString());
                buf.writeVarInt(r.time);
            },
            buf -> new CastingRecipe(Identifier.parse(buf.readUtf()), buf.readVarInt(), Identifier.parse(buf.readUtf()), buf.readVarInt()));

    public static final RecipeSerializer<CastingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<CastingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:casting";
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
