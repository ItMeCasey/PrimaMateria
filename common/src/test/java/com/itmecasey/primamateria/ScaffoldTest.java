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
        assertEquals(26, Ids.ITEMS.size());
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
        for (String id : Ids.MELTS) {
            assertTrue(lang.contains("\"melt.primamateria." + id + "\""), "no name for melt " + id);
        }
        assertTrue(lang.contains("\"itemGroup.primamateria\""));
    }

    @Test
    void settingsHaveDefaultsAndReadOverrides() throws IOException {
        Settings.reset();
        assertEquals(8, Settings.getInt("sieve.shakes_per_block"));
        assertEquals(0.5, Settings.getDouble("sieve.extra_block_factor"));
        assertEquals("preset_only", Settings.getString("start.island"));
        assertTrue(Settings.getBool("client.particles"));
        Settings.load(new StringReader("sieve.max_load=2\nunknown.key=5\n"));
        assertEquals(2, Settings.getInt("sieve.max_load"));
        Settings.reset();
        assertEquals(4, Settings.getInt("sieve.max_load"));
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
                new GameEvents.Where(null, 0, 64, 0, null), "minecraft:gravel", 4, "primamateria:cloth_mesh", 1, List.of()));
        assertEquals(List.of("minecraft:gravel"), seen);
        assertEquals(1, event.drops.size());
        assertTrue(event.canceled());
        GameEvents.SIFTED.clear();
        assertFalse(GameEvents.SIFTED.hasListeners());
    }

    @Test
    void viewsStartEmpty() {
        assertEquals(0, SieveView.EMPTY.load());
        assertTrue(SieveView.EMPTY.finds().isEmpty());
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = ScaffoldTest.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
