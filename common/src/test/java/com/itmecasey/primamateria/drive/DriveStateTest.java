package com.itmecasey.primamateria.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.view.DriveKind;
import org.junit.jupiter.api.Test;

class DriveStateTest {
    private static DriveState windup() {
        return new DriveState(DriveKind.WINDUP, 8, 48, 10000, 40);
    }

    private static DriveState furnace() {
        return new DriveState(DriveKind.FURNACE, 4, 48, 10000, 40);
    }

    private static DriveState powered() {
        return new DriveState(DriveKind.POWERED, 2, 48, 10000, 40);
    }

    /** Runs {@code ticks} ticks and returns the ticks (1 based) at which a shake happened. */
    private static java.util.List<Integer> run(DriveState s, int ticks, boolean running) {
        java.util.List<Integer> shakes = new java.util.ArrayList<>();
        for (int t = 1; t <= ticks; t++) {
            if (s.tick(running)) {
                shakes.add(t);
            }
        }
        return shakes;
    }

    @Test
    void windupStoresTurnsUpToTheMaximum() {
        DriveState s = windup();
        assertEquals(30, s.wind(30));
        assertEquals(18, s.wind(30));
        assertEquals(48, s.turns());
        assertEquals(0, s.wind(5));
        assertEquals(0, s.wind(-3));
        assertEquals(1F, s.charge());
        assertEquals(0, furnace().wind(5));
    }

    @Test
    void windupShakesEveryEightTicksAndSpendsOneTurnEach() {
        DriveState s = windup();
        s.wind(3);
        assertEquals(java.util.List.of(1, 9, 17), run(s, 40, true));
        assertEquals(0, s.turns());
        assertFalse(s.hasPower());
        assertTrue(run(s, 40, true).isEmpty());
    }

    @Test
    void aStoppedDriveStandsStill() {
        DriveState s = windup();
        s.wind(5);
        assertTrue(run(s, 100, false).isEmpty());
        assertEquals(5, s.turns());
        DriveState f = furnace();
        f.ignite(100);
        assertTrue(run(f, 50, false).isEmpty());
        assertEquals(100, f.burnLeft());
    }

    @Test
    void furnaceShakesEveryFourTicksAndBurnsOnlyWhileRunning() {
        DriveState s = furnace();
        assertFalse(s.hasPower());
        assertTrue(run(s, 10, true).isEmpty());
        assertTrue(s.ignite(20));
        assertFalse(s.ignite(50));
        assertEquals(java.util.List.of(1, 5, 9, 13, 17), run(s, 30, true));
        assertEquals(0, s.burnLeft());
        assertFalse(s.burning());
    }

    @Test
    void furnaceNeedsFuelToBeLitAndKeepsItsFireWhenIdle() {
        DriveState s = furnace();
        s.ignite(1600);
        run(s, 12, true);
        assertEquals(1588, s.burnLeft());
        run(s, 500, false);
        assertEquals(1588, s.burnLeft());
        assertEquals(1588 / 1600F, s.charge(), 1e-6);
        assertEquals(0, windup().burnLeft());
        assertFalse(windup().ignite(100));
    }

    @Test
    void poweredSpendsFortyPerShakeEveryTwoTicks() {
        DriveState s = powered();
        assertEquals(500, s.receive(500, false));
        assertEquals(java.util.List.of(1, 3, 5), run(s, 6, true));
        assertEquals(380, s.energy());
        assertEquals(40, s.receive(40, true) + 0 * s.energy());
        assertEquals(380, s.energy());
    }

    @Test
    void poweredStopsWhenTheEnergyRunsOutAndRestartsWhenFed() {
        DriveState s = powered();
        s.receive(79, false);
        assertEquals(1, run(s, 20, true).size());
        assertEquals(39, s.energy());
        assertFalse(s.hasPower());
        s.receive(1, false);
        assertTrue(s.hasPower());
        assertEquals(1, run(s, 4, true).size());
    }

    @Test
    void energyIsCappedAndOnlyAPoweredDriveTakesIt() {
        DriveState s = powered();
        assertEquals(10000, s.receive(50000, false));
        assertEquals(0, s.receive(1, false));
        assertEquals(0, windup().receive(100, false));
        assertEquals(0, furnace().receive(100, false));
    }

    @Test
    void chargeStepReadsFromZeroToFive() {
        DriveState s = powered();
        assertEquals(0, s.chargeStep());
        s.receive(1, false);
        assertEquals(1, s.chargeStep());
        s.receive(1999, false);
        assertEquals(1, s.chargeStep());
        s.receive(1, false);
        assertEquals(2, s.chargeStep());
        s.receive(7000, false);
        assertEquals(5, s.chargeStep());
        s.receive(5000, false);
        assertEquals(5, s.chargeStep());
        assertEquals(0, windup().chargeStep());
    }

    @Test
    void theOutputBufferBlocksTheDriveWhenItHasFewerThanThreeFreeSlots() {
        assertTrue(DriveState.blocked(0));
        assertTrue(DriveState.blocked(2));
        assertFalse(DriveState.blocked(3));
        assertFalse(DriveState.blocked(9));
    }

    @Test
    void savedValuesComeBackAndBadOnesAreClamped() {
        DriveState a = windup();
        a.wind(20);
        run(a, 3, true);
        DriveState b = windup();
        b.restore(a.turns(), a.burnLeft(), a.burnTotal(), a.energy(), a.cooldown());
        assertEquals(a.turns(), b.turns());
        assertEquals(a.cooldown(), b.cooldown());
        DriveState c = windup();
        c.restore(9999, 5, 5, 5, 999);
        assertEquals(48, c.turns());
        assertEquals(8, c.cooldown());
        assertEquals(0, c.energy());
        DriveState f = furnace();
        f.restore(0, 500, 100, 0, 0);
        assertEquals(100, f.burnLeft());
        DriveState p = powered();
        p.restore(0, 0, 0, 99999, 0);
        assertEquals(10000, p.energy());
        p.restore(0, 0, 0, -5, 0);
        assertEquals(0, p.energy());
    }

    @Test
    void anUnknownKindNeverRuns() {
        DriveState s = new DriveState(DriveKind.NONE, 2, 48, 100, 10);
        assertFalse(s.hasPower());
        assertTrue(run(s, 10, true).isEmpty());
    }
}
