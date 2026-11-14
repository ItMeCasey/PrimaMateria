package com.itmecasey.primamateria.recipe;

import com.itmecasey.primamateria.barrel.BarrelState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * {@code primamateria:soaking}: an item put into a barrel of fluid turns into something else and uses some of the fluid.
 *
 * <pre>
 * { "type": "primamateria:soaking", "fluid": "minecraft:water", "amount": 250,
 *   "input": "primamateria:stone_meal", "result": "minecraft:clay", "time": 100 }
 * </pre>
 *
 * {@code time} is in ticks. Fluids and the result are ids kept as text, so a recipe naming another mod's fluid or item loads
 * whether or not that mod is installed.
 */
public record SoakingRecipe(Identifier fluid, int amount, Ingredient input, Identifier result, int time) implements PrimaRecipe {
    public static final MapCodec<SoakingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("fluid").forGetter(SoakingRecipe::fluid),
            Codec.intRange(1, 1000).fieldOf("amount").forGetter(SoakingRecipe::amount),
            Ingredient.CODEC.fieldOf("input").forGetter(SoakingRecipe::input),
            Identifier.CODEC.fieldOf("result").forGetter(SoakingRecipe::result),
            Codec.intRange(1, 1_000_000).fieldOf("time").forGetter(SoakingRecipe::time))
            .apply(i, SoakingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoakingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.fluid.toString());
                buf.writeVarInt(r.amount);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.input);
                buf.writeUtf(r.result.toString());
                buf.writeVarInt(r.time);
            },
            buf -> new SoakingRecipe(Identifier.parse(buf.readUtf()), buf.readVarInt(), Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    Identifier.parse(buf.readUtf()), buf.readVarInt()));

    public static final RecipeSerializer<SoakingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<SoakingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:soaking";
        }
    };

    public BarrelState.SoakRule rule(String id) {
        return new BarrelState.SoakRule(id, fluid.toString(), amount, result.toString(), time);
    }

    @Override
    public RecipeSerializer<? extends Recipe<NoInput>> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<? extends Recipe<NoInput>> getType() {
        return TYPE;
    }
}
