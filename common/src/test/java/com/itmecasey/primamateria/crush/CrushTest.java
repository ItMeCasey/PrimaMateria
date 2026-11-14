package com.itmecasey.primamateria.crush;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.sift.Drop;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CrushTest {
    private static final List<CrushMath.Result> LEAVES = List.of(
            new CrushMath.Result("primamateria:plant_fiber", 1, 1, 1.0),
            new CrushMath.Result("primamateria:plant_fiber", 1, 1, 0.35),
            new CrushMath.Result("minecraft:oak_sapling", 1, 1, 0.08));

    @Test
    void bonusChanceStartsAboveTierTwoAndCapsAtOne() {
        assertEquals(0.0, CrushMath.bonusChance(1, 0.10));
        assertEquals(0.0, CrushMath.bonusChance(2, 0.10));
        assertEquals(0.10, CrushMath.bonusChance(3, 0.10), 1e-9);
        assertEquals(0.20, CrushMath.bonusChance(4, 0.10), 1e-9);
        assertEquals(0.30, CrushMath.bonusChance(5, 0.10), 1e-9);
        assertEquals(0.40, CrushMath.bonusChance(6, 0.10), 1e-9);
        assertEquals(1.0, CrushMath.bonusChance(6, 0.5));
        assertEquals(0.0, CrushMath.bonusChance(6, 0.0));
    }

    @Test
    void resultsFollowChancesAndMergeSameItems() {
        Random rng = new Random(11);
        int n = 20000;
        int fiber = 0;
        int sapling = 0;
        for (int i = 0; i < n; i++) {
            for (Drop d : CrushMath.roll(LEAVES, 1, 0.10, rng)) {
                if (d.item().equals("primamateria:plant_fiber")) {
                    fiber += d.count();
                    assertTrue(d.count() <= 2);
                } else {
                    sapling += d.count();
                }
            }
        }
        assertEquals(1.35, fiber / (double) n, 0.02);
        assertEquals(0.08, sapling / (double) n, 0.01);
    }

    @Test
    void bonusAddsOneToTheFirstDroppedResultOnly() {
        Random rng = new Random(2);
        List<CrushMath.Result> one = List.of(new CrushMath.Result("minecraft:gravel", 1, 1, 1.0));
        for (int i = 0; i < 200; i++) {
            List<Drop> drops = CrushMath.roll(one, 6, 10.0, rng);
            assertEquals(List.of(new Drop("minecraft:gravel", 2)), drops);
        }
        for (int i = 0; i < 200; i++) {
            assertEquals(List.of(new Drop("minecraft:gravel", 1)), CrushMath.roll(one, 2, 10.0, rng));
        }
        List<CrushMath.Result> never = List.of(new CrushMath.Result("minecraft:gravel", 1, 1, 0.0));
        assertTrue(CrushMath.roll(never, 6, 10.0, rng).isEmpty());
    }

    @Test
    void bonusYieldMatchesTheTierOverManyBlocks() {
        Random rng = new Random(5);
        List<CrushMath.Result> one = List.of(new CrushMath.Result("minecraft:gravel", 1, 1, 1.0));
        for (int tier = 1; tier <= 6; tier++) {
            int n = 20000;
            long total = 0;
            for (int i = 0; i < n; i++) {
                total += CrushMath.roll(one, tier, 0.10, rng).get(0).count();
            }
            assertEquals(1.0 + CrushMath.bonusChance(tier, 0.10), total / (double) n, 0.02, "tier " + tier);
        }
    }

    @Test
    void badResultsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CrushMath.Result("a:b", 0, 1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new CrushMath.Result("a:b", 3, 2, 0.5));
    }

    @Test
    void autoHammerStrikesTheOldestBlockSixTimes() {
        AutoHammerState s = new AutoHammerState();
        assertFalse(s.hasBlock());
        assertNull(s.stroke(1L, 6));
        assertTrue(s.offer("minecraft:cobblestone"));
        assertTrue(s.offer("minecraft:gravel"));
        assertEquals(2, s.queued());
        assertEquals("minecraft:cobblestone", s.next());
        for (int i = 1; i < 6; i++) {
            assertNull(s.stroke(10L + i, 6));
            assertEquals(i / 6F, s.progress(), 1e-6);
        }
        assertEquals("minecraft:cobblestone", s.stroke(20L, 6));
        assertEquals(1, s.queued());
        assertEquals(0F, s.progress());
        assertEquals("minecraft:gravel", s.next());
        for (int i = 1; i < 6; i++) {
            assertNull(s.stroke(30L + i, 6));
        }
        assertEquals("minecraft:gravel", s.stroke(40L, 6));
        assertFalse(s.hasBlock());
    }

    @Test
    void theQueueHoldsSixteenAndTheStrokeGuardWorks() {
        AutoHammerState s = new AutoHammerState();
        for (int i = 0; i < AutoHammerState.CAPACITY; i++) {
            assertTrue(s.offer("minecraft:stone"));
        }
        assertFalse(s.offer("minecraft:stone"));
        assertFalse(s.canAccept());
        assertTrue(s.ready(100, 4));
        s.stroke(100, 6);
        assertEquals(AutoHammerState.CAPACITY, s.queued());
        assertFalse(s.canAccept());
        assertFalse(s.ready(102, 4));
        assertTrue(s.ready(104, 4));
        assertFalse(new AutoHammerState().ready(100, 0));
    }

    @Test
    void aSavedAutoHammerComesBack() {
        AutoHammerState a = new AutoHammerState();
        a.offer("minecraft:cobblestone");
        a.offer("minecraft:sand");
        a.stroke(50L, 6);
        a.stroke(54L, 6);
        AutoHammerState b = new AutoHammerState();
        b.restore(a.encodeQueue(), a.current(), a.strokes(), a.needed(), a.lastStrokeTick());
        assertEquals(a.queued(), b.queued());
        assertEquals(a.next(), b.next());
        assertEquals(a.progress(), b.progress());
        assertEquals(a.lastStrokeTick(), b.lastStrokeTick());
        assertEquals(a.contents(), b.contents());
        b.restore(null, null, -3, 0, 0L);
        assertFalse(b.hasBlock());
        b.clear();
        assertEquals(0, b.queued());
    }
}
