package com.itmecasey.primamateria.neoforge;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.data.LookupSync;
import com.itmecasey.primamateria.data.LookupWire;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One frame of the recipe lookup, server to client (see {@link LookupWire}). */
public record LookupPayload(LookupWire.Frame frame) implements CustomPacketPayload {
    public static final Type<LookupPayload> TYPE = new Type<>(ModContent.id("lookup"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LookupPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.frame().sync(),
            ByteBufCodecs.VAR_INT, p -> p.frame().index(),
            ByteBufCodecs.VAR_INT, p -> p.frame().total(),
            ByteBufCodecs.byteArray(1 << 20), p -> p.frame().data(),
            (sync, index, total, data) -> new LookupPayload(new LookupWire.Frame(sync, index, total, data)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the network thread: the lookup is a plain snapshot, swapped in whole, so no game thread is needed. */
    public static void handle(LookupPayload payload, IPayloadContext context) {
        LookupSync.receive(payload.frame());
    }
}
