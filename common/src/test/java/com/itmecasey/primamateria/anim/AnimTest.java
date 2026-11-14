package com.itmecasey.primamateria.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.SieveView;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnimTest {
    /** A small sieve-shaped model: tray with a mesh and a layer locator, and a few clips. */
    static final String SIEVE = """
            { "format": 1, "name": "sieve",
              "textures": { "wood": "minecraft:block/oak_planks" },
              "bones": [
                { "name": "frame", "parent": "", "pivot": [8, 0, 8] },
                { "name": "tray", "parent": "frame", "pivot": [8, 9, 8],
                  "cubes": [ { "from": [4, 9, 4], "to": [12, 15, 5], "texture": "wood",
                               "uv": { "north": [0, 0, 8, 6], "up": [0, 0, 8, 1] } } ] },
                { "name": "mesh", "parent": "tray", "pivot": [8, 10, 8] },
                { "name": "layer", "parent": "tray", "pivot": [8, 10, 8] } ],
              "clips": {
                "idle": { "length": 0, "loop": "hold", "bones": {} },
                "load": { "length": 0.25, "loop": "once", "bones": { "tray": { "position": [[], [
                    { "t": 0, "v": 0 }, { "t": 0.1, "v": -0.5 }, { "t": 0.25, "v": 0 } ], []] } } },
                "finish": { "length": 0.5, "loop": "once", "bones": {} },
                "shake_hand": { "length": 0.6, "loop": "loop", "bones": { "tray": { "position": [[
                    { "t": 0, "v": -1.5, "i": "bezier", "rt": 0.1, "rv": 0, "lt": -0.1, "lv": 0 },
                    { "t": 0.3, "v": 1.5, "i": "bezier", "rt": 0.1, "rv": 0, "lt": -0.1, "lv": 0 },
                    { "t": 0.6, "v": -1.5 } ], [], []] } } } } }
            """;

    static AnimModel sieve() {
        return AnimJson.parse(SIEVE);
    }

    static SieveView view(int load, float progress, int shakes, long lastShake, DriveKind drive, long seed,
                          List<SieveView.Find> finds) {
        return new SieveView("primamateria:iron_mesh", "primamateria:block/mesh_iron", "minecraft:gravel", load, progress, shakes,
                lastShake, drive, false, seed, finds);
    }

    // ---- easing ----

    @Test
    void catmullRomPassesThroughItsEndPoints() {
        assertEquals(2F, Easing.catmullRom(0, 2, 5, 9, 0F), 1e-6);
        assertEquals(5F, Easing.catmullRom(0, 2, 5, 9, 1F), 1e-6);
        // collinear points give a straight line
        assertEquals(3.5F, Easing.catmullRom(-1, 2, 5, 8, 0.5F), 1e-5);
    }

    @Test
    void bezierWithStraightHandlesIsLinear() {
        // handles one third along the segment on the straight line
        float v = Easing.bezierSegment(0, 0, 1F / 3F, 1F / 3F, 1, 1, -1F / 3F, -1F / 3F, 0.4F);
        assertEquals(0.4F, v, 1e-3);
    }

    @Test
    void bezierHardStopIsFastInThenFlat() {
        // value 0 to 1 over one second, both handles pulled to the end: slow start, accelerating, sharp end
        float early = Easing.bezierSegment(0, 0, 0.6F, 0, 1, 1, -0.0F, 0, 0.2F);
        float late = Easing.bezierSegment(0, 0, 0.6F, 0, 1, 1, -0.0F, 0, 0.9F);
        assertTrue(early < 0.1F, "slow start " + early);
        assertTrue(late > 0.7F, "arrives " + late);
        assertEquals(0F, Easing.bezierSegment(0, 0, 0.6F, 0, 1, 1, 0, 0, 0F), 1e-4);
        assertEquals(1F, Easing.bezierSegment(0, 0, 0.6F, 0, 1, 1, 0, 0, 1F), 1e-4);
    }

    @Test
    void bezierHandlesBeyondTheSegmentAreClamped() {
        float a = Easing.bezierSegment(0, 0, 50F, 0, 1, 1, -50F, 0, 0.5F);
        assertTrue(a >= 0F && a <= 1F);
    }

    @Test
    void outBackOvershootsThenSettles() {
        float peak = 0F;
        for (int i = 0; i <= 100; i++) {
            peak = Math.max(peak, Easing.outBack(i / 100F, 1.7F));
        }
        assertTrue(peak > 1.02F && peak < 1.3F, "overshoot " + peak);
        assertEquals(1F, Easing.outBack(1F, 1.7F), 1e-6);
        assertEquals(0F, Easing.outBack(0F, 1.7F), 1e-6);
    }

    @Test
    void springConvergesWithoutOvershoot() {
        float[] s = {0F, 0F};
        float max = 0F;
        for (int i = 0; i < 200; i++) {
            Easing.spring(s, 0, 1F, 0.2F, 0.02F);
            max = Math.max(max, s[0]);
        }
        assertEquals(1F, s[0], 1e-3);
        assertTrue(max <= 1.001F);
    }

    // ---- tracks ----

    @Test
    void trackModes() {
        Track lin = new Track(List.of(Track.Key.linear(0, 0), Track.Key.linear(1, 10)));
        assertEquals(5F, lin.sample(0.5F), 1e-5);
        assertEquals(0F, lin.sample(-3F), 1e-6, "held before the start");
        assertEquals(10F, lin.sample(9F), 1e-6, "held after the end");

        Track step = new Track(List.of(new Track.Key(0, 1, Track.STEP, 0, 0, 0, 0), Track.Key.linear(1, 5)));
        assertEquals(1F, step.sample(0.99F), 1e-6);
        assertEquals(5F, step.sample(1F), 1e-6);

        Track cat = new Track(List.of(new Track.Key(0, 0, Track.CATMULLROM, 0, 0, 0, 0),
                new Track.Key(1, 1, Track.CATMULLROM, 0, 0, 0, 0), Track.Key.linear(2, 0)));
        assertEquals(1F, cat.sample(1F), 1e-5);
        assertTrue(cat.sample(0.5F) > 0.3F && cat.sample(0.5F) < 0.9F);
    }

    @Test
    void unsortedKeysAreSorted() {
        Track t = new Track(List.of(Track.Key.linear(1, 10), Track.Key.linear(0, 0)));
        assertEquals(5F, t.sample(0.5F), 1e-5);
    }

    // ---- clips ----

    @Test
    void loopWrapsAndOnceClamps() {
        AnimModel m = sieve();
        AnimClip shake = m.clip("shake_hand");
        assertEquals(0.1F, shake.wrap(0.7F), 1e-5);
        AnimClip load = m.clip("load");
        assertEquals(0.25F, load.wrap(9F), 1e-6);
        assertTrue(load.finished(0.25F));
        assertFalse(shake.finished(100F));
    }

    @Test
    void shakeSwingsBetweenItsStops() {
        AnimModel m = sieve();
        float[] pose = m.restPose().clone();
        int tray = m.boneIndex("tray");
        m.clip("shake_hand").sample(0F, pose);
        assertEquals(-1.5F, pose[tray * 9], 1e-4);
        m.clip("shake_hand").sample(0.3F, pose);
        assertEquals(1.5F, pose[tray * 9], 1e-4);
        m.clip("shake_hand").sample(0.6F, pose);
        assertEquals(-1.5F, pose[tray * 9], 1e-4);
    }

    // ---- loading ----

    @Test
    void loaderRejectsBadFiles() {
        assertThrows(IllegalArgumentException.class, () -> AnimJson.parse(SIEVE.replace("\"format\": 1", "\"format\": 2")));
        // an expression instead of a number
        assertThrows(IllegalArgumentException.class, () -> AnimJson.parse(SIEVE.replace("\"v\": -0.5", "\"v\": \"math.sin(q.anim_time)\"")));
        assertThrows(IllegalArgumentException.class, () -> AnimJson.parse(SIEVE.replace("\"i\": \"bezier\"", "\"i\": \"wobble\"")));
        assertThrows(IllegalArgumentException.class, () -> AnimJson.parse(SIEVE.replace("\"parent\": \"frame\"", "\"parent\": \"nope\"")));
        assertThrows(IllegalArgumentException.class, () -> AnimJson.parse(SIEVE.replace("\"texture\": \"wood\"", "\"texture\": \"gold\"")));
        assertThrows(IllegalArgumentException.class, () -> AnimJson.parse(SIEVE.replace("\"tray\": { \"position\"", "\"ghost\": { \"position\"")));
    }

    @Test
    void contractBonesAreChecked() {
        AnimModel m = sieve();
        m.requireBones("tray", "mesh", "layer");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> m.requireBones("strap_nw"));
        assertTrue(e.getMessage().contains("strap_nw"));
    }

    @Test
    void cubesAreBakedIntoQuads() {
        AnimModel m = sieve();
        AnimModel.Mesh[] meshes = m.meshes(m.boneIndex("tray"));
        assertEquals(1, meshes.length);
        assertEquals("wood", meshes[0].texture());
        assertEquals(6 * 4 * 8, meshes[0].verts().length);
        // north face (first quad): z is the cube's from.z in blocks, normal points to -z
        assertEquals(4F / 16F, meshes[0].verts()[2], 1e-6);
        assertEquals(-1F, meshes[0].verts()[7], 1e-6);
        // its uv comes from the file: 0..8 by 0..6 pixels of 16
        assertEquals(0F, meshes[0].verts()[3], 1e-6);
        assertEquals(0.5F, meshes[0].verts()[11], 1e-6);
    }

    // ---- bone math ----

    @Test
    void restPoseIsIdentity() {
        AnimModel m = sieve();
        float[] mats = new float[m.boneCount() * AnimModel.MAT];
        m.computeMatrices(m.restPose(), mats);
        for (int b = 0; b < m.boneCount(); b++) {
            assertEquals(3F, AnimModel.apply(mats, b, 3F, 4F, 5F, 0), 1e-5);
            assertEquals(4F, AnimModel.apply(mats, b, 3F, 4F, 5F, 1), 1e-5);
            assertEquals(5F, AnimModel.apply(mats, b, 3F, 4F, 5F, 2), 1e-5);
        }
    }

    @Test
    void rotationTurnsAroundThePivotAndChildrenFollow() {
        AnimModel m = sieve();
        float[] pose = m.restPose().clone();
        int tray = m.boneIndex("tray");
        int mesh = m.boneIndex("mesh");
        pose[tray * 9 + 5] = 90F; // z rotation of the tray about (8, 9, 8)
        float[] mats = new float[m.boneCount() * AnimModel.MAT];
        m.computeMatrices(pose, mats);
        // the point one unit right of the pivot goes to one unit above it
        assertEquals(8F, AnimModel.apply(mats, tray, 9F, 9F, 8F, 0), 1e-4);
        assertEquals(10F, AnimModel.apply(mats, tray, 9F, 9F, 8F, 1), 1e-4);
        // the mesh is a child, so it is carried along (rotated about the tray's pivot, not its own)
        assertEquals(8F, AnimModel.apply(mats, mesh, 9F, 9F, 8F, 0), 1e-4);
        assertEquals(10F, AnimModel.apply(mats, mesh, 9F, 9F, 8F, 1), 1e-4);
    }

    @Test
    void positionAddsAndScaleStretchesAboutThePivot() {
        AnimModel m = sieve();
        float[] pose = m.restPose().clone();
        int tray = m.boneIndex("tray");
        pose[tray * 9] = 1.5F;
        pose[tray * 9 + 7] = 2F;
        float[] mats = new float[m.boneCount() * AnimModel.MAT];
        m.computeMatrices(pose, mats);
        assertEquals(9.5F, AnimModel.apply(mats, tray, 8F, 9F, 8F, 0), 1e-5);
        assertEquals(9F, AnimModel.apply(mats, tray, 8F, 9F, 8F, 1), 1e-5, "the pivot stays");
        assertEquals(11F, AnimModel.apply(mats, tray, 8F, 10F, 8F, 1), 1e-5, "one above the pivot doubles");
    }

    // ---- the sieve animator ----

    @Test
    void emptySieveStaysAtRestAndIsNotBusy() {
        SieveAnimator a = new SieveAnimator(sieve());
        for (int i = 0; i < 20; i++) {
            a.update(SieveView.EMPTY, 100 + i, 0.05F, SieveAnimator.LOD_FULL);
        }
        assertFalse(a.busy());
        assertEquals(0F, a.layerQuarters(), 1e-3);
        assertEquals(0, a.events);
    }

    @Test
    void loadingPlopsTheLayerUpAndRaisesAnEvent() {
        SieveAnimator a = new SieveAnimator(sieve());
        a.update(SieveView.EMPTY, 100, 0.05F, SieveAnimator.LOD_FULL);
        a.update(view(1, 0F, 0, 0, DriveKind.HAND, 7, List.of()), 101, 0.05F, SieveAnimator.LOD_FULL);
        assertTrue((a.events & SieveAnimator.E_LOAD) != 0);
        assertEquals("load", a.currentClipName());
        float peak = 0F;
        for (int i = 0; i < 20; i++) {
            a.update(view(1, 0F, 0, 0, DriveKind.HAND, 7, List.of()), 102 + i, 0.025F, SieveAnimator.LOD_FULL);
            peak = Math.max(peak, a.layerQuarters());
            assertEquals(0, a.events & SieveAnimator.E_LOAD, "the event fires once");
        }
        assertTrue(peak > 4F, "the layer overshoots a little: " + peak);
        assertEquals(4F, a.layerQuarters(), 0.05F);
    }

    @Test
    void shakesSwitchToTheDriveClipAndFallBackToIdle() {
        SieveAnimator a = new SieveAnimator(sieve());
        long t = 100;
        a.update(view(2, 0.1F, 0, 0, DriveKind.HAND, 7, List.of()), t, 0.05F, SieveAnimator.LOD_FULL);
        // let the load clip run out
        for (int i = 0; i < 12; i++) {
            a.update(view(2, 0.1F, 0, 0, DriveKind.HAND, 7, List.of()), ++t, 0.05F, SieveAnimator.LOD_FULL);
        }
        a.update(view(2, 0.15F, 1, t, DriveKind.HAND, 7, List.of()), ++t, 0.05F, SieveAnimator.LOD_FULL);
        assertTrue((a.events & SieveAnimator.E_SHAKE) != 0);
        assertEquals("shake_hand", a.currentClipName());
        assertTrue(a.shakeVolume > 0.3F && a.shakeVolume <= 1F);
        // no more shakes for more than 10 ticks: back to idle
        for (int i = 0; i < 30; i++) {
            a.update(view(2, 0.15F, 1, 114, DriveKind.HAND, 7, List.of()), ++t, 0.05F, SieveAnimator.LOD_FULL);
        }
        assertEquals("idle", a.currentClipName());
    }

    @Test
    void machineShakesAreQuieterThanHandShakes() {
        SieveAnimator hand = new SieveAnimator(sieve());
        SieveAnimator machine = new SieveAnimator(sieve());
        hand.update(view(3, 0.1F, 0, 0, DriveKind.HAND, 1, List.of()), 100, 0.05F, SieveAnimator.LOD_FULL);
        machine.update(view(3, 0.1F, 0, 0, DriveKind.WINDUP, 1, List.of()), 100, 0.05F, SieveAnimator.LOD_FULL);
        hand.update(view(3, 0.1F, 1, 101, DriveKind.HAND, 1, List.of()), 101, 0.05F, SieveAnimator.LOD_FULL);
        machine.update(view(3, 0.1F, 1, 101, DriveKind.WINDUP, 1, List.of()), 101, 0.05F, SieveAnimator.LOD_FULL);
        assertTrue(machine.shakeVolume < hand.shakeVolume);
    }

    @Test
    void findsRevealWhenTheLayerSinksPastThem() {
        SieveAnimator a = new SieveAnimator(sieve());
        List<SieveView.Find> finds = List.of(new SieveView.Find("minecraft:iron_nugget", 1, 0.4F, 0),
                new SieveView.Find("minecraft:gold_nugget", 1, 0.8F, 3));
        a.update(view(1, 0F, 0, 0, DriveKind.HAND, 9, finds), 100, 0.05F, SieveAnimator.LOD_FULL);
        assertEquals(0, a.revealedCount);
        a.update(view(1, 0.5F, 1, 101, DriveKind.HAND, 9, finds), 101, 0.05F, SieveAnimator.LOD_FULL);
        assertTrue((a.events & SieveAnimator.E_REVEAL) != 0);
        assertEquals(1, a.revealedCount);
        assertTrue(a.popIn(0) < 1F || a.popIn(0) >= 0F);
        for (int i = 0; i < 12; i++) {
            a.update(view(1, 0.5F, 1, 101, DriveKind.HAND, 9, finds), 102 + i, 0.05F, SieveAnimator.LOD_FULL);
        }
        assertEquals(1F, a.popIn(0), 0.05F);
        a.update(view(1, 0.9F, 2, 114, DriveKind.HAND, 9, finds), 114, 0.05F, SieveAnimator.LOD_FULL);
        assertEquals(2, a.revealedCount);
    }

    @Test
    void finishRaisesAnEventAndStartsTheHopOfTheFinds() {
        SieveAnimator a = new SieveAnimator(sieve());
        List<SieveView.Find> finds = List.of(new SieveView.Find("minecraft:iron_nugget", 1, 0.4F, 0));
        a.update(view(1, 0.95F, 5, 100, DriveKind.HAND, 9, finds), 100, 0.05F, SieveAnimator.LOD_FULL);
        a.update(SieveView.EMPTY, 101, 0.05F, SieveAnimator.LOD_FULL);
        assertTrue((a.events & SieveAnimator.E_FINISH) != 0);
        assertEquals("finish", a.currentClipName());
        assertEquals(1, a.hopFinds().size());
        assertTrue(a.hopTime() < SieveAnimator.HOP_SECONDS);
        for (int i = 0; i < 12; i++) {
            a.update(SieveView.EMPTY, 102 + i, 0.05F, SieveAnimator.LOD_FULL);
        }
        assertEquals(SieveAnimator.HOP_SECONDS, a.hopTime(), 1e-6);
    }

    @Test
    void farSievesStayInTheRestPose() {
        SieveAnimator a = new SieveAnimator(sieve());
        a.update(view(2, 0.1F, 0, 0, DriveKind.HAND, 7, List.of()), 100, 0.05F, SieveAnimator.LOD_FULL);
        a.update(view(2, 0.1F, 1, 101, DriveKind.HAND, 7, List.of()), 101, 0.05F, SieveAnimator.LOD_FAR);
        int tray = a.model().boneIndex("tray");
        assertEquals(0F, a.pose[tray * 9], 1e-6);
        assertEquals(0F, a.pose[tray * 9 + 1], 1e-6);
    }

    @Test
    void theArraysAreReusedNotReallocated() {
        SieveAnimator a = new SieveAnimator(sieve());
        float[] pose = a.pose;
        float[] mats = a.mats;
        for (int i = 0; i < 50; i++) {
            a.update(view(2, 0.1F, i, 100 + i, DriveKind.HAND, 7, List.of()), 100 + i, 0.02F, SieveAnimator.LOD_FULL);
        }
        assertTrue(pose == a.pose && mats == a.mats);
        assertNotNull(a.model().clip("shake_hand"));
    }

    @Test
    void theSwingStaysInsideTheStopsWhileShaking() {
        SieveAnimator a = new SieveAnimator(sieve());
        int tray = a.model().boneIndex("tray");
        long t = 100;
        for (int i = 0; i < 200; i++) {
            a.update(view(2, 0.2F, i / 6, t, DriveKind.HAND, 7, List.of()), t, 0.016F, SieveAnimator.LOD_FULL);
            if (i % 6 == 0) {
                t += 6;
            }
            assertTrue(Math.abs(a.pose[tray * 9]) <= 1.6F, "swing " + a.pose[tray * 9]);
        }
    }
}
