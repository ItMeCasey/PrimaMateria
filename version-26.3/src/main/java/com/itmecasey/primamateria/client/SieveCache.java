package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.anim.AnimModel;
import com.itmecasey.primamateria.anim.SieveAnimator;
import com.itmecasey.primamateria.view.SieveView;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Everything the renderer keeps for one sieve between frames. Created once per sieve, so drawing allocates nothing of ours:
 * the pose and matrices live in the animator, the geometry writers below are reused every frame.
 */
final class SieveCache {
    final SieveAnimator animator;
    final BoneDraw[] boneDraws;
    final LayerDraw layerDraw;
    final ItemStackRenderState[] items = new ItemStackRenderState[SieveAnimator.MAX_FINDS];
    final ItemStack[] stacks = new ItemStack[SieveAnimator.MAX_FINDS];
    final String[] itemIds = new String[SieveAnimator.MAX_FINDS];
    final Matrix4f scratch = new Matrix4f();
    final int trayBone;
    final int findsBone;

    final Identifier[] boneTextures;
    String meshTextureSource = "";
    Identifier meshTexture;
    long lastNanos;
    SieveView lastView;
    /** Resource generation the model was taken from: a reload makes the cache rebuild. */
    int generation;
    String loadBlock = "";
    Identifier layerTexture;
    int tint = 0xFFFFFF;
    int mapColor = 0x8a8a8a;
    int soundClass;
    boolean dry;
    float hopDx;
    float hopDz;

    SieveCache(AnimModel model, int generation) {
        this.generation = generation;
        this.animator = new SieveAnimator(model);
        this.trayBone = model.boneIndex("tray");
        this.findsBone = model.boneIndex("finds");
        java.util.LinkedHashMap<String, java.util.List<int[]>> byTexture = new java.util.LinkedHashMap<>();
        for (int b = 0; b < model.boneCount(); b++) {
            AnimModel.Mesh[] meshes = model.meshes(b);
            for (int m = 0; m < meshes.length; m++) {
                byTexture.computeIfAbsent(meshes[m].texture(), k -> new java.util.ArrayList<>()).add(new int[] {b, m});
            }
        }
        this.boneDraws = new BoneDraw[byTexture.size()];
        int i = 0;
        for (var e : byTexture.entrySet()) {
            boneDraws[i++] = new BoneDraw(model, animator.mats, e.getKey(), e.getValue());
        }
        this.boneTextures = new Identifier[boneDraws.length];
        for (int k = 0; k < boneDraws.length; k++) {
            String tid = model.textures().get(boneDraws[k].textureKey);
            boneTextures[k] = tid == null || boneDraws[k].textureKey.equals("mesh") ? null : SieveModels.textureFile(tid);
        }
        this.layerDraw = new LayerDraw(animator, model.boneIndex("layer"));
        for (int k = 0; k < items.length; k++) {
            items[k] = new ItemStackRenderState();
        }
    }
}
