package com.itmecasey.primamateria.recipe;

import com.itmecasey.primamateria.barrel.BarrelState;
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
 * {@code primamateria:mixing}: pouring a bucket of one fluid on top of another in a barrel makes a block. Both fluids are used
 * up ({@code amount} mB of each).
 *
 * <pre>
 * { "type": "primamateria:mixing", "base_fluid": "minecraft:lava", "added_fluid": "minecraft:water",
 *   "amount": 1000, "result": "minecraft:obsidian", "needs_heatproof": true }
 * </pre>
 */
public record MixingRecipe(Identifier baseFluid, Identifier addedFluid, int amount, Identifier result, boolean needsHeatproof)
        implements PrimaRecipe {
    public static final MapCodec<MixingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("base_fluid").forGetter(MixingRecipe::baseFluid),
            Identifier.CODEC.fieldOf("added_fluid").forGetter(MixingRecipe::addedFluid),
            Codec.intRange(1, 1000).fieldOf("amount").forGetter(MixingRecipe::amount),
            Identifier.CODEC.fieldOf("result").forGetter(MixingRecipe::result),
            Codec.BOOL.optionalFieldOf("needs_heatproof", false).forGetter(MixingRecipe::needsHeatproof))
            .apply(i, MixingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MixingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.baseFluid.toString());
                buf.writeUtf(r.addedFluid.toString());
                buf.writeVarInt(r.amount);
                buf.writeUtf(r.result.toString());
                buf.writeBoolean(r.needsHeatproof);
            },
            buf -> new MixingRecipe(Identifier.parse(buf.readUtf()), Identifier.parse(buf.readUtf()), buf.readVarInt(),
                    Identifier.parse(buf.readUtf()), buf.readBoolean()));

    public static final RecipeSerializer<MixingRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    public static final RecipeType<MixingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "primamateria:mixing";
        }
    };

    public BarrelState.MixRule rule(String id) {
        return new BarrelState.MixRule(id, baseFluid.toString(), addedFluid.toString(), amount, result.toString(), needsHeatproof);
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
