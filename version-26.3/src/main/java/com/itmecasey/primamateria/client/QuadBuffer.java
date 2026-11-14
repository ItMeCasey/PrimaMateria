package com.itmecasey.primamateria.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * A reusable list of boxes (in model pixels) drawn with one texture. Filled during extraction, written when the frame is drawn.
 * Boxes can be turned about the X axis (the windup key, the stamps). No allocation after construction.
 */
final class QuadBuffer implements SubmitNodeCollector.CustomGeometryRenderer {
    private static final int FIELDS = 19;
    private float[] data = new float[FIELDS * 48];
    private int count;

    int count() {
        return count;
    }

    void clear() {
        count = 0;
    }

    /** A box. {@code topOnly} draws just the top face. uv is the rectangle of the 16 texture used by every face (v scaled by fr). */
    void box(float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int alpha, int light, boolean topOnly, float fr) {
        box(x0, y0, z0, x1, y1, z1, rgb, alpha, light, topOnly, fr, 0F, 0F, 0F);
    }

    void box(float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int alpha, int light, boolean topOnly, float fr,
             float rotXDegrees, float pivotY, float pivotZ) {
        if ((count + 1) * FIELDS > data.length) {
            data = java.util.Arrays.copyOf(data, data.length * 2);
        }
        int o = count * FIELDS;
        data[o] = x0;
        data[o + 1] = y0;
        data[o + 2] = z0;
        data[o + 3] = x1;
        data[o + 4] = y1;
        data[o + 5] = z1;
        data[o + 6] = (rgb >> 16) & 255;
        data[o + 7] = (rgb >> 8) & 255;
        data[o + 8] = rgb & 255;
        data[o + 9] = alpha;
        data[o + 10] = light;
        data[o + 11] = topOnly ? 1F : 0F;
        data[o + 12] = fr;
        data[o + 13] = rotXDegrees;
        data[o + 14] = pivotY;
        data[o + 15] = pivotZ;
        count++;
    }

    @Override
    public void render(PoseStack.Pose pose, VertexConsumer vc) {
        for (int i = 0; i < count; i++) {
            int o = i * FIELDS;
            float x0 = data[o];
            float y0 = data[o + 1];
            float z0 = data[o + 2];
            float x1 = data[o + 3];
            float y1 = data[o + 4];
            float z1 = data[o + 5];
            int r = (int) data[o + 6];
            int g = (int) data[o + 7];
            int b = (int) data[o + 8];
            int a = (int) data[o + 9];
            int light = (int) data[o + 10];
            boolean top = data[o + 11] > 0F;
            float fr = data[o + 12];
            double rad = Math.toRadians(data[o + 13]);
            float c = (float) Math.cos(rad);
            float s = (float) Math.sin(rad);
            float py = data[o + 14];
            float pz = data[o + 15];
            // up (u = x, v = z)
            face(vc, pose, r, g, b, a, light, c, s, py, pz, fr, 0, 1, 0,
                    x0, y1, z1, x0, z1, x1, y1, z1, x1, z1, x1, y1, z0, x1, z0, x0, y1, z0, x0, z0);
            if (top) {
                continue;
            }
            // north, south, west, east: u runs along the face, v with the height
            face(vc, pose, r, g, b, a, light, c, s, py, pz, fr, 0, 0, -1,
                    x1, y0, z0, 16 - x1, 16 - y0, x0, y0, z0, 16 - x0, 16 - y0, x0, y1, z0, 16 - x0, 16 - y1, x1, y1, z0, 16 - x1, 16 - y1);
            face(vc, pose, r, g, b, a, light, c, s, py, pz, fr, 0, 0, 1,
                    x0, y0, z1, x0, 16 - y0, x1, y0, z1, x1, 16 - y0, x1, y1, z1, x1, 16 - y1, x0, y1, z1, x0, 16 - y1);
            face(vc, pose, r, g, b, a, light, c, s, py, pz, fr, -1, 0, 0,
                    x0, y0, z0, z0, 16 - y0, x0, y0, z1, z1, 16 - y0, x0, y1, z1, z1, 16 - y1, x0, y1, z0, z0, 16 - y1);
            face(vc, pose, r, g, b, a, light, c, s, py, pz, fr, 1, 0, 0,
                    x1, y0, z1, 16 - z1, 16 - y0, x1, y0, z0, 16 - z0, 16 - y0, x1, y1, z0, 16 - z0, 16 - y1, x1, y1, z1, 16 - z1, 16 - y1);
        }
    }

    private static void face(VertexConsumer vc, PoseStack.Pose pose, int r, int g, int b, int a, int light, float c, float s,
                             float py, float pz, float fr, float nx, float ny, float nz,
                             float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3) {
        vertex(vc, pose, r, g, b, a, light, c, s, py, pz, nx, ny, nz, x0, y0, z0, u0, v0, fr);
        vertex(vc, pose, r, g, b, a, light, c, s, py, pz, nx, ny, nz, x1, y1, z1, u1, v1, fr);
        vertex(vc, pose, r, g, b, a, light, c, s, py, pz, nx, ny, nz, x2, y2, z2, u2, v2, fr);
        vertex(vc, pose, r, g, b, a, light, c, s, py, pz, nx, ny, nz, x3, y3, z3, u3, v3, fr);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int r, int g, int b, int a, int light, float c, float s,
                               float py, float pz, float nx, float ny, float nz, float x, float y, float z, float u, float v,
                               float fr) {
        float ry = py + (y - py) * c - (z - pz) * s;
        float rz = pz + (y - py) * s + (z - pz) * c;
        float rny = ny * c - nz * s;
        float rnz = ny * s + nz * c;
        vc.addVertex(pose, x / 16F, ry / 16F, rz / 16F).setColor(r, g, b, a).setUv(u / 16F, v / 16F * fr)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, rny, rnz);
    }
}
