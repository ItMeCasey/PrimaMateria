package com.itmecasey.primamateria.anim;

import java.util.List;

/**
 * Keyframes of one value (one axis of one channel of one bone), stored as parallel arrays so sampling allocates nothing. The
 * interpolation mode of a keyframe applies to the segment that starts at it, as in Blockbench.
 */
public final class Track {
    public static final byte LINEAR = 0;
    public static final byte STEP = 1;
    public static final byte CATMULLROM = 2;
    public static final byte BEZIER = 3;

    /** One keyframe as read from JSON. Handle times and values are offsets from the keyframe, as Blockbench stores them. */
    public record Key(float time, float value, byte mode, float leftTime, float leftValue, float rightTime, float rightValue) {
        public static Key linear(float time, float value) {
            return new Key(time, value, LINEAR, 0F, 0F, 0F, 0F);
        }
    }

    private final float[] t;
    private final float[] v;
    private final byte[] mode;
    private final float[] lt;
    private final float[] lv;
    private final float[] rt;
    private final float[] rv;

    public Track(List<Key> keys) {
        int n = keys.size();
        if (n == 0) {
            throw new IllegalArgumentException("A track needs at least one keyframe");
        }
        t = new float[n];
        v = new float[n];
        mode = new byte[n];
        lt = new float[n];
        lv = new float[n];
        rt = new float[n];
        rv = new float[n];
        List<Key> sorted = keys.stream().sorted((a, b) -> Float.compare(a.time(), b.time())).toList();
        for (int i = 0; i < n; i++) {
            Key k = sorted.get(i);
            t[i] = k.time();
            v[i] = k.value();
            mode[i] = k.mode();
            lt[i] = k.leftTime();
            lv[i] = k.leftValue();
            rt[i] = k.rightTime();
            rv[i] = k.rightValue();
        }
    }

    public int size() {
        return t.length;
    }

    public float lastTime() {
        return t[t.length - 1];
    }

    /** Value at {@code time}: held before the first and after the last keyframe. */
    public float sample(float time) {
        int n = t.length;
        if (n == 1 || time <= t[0]) {
            return v[0];
        }
        if (time >= t[n - 1]) {
            return v[n - 1];
        }
        int lo = 0;
        int hi = n - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (t[mid] <= time) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        int i = lo;
        int j = i + 1;
        float span = t[j] - t[i];
        float u = span <= 0F ? 1F : (time - t[i]) / span;
        switch (mode[i]) {
            case STEP:
                return v[i];
            case CATMULLROM: {
                float p0 = i > 0 ? v[i - 1] : v[i];
                float p3 = j < n - 1 ? v[j + 1] : v[j];
                return Easing.catmullRom(p0, v[i], v[j], p3, u);
            }
            case BEZIER:
                return Easing.bezierSegment(t[i], v[i], rt[i], rv[i], t[j], v[j], lt[j], lv[j], time);
            default:
                return v[i] + (v[j] - v[i]) * u;
        }
    }
}
