package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.anim.AnimModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Writes the cubes of every moving bone that uses one texture, with the bone matrices the animator sampled this frame. One
 * instance per sieve and texture, reused every frame: the fields are set during extraction and read when the frame is drawn.
 */
final class BoneDraw implements SubmitNodeCollector.CustomGeometryRenderer {
    private final AnimModel model;
    private final float[] mats;
    final String textureKey;
    private final int[] bones;
    private final int[] meshes;
    int light;

    BoneDraw(AnimModel model, float[] mats, String textureKey, List<int[]> parts) {
        this.model = model;
        this.mats = mats;
        this.textureKey = textureKey;
        this.bones = new int[parts.size()];
        this.meshes = new int[parts.size()];
        for (int i = 0; i < bones.length; i++) {
            bones[i] = parts.get(i)[0];
            meshes[i] = parts.get(i)[1];
        }
    }

    @Override
    public void render(PoseStack.Pose pose, VertexConsumer vc) {
        for (int p = 0; p < bones.length; p++) {
            int bone = bones[p];
            float[] verts = model.meshes(bone)[meshes[p]].verts();
            int m = bone * AnimModel.MAT;
            float m00 = mats[m];
            float m01 = mats[m + 1];
            float m02 = mats[m + 2];
            float m03 = mats[m + 3] / 16F;
            float m10 = mats[m + 4];
            float m11 = mats[m + 5];
            float m12 = mats[m + 6];
            float m13 = mats[m + 7] / 16F;
            float m20 = mats[m + 8];
            float m21 = mats[m + 9];
            float m22 = mats[m + 10];
            float m23 = mats[m + 11] / 16F;
            for (int i = 0; i < verts.length; i += 8) {
                // vertices are in blocks; the matrices work in pixels, so only the translation is scaled
                float x = verts[i];
                float y = verts[i + 1];
                float z = verts[i + 2];
                float nx = verts[i + 5];
                float ny = verts[i + 6];
                float nz = verts[i + 7];
                vc.addVertex(pose, m00 * x + m01 * y + m02 * z + m03, m10 * x + m11 * y + m12 * z + m13,
                                m20 * x + m21 * y + m22 * z + m23)
                        .setColor(255, 255, 255, 255).setUv(verts[i + 3], verts[i + 4]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(pose, m00 * nx + m01 * ny + m02 * nz, m10 * nx + m11 * ny + m12 * nz,
                                m20 * nx + m21 * ny + m22 * nz);
            }
        }
    }
}
