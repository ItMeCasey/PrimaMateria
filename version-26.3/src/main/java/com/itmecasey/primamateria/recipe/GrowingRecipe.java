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
 * {@code primamateria:growing}: a seed put into a barrel of the right fluid grows slowly into a result and uses some fluid. Up to
 * four seeds grow at once, each on its own timer, each drawn as a crystal that gets bigger.
 *
 * <pre>
 * { "type": "primamateria:growing", "fluid": "primamateria:brine", "amount": 250, "seed": "primamateria:quartz_seed",
 *   "result": "minecraft:quartz", "count": 2, "time": 1800, "crystal": "primamateria:block/crystal_quartz" }
 * </pre>
 */
public record GrowingRecipe(Identifier fluid, int amount, Ingredient seed, Identifier result, int count, int time, String crystal)
        implements PrimaRecipe {
    public static final MapCodec<GrowingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("fluid").forGetter(GrowingRecipe::fluid),
            Codec.intRange(1, 1000).fieldOf("amount").forGetter(GrowingRecipe::amount),
            Ingredient.CODEC.fieldOf("seed").forGetter(GrowingRecipe::seed),
            Identifier.CODEC.fieldOf("result").forGetter(GrowingRecipe::result),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(GrowingRecipe::count),
            Codec.intRange(1, 10_000_000).fieldOf("time").forGetter(GrowingRecipe::time),
            Codec.STRING.optionalFieldOf("crystal", "").forGetter(GrowingRecipe::crystal))
            .apply(i, GrowingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GrowingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.fluid.toString());
                buf.writeVarInt(r.amount);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.seed);
                buf.writeUtf(r.result.toString());
                buf.writeVarInt(r.count);
                buf.writeVarInt(r.time);
                buf.writeUtf(r.crystal);
            },
            buf -> new GrowingRecipe(Identifier.parse(buf.readUtf()), buf.readVarInt(), Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    Identifier.parse(buf.readUtf()), buf.readVarInt(), buf.readVarInt(), buf.readUtf()));

    public static final RecipeSerializer<GrowingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<GrowingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:growing";
        }
    };

    public BarrelState.GrowRule rule(String id) {
        return new BarrelState.GrowRule(id, fluid.toString(), amount, result.toString(), count, time, crystal);
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
