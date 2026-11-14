package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.data.LookupWire;
import com.itmecasey.primamateria.data.RecipeLookup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server checks of the recipe lookup sync: the real, freshly built lookup survives the wire, in one frame and in many. */
final class LookupSyncSelfTest {
    private LookupSyncSelfTest() {
    }

    static void run(MinecraftServer server) {
        RecipeLookup.Snapshot real = RecipeLookup.get();
        try {
            List<LookupWire.Frame> frames = LookupWire.frames(1, real);
            LookupWire.Receiver receiver = new LookupWire.Receiver();
            RecipeLookup.Snapshot got = null;
            for (LookupWire.Frame frame : frames) {
                got = receiver.accept(frame);
            }
            SelfTest.verdict("lookup_sync_real_lookup_survives_the_wire", real.equals(got),
                    frames.size() + " frame(s), " + real.sifting().size() + " sifting recipes");

            // the same bytes cut into small frames and delivered out of order
            List<LookupWire.Frame> small = new ArrayList<>(LookupWire.split(2, LookupWire.encode(real), 700));
            Collections.shuffle(small, new Random(3));
            RecipeLookup.Snapshot got2 = null;
            for (LookupWire.Frame frame : small) {
                got2 = receiver.accept(frame);
            }
            SelfTest.verdict("lookup_sync_small_frames_out_of_order", small.size() > 1 && real.equals(got2), small.size() + " frames");
        } catch (IOException e) {
            SelfTest.verdict("lookup_sync_real_lookup_survives_the_wire", false, e.toString());
        }
        // the server never sends to its own in-JVM client: only players on a remote connection get frames
        int remote = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (com.itmecasey.primamateria.data.LookupSync.needsSync(player)) {
                remote++;
            }
        }
        SelfTest.verdict("lookup_sync_no_players_need_it_in_a_headless_run", remote == 0, remote + " remote players");
    }
}
