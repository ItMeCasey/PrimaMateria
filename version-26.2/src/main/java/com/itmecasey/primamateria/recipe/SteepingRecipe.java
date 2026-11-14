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
 * {@code primamateria:steeping}: items dissolved in a barrel of fluid change the fluid over time. Put in {@code count} of the
 * input; then the timer runs and the whole fluid becomes {@code result_fluid}.
 *
 * <pre>
 * { "type": "primamateria:steeping", "fluid": "minecraft:water", "amount": 1000, "input": "primamateria:salt_dust",
 *   "count": 4, "result_fluid": "primamateria:brine", "time": 600 }
 * </pre>
 *
 * {@code amount} is the least fluid the barrel must hold (default 1000).
 */
public record SteepingRecipe(Identifier fluid, int amount, Ingredient input, int count, Identifier resultFluid, int time)
        implements PrimaRecipe {
    public static final MapCodec<SteepingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("fluid").forGetter(SteepingRecipe::fluid),
            Codec.intRange(1, 1000).optionalFieldOf("amount", 1000).forGetter(SteepingRecipe::amount),
            Ingredient.CODEC.fieldOf("input").forGetter(SteepingRecipe::input),
            Codec.intRange(1, 64).fieldOf("count").forGetter(SteepingRecipe::count),
            Identifier.CODEC.fieldOf("result_fluid").forGetter(SteepingRecipe::resultFluid),
            Codec.intRange(1, 1_000_000).fieldOf("time").forGetter(SteepingRecipe::time))
            .apply(i, SteepingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SteepingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.fluid.toString());
                buf.writeVarInt(r.amount);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.input);
                buf.writeVarInt(r.count);
                buf.writeUtf(r.resultFluid.toString());
                buf.writeVarInt(r.time);
            },
            buf -> new SteepingRecipe(Identifier.parse(buf.readUtf()), buf.readVarInt(), Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    buf.readVarInt(), Identifier.parse(buf.readUtf()), buf.readVarInt()));

    public static final RecipeSerializer<SteepingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<SteepingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:steeping";
        }
    };

    public BarrelState.SteepRule rule(String id) {
        return new BarrelState.SteepRule(id, fluid.toString(), amount, count, resultFluid.toString(), time);
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
