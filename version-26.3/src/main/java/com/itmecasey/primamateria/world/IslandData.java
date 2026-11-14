package com.itmecasey.primamateria.world;

import com.itmecasey.primamateria.ModContent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Remembers in the world's save that the start island was placed, so it is never placed twice (or back after it is mined). */
public final class IslandData extends SavedData {
    public static final Codec<IslandData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("placed", false).forGetter(IslandData::placed))
            .apply(i, IslandData::new));

    public static final SavedDataType<IslandData> TYPE = new SavedDataType<>(ModContent.id("start_island"), IslandData::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private boolean placed;

    public IslandData() {
        this(false);
    }

    public IslandData(boolean placed) {
        this.placed = placed;
    }

    public boolean placed() {
        return placed;
    }

    public void markPlaced() {
        placed = true;
        setDirty();
    }
}
