package com.itmecasey.primamateria.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IslandTest {
    private static long count(IslandPlan.Kind kind) {
        return IslandPlan.parts().stream().filter(p -> p.kind() == kind).count();
    }

    @Test
    void theIslandIsSevenBySevenAndThreeDeep() {
        assertEquals(49, count(IslandPlan.Kind.GRASS));
        assertEquals(98, count(IslandPlan.Kind.DIRT));
        for (IslandPlan.Part p : IslandPlan.parts()) {
            if (p.kind() == IslandPlan.Kind.GRASS || p.kind() == IslandPlan.Kind.DIRT) {
                assertTrue(Math.abs(p.dx()) <= 3 && Math.abs(p.dz()) <= 3);
                assertTrue(p.dy() >= -2 && p.dy() <= 0);
                assertEquals(p.dy() == 0, p.kind() == IslandPlan.Kind.GRASS);
            }
        }
    }

    @Test
    void oneTreeWithAFourHighTrunkStandsOnTheGrass() {
        assertEquals(4, count(IslandPlan.Kind.LOG));
        Set<Integer> heights = new HashSet<>();
        for (IslandPlan.Part p : IslandPlan.parts()) {
            if (p.kind() == IslandPlan.Kind.LOG) {
                assertEquals(IslandPlan.TREE_OFFSET, p.dx());
                assertEquals(IslandPlan.TREE_OFFSET, p.dz());
                heights.add(p.dy());
            }
        }
        assertEquals(Set.of(1, 2, 3, 4), heights);
        assertTrue(count(IslandPlan.Kind.LEAVES) > 20);
    }

    @Test
    void noTwoPartsShareAPlaceAndTheSpawnSpotIsFree() {
        Set<String> seen = new HashSet<>();
        for (IslandPlan.Part p : IslandPlan.parts()) {
            assertTrue(seen.add(p.dx() + "," + p.dy() + "," + p.dz()), "duplicate at " + p);
        }
        int[] spawn = IslandPlan.spawnOffset();
        assertFalse(seen.contains(spawn[0] + "," + spawn[1] + "," + spawn[2]));
        assertTrue(seen.contains(spawn[0] + ",0," + spawn[2]), "grass below the spawn spot");
    }

    @Test
    void leavesTouchTheTrunk() {
        boolean touching = IslandPlan.parts().stream().anyMatch(p -> p.kind() == IslandPlan.Kind.LEAVES
                && p.dy() == 4 && Math.abs(p.dx() - IslandPlan.TREE_OFFSET) + Math.abs(p.dz() - IslandPlan.TREE_OFFSET) == 1);
        assertTrue(touching);
    }

    @Test
    void modesParseAndDecide() {
        assertEquals(StartMode.PRESET_ONLY, StartMode.parse("preset_only"));
        assertEquals(StartMode.PRESET_ONLY, StartMode.parse(" nonsense "));
        assertEquals(StartMode.PRESET_ONLY, StartMode.parse(null));
        assertEquals(StartMode.ALWAYS, StartMode.parse("ALWAYS"));
        assertEquals(StartMode.NEVER, StartMode.parse("never"));
        assertTrue(StartMode.PRESET_ONLY.shouldPlace(true, false));
        assertFalse(StartMode.PRESET_ONLY.shouldPlace(false, false));
        assertTrue(StartMode.ALWAYS.shouldPlace(false, false));
        assertFalse(StartMode.NEVER.shouldPlace(true, false));
        assertFalse(StartMode.ALWAYS.shouldPlace(true, true));
        assertFalse(StartMode.PRESET_ONLY.shouldPlace(true, true));
    }
}
