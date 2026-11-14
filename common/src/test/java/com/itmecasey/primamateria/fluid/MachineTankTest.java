package com.itmecasey.primamateria.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.barrel.BarrelState;
import com.itmecasey.primamateria.crucible.CrucibleState;
import com.itmecasey.primamateria.crucible.TrayState;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The pure adapters pipes and tank mods talk to: simulate against execute, capacity, the one-kind rule and the machines' own refusals. */
class MachineTankTest {
    private static final String WATER = "minecraft:water";
    private static final String LAVA = "minecraft:lava";
    private static final String BRINE = "primamateria:brine";
    private static final String COPPER = "primamateria:molten_copper";
    private static final Set<String> HOT = Set.of(LAVA, COPPER);

    private static BarrelTank wooden(BarrelState s) {
        return new BarrelTank(s);
    }

    // ---- barrel ----

    @Test
    void barrelSimulateAnswersWhatExecuteDoesAndChangesNothing() {
        BarrelState state = new BarrelState(false);
        BarrelTank tank = wooden(state);
        assertEquals(400, tank.insert(WATER, 400, true));
        assertEquals(0, state.amount());
        assertEquals("", tank.fluid());
        assertEquals(400, tank.insert(WATER, 400, false));
        assertEquals(400, state.amount());
        assertEquals(WATER, tank.fluid());
        assertEquals(300, tank.extract(WATER, 300, true));
        assertEquals(400, state.amount());
        assertEquals(300, tank.extract(WATER, 300, false));
        assertEquals(100, state.amount());
    }

    @Test
    void barrelStopsAtItsCapacityOfOneBucket() {
        BarrelState state = new BarrelState(false);
        BarrelTank tank = wooden(state);
        assertEquals(1000, tank.capacity(WATER));
        assertEquals(700, tank.insert(WATER, 700, false));
        assertEquals(300, tank.insert(WATER, 1000, true));
        assertEquals(300, tank.insert(WATER, 1000, false));
        assertEquals(1000, state.amount());
        assertEquals(0, tank.insert(WATER, 1, false));
        assertEquals(0, tank.insert(WATER, 1, true));
    }

    @Test
    void barrelHoldsOneKindAtATime() {
        BarrelState state = new BarrelState(false);
        BarrelTank tank = wooden(state);
        tank.insert(WATER, 200, false);
        assertFalse(tank.accepts(BRINE));
        assertEquals(0, tank.insert(BRINE, 100, true));
        assertEquals(0, tank.insert(BRINE, 100, false));
        assertEquals(WATER, tank.fluid());
        assertEquals(200, state.amount());
        assertEquals(0, tank.extract(BRINE, 100, false), "asking for another kind gets nothing");
        assertEquals(100, tank.extract("", 100, false), "an empty id means whatever is inside");
        assertEquals(100, tank.extract(null, 500, false));
        assertEquals("", tank.fluid());
        assertEquals(100, tank.insert(BRINE, 100, false), "empty again, any kind is welcome");
    }

    @Test
    void theWoodenBarrelRefusesLavaAndMoltenMetalsButTheClayBarrelTakesThem() {
        BarrelTank wood = wooden(new BarrelState(false));
        BarrelState clayState = new BarrelState(true);
        BarrelTank clay = new BarrelTank(clayState);
        for (String hot : new String[] {LAVA, COPPER, "primamateria:molten_iron", "primamateria:molten_gold"}) {
            assertFalse(wood.accepts(hot), hot);
            assertEquals(0, wood.insert(hot, 1000, false), hot);
            assertEquals(0, wood.insert(hot, 1000, true), hot);
            assertTrue(clay.accepts(hot), hot);
            assertEquals(1000, clay.insert(hot, 1000, false), hot);
            clay.extract("", 1000, false);
        }
        assertEquals(0, clayState.amount());
    }

    @Test
    void aBarrelWithCompostTakesNoFluid() {
        BarrelState state = new BarrelState(false);
        assertTrue(state.addCompost(3, 1.0, "minecraft:dirt", "test", 100));
        BarrelTank tank = wooden(state);
        assertEquals(0, tank.insert(WATER, 100, false));
        assertFalse(tank.accepts(WATER));
    }

    @Test
    void aPipeCannotDrainAWorkingBarrel() {
        BarrelState state = new BarrelState(false);
        BarrelTank tank = wooden(state);
        tank.insert(WATER, 1000, false);
        BarrelState.SoakRule rule = new BarrelState.SoakRule("soak_test", WATER, 250, "minecraft:clay", 200);
        assertTrue(state.canSoak(rule));
        assertTrue(state.startSoak(rule, "minecraft:dirt", 1.0));
        int before = state.amount();
        assertEquals(0, tank.extract(WATER, 100, true), "simulate says no too");
        assertEquals(0, tank.extract(WATER, 100, false));
        assertEquals(before, state.amount());
        assertEquals(WATER, tank.fluid());
    }

    @Test
    void aRolledBackBarrelGoesBackToWhatItHeld() {
        BarrelState state = new BarrelState(false);
        BarrelTank tank = wooden(state);
        tank.insert(WATER, 300, false);
        Object snap = tank.snapshot();
        tank.insert(WATER, 500, false);
        tank.extract(WATER, 700, false);
        tank.restore(snap);
        assertEquals(300, state.amount());
        assertEquals(WATER, state.fluid());
        Object empty = new BarrelTank(new BarrelState(false)).snapshot();
        tank.restore(empty);
        assertEquals(0, state.amount());
        assertEquals("", state.fluid());
    }

    // ---- crucible ----

    private static CrucibleTank crucible(CrucibleState s) {
        return new CrucibleTank(s, HOT::contains);
    }

    @Test
    void crucibleSimulateAnswersWhatExecuteDoesAndChangesNothing() {
        CrucibleState state = new CrucibleState();
        CrucibleTank tank = crucible(state);
        assertEquals(1500, tank.insert(COPPER, 1500, true));
        assertEquals(0, state.amount());
        assertEquals(1500, tank.insert(COPPER, 1500, false));
        assertEquals(1500, state.amount());
        assertEquals(COPPER, tank.fluid());
        assertEquals(500, tank.extract(COPPER, 500, true));
        assertEquals(1500, state.amount());
        assertEquals(500, tank.extract(COPPER, 500, false));
        assertEquals(1000, state.amount());
    }

    @Test
    void crucibleStopsAtFourBuckets() {
        CrucibleState state = new CrucibleState();
        CrucibleTank tank = crucible(state);
        assertEquals(4000, tank.capacity(LAVA));
        assertEquals(3500, tank.insert(LAVA, 3500, false));
        assertEquals(500, tank.insert(LAVA, 1000, true));
        assertEquals(500, tank.insert(LAVA, 1000, false));
        assertEquals(4000, state.amount());
        assertEquals(0, tank.insert(LAVA, 1, false));
    }

    @Test
    void crucibleTakesOnlyAllowedFluidsAndOneKindAtATime() {
        CrucibleState state = new CrucibleState();
        CrucibleTank tank = crucible(state);
        assertEquals(0, tank.insert(WATER, 1000, false), "water is no hot fluid");
        assertEquals(0, tank.insert(BRINE, 1000, true));
        assertEquals(1000, tank.insert(LAVA, 1000, false));
        assertEquals(0, tank.insert(COPPER, 1000, false), "copper into lava would mix");
        assertEquals(0, tank.insert(COPPER, 1000, true));
        assertEquals(LAVA, state.fluid());
        assertEquals(0, tank.extract(COPPER, 100, false));
        assertEquals(1000, tank.extract("", 4000, false));
        assertEquals(0, state.amount());
        assertEquals(1000, tank.insert(COPPER, 1000, false));
    }

    @Test
    void aRolledBackCrucibleGoesBackToWhatItHeld() {
        CrucibleState state = new CrucibleState();
        CrucibleTank tank = crucible(state);
        tank.insert(COPPER, 1200, false);
        Object snap = tank.snapshot();
        tank.extract("", 1200, false);
        tank.insert(LAVA, 3000, false);
        tank.restore(snap);
        assertEquals(1200, state.amount());
        assertEquals(COPPER, state.fluid());
    }

    // ---- casting tray ----

    private static final TrayTank.Plan INGOT = new TrayTank.Plan(100, "minecraft:copper_ingot", 1, 40);

    private static TrayTank tray(TrayState s) {
        return new TrayTank(s, fluid -> fluid.equals(COPPER) ? INGOT : null);
    }

    @Test
    void trayTakesExactlyOneCastAndStartsIt() {
        TrayState state = new TrayState();
        TrayTank tank = tray(state);
        assertEquals(100, tank.capacity(COPPER));
        assertEquals(100, tank.capacity(LAVA));
        assertEquals(0, tank.insert(COPPER, 99, true));
        assertEquals(0, tank.insert(COPPER, 99, false));
        assertTrue(state.canAccept());
        assertEquals(100, tank.insert(COPPER, 1000, true));
        assertTrue(state.canAccept(), "simulate changes nothing");
        assertEquals(100, tank.insert(COPPER, 1000, false));
        assertFalse(state.canAccept());
        assertEquals("minecraft:copper_ingot", state.result());
        assertEquals(COPPER, state.fluid());
    }

    @Test
    void aBusyTrayAndAnUncastableFluidAreRefused() {
        TrayState state = new TrayState();
        TrayTank tank = tray(state);
        assertEquals(0, tank.insert(LAVA, 1000, false), "no casting recipe");
        assertFalse(tank.accepts(WATER));
        tank.insert(COPPER, 100, false);
        assertFalse(tank.accepts(COPPER));
        assertEquals(0, tank.insert(COPPER, 1000, true));
        assertEquals(0, tank.insert(COPPER, 1000, false));
    }

    @Test
    void trayGivesNoFluidBackAndHoldsNone() {
        TrayState state = new TrayState();
        TrayTank tank = tray(state);
        tank.insert(COPPER, 100, false);
        assertEquals("", tank.fluid());
        assertEquals(0, tank.amount());
        assertEquals(0, tank.extract(COPPER, 1000, false));
        assertEquals(0, tank.extract("", 1000, true));
    }

    @Test
    void aRolledBackTrayIsEmptyAgain() {
        TrayState state = new TrayState();
        TrayTank tank = tray(state);
        Object snap = tank.snapshot();
        tank.insert(COPPER, 100, false);
        assertFalse(state.canAccept());
        tank.restore(snap);
        assertTrue(state.canAccept());
        assertEquals("", state.fluid());
    }
}
