package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.anim.AnimModel;
import com.itmecasey.primamateria.anim.SieveAnimator;
import com.itmecasey.primamateria.anim.SieveViewSource;
import com.itmecasey.primamateria.view.SieveView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the moving parts of a sieve (tray, mesh, straps), the material layer and the finds on the mesh, from the keyframes of
 * {@code primamateria_anim/sieve.json}. The static frame (and, while the sieve rests, the tray and mesh) belong to the block
 * model; this renderer only does work when something is loaded, moving or settling. See docs/SIFTING.md sections 3, 4, 7, 8.
 *
 * <p>Detail levels follow the distance: full up to half of {@code client.animation_distance}, then bones and a flat layer sampled
 * every second frame, then the rest pose and a flat layer. {@code client.animations} reduced and off draw the rest pose only.
 */
public class SieveRenderer<T extends BlockEntity & SieveViewSource> implements BlockEntityRenderer<T, SieveRenderer.State> {
    private static final float[] SPOT_X = {5.7F, 8F, 10.3F};
    private static final float[] SPOT_Z = {6.6F, 9.4F};
    private static final float FIND_Y = 11.65F;

    /** What the frame needs, copied from the block entity during extraction. Small: the heavy state is in {@link SieveCache}. */
    public static class State extends BlockEntityRenderState {
        SieveCache cache;
        boolean draw;
        boolean bones;
        boolean mesh;
        boolean layer;
        boolean finds;
        int revealed;
        float hop = 1F;
        int hopCount;
        long seed;
        Identifier meshTexture;
    }

    public SieveRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T be, State s, float partial, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        long t0 = com.itmecasey.primamateria.Dev.ENABLED ? System.nanoTime() : 0L;
        BlockEntityRenderer.super.extractRenderState(be, s, partial, cam, crumbling);
        s.draw = false;
        extract(be, s, partial, cam);
        if (com.itmecasey.primamateria.Dev.ENABLED) {
            SieveStats.nanos += System.nanoTime() - t0;
            SieveStats.calls++;
            SieveStats.drawn += s.draw ? 1 : 0;
        }
    }

    private void extract(T be, State s, float partial, Vec3 cam) {
        Level level = be.getLevel();
        AnimModel model = SieveModels.sieve();
        if (level == null || model == null) {
            return;
        }
        SieveCache c = (SieveCache) be.renderCache();
        if (c == null || c.generation != SieveModels.generation()) {
            c = new SieveCache(model, SieveModels.generation());
            be.renderCache(c);
            c.lastNanos = System.nanoTime();
        }
        int mode = ClientConfig.mode();
        BlockState bs = be.getBlockState();
        boolean moving = bs.hasProperty(StateProps.MOVING) && bs.getValue(StateProps.MOVING);
        SieveView view = be.sieveView();
        long nowNanos = System.nanoTime();
        float dt = (nowNanos - c.lastNanos) / 1.0e9F;
        c.lastNanos = nowNanos;
        SieveAnimator a = c.animator;
        double distSq = be.getBlockPos().distToCenterSqr(cam);
        float limit = ClientConfig.distance();
        int lod = mode != ClientConfig.FULL || distSq > (double) limit * limit ? SieveAnimator.LOD_FAR
                : distSq > (double) limit * limit / 4.0 ? SieveAnimator.LOD_MID : SieveAnimator.LOD_FULL;

        boolean first = c.lastView == null;
        if (first || view != c.lastView || view.load() > 0 || a.busy()) {
            if (first || view != c.lastView) {
                sync(c, view, level);
            }
            a.update(view, level.getGameTime() + partial, dt, lod);
            c.lastView = view;
            if (a.events != 0) {
                if ((a.events & SieveAnimator.E_FINISH) != 0) {
                    double dx = cam.x - (be.getBlockPos().getX() + 0.5);
                    double dz = cam.z - (be.getBlockPos().getZ() + 0.5);
                    double len = Math.max(1e-4, Math.sqrt(dx * dx + dz * dz));
                    c.hopDx = (float) (dx / len);
                    c.hopDz = (float) (dz / len);
                }
                SieveFx.handle(level, be.getBlockPos(), c, mode, distSq);
            }
        }

        boolean loaded = view.load() > 0 || a.layerQuarters() * LayerDraw.QUARTER > 0.05F;
        boolean hopping = a.hopTime() < SieveAnimator.HOP_SECONDS && mode == ClientConfig.FULL && lod == SieveAnimator.LOD_FULL;
        s.cache = c;
        s.bones = moving;
        s.mesh = moving && !view.meshId().isEmpty();
        s.layer = loaded && c.layerTexture != null && (mode != ClientConfig.OFF || moving);
        s.revealed = view.load() > 0 && mode == ClientConfig.FULL && lod == SieveAnimator.LOD_FULL ? Math.min(a.revealedCount, view.finds().size()) : 0;
        s.finds = s.revealed > 0;
        s.hop = hopping ? a.hopTime() / SieveAnimator.HOP_SECONDS : 1F;
        s.hopCount = hopping ? Math.min(a.hopFinds().size(), SieveAnimator.MAX_FINDS) : 0;
        s.seed = hopping ? a.hopSeed() : view.seed();
        s.draw = s.bones || s.layer || s.finds || s.hopCount > 0;
        if (!s.draw) {
            return;
        }
        s.meshTexture = c.meshTexture;
        for (BoneDraw d : c.boneDraws) {
            d.light = s.lightCoords;
        }
        LayerDraw l = c.layerDraw;
        l.light = s.lightCoords;
        l.r = (c.tint >> 16) & 255;
        l.g = (c.tint >> 8) & 255;
        l.b = c.tint & 255;
        l.dome = mode == ClientConfig.FULL && lod == SieveAnimator.LOD_FULL;
        l.uOffset = (view.shakeCount() & 1) == 0 || !l.dome ? 0F : 1F;
    }

    /** Called when the server state changes: picks the textures, the material class and the item models of the finds. */
    private void sync(SieveCache c, SieveView view, Level level) {
        if (!view.loadBlock().equals(c.loadBlock)) {
            SieveFx.classify(c, view.loadBlock());
        }
        if (!view.meshTexture().equals(c.meshTextureSource)) {
            c.meshTextureSource = view.meshTexture();
            c.meshTexture = view.meshTexture().isEmpty() ? null : SieveModels.textureFile(view.meshTexture());
        }
        java.util.List<SieveView.Find> finds = view.finds();
        for (int i = 0; i < SieveAnimator.MAX_FINDS && i < finds.size(); i++) {
            String id = finds.get(i).item();
            if (!id.equals(c.itemIds[i])) {
                c.itemIds[i] = id;
                Identifier parsed = Identifier.tryParse(id);
                ItemStack stack = parsed == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(parsed));
                c.stacks[i] = stack;
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(c.items[i], stack, ItemDisplayContext.GROUND, level, null, i);
            }
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!s.draw) {
            return;
        }
        long t0 = com.itmecasey.primamateria.Dev.ENABLED ? System.nanoTime() : 0L;
        submitAll(s, pose, collector);
        if (com.itmecasey.primamateria.Dev.ENABLED) {
            SieveStats.nanos += System.nanoTime() - t0;
        }
    }

    private void submitAll(State s, PoseStack pose, SubmitNodeCollector collector) {
        SieveCache c = s.cache;
        if (s.bones) {
            for (int i = 0; i < c.boneDraws.length; i++) {
                BoneDraw d = c.boneDraws[i];
                Identifier tex = d.textureKey.equals("mesh") ? (s.mesh ? s.meshTexture : null) : c.boneTextures[i];
                if (tex != null) {
                    collector.submitCustomGeometry(pose, RenderTypes.entityCutout(tex), d);
                }
            }
        }
        if (s.layer) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(c.layerTexture), c.layerDraw);
        }
        if (s.finds) {
            for (int i = 0; i < s.revealed; i++) {
                drawFind(c, i, 1F, 0F, s, pose, collector);
            }
        }
        for (int i = 0; i < s.hopCount; i++) {
            drawFind(c, i, 1F, s.hop, s, pose, collector);
        }
    }

    /** Lays find {@code i} flat on its spot of the mesh; {@code hop} above 0 and below 1 moves it along the finish arc. */
    private void drawFind(SieveCache c, int i, float scale, float hop, State s, PoseStack pose, SubmitNodeCollector collector) {
        SieveView.Find find = c.lastView.finds().size() > i ? c.lastView.finds().get(i) : null;
        int spot = find != null ? find.spot() : i;
        if (hop > 0F && c.animator.hopFinds().size() > i) {
            spot = c.animator.hopFinds().get(i).spot();
        }
        float pop = hop > 0F ? 1F : c.animator.popIn(i);
        if (pop <= 0.01F || c.itemIds[i] == null) {
            return;
        }
        float[] mats = c.animator.mats;
        int m = c.findsBone * AnimModel.MAT;
        var mx = c.scratch;
        mx.set(mats[m], mats[m + 4], mats[m + 8], 0F, mats[m + 1], mats[m + 5], mats[m + 9], 0F, mats[m + 2], mats[m + 6], mats[m + 10], 0F,
                mats[m + 3] / 16F, mats[m + 7] / 16F, mats[m + 11] / 16F, 1F);
        float x = SPOT_X[spot % 3];
        float z = SPOT_Z[(spot / 3) % 2];
        float y = FIND_Y;
        float yaw = (float) Math.toRadians(((s.seed * 31L + i * 97L) % 360L));
        if (hop > 0F) {
            x += c.hopDx * hop * 14F;
            z += c.hopDz * hop * 14F;
            y += (float) Math.sin(Math.PI * hop) * 7F;
            yaw += hop * 6.2832F;
        }
        mx.translate(x / 16F, y / 16F, z / 16F).rotateY(yaw).rotateX(-(float) (Math.PI / 2.0)).scale(0.5F * pop * scale);
        pose.pushPose();
        pose.mulPose(mx);
        c.items[i].submit(pose, collector, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
