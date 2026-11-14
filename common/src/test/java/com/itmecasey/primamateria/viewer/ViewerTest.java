package com.itmecasey.primamateria.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.data.RecipeLookup.Snapshot;
import com.itmecasey.primamateria.view.BarrelView;
import com.itmecasey.primamateria.view.CrucibleView;
import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.DriveView;
import com.itmecasey.primamateria.view.SieveView;
import com.itmecasey.primamateria.view.TrayView;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The recipe cards, the drop table page and the in-world lines, worked out without a game. */
class ViewerTest {
    private final Snapshot s = Samples.snapshot();

    @Test
    void valuesAreNumbersWithUnits() {
        assertEquals("100%", Fmt.chance(1.0));
        assertEquals("16%", Fmt.chance(0.16));
        assertEquals("4.5%", Fmt.chance(0.045));
        assertEquals("0.35%", Fmt.chance(0.0035));
        assertEquals("", Fmt.range(2, 2));
        assertEquals("1-2", Fmt.range(1, 2));
        assertEquals("20 s", Fmt.seconds(400));
        assertEquals("0.5 s", Fmt.seconds(10));
        assertEquals("T3-5", Fmt.tiers(3, 5));
        assertEquals("T2", Fmt.tiers(2, 2));
    }

    @Test
    void siftingGroupsTiersThatGiveTheSameDrops() {
        List<Cards.Card> gravel = Cards.sifting(s).stream().filter(c -> c.inputs().get(0).id().equals("minecraft:gravel")).toList();
        // flint T1+, iron T3+, gold T4-5: runs are T1-2, T3, T4-5
        assertEquals(3, gravel.size());
        assertEquals("T1-2", gravel.get(0).values().get(0));
        assertEquals("T3", gravel.get(1).values().get(0));
        assertEquals("T4-5", gravel.get(2).values().get(0));
        // first run: flint and the residue sand
        assertEquals(2, gravel.get(0).outputs().size());
        assertEquals("minecraft:flint", gravel.get(0).outputs().get(0).entry().id());
        assertEquals("50%", gravel.get(0).outputs().get(0).top());
        assertEquals("1-2", gravel.get(0).outputs().get(0).bottom());
        assertEquals("minecraft:sand", gravel.get(0).outputs().get(1).entry().id());
        // catalysts are the meshes of the run
        assertEquals(List.of("primamateria:cloth_mesh", "primamateria:copper_mesh"),
                gravel.get(0).catalysts().stream().map(Cards.Entry::id).toList());
        // last run has the rare gold drop with its small chance
        assertTrue(gravel.get(2).outputs().stream().anyMatch(o -> o.top().equals("0.35%")));
        // dirt gives the same drops at every tier: one card
        assertEquals(1, Cards.sifting(s).stream().filter(c -> c.inputs().get(0).id().equals("minecraft:dirt")).count());
    }

    @Test
    void aLongDropListContinuesOnASecondCard() {
        List<RecipeLookup.Sifting> many = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(new RecipeLookup.Sifting("x" + i, "minecraft:sand", "minecraft:item_" + i, 1, 1, 0.1, 1, 0));
        }
        Snapshot big = new Snapshot(List.of(), many, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
        List<Cards.Card> cards = Cards.sifting(big);
        assertEquals(2, cards.size());
        assertEquals(Cards.MAX_GRID_OUTPUTS, cards.get(0).outputs().size());
        assertEquals(2, cards.get(1).outputs().size());
        assertEquals("1/2", cards.get(0).values().get(1));
    }

    @Test
    void crushingNeedsAStrongEnoughHammer() {
        Cards.Card c = Cards.crushing(s).get(0);
        assertEquals("#minecraft:leaves", c.inputs().get(0).id());
        assertTrue(c.inputs().get(0).tag());
        assertEquals("primamateria:wooden_hammer", c.catalysts().get(0).id());
        assertTrue(c.catalysts().stream().anyMatch(e -> e.id().equals("primamateria:auto_hammer")));
        assertEquals("35%", c.outputs().get(1).top());
    }

    @Test
    void everyCategoryProducesCardsWithFluidsAndMelts() {
        assertEquals(1, Cards.composting(s).size());
        assertEquals("20 s", Cards.composting(s).get(0).values().get(0));
        assertEquals("20 u", Cards.composting(s).get(0).inputNotes().get(0));
        List<Cards.Card> barrel = Cards.barrel(s);
        assertEquals(4, barrel.size());
        assertEquals(Cards.Kind.FLUID, barrel.get(0).inputs().get(0).kind());
        // the heatproof mix needs the clay barrel only
        assertEquals(List.of("primamateria:clay_barrel"), barrel.get(1).catalysts().stream().map(Cards.Entry::id).toList());
        List<Cards.Card> melting = Cards.melting(s);
        assertEquals(Cards.Kind.MELT, melting.get(0).outputs().get(0).entry().kind());
        assertEquals(Cards.Kind.FLUID, melting.get(1).outputs().get(0).entry().kind());
        assertEquals("primamateria:crucible", melting.get(0).catalysts().get(0).id());
        assertEquals("minecraft:campfire", melting.get(0).catalysts().get(1).id());
        Cards.Card cast = Cards.casting(s).get(0);
        assertEquals(Cards.Kind.MELT, cast.inputs().get(0).kind());
        assertEquals("100 mB", cast.inputNotes().get(0));
        assertEquals("3 s", cast.values().get(0));
        assertEquals(Cards.all(s).size(), Cards.sifting(s).size() + 1 + 1 + 4 + 2 + 1 + 2);
    }

    @Test
    void layoutKeepsEverythingInsideTheCard() {
        for (Cards.Card c : Cards.all(s)) {
            Cards.Layout l = Cards.layout(c);
            Cards.Category cat = c.category();
            assertEquals(c.inputs().size(), l.inputs().size(), c.id());
            assertEquals(c.outputs().size(), l.outputs().size(), c.id());
            List<Cards.Spot> spots = new ArrayList<>(l.inputs());
            spots.addAll(l.outputs());
            spots.add(l.catalyst());
            for (Cards.Spot p : spots) {
                assertTrue(p.x() >= 0 && p.x() + Cards.SLOT <= cat.width && p.y() >= 0 && p.y() + Cards.SLOT <= cat.height,
                        c.id() + " slot at " + p);
            }
            for (Cards.Text t : l.texts()) {
                assertTrue(t.y() >= 0 && t.y() + 8 <= cat.height + 1, c.id() + " text '" + t.text() + "' at " + t.y());
                assertTrue(t.x() >= 0 && t.x() <= cat.width, c.id() + " text x " + t.x());
            }
        }
    }

    @Test
    void textIsOnlyValues() {
        for (Cards.Card c : Cards.all(s)) {
            List<String> texts = new ArrayList<>(c.values());
            texts.addAll(c.inputNotes());
            c.outputs().forEach(o -> {
                texts.add(o.top());
                texts.add(o.bottom());
            });
            for (String whole : texts) {
                for (String t : whole.split("[|]")) {
                    assertTrue(t.length() <= 9, "'" + t + "' is longer than a value");
                    assertFalse(t.contains(" the ") || t.endsWith("."), t);
                }
            }
        }
    }

    @Test
    void dropsCardsCarryOneChanceCellPerTier() {
        List<Cards.Card> drops = Cards.drops(s);
        assertEquals(2, drops.size());
        Cards.Card gravel = drops.get(0);
        assertEquals("T1|T2|T3|T4|T5", gravel.values().get(0));
        assertEquals("-|-|16%|16%|16%", gravel.outputs().get(1).top());
        Cards.Layout l = Cards.layout(gravel);
        assertEquals(4, l.outputs().size());
        assertTrue(l.texts().stream().anyMatch(t -> t.text().equals("T3")));
        assertTrue(l.texts().stream().anyMatch(t -> t.text().equals("16%")));
    }

    @Test
    void dropTablePageHasOneColumnPerTierAndTheResidueRow() {
        List<DropTable.Table> tables = DropTable.all(s);
        assertEquals(2, tables.size());
        DropTable.Table gravel = tables.get(0);
        assertEquals("minecraft:gravel", gravel.input());
        assertEquals(List.of(1, 2, 3, 4, 5), gravel.tiers());
        DropTable.Row iron = gravel.rows().get(1);
        assertEquals(List.of("-", "-", "16%", "16%", "16%"), iron.cells());
        DropTable.Row gold = gravel.rows().get(2);
        assertEquals(List.of("-", "-", "-", "0.35%", "0.35%"), gold.cells());
        DropTable.Row sand = gravel.rows().get(3);
        assertTrue(sand.residue());
        assertEquals("15%", sand.cells().get(0));
        String md = DropTable.markdown(gravel, id -> id);
        assertTrue(md.contains("| T1 | T2 | T3 | T4 | T5 |") || md.contains(" T1 | T2 |"));
        assertTrue(md.contains("minecraft:sand (residue)"));
    }

    @Test
    void inWorldLinesAreStateOnly() {
        SieveView sieve = new SieveView("primamateria:iron_mesh", "", "minecraft:gravel", 3, 0.5F, 4, 0, DriveKind.WINDUP, false, 1, List.of());
        List<InfoLines.Line> lines = InfoLines.sieve(sieve, id -> id);
        assertEquals("mesh", lines.get(0).label());
        assertEquals("primamateria:iron_mesh", lines.get(0).value());
        assertEquals("3 minecraft:gravel", lines.get(1).value());
        assertEquals(0.5, lines.get(2).fraction(), 1e-9);
        assertEquals("50%", lines.get(2).value());
        assertEquals("windup", lines.get(3).value());
        assertEquals("-", InfoLines.sieve(SieveView.EMPTY, id -> id).get(0).value());

        BarrelView barrel = new BarrelView(true, BarrelView.Contents.FLUID, "minecraft:water", 500, 0x3F76E4, BarrelView.Stage.SOAKING,
                0.25F, "", "", List.of(new BarrelView.Crystal("t", 0.4F, 1)));
        List<InfoLines.Line> b = InfoLines.barrel(barrel, id -> id);
        assertEquals("minecraft:water 500 mB", b.get(0).value());
        assertEquals("soaking 25%", b.get(1).value());
        assertTrue(b.stream().anyMatch(l -> l.label().equals("crystal") && l.value().equals("2 40%")));

        assertEquals("-", InfoLines.crucible(CrucibleView.EMPTY, id -> id).get(0).value());
        assertEquals("-", InfoLines.tray(TrayView.EMPTY, id -> id).get(0).value());
        assertEquals("100%", InfoLines.drive(new DriveView(DriveKind.POWERED, 1F, false, false, 0)).get(0).value());
        assertTrue(InfoLines.of("not a view", id -> id).isEmpty());
    }
}
