package com.itmecasey.primamateria.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.viewer.Samples;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** The recipe lookup as it travels from a dedicated server to a client: encode, cut into frames, put back together. */
class LookupWireTest {
    private final RecipeLookup.Snapshot sample = Samples.snapshot();

    @Test
    void roundTripKeepsEveryFamilyAndField() throws IOException {
        RecipeLookup.Snapshot back = LookupWire.decode(LookupWire.encode(sample));
        assertEquals(sample, back);
    }

    @Test
    void emptySnapshotRoundTrips() throws IOException {
        assertEquals(RecipeLookup.Snapshot.EMPTY, LookupWire.decode(LookupWire.encode(RecipeLookup.Snapshot.EMPTY)));
    }

    @Test
    void smallSnapshotIsOneFrame() {
        List<LookupWire.Frame> frames = LookupWire.frames(7, sample);
        assertEquals(1, frames.size());
        assertEquals(1, frames.get(0).total());
        assertTrue(frames.get(0).data().length < LookupWire.FRAME_BYTES);
    }

    @Test
    void frameSizesStayUnderTheLimitAndJoinBack() throws IOException {
        byte[] data = new byte[1_000_003];
        new Random(5).nextBytes(data);
        List<LookupWire.Frame> frames = LookupWire.split(3, data, LookupWire.FRAME_BYTES);
        assertEquals(6, frames.size());
        for (LookupWire.Frame f : frames) {
            assertTrue(f.data().length <= LookupWire.FRAME_BYTES);
            assertEquals(6, f.total());
        }
        assertEquals(data.length, frames.stream().mapToInt(f -> f.data().length).sum());
    }

    /** A pack with many recipes makes a lookup that needs several frames: send, shuffle and receive them. */
    @Test
    void largeSnapshotSplitsAndArrivesInAnyOrder() throws IOException {
        List<RecipeLookup.Sifting> many = new ArrayList<>();
        Random random = new Random(11);
        for (int i = 0; i < 90_000; i++) {
            many.add(new RecipeLookup.Sifting("pack:r" + i + "_" + Long.toHexString(random.nextLong()), "minecraft:gravel",
                    "pack:item_" + Long.toHexString(random.nextLong()), 1, 1 + i % 4, random.nextDouble(), 1 + i % 5, 0));
        }
        RecipeLookup.Snapshot big = new RecipeLookup.Snapshot(sample.crushing(), many, sample.residue(), sample.composting(),
                sample.soaking(), sample.mixing(), sample.steeping(), sample.growing(), sample.melting(), sample.casting(),
                sample.meshes(), sample.heat(), sample.compost());
        List<LookupWire.Frame> frames = new ArrayList<>(LookupWire.frames(1, big));
        assertTrue(frames.size() > 1, "frames: " + frames.size());
        Collections.shuffle(frames, new Random(2));
        LookupWire.Receiver receiver = new LookupWire.Receiver();
        RecipeLookup.Snapshot got = null;
        for (int i = 0; i < frames.size(); i++) {
            RecipeLookup.Snapshot result = receiver.accept(frames.get(i));
            if (i < frames.size() - 1) {
                assertNull(result);
            } else {
                got = result;
            }
        }
        assertEquals(big, got);
    }

    @Test
    void duplicateFramesCountOnce() throws IOException {
        byte[] data = new byte[LookupWire.FRAME_BYTES + 10];
        List<LookupWire.Frame> frames = LookupWire.split(1, data, LookupWire.FRAME_BYTES);
        LookupWire.Receiver receiver = new LookupWire.Receiver();
        assertNull(receiver.accept(frames.get(0)));
        assertNull(receiver.accept(frames.get(0)));
    }

    @Test
    void newerSendReplacesAnUnfinishedOlderOne() throws IOException {
        byte[] encoded = LookupWire.encode(sample);
        int cut = (encoded.length + 1) / 2;
        List<LookupWire.Frame> old = LookupWire.split(1, encoded, cut);
        assertEquals(2, old.size());
        LookupWire.Receiver receiver = new LookupWire.Receiver();
        assertNull(receiver.accept(old.get(0)));
        // a whole new send arrives before the old one finished
        RecipeLookup.Snapshot got = null;
        for (LookupWire.Frame f : LookupWire.split(2, encoded, cut)) {
            got = receiver.accept(f);
        }
        assertEquals(sample, got);
        // a late frame of the dropped send starts a new pending send and never completes it by itself
        assertNull(receiver.accept(old.get(1)));
    }

    @Test
    void impossibleFramesAreRefused() {
        LookupWire.Receiver receiver = new LookupWire.Receiver();
        assertThrows(IOException.class, () -> receiver.accept(new LookupWire.Frame(1, 0, 0, new byte[0])));
        assertThrows(IOException.class, () -> receiver.accept(new LookupWire.Frame(1, 3, 3, new byte[0])));
        assertThrows(IOException.class, () -> receiver.accept(new LookupWire.Frame(1, -1, 3, new byte[0])));
        assertThrows(IOException.class, () -> receiver.accept(new LookupWire.Frame(1, 0, LookupWire.MAX_FRAMES + 1, new byte[0])));
    }

    @Test
    void garbageIsAnErrorNotACrash() {
        LookupWire.Receiver receiver = new LookupWire.Receiver();
        assertThrows(IOException.class, () -> receiver.accept(new LookupWire.Frame(1, 0, 1, new byte[] {1, 2, 3, 4})));
        assertThrows(IOException.class, () -> LookupWire.decode(new byte[0]));
    }

    @Test
    void receiverIsUsableAfterAnError() throws IOException {
        LookupWire.Receiver receiver = new LookupWire.Receiver();
        assertThrows(IOException.class, () -> receiver.accept(new LookupWire.Frame(1, 0, 1, new byte[] {9, 9})));
        RecipeLookup.Snapshot got = receiver.accept(LookupWire.frames(2, sample).get(0));
        assertNotNull(got);
        assertEquals(sample, got);
    }

    @Test
    void jsonWithoutAFamilyIsRefusedNotNull() throws IOException {
        // a hand-made payload with a missing list must not hand out a snapshot with null lists
        byte[] bytes = gzip("{\"crushing\":[]}");
        assertThrows(IOException.class, () -> LookupWire.decode(bytes));
    }

    private static byte[] gzip(String text) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream g = new java.util.zip.GZIPOutputStream(out)) {
            g.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }
}
