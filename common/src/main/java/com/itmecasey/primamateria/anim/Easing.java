package com.itmecasey.primamateria.anim;

/** Pure easing and curve helpers used by the keyframe sampler and the sieve animator. No allocation. */
public final class Easing {
    private Easing() {
    }

    public static float clamp01(float x) {
        return x < 0F ? 0F : Math.min(x, 1F);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Standard uniform Catmull-Rom between p1 and p2 (p0 and p3 are the neighbors), u in 0..1. */
    public static float catmullRom(float p0, float p1, float p2, float p3, float u) {
        float u2 = u * u;
        float u3 = u2 * u;
        return 0.5F * ((2F * p1) + (-p0 + p2) * u + (2F * p0 - 5F * p1 + 4F * p2 - p3) * u2 + (-p0 + 3F * p1 - 3F * p2 + p3) * u3);
    }

    /** One coordinate of a cubic Bezier with control values a, b, c, d at parameter s. */
    public static float cubic(float a, float b, float c, float d, float s) {
        float m = 1F - s;
        return m * m * m * a + 3F * m * m * s * b + 3F * m * s * s * c + s * s * s * d;
    }

    /**
     * Cubic Bezier segment in (time, value) space: end points (t0,v0) and (t1,v1), handles at (t0+rt0, v0+rv0) and
     * (t1+lt1, v1+lv1). Handles are clamped inside the segment in time so the curve is a function of time. Returns the value at
     * {@code time}.
     */
    public static float bezierSegment(float t0, float v0, float rt0, float rv0, float t1, float v1, float lt1, float lv1,
                                      float time) {
        float span = t1 - t0;
        if (span <= 0F) {
            return v1;
        }
        float x1 = t0 + Math.max(0F, Math.min(rt0, span));
        float x2 = t1 + Math.min(0F, Math.max(lt1, -span));
        float y1 = v0 + rv0;
        float y2 = v1 + lv1;
        // solve x(s) = time by bisection (monotonic because the handles are clamped), then read y(s)
        float lo = 0F;
        float hi = 1F;
        float s = (time - t0) / span;
        for (int i = 0; i < 16; i++) {
            float x = cubic(t0, x1, x2, t1, s);
            if (Math.abs(x - time) < span * 1e-5F) {
                break;
            }
            if (x < time) {
                lo = s;
            } else {
                hi = s;
            }
            s = 0.5F * (lo + hi);
        }
        return cubic(v0, y1, y2, v1, s);
    }

    public static float outCubic(float t) {
        float m = 1F - clamp01(t);
        return 1F - m * m * m;
    }

    /** Ease-out with overshoot (the plop of a block landing in the tray). {@code back} about 1.7 gives a small overshoot. */
    public static float outBack(float t, float back) {
        float x = clamp01(t) - 1F;
        return 1F + (back + 1F) * x * x * x + back * x * x;
    }

    public static float inCubic(float t) {
        float x = clamp01(t);
        return x * x * x;
    }

    /**
     * One step of a critically damped spring: moves {@code state[at]} toward {@code target} and updates the velocity kept at
     * {@code state[at + 1]}, so nothing is allocated. {@code smoothTime} is roughly the time to settle, in seconds.
     */
    public static void spring(float[] state, int at, float target, float smoothTime, float dt) {
        float omega = 2F / Math.max(0.0001F, smoothTime);
        float x = omega * dt;
        float exp = 1F / (1F + x + 0.48F * x * x + 0.235F * x * x * x);
        float change = state[at] - target;
        float temp = (state[at + 1] + omega * change) * dt;
        state[at + 1] = (state[at + 1] - omega * temp) * exp;
        state[at] = target + (change + temp) * exp;
    }
}
