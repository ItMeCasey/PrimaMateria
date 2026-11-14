package com.itmecasey.primamateria.forge;

import com.itmecasey.primamateria.data.LookupSync;
import com.itmecasey.primamateria.data.LookupWire;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** One frame of the recipe lookup, server to client (see {@link LookupWire}). */
public record LookupMessage(LookupWire.Frame frame) {
    static final StreamCodec<FriendlyByteBuf, LookupMessage> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, m -> m.frame().sync(),
            ByteBufCodecs.VAR_INT, m -> m.frame().index(),
            ByteBufCodecs.VAR_INT, m -> m.frame().total(),
            ByteBufCodecs.byteArray(1 << 20), m -> m.frame().data(),
            (sync, index, total, data) -> new LookupMessage(new LookupWire.Frame(sync, index, total, data)));

    static void handle(LookupMessage message, CustomPayloadEvent.Context context) {
        // a sender means the message came from a client: only the server may send the lookup
        if (context.getSender() == null) {
            LookupSync.receive(message.frame());
        }
    }
}
