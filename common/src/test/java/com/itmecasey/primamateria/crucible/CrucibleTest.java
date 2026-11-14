package com.itmecasey.primamateria.crucible;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.fluid.FluidBuckets;
import com.itmecasey.primamateria.fluid.FluidLook;
import com.itmecasey.primamateria.view.TrayView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class CrucibleTest {
    private static final Function<String, CrucibleState.Melt> MELTS = item -> switch (item) {
        case "minecraft:cobblestone" -> new CrucibleState.Melt("minecraft:lava", 200);
        case "primamateria:copper_dust" -> new CrucibleState.Melt("primamateria:molten_copper", 20);
        case "primamateria:iron_dust" -> new CrucibleState.Melt("primamateria:molten_iron", 20);
        case "minecraft:iron_ingot" -> new CrucibleState.Melt("primamateria:molten_iron", 100);
        default -> null;
    };

    private static List<CrucibleState.Done> run(CrucibleState s, int heat, int ticks) {
        List<CrucibleState.Done> done = new ArrayList<>();
        for (int i = 0; i < ticks; i++) {
            done.addAll(s.tick(heat, 2, MELTS));
        }
        return done;
    }

    @Test
    void noHeatNoMelting() {
        CrucibleState s = new CrucibleState();
        s.addSolid("minecraft:cobblestone");
        assertTrue(run(s, 0, 500).isEmpty());
        assertEquals(0, s.amount());
        assertEquals(1, s.solidCount());
    }

    @Test
    void stoneMeltsToLavaAtTwoMbPerTickPerHeat() {
        CrucibleState s = new CrucibleState();
        s.addSolid("minecraft:cobblestone");
        run(s, 1, 50);
        assertEquals(100, s.amount());
        assertEquals("minecraft:lava", s.fluid());
        assertEquals(0.5F, s.meltProgress(), 1e-6);
        List<CrucibleState.Done> done = run(s, 1, 50);
        assertEquals(1, done.size());
        assertEquals(new CrucibleState.Done("minecraft:cobblestone", "minecraft:lava", 200), done.get(0));
        assertEquals(200, s.amount());
        assertEquals(0, s.solidCount());
        assertEquals(0F, s.meltProgress());
    }

    @Test
    void moreHeatIsFasterAndNeverOvershoots() {
        CrucibleState s = new CrucibleState();
        s.addSolid("minecraft:cobblestone");
        run(s, 4, 25);
        assertEquals(200, s.amount());
        assertEquals(0, s.solidCount());
        CrucibleState t = new CrucibleState();
        t.addSolid("primamateria:copper_dust");
        run(t, 5, 3);
        assertEquals(20, t.amount());
    }

    @Test
    void oneFluidKindAtATime() {
        CrucibleState s = new CrucibleState();
        s.fill("minecraft:lava", 1000);
        assertFalse(s.canFill("primamateria:molten_iron", 100));
        s.addSolid("primamateria:iron_dust");
        run(s, 3, 100);
        assertEquals(1000, s.amount());
        assertEquals(1, s.solidCount(), "iron dust cannot melt into a crucible of lava");
        s.addSolid("minecraft:cobblestone");
        run(s, 3, 100);
        assertEquals(1200, s.amount());
        assertEquals(1, s.solidCount());
        assertEquals("primamateria:iron_dust", s.solids().get(0));
    }

    @Test
    void aSolidThatCannotMeltDoesNotBlockTheOthers() {
        CrucibleState s = new CrucibleState();
        s.addSolid("minecraft:stick");
        s.addSolid("primamateria:copper_dust");
        run(s, 1, 20);
        assertEquals(20, s.amount());
        assertEquals(List.of("minecraft:stick"), s.solids());
    }

    @Test
    void capacityStopsMeltingUntilThereIsRoom() {
        CrucibleState s = new CrucibleState();
        assertTrue(s.fill("minecraft:lava", 3900));
        s.addSolid("minecraft:cobblestone");
        run(s, 2, 200);
        assertEquals(CrucibleState.CAPACITY, s.amount());
        assertEquals(1, s.solidCount());
        assertTrue(s.drain(1000));
        run(s, 2, 200);
        assertEquals(3100, s.amount(), "3000 after the drain plus the last 100 mB of the stone");
        assertEquals(0, s.solidCount());
    }

    @Test
    void bucketsAreAllOrNothing() {
        CrucibleState s = new CrucibleState();
        assertTrue(s.fill("minecraft:lava", 1000));
        assertTrue(s.fill("minecraft:lava", 3000));
        assertFalse(s.fill("minecraft:lava", 1));
        assertFalse(s.drain(4001));
        assertTrue(s.drain(4000));
        assertEquals("", s.fluid());
        assertEquals(0, s.amount());
        assertFalse(s.drain(1));
        assertFalse(s.fill("a:b", 0));
    }

    @Test
    void theBufferHoldsFourSolids() {
        CrucibleState s = new CrucibleState();
        for (int i = 0; i < 4; i++) {
            assertTrue(s.addSolid("minecraft:cobblestone"));
        }
        assertFalse(s.canAddSolid());
        assertFalse(s.addSolid("minecraft:cobblestone"));
        assertEquals("minecraft:cobblestone", s.removeLastSolid());
        assertEquals(3, s.solidCount());
    }

    @Test
    void aPartlyMeltedSolidCannotBeTakenBack() {
        CrucibleState s = new CrucibleState();
        s.addSolid("minecraft:cobblestone");
        run(s, 1, 10);
        assertNull(s.removeLastSolid());
        s.addSolid("primamateria:copper_dust");
        assertEquals("primamateria:copper_dust", s.removeLastSolid());
    }

    @Test
    void aSavedStateComesBackTheSame() {
        CrucibleState a = new CrucibleState();
        a.addSolid("minecraft:cobblestone");
        a.addSolid("primamateria:copper_dust");
        run(a, 1, 30);
        CrucibleState b = new CrucibleState();
        b.restore(a.fluid(), a.amount(), a.encodeSolids(), a.meltLeft(), a.meltTotal(), a.meltFluid());
        assertEquals(a.fluid(), b.fluid());
        assertEquals(a.amount(), b.amount());
        assertEquals(a.solids(), b.solids());
        assertEquals(a.meltProgress(), b.meltProgress());
        run(a, 1, 200);
        run(b, 1, 200);
        assertEquals(a.amount(), b.amount());
    }

    @Test
    void damagedSavesDoNotCrash() {
        CrucibleState s = new CrucibleState();
        s.restore(null, 99999, ";;a;b;c;d;e", 5, 0, "x");
        assertEquals(0, s.amount());
        assertEquals(4, s.solidCount());
        assertEquals(0F, s.meltProgress());
        s.restore("minecraft:lava", 99999, "", -3, 0, "");
        assertEquals(CrucibleState.CAPACITY, s.amount());
    }

    @Test
    void heatRulesTakeTheHighestMatch() {
        List<HeatRule> rules = List.of(
                new HeatRule("minecraft:torch", "", Map.of(), 1),
                new HeatRule("minecraft:campfire", "", Map.of("lit", "true"), 2),
                new HeatRule("", "minecraft:fires", Map.of(), 3),
                new HeatRule("minecraft:lava", "", Map.of(), 4));
        assertEquals(2, HeatRule.best(rules, r -> r.block().equals("minecraft:campfire")));
        assertEquals(4, HeatRule.best(rules, r -> !r.block().equals("minecraft:campfire")));
        assertEquals(3, HeatRule.best(rules, r -> r.tag().equals("minecraft:fires")));
        assertEquals(0, HeatRule.best(rules, r -> false));
        assertThrows(IllegalArgumentException.class, () -> new HeatRule("", "", Map.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> new HeatRule("a:b", "c:d", Map.of(), 1));
        assertThrows(IllegalArgumentException.class, () -> new HeatRule("a:b", "", Map.of(), 0));
    }

    @Test
    void aTrayFillsCoolsAndHoldsItsCastUntilTaken() {
        TrayState t = new TrayState();
        assertTrue(t.canAccept());
        assertTrue(t.fill("primamateria:molten_iron", "minecraft:iron_ingot", 1, 60));
        assertFalse(t.canAccept());
        assertFalse(t.fill("primamateria:molten_iron", "minecraft:iron_ingot", 1, 60));
        assertEquals(TrayView.Stage.FILLING, t.stage());
        for (int i = 0; i < TrayState.FILL_TICKS; i++) {
            assertNull(t.tick());
        }
        assertEquals(TrayView.Stage.COOLING, t.stage());
        for (int i = 0; i < 59; i++) {
            assertNull(t.tick());
        }
        assertEquals(59 / 60F, t.progress(), 1e-6);
        TrayState.Cast cast = t.tick();
        assertEquals(new TrayState.Cast("primamateria:molten_iron", "minecraft:iron_ingot", 1), cast);
        assertTrue(t.done());
        assertNull(t.tick());
        assertEquals(1, t.takeCount());
        assertEquals("minecraft:iron_ingot", t.takeResult());
        assertTrue(t.canAccept());
        assertNull(t.takeResult());
    }

    @Test
    void aTraySavesAndRestores() {
        TrayState a = new TrayState();
        a.fill("primamateria:molten_gold", "minecraft:gold_ingot", 10, 60);
        for (int i = 0; i < 25; i++) {
            a.tick();
        }
        TrayState b = new TrayState();
        b.restore(a.stage().ordinal(), a.fluid(), a.result(), a.count(), a.cool(), a.ticks());
        assertEquals(a.stage(), b.stage());
        assertEquals(a.progress(), b.progress());
        for (int i = 0; i < 100; i++) {
            a.tick();
            b.tick();
        }
        assertTrue(a.done() && b.done());
        assertEquals(10, b.takeCount());
        TrayState bad = new TrayState();
        bad.restore(99, "", "", 0, 0, 0);
        assertTrue(bad.canAccept());
    }

    @Test
    void bucketsMapBothWays() {
        assertEquals("minecraft:lava", FluidBuckets.fluidOf("minecraft:lava_bucket").orElseThrow());
        assertEquals("primamateria:molten_copper_bucket", FluidBuckets.bucketOf("primamateria:molten_copper").orElseThrow());
        assertTrue(FluidBuckets.fluidOf("minecraft:bucket").isEmpty());
        assertEquals(0xFF6A00, FluidLook.color("minecraft:lava"));
        assertEquals(15, FluidLook.glow("minecraft:lava"));
        assertEquals(0, FluidLook.glow("minecraft:water"));
    }
}
