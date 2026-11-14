package com.itmecasey.primamateria.anim;

/**
 * One named animation. Tracks are indexed {@code bone * 9 + channel * 3 + axis} (channel 0 position, 1 rotation, 2 scale); an
 * absent track leaves the rest value in place.
 */
public final class AnimClip {
    public enum Loop { ONCE, LOOP, HOLD }

    public static final int POSITION = 0;
    public static final int ROTATION = 1;
    public static final int SCALE = 2;

    private final String name;
    private final float length;
    private final Loop loop;
    private final Track[] tracks;

    public AnimClip(String name, float length, Loop loop, Track[] tracks) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.tracks = tracks;
    }

    public String name() {
        return name;
    }

    public float length() {
        return length;
    }

    public Loop loop() {
        return loop;
    }

    /** True once a one-shot clip has played to its end. */
    public boolean finished(float time) {
        return loop == Loop.ONCE && time >= length;
    }

    /** Maps a clock value to a clip time: loops wrap, everything else clamps. */
    public float wrap(float time) {
        if (length <= 0F) {
            return 0F;
        }
        if (loop == Loop.LOOP) {
            float m = time % length;
            return m < 0F ? m + length : m;
        }
        return Math.max(0F, Math.min(time, length));
    }

    /** Writes every animated value of the clip at {@code time} into {@code pose}; other entries are untouched. */
    public void sample(float time, float[] pose) {
        float tt = wrap(time);
        for (int i = 0; i < tracks.length; i++) {
            Track tr = tracks[i];
            if (tr != null) {
                pose[i] = tr.sample(tt);
            }
        }
    }

    public Track track(int index) {
        return tracks[index];
    }

    public int trackCount() {
        return tracks.length;
    }
}
