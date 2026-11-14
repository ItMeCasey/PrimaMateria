package com.itmecasey.primamateria.anim;

import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.SieveView;
import java.util.List;

/**
 * Client side animation state of one sieve. Pure Java: it reads a {@link SieveView}, keeps a clock, picks and blends clips from the
 * sieve's {@link AnimModel}, springs the material layer toward the server's progress, and reports one-time events (a block landed,
 * a shake stopped, a find showed, the load finished) for the sound and particle code to react to.
 *
 * <p>Allocation free per call: the pose, the blend source and the bone matrices are arrays owned by the instance.
 */
public final class SieveAnimator {
    public static final int E_LOAD = 1;
    public static final int E_SHAKE = 2;
    public static final int E_REVEAL = 4;
    public static final int E_FINISH = 8;
    public static final int E_MESH = 16;

    /** Level of detail: 0 full, 1 sampled every second frame, 2 rest pose only. */
    public static final int LOD_FULL = 0;
    public static final int LOD_MID = 1;
    public static final int LOD_FAR = 2;

    public static final float BLEND_SECONDS = 0.15F;
    public static final float HOP_SECONDS = 0.4F;
    public static final float POP_SECONDS = 0.25F;
    /** Game ticks without a shake before the clip falls back to idle. */
    public static final float SHAKE_TIMEOUT_TICKS = 10F;
    public static final int MAX_FINDS = SieveView.FIND_SPOTS;

    private final AnimModel model;
    private final int trayBone;
    private final int meshBone;

    /** Sampled pose (9 floats per bone) and its matrices (12 per bone). Read by the renderer. */
    public final float[] pose;
    public final float[] mats;
    private final float[] from;
    private final float[] sampled;

    private AnimClip current;
    private AnimClip base;
    private float clipClock;
    private float blendW = 1F;
    private float phaseCorr;
    private boolean oneShot;
    private boolean sampledOnce;
    private int frameCount;
    private boolean restReady;

    // layer: [height in quarters, velocity]
    private final float[] layer = new float[2];
    private float lastTrayX;
    private float lastTrayZ;
    private final float[] slump = new float[4];

    // last seen view fields
    private boolean initialized;
    private int lastLoad;
    private int lastShakeCount;
    private String lastMesh = "";
    private long lastSeed;
    private int revealed;
    private float shakeNudge;
    private SieveView lastView = SieveView.EMPTY;

    private final float[] popT = new float[MAX_FINDS];
    private float hopT = HOP_SECONDS;
    private List<SieveView.Find> hopFinds = List.of();
    private long hopSeed;

    /** Bit set of E_* events raised by the last update. */
    public int events;
    /** Volume factor for the shake sound of the last update (0 to 1): louder with a thick layer. */
    public float shakeVolume;
    /** How many finds are showing now (the pitch index of the next reveal tick). */
    public int revealedCount;

    public SieveAnimator(AnimModel model) {
        model.requireBones("tray", "mesh", "layer");
        this.model = model;
        this.trayBone = model.boneIndex("tray");
        this.meshBone = model.boneIndex("mesh");
        this.pose = new float[model.boneCount() * AnimModel.POSE];
        this.from = new float[pose.length];
        this.sampled = new float[pose.length];
        this.mats = new float[model.boneCount() * AnimModel.MAT];
        System.arraycopy(model.restPose(), 0, pose, 0, pose.length);
        model.computeMatrices(pose, mats);
        this.base = model.clip("idle");
        this.current = base;
    }

    public AnimModel model() {
        return model;
    }

    public String currentClipName() {
        return current == null ? "" : current.name();
    }

    /** Layer height in quarters of the tray (0 to 4), smoothed. */
    public float layerQuarters() {
        return Math.max(0F, layer[0]);
    }

    /** Sideways lean of the layer surface toward the trailing side of the swing, in model pixels. */
    public float slumpX() {
        return slump[0];
    }

    public float slumpZ() {
        return slump[2];
    }

    /** Seconds into the finish hop of the finds (0 to HOP_SECONDS), or HOP_SECONDS when no hop is running. */
    public float hopTime() {
        return hopT;
    }

    public List<SieveView.Find> hopFinds() {
        return hopFinds;
    }

    public long hopSeed() {
        return hopSeed;
    }

    /** 0 to 1 pop-in of the find in this slot (index in the finds list of the last view). */
    public float popIn(int index) {
        return index < 0 || index >= MAX_FINDS ? 1F : Easing.outBack(popT[index] / POP_SECONDS, 2.2F);
    }

    /** True while anything on the sieve is moving or settling, so a resting sieve can be skipped entirely. */
    public boolean busy() {
        return !(current == base && base != null && base.length() <= 0F) || Math.abs(layer[1]) > 0.01F
                || hopT < HOP_SECONDS || blendW < 1F || anyPop();
    }

    private boolean anyPop() {
        for (int i = 0; i < MAX_FINDS; i++) {
            if (popT[i] > 0F && popT[i] < POP_SECONDS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Advances the animation.
     *
     * @param view     the latest server state of the sieve
     * @param gameTime game time in ticks including the partial tick
     * @param dt       real seconds since the last update of this sieve (clamped here to 0.1)
     * @param lod      one of the LOD constants
     */
    public void update(SieveView view, double gameTime, float dt, int lod) {
        events = 0;
        dt = Math.max(0F, Math.min(dt, 0.1F));
        if (!initialized) {
            initialized = true;
            lastLoad = view.load();
            lastShakeCount = view.shakeCount();
            lastMesh = view.meshId();
            lastSeed = view.seed();
            revealed = countRevealed(view);
            revealedCount = revealed;
            for (int i = 0; i < MAX_FINDS; i++) {
                popT[i] = POP_SECONDS;
            }
            layer[0] = target(view);
        }
        detect(view, gameTime);

        // the layer settles toward the server's progress
        float target = target(view);
        underdamped(layer, target, 34F, 0.72F, dt);
        lastView = view;

        for (int i = 0; i < MAX_FINDS; i++) {
            if (popT[i] < POP_SECONDS && popT[i] > 0F) {
                popT[i] = Math.min(POP_SECONDS, popT[i] + dt);
            }
        }
        if (hopT < HOP_SECONDS) {
            hopT = Math.min(HOP_SECONDS, hopT + dt);
        }

        if (lod == LOD_FAR) {
            if (!restReady) {
                System.arraycopy(model.restPose(), 0, pose, 0, pose.length);
                model.computeMatrices(pose, mats);
                restReady = true;
            }
            current = base;
            return;
        }
        restReady = false;
        pickClip(view, gameTime);
        frameCount++;
        if (lod == LOD_MID && (frameCount & 1) == 1 && sampledOnce) {
            clipClock += dt;
            return;
        }
        float step = lod == LOD_MID ? dt * 2F : dt;
        advanceAndSample(view, step);
        model.computeMatrices(pose, mats);
        // slump from the tray's velocity
        float tx = pose[trayBone * AnimModel.POSE];
        float tz = pose[trayBone * AnimModel.POSE + 2];
        if (step > 0.0001F) {
            float vx = (tx - lastTrayX) / step;
            float vz = (tz - lastTrayZ) / step;
            Easing.spring(slump, 0, clampf(-vx * 0.03F, -0.7F, 0.7F), 0.08F, step);
            Easing.spring(slump, 2, clampf(-vz * 0.03F, -0.7F, 0.7F), 0.08F, step);
        }
        lastTrayX = tx;
        lastTrayZ = tz;
    }

    private float target(SieveView v) {
        if (v.load() <= 0) {
            return 0F;
        }
        return v.load() * Easing.clamp01(1F - v.progress());
    }

    private int countRevealed(SieveView v) {
        int n = 0;
        List<SieveView.Find> finds = v.finds();
        for (int i = 0; i < finds.size() && i < MAX_FINDS; i++) {
            if (v.progress() >= finds.get(i).revealAt()) {
                n++;
            }
        }
        return n;
    }

    private void detect(SieveView v, double gameTime) {
        if (v.seed() != lastSeed && v.load() > 0) {
            // a new load: forget which finds were showing
            lastSeed = v.seed();
            revealed = 0;
            revealedCount = 0;
            for (int i = 0; i < MAX_FINDS; i++) {
                popT[i] = 0F;
            }
        }
        if (v.load() > lastLoad) {
            events |= E_LOAD;
            layer[1] += 7F;
            startOneShot("load");
        }
        if (lastLoad > 0 && v.load() == 0) {
            events |= E_FINISH;
            hopFinds = lastView.finds();
            hopSeed = lastView.seed();
            hopT = hopFinds.isEmpty() ? HOP_SECONDS : 0F;
            startOneShot("finish");
        }
        if (v.shakeCount() != lastShakeCount) {
            if (v.load() > 0 && v.shakeCount() > lastShakeCount) {
                events |= E_SHAKE;
                layer[1] -= 0.9F;
                shakeNudge = 1F;
                shakeVolume = (0.35F + 0.65F * Easing.clamp01(layerQuarters() / 4F))
                        * (v.drive() == DriveKind.HAND ? 1F : 0.6F);
            }
        }
        if (!v.meshId().isEmpty() && !v.meshId().equals(lastMesh)) {
            events |= E_MESH;
            startOneShot("mesh_swap");
        } else if (v.meshId().isEmpty() && !lastMesh.isEmpty()) {
            events |= E_MESH;
        }
        int now = countRevealed(v);
        if (now > revealed) {
            events |= E_REVEAL;
            for (int i = 0; i < Math.min(now, MAX_FINDS); i++) {
                if (popT[i] <= 0F && i >= revealed) {
                    popT[i] = 0.0001F;
                }
            }
        }
        revealed = now;
        revealedCount = now;
        lastLoad = v.load();
        lastShakeCount = v.shakeCount();
        lastMesh = v.meshId();
        if (v.load() == 0) {
            revealed = 0;
        }
    }

    private void startOneShot(String name) {
        AnimClip c = model.clip(name);
        if (c == null) {
            return;
        }
        beginBlend();
        current = c;
        clipClock = 0F;
        oneShot = true;
    }

    private void pickClip(SieveView v, double gameTime) {
        double since = gameTime - v.lastShakeTick();
        boolean shaking = v.load() > 0 && v.shakeCount() > 0 && since >= -2.0 && since <= SHAKE_TIMEOUT_TICKS;
        String name;
        if (v.blocked() && v.load() > 0) {
            name = "blocked";
        } else if (shaking) {
            name = switch (v.drive()) {
                case WINDUP -> "shake_windup";
                case FURNACE, POWERED -> "shake_furnace";
                default -> "shake_hand";
            };
        } else {
            name = "idle";
        }
        AnimClip wanted = model.clip(name);
        if (wanted == null) {
            wanted = model.clip("idle");
        }
        if (wanted != base) {
            base = wanted;
            if (!oneShot) {
                beginBlend();
                current = base;
                clipClock = 0F;
            }
        }
        if (oneShot && current != null && current.finished(clipClock)) {
            oneShot = false;
            beginBlend();
            current = base;
            clipClock = 0F;
        }
    }

    private void beginBlend() {
        System.arraycopy(pose, 0, from, 0, pose.length);
        blendW = sampledOnce ? 0F : 1F;
    }

    private void advanceAndSample(SieveView v, float dt) {
        if (current == null) {
            System.arraycopy(model.restPose(), 0, pose, 0, pose.length);
            return;
        }
        clipClock += dt;
        if (shakeNudge > 0F && current.loop() == AnimClip.Loop.LOOP && !oneShot && current.length() > 0F) {
            // line the visual stop up with the server's shake: snap to the nearest stop over 0.1 s
            float interval = current.length() / 2F;
            float nearest = Math.round(clipClock / interval) * interval;
            phaseCorr = nearest - clipClock;
            shakeNudge = 0F;
        }
        if (phaseCorr != 0F) {
            float s = phaseCorr * Math.min(1F, dt / 0.1F);
            clipClock += s;
            phaseCorr -= s;
            if (Math.abs(phaseCorr) < 0.0005F) {
                phaseCorr = 0F;
            }
        }
        System.arraycopy(model.restPose(), 0, sampled, 0, sampled.length);
        current.sample(clipClock, sampled);
        // the mesh sags a little under a full load
        sampled[meshBone * AnimModel.POSE + 1] -= 0.25F * Easing.clamp01(layerQuarters() / 4F);
        if (blendW < 1F) {
            blendW = Math.min(1F, blendW + dt / BLEND_SECONDS);
            float w = blendW;
            for (int i = 0; i < pose.length; i++) {
                pose[i] = from[i] + (sampled[i] - from[i]) * w;
            }
        } else {
            System.arraycopy(sampled, 0, pose, 0, pose.length);
        }
        sampledOnce = true;
    }

    /** Spring with damping ratio {@code zeta} (below 1 overshoots a little), omega in 1/seconds. Four sub-steps for stability. */
    static void underdamped(float[] s, float target, float omega, float zeta, float dt) {
        int steps = 4;
        float h = dt / steps;
        for (int i = 0; i < steps; i++) {
            float acc = omega * omega * (target - s[0]) - 2F * zeta * omega * s[1];
            s[1] += acc * h;
            s[0] += s[1] * h;
        }
    }

    private static float clampf(float x, float lo, float hi) {
        return x < lo ? lo : Math.min(x, hi);
    }
}
