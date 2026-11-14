package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.anim.AnimModel;
import com.itmecasey.primamateria.anim.SieveAnimator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * The material layer in the tray: a box textured with the loaded block's own texture, drawn at the {@code layer} bone so it
 * moves with the tray. The top is a 3 by 3 grid of heights: domed (thick in the middle, thin at the rim) and leaning toward the
 * trailing side of the swing; flat when {@link #dome} is false. 12 quads, no allocation.
 */
final class LayerDraw implements SubmitNodeCollector.CustomGeometryRenderer {
    /** Height of one quarter of a load, in model pixels. */
    static final float QUARTER = 1.1F;
    private static final float X0 = 4.45F;
    private static final float X1 = 11.55F;
    private static final float BASE = 10F;
    private static final float[] GRID = {X0, 8F, X1};

    private final SieveAnimator animator;
    private final int bone;
    private final float[] h = new float[9];

    int light;
    int r = 255;
    int g = 255;
    int b = 255;
    boolean dome;
    /** Texture shuffle of alternate shakes, in pixels. */
    float uOffset;

    private float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23;
    private PoseStack.Pose pose;
    private VertexConsumer vc;

    LayerDraw(SieveAnimator animator, int bone) {
        this.animator = animator;
        this.bone = bone;
    }

    /** True when the layer is thick enough to be worth drawing. */
    boolean visible() {
        return animator.layerQuarters() * QUARTER > 0.05F;
    }

    @Override
    public void render(PoseStack.Pose pose, VertexConsumer vc) {
        this.pose = pose;
        this.vc = vc;
        int m = bone * AnimModel.MAT;
        float[] mats = animator.mats;
        m00 = mats[m];
        m01 = mats[m + 1];
        m02 = mats[m + 2];
        m03 = mats[m + 3];
        m10 = mats[m + 4];
        m11 = mats[m + 5];
        m12 = mats[m + 6];
        m13 = mats[m + 7];
        m20 = mats[m + 8];
        m21 = mats[m + 9];
        m22 = mats[m + 10];
        m23 = mats[m + 11];
        float top = animator.layerQuarters() * QUARTER;
        float k = dome ? 1F : 0F;
        float sx = dome ? animator.slumpX() : 0F;
        float sz = dome ? animator.slumpZ() : 0F;
        for (int zi = 0; zi < 3; zi++) {
            for (int xi = 0; xi < 3; xi++) {
                boolean cx = xi == 1;
                boolean cz = zi == 1;
                float shape = cx && cz ? 1.2F : (cx || cz) ? 0.88F : 0.7F;
                float height = top * (1F + (shape - 1F) * k) + sx * (GRID[xi] - 8F) / 3.55F * 0.5F + sz * (GRID[zi] - 8F) / 3.55F * 0.5F;
                h[zi * 3 + xi] = Math.max(0.05F, height);
            }
        }
        // top: four quads
        for (int zi = 0; zi < 2; zi++) {
            for (int xi = 0; xi < 2; xi++) {
                float xa = GRID[xi];
                float xb = GRID[xi + 1];
                float za = GRID[zi];
                float zb = GRID[zi + 1];
                float ha = BASE + h[zi * 3 + xi];
                float hb = BASE + h[zi * 3 + xi + 1];
                float hc = BASE + h[(zi + 1) * 3 + xi + 1];
                float hd = BASE + h[(zi + 1) * 3 + xi];
                // a (xa, zb) b (xb, zb) c (xb, za) d (xa, za): the order of the up face of a cube
                v(xa, hd, zb, xa + uOffset, zb, 0, 1, 0);
                v(xb, hc, zb, xb + uOffset, zb, 0, 1, 0);
                v(xb, hb, za, xb + uOffset, za, 0, 1, 0);
                v(xa, ha, za, xa + uOffset, za, 0, 1, 0);
            }
        }
        // sides: two quads each, from the base up to the edge heights
        for (int s = 0; s < 2; s++) {
            float xa = GRID[s];
            float xb = GRID[s + 1];
            float n0 = BASE + h[s];
            float n1 = BASE + h[s + 1];
            float s0 = BASE + h[6 + s];
            float s1 = BASE + h[6 + s + 1];
            float w0 = BASE + h[s * 3];
            float w1 = BASE + h[(s + 1) * 3];
            float e0 = BASE + h[s * 3 + 2];
            float e1 = BASE + h[(s + 1) * 3 + 2];
            // north (z = X0, faces -z)
            v(xb, BASE, X0, 16F - xb + uOffset, 16F - BASE, 0, 0, -1);
            v(xa, BASE, X0, 16F - xa + uOffset, 16F - BASE, 0, 0, -1);
            v(xa, n0, X0, 16F - xa + uOffset, 16F - n0, 0, 0, -1);
            v(xb, n1, X0, 16F - xb + uOffset, 16F - n1, 0, 0, -1);
            // south (z = X1, faces +z)
            v(xa, BASE, X1, xa + uOffset, 16F - BASE, 0, 0, 1);
            v(xb, BASE, X1, xb + uOffset, 16F - BASE, 0, 0, 1);
            v(xb, s1, X1, xb + uOffset, 16F - s1, 0, 0, 1);
            v(xa, s0, X1, xa + uOffset, 16F - s0, 0, 0, 1);
            // west (x = X0, faces -x)
            v(X0, BASE, xa, xa, 16F - BASE, -1, 0, 0);
            v(X0, BASE, xb, xb, 16F - BASE, -1, 0, 0);
            v(X0, w1, xb, xb, 16F - w1, -1, 0, 0);
            v(X0, w0, xa, xa, 16F - w0, -1, 0, 0);
            // east (x = X1, faces +x)
            v(X1, BASE, xb, 16F - xb, 16F - BASE, 1, 0, 0);
            v(X1, BASE, xa, 16F - xa, 16F - BASE, 1, 0, 0);
            v(X1, e0, xa, 16F - xa, 16F - e0, 1, 0, 0);
            v(X1, e1, xb, 16F - xb, 16F - e1, 1, 0, 0);
        }
    }

    /** One vertex: a point in model pixels (moved by the layer bone's matrix), a uv in pixels of a 16 texture, a normal. */
    private void v(float x, float y, float z, float u, float vv, float nx, float ny, float nz) {
        vc.addVertex(pose, (m00 * x + m01 * y + m02 * z + m03) / 16F, (m10 * x + m11 * y + m12 * z + m13) / 16F,
                        (m20 * x + m21 * y + m22 * z + m23) / 16F)
                .setColor(r, g, b, 255).setUv(u / 16F, vv / 16F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, m00 * nx + m01 * ny + m02 * nz, m10 * nx + m11 * ny + m12 * nz, m20 * nx + m21 * ny + m22 * nz);
    }
}
