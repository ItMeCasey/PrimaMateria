package com.itmecasey.primamateria.client;

import com.itmecasey.primamateria.anim.MachineViewSource;
import com.itmecasey.primamateria.view.AutoHammerView;
import com.itmecasey.primamateria.view.BarrelView;
import com.itmecasey.primamateria.view.CrucibleView;
import com.itmecasey.primamateria.view.DriveView;
import com.itmecasey.primamateria.view.TrayView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Makes the state of the barrels, the crucible, the casting tray, the drives and the auto hammer readable from the world: the
 * fluid or compost surface at its level and color, growing crystals, what is soaking or finished, the molten level and pour, the
 * tray cooling, the windup key and its stored turns, the furnace fire, the powered drive's charge strip and the stamps. Everything
 * is a handful of boxes rebuilt into reusable buffers each frame; nothing is drawn for an empty machine. Beyond half of
 * {@code client.animation_distance} only the surface is drawn; {@code client.animations=off} draws the surface only.
 */
public class MachineRenderer<T extends BlockEntity & MachineViewSource> implements BlockEntityRenderer<T, MachineRenderer.State> {
    private static final int FULL_BRIGHT = 15728880;
    private static final float[] SLOT_X = {5.5F, 10.5F, 5.5F, 10.5F};
    private static final float[] SLOT_Z = {5.5F, 5.5F, 10.5F, 10.5F};

    enum Kind { WOODEN_BARREL, CLAY_BARREL, CRUCIBLE, CASTING_TRAY, WINDUP_DRIVE, FURNACE_DRIVE, POWERED_DRIVE, AUTO_HAMMER, NONE }

    /** Per block state kept between frames. */
    static final class Cache {
        final Kind kind;
        final QuadBuffer surface = new QuadBuffer();
        final QuadBuffer crystals = new QuadBuffer();
        final QuadBuffer brass = new QuadBuffer();
        final QuadBuffer iron = new QuadBuffer();
        final QuadBuffer wood = new QuadBuffer();
        final QuadBuffer glow = new QuadBuffer();
        final ItemStackRenderState item = new ItemStackRenderState();
        String itemId = "";
        final Matrix4f scratch = new Matrix4f();
        int generation;

        Cache(Kind kind, int generation) {
            this.kind = kind;
            this.generation = generation;
        }
    }

    public static class State extends BlockEntityRenderState {
        Cache cache;
        boolean draw;
        Identifier surfaceTexture;
        boolean translucent;
        Identifier crystalTexture;
        boolean showItem;
        float ix;
        float iy;
        float iz;
        float iscale;
        float ispin;
        float yaw;
    }

    public MachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static Kind kindOf(BlockState bs) {
        String path = BuiltInRegistries.BLOCK.getKey(bs.getBlock()).getPath();
        return switch (path) {
            case "wooden_barrel" -> Kind.WOODEN_BARREL;
            case "clay_barrel" -> Kind.CLAY_BARREL;
            case "crucible" -> Kind.CRUCIBLE;
            case "casting_tray" -> Kind.CASTING_TRAY;
            case "windup_drive" -> Kind.WINDUP_DRIVE;
            case "furnace_drive" -> Kind.FURNACE_DRIVE;
            case "powered_drive" -> Kind.POWERED_DRIVE;
            case "auto_hammer" -> Kind.AUTO_HAMMER;
            default -> Kind.NONE;
        };
    }

    @Override
    public void extractRenderState(T be, State s, float partial, Vec3 cam, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, s, partial, cam, crumbling);
        s.draw = false;
        Level level = be.getLevel();
        Object view = be.machineView();
        if (level == null || view == null) {
            return;
        }
        BlockState bs = be.getBlockState();
        Cache c = (Cache) be.renderCache();
        int gen = SieveModels.generation();
        if (c == null || c.generation != gen) {
            c = new Cache(kindOf(bs), gen);
            be.renderCache(c);
        }
        if (c.kind == Kind.NONE) {
            return;
        }
        int mode = ClientConfig.mode();
        double distSq = be.getBlockPos().distToCenterSqr(cam);
        float limit = ClientConfig.distance();
        boolean near = mode == ClientConfig.FULL && distSq < (double) limit * limit / 4.0;
        c.surface.clear();
        c.crystals.clear();
        c.brass.clear();
        c.iron.clear();
        c.wood.clear();
        c.glow.clear();
        s.cache = c;
        s.showItem = false;
        s.translucent = false;
        s.surfaceTexture = null;
        s.crystalTexture = null;
        s.yaw = 0F;
        if (bs.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            Direction d = bs.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            s.yaw = (float) Math.toRadians(180.0F - d.toYRot());
        }
        double time = level.getGameTime() + partial;
        int light = s.lightCoords;
        switch (c.kind) {
            case WOODEN_BARREL, CLAY_BARREL -> barrel(c, s, (BarrelView) view, light, near, level);
            case CRUCIBLE -> crucible(c, s, (CrucibleView) view, light, near, level);
            case CASTING_TRAY -> tray(c, s, (TrayView) view, light, near, level);
            case WINDUP_DRIVE, FURNACE_DRIVE, POWERED_DRIVE -> {
                if (mode != ClientConfig.OFF) {
                    drive(c, s, (DriveView) view, light, near, time);
                }
            }
            case AUTO_HAMMER -> {
                if (mode != ClientConfig.OFF) {
                    hammer(c, s, (AutoHammerView) view, light, near, time, level);
                }
            }
            default -> {
            }
        }
        s.draw = c.surface.count() + c.crystals.count() + c.brass.count() + c.iron.count() + c.wood.count() + c.glow.count() > 0
                || s.showItem;
    }

    // ---- surfaces ----

    private static void surface(Cache c, State s, Identifier tex, boolean translucent, int rgb, int alpha, int light, float x0, float z0,
                                float x1, float z1, float y) {
        s.surfaceTexture = tex;
        s.translucent = translucent;
        c.surface.box(x0, y - 0.01F, z0, x1, y, z1, rgb, alpha, light, true, MachineAssets.frame(tex));
    }

    private static void barrel(Cache c, State s, BarrelView v, int light, boolean near, Level level) {
        boolean clay = c.kind == Kind.CLAY_BARREL;
        float lo = clay ? 3.5F : 2.5F;
        float hi = clay ? 12.5F : 13.5F;
        float floor = 2F;
        float top = clay ? 14F : 15F;
        float y = floor + (top - floor) * Math.min(1F, v.amount() / (float) BarrelView.CAPACITY);
        if (v.amount() > 0 && v.contents() == BarrelView.Contents.FLUID) {
            Identifier tex = MachineAssets.fluid(v.contentId());
            boolean lava = tex == MachineAssets.LAVA;
            surface(c, s, tex, !lava, lava ? 0xFFFFFF : v.color(), lava ? 255 : 210, lava ? FULL_BRIGHT : light, lo, lo, hi, hi, y);
        } else if (v.amount() > 0 && v.contents() == BarrelView.Contents.COMPOST) {
            surface(c, s, MachineAssets.COMPOST, false, v.color(), 255, light, lo, lo, hi, hi, y);
        }
        if (!near) {
            return;
        }
        if (!v.crystals().isEmpty()) {
            s.crystalTexture = MachineAssets.crystal(v.crystals().get(0).texture());
            for (BarrelView.Crystal cr : v.crystals()) {
                crystal(c.crystals, SLOT_X[cr.slot() & 3], floor, SLOT_Z[cr.slot() & 3], 0.2F + 0.8F * cr.progress(), light);
            }
        }
        if (!v.result().isEmpty()) {
            item(c, s, v.result(), 8F, Math.max(y, floor) + 1.2F, 8F, 0.7F, 0F, level);
        } else if (!v.soakItem().isEmpty()) {
            float p = v.stageProgress();
            item(c, s, v.soakItem(), 8F, Math.max(y, floor) + 0.6F - 0.8F * p, 8F, 0.5F * (1F - 0.6F * p), p * 90F, level);
        }
    }

    /** Three prisms on a small base, grown from the middle of their base ({@code docs/ART.md}, crystal model). */
    private static void crystal(QuadBuffer b, float cx, float cy, float cz, float k, int light) {
        float[][] p = {{-2F, 0F, -2F, 0F, 8F, 0F}, {0F, 0F, -1F, 2F, 5F, 1F}, {-2F, 0F, 0F, 0F, 4F, 2F}};
        for (float[] q : p) {
            b.box(cx + q[0] * k, cy + q[1] * k, cz + q[2] * k, cx + q[3] * k, cy + q[4] * k, cz + q[5] * k, 0xFFFFFF, 255, light, false, 1F);
        }
    }

    private static void crucible(Cache c, State s, CrucibleView v, int light, boolean near, Level level) {
        float floor = 8F;
        float top = 14F;
        float y = floor + (top - floor) * Math.min(1F, v.amount() / (float) CrucibleView.CAPACITY);
        if (v.amount() > 0) {
            int glow = v.glow() > 0 ? FULL_BRIGHT : light;
            if (v.contentId().startsWith("primamateria:molten_")) {
                surface(c, s, MachineAssets.MELT, false, v.color(), 255, glow, 3.5F, 3.5F, 12.5F, 12.5F, y);
            } else {
                Identifier tex = MachineAssets.fluid(v.contentId());
                boolean lava = tex == MachineAssets.LAVA;
                surface(c, s, tex, !lava, lava ? 0xFFFFFF : v.color(), lava ? 255 : 210, lava ? FULL_BRIGHT : glow, 3.5F, 3.5F, 12.5F, 12.5F, y);
            }
        }
        if (!near) {
            return;
        }
        if (v.solids() > 0 && !v.solidItem().isEmpty()) {
            item(c, s, v.solidItem(), 8F, Math.max(y, floor) + 1.0F + 0.5F * v.solids(), 8F, 0.7F, 0F, level);
        }
        // pouring: a thin stream from each lip in use down to the tray below (bit = horizontal index: south, west, north, east)
        if (v.pourSides() != 0 && v.amount() > 0) {
            int rgb = v.color();
            if ((v.pourSides() & 1) != 0) {
                c.glow.box(7.5F, 3F, 14.5F, 8.5F, 15F, 15.5F, rgb, 255, FULL_BRIGHT, false, 1F);
            }
            if ((v.pourSides() & 2) != 0) {
                c.glow.box(0.5F, 3F, 7.5F, 1.5F, 15F, 8.5F, rgb, 255, FULL_BRIGHT, false, 1F);
            }
            if ((v.pourSides() & 4) != 0) {
                c.glow.box(7.5F, 3F, 0.5F, 8.5F, 15F, 1.5F, rgb, 255, FULL_BRIGHT, false, 1F);
            }
            if ((v.pourSides() & 8) != 0) {
                c.glow.box(14.5F, 3F, 7.5F, 15.5F, 15F, 8.5F, rgb, 255, FULL_BRIGHT, false, 1F);
            }
        }
    }

    private static void tray(Cache c, State s, TrayView v, int light, boolean near, Level level) {
        if (v.stage() == TrayView.Stage.EMPTY || v.fluidId().isEmpty()) {
            return;
        }
        float fill = v.stage() == TrayView.Stage.FILLING ? Math.max(0.15F, v.progress()) : 1F;
        float cool = v.stage() == TrayView.Stage.COOLING ? v.progress() : v.stage() == TrayView.Stage.DONE ? 1F : 0F;
        int rgb = mixRgb(v.color(), 0x7A7A7A, cool * 0.7F);
        int glow = cool < 0.8F ? FULL_BRIGHT : light;
        surface(c, s, MachineAssets.MELT, false, rgb, 255, glow, 2.5F, 2.5F, 13.5F, 13.5F, 2F + 2.5F * fill);
        if (near && !v.result().isEmpty() && (v.stage() == TrayView.Stage.DONE || cool > 0.6F)) {
            item(c, s, v.result(), 8F, 5.2F, 8F, 0.7F, 0F, level);
        }
    }

    // ---- drives and the auto hammer ----

    private static void drive(Cache c, State s, DriveView v, int light, boolean near, double time) {
        if (!near) {
            return;
        }
        float pulse = 0.75F + 0.25F * (float) Math.sin(time * 0.5);
        switch (c.kind) {
            case WINDUP_DRIVE -> {
                // the key turns back as the stored turns run down: 4 full turns when wound up
                float angle = -v.charge() * 1440F;
                c.iron.box(14.5F, 6.5F, 7F, 16F, 8F, 9F, 0xFFFFFF, 255, light, false, 1F, angle, 7.25F, 8F);
                c.brass.box(15F, 4F, 6.5F, 16F, 10.5F, 7.5F, 0xFFFFFF, 255, light, false, 1F, angle, 7.25F, 8F);
                c.brass.box(15F, 4F, 8.5F, 16F, 10.5F, 9.5F, 0xFFFFFF, 255, light, false, 1F, angle, 7.25F, 8F);
            }
            case FURNACE_DRIVE -> {
                if (v.charge() > 0F) {
                    int rgb = mixRgb(0xE8691C, 0xFDD97A, pulse * Math.min(1F, 0.4F + v.charge()));
                    c.glow.box(4.5F, 5F, 0.45F, 11.5F, 9F, 0.5F, rgb, 255, FULL_BRIGHT, false, 1F);
                }
            }
            default -> {
                int lit = (int) Math.ceil(Math.min(1F, v.charge()) * 5F - 1e-4);
                for (int i = 0; i < lit; i++) {
                    c.glow.box(3.5F + 2 * i, 6F, 0.5F, 5F + 2 * i, 8F, 1F, 0xFFFFFF, 255, FULL_BRIGHT, false, 1F);
                }
            }
        }
    }

    private static void hammer(Cache c, State s, AutoHammerView v, int light, boolean near, double time, Level level) {
        if (!near) {
            return;
        }
        double since = Math.max(0.0, time - v.lastShakeTick());
        float t = v.drive() == com.itmecasey.primamateria.view.DriveKind.NONE || since > 12.0 ? 1F : (float) (since / 6.0 % 1.0);
        float lift = 4F * (float) Math.sqrt(Math.min(1F, t));
        float liftB = v.drive() == com.itmecasey.primamateria.view.DriveKind.NONE || since > 12.0 ? 4F : 4F - lift;
        float[] xs = {4.5F, 9.5F};
        float[] lifts = {lift, liftB};
        for (int i = 0; i < 2; i++) {
            float x = xs[i];
            c.wood.box(x + 0.5F, 6F + lifts[i], 6F, x + 1.5F, 12F, 10F, 0xFFFFFF, 255, light, false, 1F);
            c.brass.box(x, 5F + lifts[i], 5.5F, x + 2F, 6F + lifts[i], 10.5F, 0xFFFFFF, 255, light, false, 1F);
        }
        if (!v.inputBlock().isEmpty()) {
            item(c, s, v.inputBlock(), 8F, 4.2F, 8F, 0.55F, 0F, level);
        }
    }

    private static int mixRgb(int a, int b, float t) {
        t = Math.max(0F, Math.min(1F, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    private static void item(Cache c, State s, String id, float x, float y, float z, float scale, float spin, Level level) {
        if (!id.equals(c.itemId)) {
            c.itemId = id;
            Identifier parsed = Identifier.tryParse(id);
            ItemStack stack = parsed == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(parsed));
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(c.item, stack, ItemDisplayContext.GROUND, level, null, 0);
        }
        s.showItem = !c.item.isEmpty();
        s.ix = x;
        s.iy = y;
        s.iz = z;
        s.iscale = scale;
        s.ispin = spin;
    }

    // ---- drawing ----

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!s.draw) {
            return;
        }
        Cache c = s.cache;
        pose.pushPose();
        if (s.yaw != 0F) {
            pose.mulPose(c.scratch.identity().translate(0.5F, 0F, 0.5F).rotateY(s.yaw).translate(-0.5F, 0F, -0.5F));
        }
        if (c.surface.count() > 0 && s.surfaceTexture != null) {
            collector.submitCustomGeometry(pose, s.translucent ? RenderTypes.entityTranslucent(s.surfaceTexture)
                    : RenderTypes.entityCutout(s.surfaceTexture), c.surface);
        }
        if (c.crystals.count() > 0 && s.crystalTexture != null) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(s.crystalTexture), c.crystals);
        }
        if (c.brass.count() > 0) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(MachineAssets.BRASS), c.brass);
        }
        if (c.iron.count() > 0) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(MachineAssets.IRON), c.iron);
        }
        if (c.wood.count() > 0) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(MachineAssets.WOOD), c.wood);
        }
        if (c.glow.count() > 0) {
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(c.kind == Kind.POWERED_DRIVE ? MachineAssets.AMETHYST : MachineAssets.MELT), c.glow);
        }
        if (s.showItem) {
            pose.pushPose();
            pose.mulPose(c.scratch.identity().translate(s.ix / 16F, s.iy / 16F, s.iz / 16F).rotateY((float) Math.toRadians(s.ispin))
                    .rotateX(-(float) (Math.PI / 2.0)).scale(0.5F * s.iscale));
            c.item.submit(pose, collector, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }
}
