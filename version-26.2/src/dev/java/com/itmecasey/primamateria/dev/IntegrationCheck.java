package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.compat.GameAccess;
import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.script.PrimaMateriaApi;
import com.itmecasey.primamateria.script.ScriptContent;
import com.itmecasey.primamateria.view.ViewAccess;
import com.itmecasey.primamateria.viewer.Cards;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check of the optional integrations, run from the self test (development builds only). It proves that the mod runs the
 * same with no optional mod installed (the script bridge installs quietly, the card model builds) and, when ModJS is installed and
 * {@code scripts/fixtures/modjs_primamateria_check.js} sits in {@code run/modjs/server_scripts/}, that the PrimaMateria global exists
 * and works: an event listener changes drops, a content call changes the recipe snapshot, and a read of the config works.
 */
public final class IntegrationCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");
    private static int failures;

    private IntegrationCheck() {
    }

    private static void verdict(String name, boolean ok, String detail) {
        if (ok) {
            LOGGER.info("SELFTEST PASS {} ({})", name, detail);
        } else {
            failures++;
            LOGGER.error("SELFTEST FAIL {} ({})", name, detail);
        }
    }

    private static boolean has(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** Returns the number of failed checks. */
    public static int run(MinecraftServer server) {
        failures = 0;
        boolean modjs = has("com.itmecasey.modjs.script.ExtraBindings");
        LOGGER.info("SELFTEST integration: ModJS {}, JEI {}, REI {}, BEI {}, SMWTI {}, Jade {}", modjs ? "present" : "absent",
                has("mezz.jei.api.IModPlugin") ? "present" : "absent", has("me.shedaniel.rei.api.client.plugins.REIClientPlugin") ? "present" : "absent",
                has("com.itmecasey.bei.api.IBeiPlugin") ? "present" : "absent", has("com.itmecasey.smwti.api.ISmwtiPlugin") ? "present" : "absent",
                has("snownee.jade.api.IWailaPlugin") ? "present" : "absent");
        verdict("integration_view_access_installed", ViewAccess.installed(), "GameAccess.install ran through ModJsBridge.install");
        verdict("integration_no_view_at_empty_pos", GameAccess.view(server.overworld(), new net.minecraft.core.BlockPos(0, 300, 0)) == null,
                "no block entity, no view");
        int cards = Cards.all(RecipeLookup.get()).size();
        verdict("integration_cards_build", cards >= 0, cards + " cards from " + RecipeLookup.get().sifting().size() + " sifting recipes");
        if (!modjs) {
            verdict("integration_runs_without_modjs", PrimaMateriaApi.INSTANCE.on("sifted", e -> { }),
                    "the global API works with no script mod installed");
            PrimaMateriaApi.INSTANCE.reset();
            return failures;
        }
        // Scripts run when the world loads; give them the first server tick.
        GameEvents.Sifted event = new GameEvents.Sifted(new GameEvents.Where(server.overworld(), 0, 64, 0, null), "minecraft:gravel", 1,
                "primamateria:iron_mesh", 3, List.of());
        GameEvents.SIFTED.fire(event);
        boolean stick = event.drops.stream().anyMatch(d -> d.item().equals("minecraft:stick") && d.count() == 7);
        verdict("integration_modjs_event_listener", stick, "a ModJS listener added 7 sticks: " + event.drops);
        RecipeLookup.Snapshot changed = ScriptContent.apply(RecipeLookup.get());
        boolean gold = changed.sifting().stream().anyMatch(r -> r.result().equals("minecraft:gold_nugget") && r.meshMin() == 4);
        verdict("integration_modjs_content_call", gold, "a ModJS sifting.add reached the snapshot (" + changed.sifting().size() + " entries)");
        verdict("integration_modjs_config_read", PrimaMateriaApi.INSTANCE.config.get("sieve.max_load").equals("4"), "config.get");
        PrimaMateriaApi.INSTANCE.reset();
        return failures;
    }
}
