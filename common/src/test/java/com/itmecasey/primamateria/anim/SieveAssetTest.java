package com.itmecasey.primamateria.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.SieveView;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The shipped sieve animation file, as exported from branding/models/sieve.bbmodel. */
class SieveAssetTest {
    static AnimModel load() throws Exception {
        try (Reader r = new InputStreamReader(SieveAssetTest.class.getResourceAsStream(
                "/assets/primamateria/primamateria_anim/sieve.json"), StandardCharsets.UTF_8)) {
            return AnimJson.parse(r);
        }
    }

    @Test
    void shippedFileHasTheContractBonesAndAllClips() throws Exception {
        AnimModel m = load();
        m.requireBones("frame", "tray", "strap_nw", "strap_ne", "strap_sw", "strap_se", "mesh", "layer", "finds", "chute");
        for (String clip : List.of("idle", "load", "shake_hand", "shake_windup", "shake_furnace", "finish", "mesh_swap", "blocked")) {
            assertNotNull(m.clip(clip), clip);
        }
        assertEquals(AnimClip.Loop.LOOP, m.clip("shake_hand").loop());
        assertEquals(0.6F, m.clip("shake_hand").length(), 1e-5);
        assertEquals(0.8F, m.clip("shake_windup").length(), 1e-5);
        assertEquals(0.4F, m.clip("shake_furnace").length(), 1e-5);
    }

    @Test
    void movingGeometryStaysSmallAndInsideTheBlock() throws Exception {
        AnimModel m = load();
        int cubes = 0;
        for (int b = 0; b < m.boneCount(); b++) {
            for (AnimModel.Mesh mesh : m.meshes(b)) {
                cubes += mesh.verts().length / 192;
                for (int i = 0; i < mesh.verts().length; i += 8) {
                    for (int a = 0; a < 3; a++) {
                        assertTrue(mesh.verts()[i + a] >= 0F && mesh.verts()[i + a] <= 1F);
                    }
                }
            }
        }
        assertTrue(cubes <= 40, "cubes " + cubes);
    }

    @Test
    void everyShakeClipStaysInsideTheFrameOpening() throws Exception {
        AnimModel m = load();
        int tray = m.boneIndex("tray");
        for (String name : List.of("shake_hand", "shake_windup", "shake_furnace")) {
            AnimClip c = m.clip(name);
            float[] pose = m.restPose().clone();
            for (int i = 0; i <= 400; i++) {
                c.sample(c.length() * i / 400F, pose);
                assertTrue(Math.abs(pose[tray * 9]) <= 1.25F, name + " x " + pose[tray * 9]);
                assertTrue(Math.abs(pose[tray * 9 + 2]) <= 1.0F, name + " z " + pose[tray * 9 + 2]);
            }
        }
    }

    @Test
    void animatorRunsEveryClipOfTheShippedModel() throws Exception {
        SieveAnimator a = new SieveAnimator(load());
        long t = 100;
        for (DriveKind kind : DriveKind.values()) {
            for (int i = 0; i < 60; i++) {
                SieveView v = new SieveView("primamateria:iron_mesh", "primamateria:block/mesh_iron", "minecraft:gravel", 3,
                        i / 80F, i, t, kind, i > 50 && kind == DriveKind.NONE, 5, List.of());
                a.update(v, t, 0.02F, SieveAnimator.LOD_FULL);
                t += 1;
                for (float f : a.mats) {
                    assertTrue(Float.isFinite(f));
                }
            }
        }
    }
}
