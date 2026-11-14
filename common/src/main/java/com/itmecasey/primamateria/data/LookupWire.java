package com.itmecasey.primamateria.data;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The wire form of {@link RecipeLookup.Snapshot} for the server to client sync: the snapshot as JSON, gzip compressed, cut into
 * frames small enough for one network payload (a clientbound custom payload may carry about 1 MiB), and a receiver that puts the
 * frames back together. Pure Java so the loaders only move bytes; the framing and the decoding are unit tested here.
 */
public final class LookupWire {
    /** The largest frame body the sender makes, well under the 1 MiB payload limit of the game. */
    public static final int FRAME_BYTES = 200_000;
    /** A snapshot that needs more frames than this, or inflates past {@link #MAX_JSON_BYTES}, is refused by the receiver. */
    public static final int MAX_FRAMES = 160;
    public static final int MAX_JSON_BYTES = 48 * 1024 * 1024;

    private static final Gson GSON = new Gson();

    private LookupWire() {
    }

    /** One piece of an encoded snapshot. {@code sync} tells the pieces of one send from another. */
    public record Frame(int sync, int index, int total, byte[] data) {
    }

    public static byte[] encode(RecipeLookup.Snapshot snapshot) {
        byte[] json = GSON.toJson(snapshot).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, json.length / 6));
        try (OutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(json);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    public static RecipeLookup.Snapshot decode(byte[] bytes) throws IOException {
        byte[] json;
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, bytes.length * 6));
            byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) {
                if (out.size() + n > MAX_JSON_BYTES) {
                    throw new IOException("recipe lookup is larger than " + MAX_JSON_BYTES + " bytes");
                }
                out.write(buffer, 0, n);
            }
            json = out.toByteArray();
        }
        try {
            RecipeLookup.Snapshot snapshot = GSON.fromJson(new String(json, StandardCharsets.UTF_8), RecipeLookup.Snapshot.class);
            if (snapshot == null) {
                throw new IOException("empty recipe lookup");
            }
            return snapshot;
        } catch (RuntimeException e) {
            throw new IOException("unreadable recipe lookup: " + e.getMessage(), e);
        }
    }

    /** Cuts encoded bytes into frames of at most {@code frameBytes}. An empty input still gives one (empty) frame. */
    public static List<Frame> split(int sync, byte[] bytes, int frameBytes) {
        int total = Math.max(1, (bytes.length + frameBytes - 1) / frameBytes);
        List<Frame> frames = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            int from = i * frameBytes;
            int to = Math.min(bytes.length, from + frameBytes);
            byte[] part = new byte[Math.max(0, to - from)];
            System.arraycopy(bytes, from, part, 0, part.length);
            frames.add(new Frame(sync, i, total, part));
        }
        return frames;
    }

    public static List<Frame> frames(int sync, RecipeLookup.Snapshot snapshot) {
        return split(sync, encode(snapshot), FRAME_BYTES);
    }

    /** Collects the frames of one send at a time; a frame of a newer send drops what is unfinished of an older one. */
    public static final class Receiver {
        private int sync = Integer.MIN_VALUE;
        private byte[][] parts;
        private int received;

        /** The snapshot once its last frame is in, otherwise {@code null}. Throws for a frame that cannot be right. */
        public synchronized RecipeLookup.Snapshot accept(Frame frame) throws IOException {
            if (frame.total() < 1 || frame.total() > MAX_FRAMES || frame.index() < 0 || frame.index() >= frame.total()) {
                reset();
                throw new IOException("bad recipe lookup frame " + frame.index() + " of " + frame.total());
            }
            if (parts == null || frame.sync() != sync || parts.length != frame.total()) {
                sync = frame.sync();
                parts = new byte[frame.total()][];
                received = 0;
            }
            if (parts[frame.index()] == null) {
                parts[frame.index()] = frame.data();
                received++;
            }
            if (received < parts.length) {
                return null;
            }
            int size = 0;
            for (byte[] part : parts) {
                size += part.length;
            }
            byte[] all = new byte[size];
            int at = 0;
            for (byte[] part : parts) {
                System.arraycopy(part, 0, all, at, part.length);
                at += part.length;
            }
            reset();
            return decode(all);
        }

        public synchronized void reset() {
            parts = null;
            received = 0;
        }
    }
}
