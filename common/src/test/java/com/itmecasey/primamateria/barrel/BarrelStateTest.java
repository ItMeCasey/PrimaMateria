package com.itmecasey.primamateria.barrel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.barrel.BarrelState.Finished;
import com.itmecasey.primamateria.barrel.BarrelState.GrowRule;
import com.itmecasey.primamateria.barrel.BarrelState.MixRule;
import com.itmecasey.primamateria.barrel.BarrelState.SoakRule;
import com.itmecasey.primamateria.barrel.BarrelState.SteepRule;
import com.itmecasey.primamateria.sift.Drop;
import com.itmecasey.primamateria.view.BarrelView;
import java.util.List;
import org.junit.jupiter.api.Test;

class BarrelStateTest {
    private static final String WATER = "minecraft:water";
    private static final String LAVA = "minecraft:lava";
    private static final String BRINE = "primamateria:brine";
    private static final SoakRule CLAY = new SoakRule("clay", WATER, 250, "minecraft:clay", 100);
    private static final SteepRule SALT = new SteepRule("brine", WATER, 1000, 4, BRINE, 600);
    private static final GrowRule QUARTZ = new GrowRule("quartz", BRINE, 250, "minecraft:quartz", 2, 1800, "primamateria:block/crystal_quartz");
    private static final List<MixRule> MIXES = List.of(
            new MixRule("obsidian", LAVA, WATER, 1000, "minecraft:obsidian", true),
            new MixRule("stone", WATER, LAVA, 1000, "minecraft:stone", true));

    @Test
    void fillsToCapacityAndKeepsOneFluid() {
        BarrelState b = new BarrelState(false);
        assertEquals(1000, b.fill(WATER, 1000));
        assertEquals(0, b.fill(WATER, 1));
        assertEquals(0, b.fill(BRINE, 1));
        assertEquals(BarrelView.Contents.FLUID, b.contents());
        assertEquals(WATER, b.fluid());
        assertEquals(WATER, b.drain(1000));
        assertEquals(BarrelView.Contents.EMPTY, b.contents());
        assertEquals("", b.fluid());
        assertEquals(300, b.fill(BRINE, 300));
        assertEquals(BRINE, b.fluid());
    }

    @Test
    void aWoodenBarrelRefusesHotFluidsAndAClayBarrelTakesThem() {
        BarrelState wood = new BarrelState(false);
        assertEquals(0, wood.fill(LAVA, 1000));
        assertEquals(0, wood.fill("primamateria:molten_iron", 100));
        BarrelState clay = new BarrelState(true);
        assertEquals(1000, clay.fill(LAVA, 1000));
        assertEquals(BarrelColors.fluid(LAVA), clay.color());
    }

    @Test
    void aBucketCannotTakeFluidWhileSomethingWorks() {
        BarrelState b = new BarrelState(false);
        b.fill(WATER, 1000);
        assertTrue(b.startSoak(CLAY, "minecraft:dirt", 1.0));
        assertNull(b.drain(500));
        for (int i = 0; i < 100; i++) {
            b.tick(1);
        }
        assertEquals(WATER, b.drain(500));
    }

    @Test
    void compostFillsMaturesAndBecomesDirt() {
        BarrelState b = new BarrelState(false);
        for (int i = 0; i < 9; i++) {
            assertTrue(b.addCompost(100, 0, "minecraft:dirt", "dirt", 400));
        }
        assertEquals(900, b.compost());
        assertEquals(BarrelView.Stage.COMPOSTING, b.stage());
        assertEquals(0.9F, b.stageProgress(), 1e-6);
        assertFalse(b.maturing());
        assertTrue(b.addCompost(100, 0, "minecraft:dirt", "dirt", 400));
        assertTrue(b.maturing());
        assertEquals(BarrelView.Stage.MATURING, b.stage());
        assertFalse(b.addCompost(10, 0, "minecraft:dirt", "dirt", 400));
        assertEquals(0, b.fill(WATER, 100));
        List<Finished> done = b.tick(399);
        assertTrue(done.isEmpty());
        assertEquals(BarrelColors.mix(BarrelColors.COMPOST_FRESH, BarrelColors.COMPOST_RIPE, 399 / 400F), b.color());
        done = b.tick(1);
        assertEquals(List.of(new Finished("composting", "dirt", "minecraft:dirt", 1)), done);
        assertEquals(0, b.compost());
        assertEquals(BarrelView.Stage.DONE, b.stage());
        assertEquals(new Drop("minecraft:dirt", 1), b.takeResult());
        assertEquals(BarrelView.Stage.IDLE, b.stage());
    }

    @Test
    void compostRefusesToMixWithFluid() {
        BarrelState b = new BarrelState(false);
        b.fill(WATER, 100);
        assertFalse(b.addCompost(100, 0, "minecraft:dirt", "dirt", 400));
        BarrelState c = new BarrelState(false);
        c.addCompost(100, 0, "minecraft:dirt", "dirt", 400);
        assertEquals(0, c.fill(WATER, 100));
    }

    @Test
    void boneMealHalvesMaturingOnce() {
        BarrelState b = new BarrelState(false);
        for (int i = 0; i < 9; i++) {
            b.addCompost(100, 0, "minecraft:dirt", "dirt", 400);
        }
        assertTrue(b.addCompost(100, 0.5, "minecraft:dirt", "dirt", 400));
        assertTrue(b.maturing());
        assertTrue(b.tick(199).isEmpty());
        assertEquals(1, b.tick(1).size());

        BarrelState c = new BarrelState(false);
        for (int i = 0; i < 10; i++) {
            c.addCompost(100, 0, "minecraft:dirt", "dirt", 400);
        }
        assertTrue(c.addCompost(40, 0.5, "minecraft:dirt", "dirt", 400));
        assertTrue(c.tick(199).isEmpty());
        assertEquals(1, c.tick(1).size());
        assertFalse(c.addCompost(40, 0.5, "minecraft:dirt", "dirt", 400));
    }

    @Test
    void soakingUsesFluidAndGivesAResultAfterItsTime() {
        BarrelState b = new BarrelState(false);
        b.fill(WATER, 1000);
        assertFalse(b.startSoak(new SoakRule("wrong", BRINE, 250, "x:y", 10), "minecraft:dirt", 1.0));
        assertTrue(b.startSoak(CLAY, "primamateria:stone_meal", 1.0));
        assertEquals(750, b.amount());
        assertEquals("primamateria:stone_meal", b.soakItem());
        assertFalse(b.startSoak(CLAY, "primamateria:stone_meal", 1.0));
        assertEquals(BarrelView.Stage.SOAKING, b.stage());
        assertTrue(b.tick(99).isEmpty());
        assertEquals(0.99F, b.stageProgress(), 1e-6);
        List<Finished> done = b.tick(1);
        assertEquals(List.of(new Finished("soaking", "clay", "minecraft:clay", 1)), done);
        assertEquals(new Drop("minecraft:clay", 1), b.peekResult());
        assertFalse(b.startSoak(CLAY, "primamateria:stone_meal", 1.0));
        b.takeResult();
        assertTrue(b.startSoak(CLAY, "primamateria:stone_meal", 0.5));
        assertEquals(0, b.tick(49).size());
        assertEquals(1, b.tick(1).size());
    }

    @Test
    void soakNeedsEnoughFluid() {
        BarrelState b = new BarrelState(false);
        b.fill(WATER, 200);
        assertFalse(b.canSoak(CLAY));
        b.fill(WATER, 50);
        assertTrue(b.canSoak(CLAY));
        assertTrue(b.startSoak(CLAY, "minecraft:dirt", 1.0));
        assertEquals(0, b.amount());
        assertEquals("", b.fluid());
    }

    @Test
    void steepingNeedsAllItemsThenTurnsWaterIntoBrine() {
        BarrelState b = new BarrelState(false);
        b.fill(WATER, 1000);
        for (int i = 0; i < 4; i++) {
            assertTrue(b.addSteepItem(SALT, "primamateria:salt_dust", 1.0));
            assertEquals(i < 3 ? BarrelView.Stage.STEEPING : BarrelView.Stage.STEEPING, b.stage());
        }
        assertFalse(b.addSteepItem(SALT, "primamateria:salt_dust", 1.0));
        assertNull(b.drain(1000));
        assertTrue(b.tick(599).isEmpty());
        List<Finished> done = b.tick(1);
        assertEquals(List.of(new Finished("steeping", "brine", BRINE, 1000)), done);
        assertEquals(BRINE, b.fluid());
        assertEquals(1000, b.amount());
        assertEquals(BarrelView.Stage.IDLE, b.stage());
    }

    @Test
    void steepingWaitsForMoreItemsWithoutATimer() {
        BarrelState b = new BarrelState(false);
        b.fill(WATER, 1000);
        b.addSteepItem(SALT, "primamateria:salt_dust", 1.0);
        assertTrue(b.tick(100000).isEmpty());
        assertEquals(WATER, b.fluid());
        assertFalse(b.busy());
        assertEquals(0.25F, b.stageProgress(), 1e-6);
        assertFalse(b.canSoak(CLAY));
        assertFalse(b.canPlant(QUARTZ));
    }

    @Test
    void steepingNeedsEnoughWater() {
        BarrelState b = new BarrelState(false);
        b.fill(WATER, 600);
        assertFalse(b.addSteepItem(SALT, "primamateria:salt_dust", 1.0));
    }

    @Test
    void fourCrystalsGrowAtOnceEachOnItsOwnTimer() {
        BarrelState b = new BarrelState(false);
        b.fill(BRINE, 1000);
        assertTrue(b.plantSeed(QUARTZ, 1.0));
        b.tick(900);
        assertTrue(b.plantSeed(QUARTZ, 1.0));
        assertTrue(b.plantSeed(QUARTZ, 1.0));
        assertTrue(b.plantSeed(QUARTZ, 1.0));
        assertFalse(b.plantSeed(QUARTZ, 1.0));
        assertEquals(0, b.amount());
        assertEquals(4, b.crystalCount());
        assertEquals(BarrelView.Stage.GROWING, b.stage());
        List<BarrelView.Crystal> v = b.crystalViews();
        assertEquals(0.5F, v.get(0).progress(), 1e-6);
        assertEquals(0F, v.get(1).progress(), 1e-6);
        List<Finished> done = b.tick(900);
        assertEquals(List.of(new Finished("growing", "quartz", "minecraft:quartz", 2)), done);
        assertEquals(3, b.crystalCount());
        assertEquals(1, b.crystalViews().get(0).slot() + 1);
        done = b.tick(900);
        assertEquals(3, done.size());
        assertEquals(0, b.crystalCount());
        assertEquals(new Drop("minecraft:quartz", 2), b.takeResult());
        assertEquals(new Drop("minecraft:quartz", 2), b.takeResult());
        assertEquals(new Drop("minecraft:quartz", 2), b.takeResult());
        assertEquals(new Drop("minecraft:quartz", 2), b.takeResult());
        assertNull(b.takeResult());
    }

    @Test
    void aSeedNeedsTheRightFluidAndEnoughOfIt() {
        BarrelState b = new BarrelState(false);
        b.fill(WATER, 1000);
        assertFalse(b.plantSeed(QUARTZ, 1.0));
        BarrelState c = new BarrelState(false);
        c.fill(BRINE, 200);
        assertFalse(c.plantSeed(QUARTZ, 1.0));
    }

    @Test
    void mixingNeedsTheRightOrderAndAClayBarrel() {
        BarrelState clay = new BarrelState(true);
        clay.fill(LAVA, 1000);
        assertNull(clay.mix(MIXES, BRINE, 1000));
        assertNull(clay.mix(MIXES, WATER, 500));
        Finished f = clay.mix(MIXES, WATER, 1000);
        assertEquals(new Finished("mixing", "obsidian", "minecraft:obsidian", 1), f);
        assertEquals(0, clay.amount());
        assertEquals(new Drop("minecraft:obsidian", 1), clay.takeResult());

        BarrelState clay2 = new BarrelState(true);
        clay2.fill(WATER, 1000);
        assertEquals("minecraft:stone", clay2.mix(MIXES, LAVA, 1000).result());

        BarrelState wood = new BarrelState(false);
        wood.fill(WATER, 1000);
        assertNull(wood.mix(MIXES, LAVA, 1000));
        assertEquals(1000, wood.amount());
    }

    @Test
    void dewFillsSlowlyAndRainFillsFaster() {
        BarrelState b = new BarrelState(false);
        assertEquals(0, b.dew(3, 4, false, 8));
        assertEquals(1, b.dew(1, 4, false, 8));
        assertEquals(1, b.amount());
        assertEquals(2, b.dew(1, 4, true, 8));
        assertEquals(3, b.amount());
        assertEquals(0, b.dew(1000, 0, false, 8));
        assertEquals(3, b.amount());
        BarrelState full = new BarrelState(false);
        full.fill(WATER, 1000);
        assertEquals(0, full.dew(1000, 4, true, 8));
        BarrelState brine = new BarrelState(false);
        brine.fill(BRINE, 10);
        assertEquals(0, brine.dew(1000, 4, false, 8));
        BarrelState compost = new BarrelState(false);
        compost.addCompost(50, 0, "minecraft:dirt", "dirt", 400);
        assertEquals(0, compost.dew(1000, 4, false, 8));
    }

    @Test
    void aFullBarrelFromDewTakesAboutThreeMinutes() {
        BarrelState b = new BarrelState(false);
        int ticks = 0;
        while (b.amount() < 1000 && ticks < 100000) {
            b.dew(1, 4, false, 8);
            ticks++;
        }
        assertEquals(4000, ticks);
    }

    @Test
    void resultsCanBeTakenInPartsAndStayInOrder() {
        BarrelState b = new BarrelState(true);
        b.fill(LAVA, 1000);
        b.mix(MIXES, WATER, 1000);
        BarrelState c = new BarrelState(false);
        c.fill(BRINE, 1000);
        c.plantSeed(QUARTZ, 1.0);
        c.tick(1800);
        assertEquals(new Drop("minecraft:quartz", 1), c.takeResult(1));
        assertEquals(new Drop("minecraft:quartz", 1), c.takeResult(5));
        assertNull(c.takeResult(1));
        assertNotNull(b.peekResult());
    }

    @Test
    void savedStateComesBackTheSameInEveryStage() {
        BarrelState a = new BarrelState(true);
        a.fill(WATER, 1000);
        a.addSteepItem(SALT, "primamateria:salt_dust", 0.5);
        a.addSteepItem(SALT, "primamateria:salt_dust", 0.5);
        check(a);

        a = new BarrelState(true);
        a.fill(BRINE, 1000);
        a.plantSeed(QUARTZ, 1.0);
        a.plantSeed(QUARTZ, 1.0);
        a.startSoak(new SoakRule("soul", BRINE, 250, "minecraft:soul_sand", 100), "primamateria:scoria", 1.0);
        a.tick(30);
        check(a);

        a = new BarrelState(false);
        for (int i = 0; i < 10; i++) {
            a.addCompost(100, 0.25, "minecraft:dirt", "dirt", 400);
        }
        a.tick(50);
        check(a);

        a = new BarrelState(false);
        a.fill(WATER, 300);
        a.dew(2, 4, false, 8);
        a.startSoak(CLAY, "minecraft:dirt", 1.0);
        a.tick(100);
        check(a);
    }

    private static void check(BarrelState a) {
        BarrelState b = new BarrelState(a.heatproof());
        b.load(a.save());
        assertEquals(a.save(), b.save());
        assertEquals(a.fluid(), b.fluid());
        assertEquals(a.amount(), b.amount());
        assertEquals(a.compost(), b.compost());
        assertEquals(a.stage(), b.stage());
        assertEquals(a.stageProgress(), b.stageProgress());
        assertEquals(a.pending(), b.pending());
        assertEquals(a.crystalViews(), b.crystalViews());
        assertEquals(a.soakItem(), b.soakItem());
        assertEquals(a.tick(5000).size(), b.tick(5000).size());
    }

    @Test
    void damagedSavesDoNotCrash() {
        BarrelState b = new BarrelState(false);
        b.load(null);
        b.load("");
        b.load("garbage");
        b.load("a\tb\tc\td\te\tf\tg\th\ti\tj\tk\tl\tm\tn\to");
        b.load("minecraft:water\t99999\t5\tx");
        assertTrue(b.amount() == 0 || b.compost() == 0);
        assertTrue(b.amount() <= 1000);
    }

    @Test
    void colorsAreStable() {
        assertEquals(0x3F76E4, BarrelColors.fluid(WATER));
        assertEquals(BarrelColors.fluid("some:fluid"), BarrelColors.fluid("some:fluid"));
        assertEquals(0x000000, BarrelColors.mix(0, 0xFFFFFF, 0));
        assertEquals(0xFFFFFF, BarrelColors.mix(0, 0xFFFFFF, 1));
        assertEquals(0x808080, BarrelColors.mix(0, 0xFFFFFF, 0.5F));
    }

    @Test
    void anEmptyBarrelReportsEmpty() {
        BarrelState b = new BarrelState(false);
        assertTrue(b.empty());
        b.fill(WATER, 1);
        assertFalse(b.empty());
        assertEquals(BarrelView.Stage.IDLE, b.stage());
    }
}
