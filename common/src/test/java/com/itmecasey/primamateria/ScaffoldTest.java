package com.itmecasey.primamateria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.view.SieveView;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks of the frozen scaffold: ids, names, settings and the event channels. */
class ScaffoldTest {
    @Test
    void idsAreUniqueAndComplete() {
        List<String> all = new ArrayList<>(Ids.BLOCKS);
        all.addAll(Ids.ITEMS);
        assertEquals(all.size(), new HashSet<>(all).size(), "a block and an item share an id");
        assertEquals(12, Ids.BLOCKS.size());
        assertEquals(29 + com.itmecasey.primamateria.fluid.Metals.ALL.size() - 3, Ids.ITEMS.size());
        assertEquals(Ids.HAMMERS.size(), Ids.HAMMER_DURABILITY.size());
        assertEquals(5, Ids.MESHES.size());
        assertTrue(Ids.BLOCKS.containsAll(Ids.BLOCK_ENTITIES));
        assertEquals(Ids.SOUNDS.size(), new HashSet<>(Ids.SOUNDS).size());
        for (String id : all) {
            assertTrue(id.matches("[a-z0-9_]+"), id);
        }
    }

    @Test
    void everyBlockAndItemHasANameAndAModel() throws IOException {
        String lang = resource("assets/primamateria/lang/en_us.json");
        assertNotNull(lang, "lang file missing");
        for (String id : Ids.BLOCKS) {
            assertTrue(lang.contains("\"block.primamateria." + id + "\""), "no name for block " + id);
            assertNotNull(resource("assets/primamateria/blockstates/" + id + ".json"), id);
            assertNotNull(resource("assets/primamateria/items/" + id + ".json"), id);
        }
        for (String id : Ids.ITEMS) {
            assertTrue(lang.contains("\"item.primamateria." + id + "\""), "no name for item " + id);
            assertNotNull(resource("assets/primamateria/items/" + id + ".json"), id);
        }
        for (String id : Ids.FLUIDS) {
            assertTrue(lang.contains("\"fluid.primamateria." + id + "\""), "no name for fluid " + id);
        }
        assertTrue(lang.contains("\"itemGroup.primamateria\""));
    }

    @Test
    void settingsHaveDefaultsAndReadOverrides() throws IOException {
        Settings.reset();
        assertEquals(8, Settings.getInt("sieve.shakes_per_block"));
        assertTrue(Settings.getBool("sieve.link"));
        assertEquals("preset_only", Settings.getString("start.island"));
        assertTrue(Settings.getBool("client.particles"));
        Settings.apply("sieve.link_radius", 2);
        assertEquals(2, Settings.getInt("sieve.link_radius"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Settings.apply("unknown.key", 5));
        Settings.reset();
        assertEquals(1, Settings.getInt("sieve.link_radius"));
    }

    @Test
    void eventChannelsDeliverAndSurviveABadListener() {
        List<String> seen = new ArrayList<>();
        GameEvents.SIFTED.clear();
        GameEvents.SIFTED.listen(e -> {
            throw new IllegalStateException("bad listener");
        });
        GameEvents.SIFTED.listen(e -> {
            seen.add(e.input);
            e.drops.add(new GameEvents.Drop("minecraft:flint", 1));
            e.cancel();
        });
        GameEvents.Sifted event = GameEvents.SIFTED.fire(new GameEvents.Sifted(
                new GameEvents.Where(null, 0, 64, 0, null), "minecraft:gravel", "primamateria:cloth_mesh", 1, List.of()));
        assertEquals(List.of("minecraft:gravel"), seen);
        assertEquals(1, event.drops.size());
        assertTrue(event.canceled());
        GameEvents.SIFTED.clear();
        assertFalse(GameEvents.SIFTED.hasListeners());
    }

    @Test
    void viewsStartEmpty() {
        assertFalse(SieveView.EMPTY.filled());
        assertTrue(SieveView.EMPTY.finds().isEmpty());
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = ScaffoldTest.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
