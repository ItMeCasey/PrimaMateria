package com.itmecasey.primamateria.sift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.view.SieveView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SiftTest {
    private static final List<DropEntry> GRAVEL = List.of(
            new DropEntry("minecraft:flint", 1, 1, 0.22, 1, 0),
            new DropEntry("primamateria:copper_dust", 1, 2, 0.14, 1, 0),
            new DropEntry("primamateria:iron_dust", 1, 1, 0.10, 2, 2),
            new DropEntry("primamateria:iron_dust", 1, 2, 0.16, 3, 0),
            new DropEntry("primamateria:gold_dust", 1, 1, 0.05, 3, 0));
    private static final List<ResidueEntry> GRAVEL_RESIDUE = List.of(new ResidueEntry("minecraft:sand", 0.15));

    @Test
    void shakesNeededDividesByMeshSpeedAndRoundsUp() {
        assertEquals(8, SiftMath.shakesNeeded(8, 1.0));
        assertEquals(7, SiftMath.shakesNeeded(8, 1.25));
        assertEquals(6, SiftMath.shakesNeeded(8, 1.5));
        assertEquals(4, SiftMath.shakesNeeded(8, 2.0));
        assertEquals(8, SiftMath.shakesNeeded(8, 0));
        assertEquals(1, SiftMath.shakesNeeded(0, 5.0));
    }

    @Test
    void comparatorSignalRunsOneToFifteen() {
        assertEquals(0, SiftMath.comparatorSignal(false, 0.7F));
        assertEquals(1, SiftMath.comparatorSignal(true, 0F));
        assertEquals(15, SiftMath.comparatorSignal(true, 1F));
        assertEquals(15, SiftMath.comparatorSignal(true, 2F));
        int last = 0;
        for (int i = 0; i <= 10; i++) {
            int s = SiftMath.comparatorSignal(true, i / 10F);
            assertTrue(s >= last);
            last = s;
        }
    }

    @Test
    void linkOffsetsFormTheSquareNearestFirst() {
        List<int[]> r1 = SiftMath.linkOffsets(1);
        assertEquals(8, r1.size());
        assertEquals(24, SiftMath.linkOffsets(2).size());
        assertEquals(48, SiftMath.linkOffsets(3).size());
        assertEquals(8, SiftMath.linkOffsets(0).size());
        assertEquals(48, SiftMath.linkOffsets(9).size());
        for (int[] o : r1) {
            assertFalse(o[0] == 0 && o[1] == 0);
        }
        List<int[]> r2 = SiftMath.linkOffsets(2);
        int last = 0;
        for (int[] o : r2) {
            int d = o[0] * o[0] + o[1] * o[1];
            assertTrue(d >= last);
            last = d;
        }
    }

    @Test
    void meshTiersGateTheDrops() {
        Random rng = new Random(1);
        Set<String> seenTier1 = new HashSet<>();
        Set<String> seenTier2 = new HashSet<>();
        Set<String> seenTier3 = new HashSet<>();
        for (int i = 0; i < 4000; i++) {
            DropRoller.roll(GRAVEL, GRAVEL_RESIDUE, 1, 1.0, rng).drops().forEach(d -> seenTier1.add(d.item()));
            DropRoller.roll(GRAVEL, GRAVEL_RESIDUE, 2, 1.0, rng).drops().forEach(d -> seenTier2.add(d.item()));
            DropRoller.roll(GRAVEL, GRAVEL_RESIDUE, 3, 1.0, rng).drops().forEach(d -> seenTier3.add(d.item()));
        }
        assertEquals(Set.of("minecraft:flint", "primamateria:copper_dust"), seenTier1);
        assertTrue(seenTier2.contains("primamateria:iron_dust"));
        assertFalse(seenTier2.contains("primamateria:gold_dust"));
        assertTrue(seenTier3.contains("primamateria:gold_dust"));
    }

    @Test
    void chancesAndCountsFollowTheTable() {
        Random rng = new Random(7);
        int n = 20000;
        int flint = 0;
        int copper = 0;
        int copperItems = 0;
        int sand = 0;
        for (int i = 0; i < n; i++) {
            DropRoller.Rolled r = DropRoller.roll(GRAVEL, GRAVEL_RESIDUE, 1, 1.0, rng);
            for (Drop d : r.drops()) {
                if (d.item().equals("minecraft:flint")) {
                    flint++;
                    assertEquals(1, d.count());
                } else if (d.item().equals("primamateria:copper_dust")) {
                    copper++;
                    copperItems += d.count();
                    assertTrue(d.count() == 1 || d.count() == 2);
                }
            }
            sand += r.residue().size();
        }
        assertEquals(0.22, flint / (double) n, 0.012);
        assertEquals(0.14, copper / (double) n, 0.012);
        assertEquals(1.5, copperItems / (double) copper, 0.06);
        assertEquals(0.15, sand / (double) n, 0.012);
    }

    @Test
    void multiplierScalesAndCapsChances() {
        Random rng = new Random(3);
        DropEntry sure = new DropEntry("minecraft:flint", 1, 1, 0.4, 1, 0);
        int hits = 0;
        for (int i = 0; i < 2000; i++) {
            hits += DropRoller.roll(List.of(sure), List.of(), 1, 5.0, rng).drops().size();
        }
        assertEquals(2000, hits);
        for (int i = 0; i < 2000; i++) {
            assertTrue(DropRoller.roll(List.of(sure), List.of(), 1, 0.0, rng).drops().isEmpty());
        }
    }

    @Test
    void sameItemFromTwoEntriesMergesIntoOneStack() {
        Random rng = new Random(0);
        List<DropEntry> both = List.of(new DropEntry("a:x", 2, 2, 1.0, 1, 0), new DropEntry("a:x", 3, 3, 1.0, 1, 0));
        DropRoller.Rolled r = DropRoller.roll(both, List.of(), 1, 1.0, rng);
        assertEquals(List.of(new Drop("a:x", 5)), r.drops());
    }

    @Test
    void sameSeedSameRolls() {
        for (long seed = 0; seed < 5; seed++) {
            assertEquals(DropRoller.roll(GRAVEL, GRAVEL_RESIDUE, 3, 1.0, new Random(seed)),
                    DropRoller.roll(GRAVEL, GRAVEL_RESIDUE, 3, 1.0, new Random(seed)));
        }
    }

    @Test
    void badEntriesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DropEntry("a:b", 0, 1, 0.5, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DropEntry("a:b", 2, 1, 0.5, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DropEntry("a:b", 1, 1, 0.5, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Drop("a:b", 0));
    }

    @Test
    void findsSpreadRevealPointsAndKeepSpotsDistinct() {
        List<Drop> drops = List.of(new Drop("a:1", 1), new Drop("a:2", 2), new Drop("a:3", 1), new Drop("a:4", 1));
        List<SieveView.Find> finds = SiftMath.finds(drops, 42L);
        assertEquals(4, finds.size());
        assertEquals(SiftMath.finds(drops, 42L), finds);
        assertNotEquals(SiftMath.finds(drops, 43L), finds);
        Set<Integer> spots = new HashSet<>();
        Set<String> items = new HashSet<>();
        float last = 0;
        for (SieveView.Find f : finds) {
            spots.add(f.spot());
            items.add(f.item());
            assertTrue(f.revealAt() >= SiftMath.REVEAL_FIRST && f.revealAt() <= SiftMath.REVEAL_LAST);
            assertTrue(f.revealAt() > last);
            last = f.revealAt();
        }
        assertEquals(4, spots.size());
        assertEquals(4, items.size());
        assertTrue(SiftMath.finds(List.of(), 1L).isEmpty());
        assertEquals(1, SiftMath.finds(List.of(new Drop("a:1", 1)), 1L).size());
        assertEquals(9, SiftMath.finds(new ArrayList<>(java.util.Collections.nCopies(9, new Drop("a:1", 1))), 1L).size());
    }

    @Test
    void aSieveNeedsAMeshAndTakesOneBlockAtATime() {
        SiftState s = new SiftState();
        DropRoller.Rolled none = new DropRoller.Rolled(List.of(), List.of());
        assertFalse(s.start("minecraft:gravel", none, 8, 1L));
        s.setMesh("primamateria:cloth_mesh", 1, 1.0);
        assertTrue(s.canAccept());
        assertTrue(s.start("minecraft:gravel", none, 8, 1L));
        assertFalse(s.canAccept());
        assertFalse(s.start("minecraft:sand", none, 8, 2L));
        assertEquals("minecraft:gravel", s.block());
    }

    @Test
    void shakingRunsToDoneThenTheSieveIsEmptyAgain() {
        SiftState s = new SiftState();
        s.setMesh("primamateria:iron_mesh", 3, 1.25);
        DropRoller.Rolled rolled = new DropRoller.Rolled(List.of(new Drop("minecraft:flint", 1)), List.of(new Drop("minecraft:sand", 1)));
        assertEquals(SiftState.Shake.NONE, s.shake(10L));
        assertTrue(s.start("minecraft:gravel", rolled, 8, 5L));
        assertEquals(7, s.needed());
        assertEquals(0F, s.progress());
        long t = 100;
        for (int i = 1; i < 7; i++) {
            assertEquals(SiftState.Shake.PROGRESS, s.shake(t += 4));
            assertEquals(i / 7F, s.progress(), 1e-6);
        }
        assertEquals(SiftState.Shake.DONE, s.shake(t += 4));
        assertEquals(1F, s.progress());
        assertEquals(List.of(new Drop("minecraft:flint", 1)), s.drops());
        assertEquals(List.of(new Drop("minecraft:sand", 1)), s.residue());
        s.clearBlock();
        assertFalse(s.filled());
        assertTrue(s.hasMesh());
        assertTrue(s.canAccept());
        assertEquals(0F, s.progress());
    }

    @Test
    void handShakesAreSpacedByTheGuard() {
        SiftState s = new SiftState();
        s.setMesh("primamateria:cloth_mesh", 1, 1.0);
        s.start("minecraft:gravel", new DropRoller.Rolled(List.of(), List.of()), 8, 1L);
        assertTrue(s.ready(100, SiftMath.HAND_MIN_TICKS));
        s.shake(100);
        assertFalse(s.ready(101, SiftMath.HAND_MIN_TICKS));
        assertFalse(s.ready(102, SiftMath.HAND_MIN_TICKS));
        assertTrue(s.ready(103, SiftMath.HAND_MIN_TICKS));
    }

    @Test
    void aSavedStateComesBackTheSame() {
        SiftState a = new SiftState();
        a.setMesh("primamateria:copper_mesh", 2, 1.0);
        a.start("minecraft:sand", new DropRoller.Rolled(List.of(new Drop("minecraft:cactus", 1), new Drop("primamateria:salt_dust", 2)),
                List.of(new Drop("primamateria:stone_meal", 1))), 8, 77L);
        a.shake(500L);
        a.shake(504L);
        SiftState b = new SiftState();
        b.restore(a.meshId(), a.meshTier(), a.meshSpeed(), a.block(), a.shakes(), a.needed(), a.seed(),
                SiftState.encode(a.drops()), SiftState.encode(a.residue()), a.lastShakeTick());
        assertEquals(a.block(), b.block());
        assertEquals(a.shakes(), b.shakes());
        assertEquals(a.needed(), b.needed());
        assertEquals(a.seed(), b.seed());
        assertEquals(a.drops(), b.drops());
        assertEquals(a.residue(), b.residue());
        assertEquals(a.lastShakeTick(), b.lastShakeTick());
        assertEquals(a.meshId(), b.meshId());
        assertEquals(a.progress(), b.progress());
    }

    @Test
    void damagedSavesDoNotCrash() {
        assertEquals(List.of(new Drop("a:b", 3)), SiftState.decode("a:b|3;junk;c:d|x;e:f|0"));
        assertTrue(SiftState.decode(null).isEmpty());
        SiftState s = new SiftState();
        s.restore("", 0, 0, "", -4, 0, 0, "|||", null, 0);
        assertFalse(s.filled());
        assertFalse(s.hasMesh());
    }

    @Test
    void removingTheMeshKeepsTheBlockButNotTheSpeed() {
        SiftState s = new SiftState();
        s.setMesh("primamateria:diamond_mesh", 4, 1.5);
        s.start("minecraft:sand", new DropRoller.Rolled(List.of(), List.of()), 8, 1L);
        s.setMesh("", 0, 5.0);
        assertFalse(s.hasMesh());
        assertEquals(1.0, s.meshSpeed());
        assertTrue(s.filled());
    }
}
