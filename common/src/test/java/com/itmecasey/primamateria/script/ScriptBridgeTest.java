package com.itmecasey.primamateria.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.modjs.script.ExtraBindings;
import com.itmecasey.modjs.script.ScriptType;
import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.SieveView;
import com.itmecasey.primamateria.view.ViewAccess;
import com.itmecasey.primamateria.viewer.Samples;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Runs real JavaScript (Rhino, the engine ModJS uses) against the PrimaMateria global through the reflective ModJS bridge. */
class ScriptBridgeTest {
    private static final Object LEVEL = "overworld";

    @AfterEach
    void tearDown() {
        PrimaMateriaApi.INSTANCE.reset();
        ExtraBindings.PROVIDERS.clear();
        ViewAccess.install(null);
        System.clearProperty("pm.test");
    }

    Object run(ScriptType type, String js) {
        assertTrue(ModJsBridge.install(PrimaMateriaApi.INSTANCE));
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            ExtraBindings.PROVIDERS.get(0).install(type, cx, scope);
            return cx.evaluateString(scope, js, "test.js", 1, null);
        } finally {
            Context.exit();
        }
    }

    private static GameEvents.Where where() {
        return new GameEvents.Where(LEVEL, 1, 64, -3, null);
    }

    @Test
    void siftedListenerSeesTheLoadAndCanChangeTheDrops() {
        run(ScriptType.SERVER, "PrimaMateria.on('sifted', e => {"
                + " if (e.input() == 'minecraft:gravel' && e.count() == 1 && e.tier() == 3 && e.meshId() == 'primamateria:iron_mesh') {"
                + "   e.removeDrops('minecraft:flint'); e.addDrop('minecraft:diamond', 1); } });");
        GameEvents.Sifted event = new GameEvents.Sifted(where(), "minecraft:gravel", "primamateria:iron_mesh", 3,
                List.of(new GameEvents.Drop("minecraft:flint", 2), new GameEvents.Drop("minecraft:sand", 1)));
        GameEvents.SIFTED.fire(event);
        assertEquals(List.of(new GameEvents.Drop("minecraft:sand", 1), new GameEvents.Drop("minecraft:diamond", 1)), event.drops);
    }

    @Test
    void siftedCanBeCanceledButOtherEventsIgnoreCancel() {
        run(ScriptType.SERVER, "PrimaMateria.on('sifted', e => e.cancel());"
                + "PrimaMateria.on('crushed', e => { e.cancel(); java.lang.System.setProperty('pm.test', e.hammerId() + ':' + e.tier()); });");
        GameEvents.Sifted sifted = new GameEvents.Sifted(where(), "minecraft:sand", "primamateria:cloth_mesh", 1, List.of());
        GameEvents.SIFTED.fire(sifted);
        assertTrue(sifted.canceled());
        GameEvents.CRUSHED.fire(new GameEvents.Crushed(where(), "minecraft:cobblestone", "primamateria:stone_hammer", 2, false, List.of()));
        assertEquals("primamateria:stone_hammer:2", System.getProperty("pm.test"));
    }

    @Test
    void everyEventChannelReachesScripts() {
        run(ScriptType.SERVER, "function note(t) { java.lang.System.setProperty('pm.log', java.lang.System.getProperty('pm.log', '') + t + ';'); }"
                + "PrimaMateria.on('barrelFinished', e => note(e.family() + '/' + e.result() + '/' + e.count()));"
                + "PrimaMateria.on('melted', e => note(e.input() + '/' + e.result() + '/' + e.amount() + '/' + e.flag()));"
                + "PrimaMateria.on('cast', e => note(e.meshId() + '/' + e.result() + '/' + e.count()));"
                + "PrimaMateria.on('driveWound', e => note('turns' + e.count()));"
                + "PrimaMateria.on('crushed', e => note(e.flag() + '/' + e.x() + '/' + e.name()));"
                + "PrimaMateria.on('sifted', e => note(e.level() + '/' + e.player()));");
        GameEvents.BARREL_FINISHED.fire(new GameEvents.BarrelFinished(where(), "soaking", "r", "minecraft:clay", 1));
        GameEvents.MELTED.fire(new GameEvents.Melted(where(), "primamateria:iron_dust", "primamateria:molten_iron", 20));
        GameEvents.CAST.fire(new GameEvents.Cast(where(), "primamateria:molten_iron", "minecraft:iron_ingot", 1));
        GameEvents.DRIVE_WOUND.fire(new GameEvents.DriveWound(where(), 12));
        GameEvents.CRUSHED.fire(new GameEvents.Crushed(where(), "minecraft:stone", "primamateria:iron_hammer", 4, true, List.of()));
        GameEvents.SIFTED.fire(new GameEvents.Sifted(where(), "minecraft:dirt", "", 0, List.of()));
        assertEquals("soaking/minecraft:clay/1;primamateria:iron_dust/primamateria:molten_iron/20/true;"
                + "primamateria:molten_iron/minecraft:iron_ingot/1;turns12;true/1/crushed;overworld/null;", System.getProperty("pm.log"));
        System.clearProperty("pm.log");
    }

    @Test
    void anUnknownEventNameIsRefusedWithoutAnError() {
        assertEquals(Boolean.FALSE, run(ScriptType.SERVER, "PrimaMateria.on('nothing', e => 1)"));
    }

    @Test
    void aScriptThatThrowsNeverBreaksTheGame() {
        run(ScriptType.SERVER, "PrimaMateria.on('sifted', e => { throw new java.lang.RuntimeException('boom'); });"
                + "PrimaMateria.on('sifted', e => e.addDrop('minecraft:stick', 1));");
        GameEvents.Sifted event = new GameEvents.Sifted(where(), "minecraft:dirt", "", 0, List.of());
        GameEvents.SIFTED.fire(event);
        assertEquals(1, event.drops.size(), "the second listener still ran");
    }

    @Test
    void resetRemovesOnlyScriptListeners() {
        GameEvents.SIFTED.listen(e -> System.setProperty("pm.test", "mod"));
        run(ScriptType.SERVER, "PrimaMateria.on('sifted', e => e.addDrop('minecraft:stick', 1)); PrimaMateria.reset();");
        GameEvents.Sifted event = new GameEvents.Sifted(where(), "minecraft:dirt", "", 0, List.of());
        GameEvents.SIFTED.fire(event);
        assertTrue(event.drops.isEmpty());
        assertEquals("mod", System.getProperty("pm.test"), "a listener that is not a script's stays");
        GameEvents.SIFTED.clear();
    }

    @Test
    void clientScriptsGetNoGlobal() {
        assertEquals("undefined", run(ScriptType.CLIENT, "typeof PrimaMateria"));
        ExtraBindings.PROVIDERS.clear();
        assertEquals("object", run(ScriptType.SERVER, "typeof PrimaMateria"));
    }

    @Test
    void stateReadsGoThroughTheInstalledLookup() {
        SieveView view = new SieveView("primamateria:iron_mesh", "", "minecraft:gravel", 0.25F, 1, 0, DriveKind.NONE, false, 7, List.of());
        ViewAccess.install((level, x, y, z) -> x == 1 && y == 64 && z == -3 ? view : null);
        assertEquals("minecraft:gravel", String.valueOf(run(ScriptType.SERVER, "'' + PrimaMateria.sieveAt('overworld', 1, 64, -3).block()")));
        assertEquals(Boolean.TRUE, run(ScriptType.SERVER, "PrimaMateria.sieveAt('overworld', 5, 5, 5) == null"));
        assertEquals(Boolean.TRUE, run(ScriptType.SERVER, "PrimaMateria.barrelAt('overworld', 1, 64, -3) == null"));
        assertNull(PrimaMateriaApi.INSTANCE.barrelAt(LEVEL, 1, 64, -3), "a sieve is not a barrel");
        assertNull(PrimaMateriaApi.INSTANCE.sieveAt(null, 1, 64, -3), "no level, no state");
    }

    @Test
    void stateReadsAreNullWhenNothingIsInstalled() {
        assertNull(PrimaMateriaApi.INSTANCE.sieveAt(LEVEL, 1, 64, -3));
        assertNull(PrimaMateriaApi.INSTANCE.driveAt(LEVEL, new Object()));
    }

    @Test
    void configReadsAreReadOnlyAndSafe() {
        assertEquals("1", String.valueOf(run(ScriptType.SERVER, "'' + PrimaMateria.config.get('sieve.link_radius')")));
        assertEquals("", String.valueOf(run(ScriptType.SERVER, "'' + PrimaMateria.config.get('no.such.key')")));
        assertEquals(1.0, ((Number) run(ScriptType.SERVER, "PrimaMateria.config.number('sieve.drop_multiplier')")).doubleValue(), 1e-9);
        assertEquals(Boolean.TRUE, run(ScriptType.SERVER, "PrimaMateria.config.bool('client.particles')"));
    }

    @Test
    void contentCallsChangeTheSnapshotInScriptOrder() {
        run(ScriptType.SERVER, "PrimaMateria.sifting.add({ input: 'minecraft:sand', result: 'minecraft:gold_nugget', count: [1, 2],"
                + " chance: 0.02, mesh_min: 4 });"
                + "PrimaMateria.sifting.remove({ input: 'minecraft:gravel', result: 'primamateria:iron_dust' });"
                + "PrimaMateria.sifting.replace({ result: 'minecraft:flint' }, { input: 'minecraft:gravel', result: 'minecraft:flint', chance: 0.9 });"
                + "PrimaMateria.meshes.add({ item: 'minecraft:string', tier: 1, speed: 0.5, slot: 1 });"
                + "PrimaMateria.crushing.add({ input: 'minecraft:sand', hammer_tier: 2, results: [{ item: 'minecraft:glass', chance: 0.5 }] });"
                + "PrimaMateria.melting.add({ input: 'minecraft:gold_nugget', result: { melt: 'primamateria:molten_gold' }, amount: 10 });"
                + "PrimaMateria.heat.add({ tag: 'minecraft:fire', heat: 3 });"
                + "PrimaMateria.compost.remove({ item: '#minecraft:leaves' });");
        RecipeLookup.Snapshot out = ScriptContent.apply(Samples.snapshot());
        assertEquals(4, out.sifting().size());
        assertTrue(out.sifting().stream().noneMatch(r -> r.result().equals("primamateria:iron_dust")));
        RecipeLookup.Sifting flint = out.sifting().stream().filter(r -> r.result().equals("minecraft:flint")).findFirst().orElseThrow();
        assertEquals(0.9, flint.chance(), 1e-9);
        RecipeLookup.Sifting gold = out.sifting().stream().filter(r -> r.result().equals("minecraft:gold_nugget")).findFirst().orElseThrow();
        assertEquals(1, gold.min());
        assertEquals(2, gold.max());
        assertEquals(4, gold.meshMin());
        assertEquals(6, out.meshes().size());
        assertEquals(2, out.crushing().size());
        assertEquals("primamateria:molten_gold", out.melting().get(2).fluid());
        assertEquals("#minecraft:fire", out.heat().get(1).block());
        assertTrue(out.compost().isEmpty());
        // the snapshot given in is not touched
        assertEquals(4, Samples.snapshot().sifting().size());
    }

    @Test
    void anInvalidContentCallIsRefusedAndTheRestStillWork() {
        Object ok = run(ScriptType.SERVER, "var a = PrimaMateria.sifting.add({ input: 'minecraft:sand' });"
                + "var b = PrimaMateria.sifting.add({ input: 'minecraft:sand', result: 'minecraft:stick', chance: 'lots' });"
                + "var c = PrimaMateria.sifting.remove({});"
                + "var d = PrimaMateria.sifting.add({ input: 'minecraft:sand', result: 'minecraft:stick' });"
                + "[a, b, c, d].join(',')");
        assertEquals("false,false,false,true", String.valueOf(ok));
        assertEquals(5, ScriptContent.apply(Samples.snapshot()).sifting().size());
    }

    @Test
    void resetDropsTheContentChanges() {
        run(ScriptType.SERVER, "PrimaMateria.sifting.add({ input: 'minecraft:sand', result: 'minecraft:stick' }); PrimaMateria.reset();");
        assertFalse(ScriptContent.hasChanges());
        RecipeLookup.Snapshot base = Samples.snapshot();
        assertTrue(ScriptContent.apply(base) == base);
    }
}
