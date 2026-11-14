package com.itmecasey.primamateria.data;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the server's {@link RecipeLookup} to players who join a dedicated server, so the recipe viewers, SMWTI and Jade have the
 * recipes there as well. In single player and on a LAN host the client shares the server's JVM and so the same lookup, and is
 * skipped. Loader code only supplies the transport: a frame sender on the server and {@link #receive} on the client.
 */
public final class LookupSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");
    private static final AtomicInteger SYNC_IDS = new AtomicInteger();
    private static final LookupWire.Receiver RECEIVER = new LookupWire.Receiver();
    /** True while the lookup in memory is a copy that came from a remote server. */
    private static volatile boolean fromServer;

    private LookupSync() {
    }

    /** Server side: whether this player's client runs in another JVM and so needs the lookup sent. */
    public static boolean needsSync(ServerPlayer player) {
        return !player.connection.getConnection().isMemoryConnection();
    }

    /** Server side: cuts the current lookup into frames and hands each to {@code send}. Returns the number of frames. */
    public static int send(ServerPlayer player, Consumer<LookupWire.Frame> send) {
        if (!needsSync(player)) {
            return 0;
        }
        RecipeLookup.Snapshot snapshot = RecipeLookup.get();
        List<LookupWire.Frame> frames = LookupWire.frames(SYNC_IDS.incrementAndGet(), snapshot);
        long bytes = 0;
        for (LookupWire.Frame frame : frames) {
            bytes += frame.data().length;
            send.accept(frame);
        }
        LOGGER.info("Recipe lookup sent to {}: {} sifting recipes, {} frame(s), {} bytes", player.getGameProfile().name(),
                snapshot.sifting().size(), frames.size(), bytes);
        return frames.size();
    }

    /** Client side: takes one frame; when the last one of a send is in, the lookup replaces the client's. */
    public static void receive(LookupWire.Frame frame) {
        try {
            RecipeLookup.Snapshot snapshot = RECEIVER.accept(frame);
            if (snapshot != null) {
                RecipeLookup.set(snapshot);
                fromServer = true;
                LOGGER.info("Recipe lookup from the server payload: {} sifting, {} crushing, {} barrel, {} crucible recipes, {} meshes",
                        snapshot.sifting().size(), snapshot.crushing().size(),
                        snapshot.soaking().size() + snapshot.mixing().size() + snapshot.steeping().size() + snapshot.growing().size()
                                + snapshot.composting().size(),
                        snapshot.melting().size() + snapshot.casting().size(), snapshot.meshes().size());
            }
        } catch (IOException e) {
            LOGGER.warn("The recipe lookup from the server could not be read: {}", e.getMessage());
        }
    }

    /** Client side, on leaving a server: the copy that came from it is dropped, so it never shows in the next world. */
    public static void onDisconnect() {
        RECEIVER.reset();
        if (fromServer) {
            fromServer = false;
            RecipeLookup.set(RecipeLookup.Snapshot.EMPTY);
        }
    }
}
