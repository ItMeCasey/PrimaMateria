package com.itmecasey.primamateria.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * {@code primamateria:composting}: what a full barrel of compost becomes after {@code time} ticks. The compost values of items
 * are a separate data folder ({@code data/<namespace>/primamateria/compost/}). The first composting recipe found is used.
 *
 * <pre>
 * { "type": "primamateria:composting", "result": "minecraft:dirt", "time": 400 }
 * </pre>
 */
public record CompostingRecipe(Identifier result, int time) implements PrimaRecipe {
    public static final MapCodec<CompostingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("result").forGetter(CompostingRecipe::result),
            Codec.intRange(1, 10_000_000).fieldOf("time").forGetter(CompostingRecipe::time))
            .apply(i, CompostingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompostingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.result.toString());
                buf.writeVarInt(r.time);
            },
            buf -> new CompostingRecipe(Identifier.parse(buf.readUtf()), buf.readVarInt()));

    public static final RecipeSerializer<CompostingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<CompostingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:composting";
        }
    };

    @Override
    public RecipeSerializer<? extends Recipe<NoInput>> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<? extends Recipe<NoInput>> getType() {
        return TYPE;
    }
}
