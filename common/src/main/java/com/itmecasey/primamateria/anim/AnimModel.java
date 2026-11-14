package com.itmecasey.primamateria.anim;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The moving part of a model: a bone tree (parents always come before children), baked cube geometry per bone and texture, and
 * the named clips. Plain Java, no game classes. Units are model pixels (16 per block); the renderer divides by 16.
 *
 * <p>Bone math: a bone with pose (position p, rotation r in degrees, scale s) maps a point x to
 * {@code pivot + p + Rz(r.z) Ry(r.y) Rx(r.x) * (s * (x - pivot))}, then its parent's transform applies on top. All coordinates are
 * absolute model coordinates, as in Blockbench.
 */
public final class AnimModel {
    /** Quads for one texture of one bone: 8 floats per vertex (x y z u v nx ny nz), 4 vertices per quad, in blocks. */
    public record Mesh(String texture, float[] verts) {
    }

    public static final int POSE = 9;
    public static final int MAT = 12;

    private final String name;
    private final String[] boneNames;
    private final int[] parent;
    private final float[] pivot;
    private final Mesh[][] meshes;
    private final Map<String, String> textures;
    private final Map<String, AnimClip> clips;
    private final float[] rest;

    public AnimModel(String name, String[] boneNames, int[] parent, float[] pivot, Mesh[][] meshes, Map<String, String> textures,
                     Map<String, AnimClip> clips) {
        this.name = name;
        this.boneNames = boneNames;
        this.parent = parent;
        this.pivot = pivot;
        this.meshes = meshes;
        this.textures = textures;
        this.clips = new LinkedHashMap<>(clips);
        this.rest = new float[boneNames.length * POSE];
        for (int b = 0; b < boneNames.length; b++) {
            rest[b * POSE + 6] = 1F;
            rest[b * POSE + 7] = 1F;
            rest[b * POSE + 8] = 1F;
            if (parent[b] >= b) {
                throw new IllegalArgumentException("Bone " + boneNames[b] + " must come after its parent");
            }
        }
    }

    public String name() {
        return name;
    }

    public int boneCount() {
        return boneNames.length;
    }

    public String boneName(int bone) {
        return boneNames[bone];
    }

    public int parent(int bone) {
        return parent[bone];
    }

    /** Index of a bone, or -1. */
    public int boneIndex(String bone) {
        for (int i = 0; i < boneNames.length; i++) {
            if (boneNames[i].equals(bone)) {
                return i;
            }
        }
        return -1;
    }

    public Mesh[] meshes(int bone) {
        return meshes[bone];
    }

    /** Texture key to texture id (for example {@code minecraft:block/oak_planks}). */
    public Map<String, String> textures() {
        return textures;
    }

    public AnimClip clip(String clipName) {
        return clips.get(clipName);
    }

    public Map<String, AnimClip> clips() {
        return clips;
    }

    /** The rest pose (position 0, rotation 0, scale 1 for every bone). Do not modify. */
    public float[] restPose() {
        return rest;
    }

    public float pivotX(int bone) {
        return pivot[bone * 3];
    }

    public float pivotY(int bone) {
        return pivot[bone * 3 + 1];
    }

    public float pivotZ(int bone) {
        return pivot[bone * 3 + 2];
    }

    /** Fails with a clear message when a bone the renderer relies on is missing. */
    public void requireBones(String... required) {
        for (String bone : required) {
            if (boneIndex(bone) < 0) {
                throw new IllegalArgumentException("Model " + name + " is missing the bone " + bone);
            }
        }
    }

    /**
     * Turns a pose (9 floats per bone) into one 3 by 4 affine matrix per bone (12 floats, row major) in {@code mats}, parents
     * first. Allocation free.
     */
    public void computeMatrices(float[] pose, float[] mats) {
        for (int b = 0; b < boneNames.length; b++) {
            int p = b * POSE;
            float sx = pose[p + 6];
            float sy = pose[p + 7];
            float sz = pose[p + 8];
            double rx = Math.toRadians(pose[p + 3]);
            double ry = Math.toRadians(pose[p + 4]);
            double rz = Math.toRadians(pose[p + 5]);
            float cx = (float) Math.cos(rx);
            float cxs = (float) Math.sin(rx);
            float cy = (float) Math.cos(ry);
            float cys = (float) Math.sin(ry);
            float cz = (float) Math.cos(rz);
            float czs = (float) Math.sin(rz);
            // R = Rz * Ry * Rx
            float r00 = cz * cy;
            float r01 = cz * cys * cxs - czs * cx;
            float r02 = cz * cys * cx + czs * cxs;
            float r10 = czs * cy;
            float r11 = czs * cys * cxs + cz * cx;
            float r12 = czs * cys * cx - cz * cxs;
            float r20 = -cys;
            float r21 = cy * cxs;
            float r22 = cy * cx;
            // A = R * S
            float a00 = r00 * sx;
            float a01 = r01 * sy;
            float a02 = r02 * sz;
            float a10 = r10 * sx;
            float a11 = r11 * sy;
            float a12 = r12 * sz;
            float a20 = r20 * sx;
            float a21 = r21 * sy;
            float a22 = r22 * sz;
            float px = pivot[b * 3];
            float py = pivot[b * 3 + 1];
            float pz = pivot[b * 3 + 2];
            float tx = px + pose[p] - (a00 * px + a01 * py + a02 * pz);
            float ty = py + pose[p + 1] - (a10 * px + a11 * py + a12 * pz);
            float tz = pz + pose[p + 2] - (a20 * px + a21 * py + a22 * pz);
            int m = b * MAT;
            int par = parent[b];
            if (par < 0) {
                mats[m] = a00;
                mats[m + 1] = a01;
                mats[m + 2] = a02;
                mats[m + 3] = tx;
                mats[m + 4] = a10;
                mats[m + 5] = a11;
                mats[m + 6] = a12;
                mats[m + 7] = ty;
                mats[m + 8] = a20;
                mats[m + 9] = a21;
                mats[m + 10] = a22;
                mats[m + 11] = tz;
            } else {
                int q = par * MAT;
                for (int row = 0; row < 3; row++) {
                    float p0 = mats[q + row * 4];
                    float p1 = mats[q + row * 4 + 1];
                    float p2 = mats[q + row * 4 + 2];
                    float pt = mats[q + row * 4 + 3];
                    // the parent's row times the local columns
                    mats[m + row * 4] = p0 * a00 + p1 * a10 + p2 * a20;
                    mats[m + row * 4 + 1] = p0 * a01 + p1 * a11 + p2 * a21;
                    mats[m + row * 4 + 2] = p0 * a02 + p1 * a12 + p2 * a22;
                    mats[m + row * 4 + 3] = p0 * tx + p1 * ty + p2 * tz + pt;
                }
            }
        }
    }

    /** Applies the bone matrix to a point and returns one coordinate (0 = x, 1 = y, 2 = z). */
    public static float apply(float[] mats, int bone, float x, float y, float z, int axis) {
        int m = bone * MAT + axis * 4;
        return mats[m] * x + mats[m + 1] * y + mats[m + 2] * z + mats[m + 3];
    }
}
