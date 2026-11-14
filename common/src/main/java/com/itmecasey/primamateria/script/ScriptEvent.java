package com.itmecasey.primamateria.script;

import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.event.GameEvents.Drop;
import java.util.ArrayList;
import java.util.List;

/**
 * What a script listener receives: one object for every event, with plain getters. Fields an event does not have return an empty
 * text or 0. {@code drops()} is the live list of the event, so a script that changes it changes the real drops.
 */
public final class ScriptEvent {
    private final String name;
    private final GameEvents.Where where;
    private final String input;
    private final String result;
    private final String family;
    private final String hammerId;
    private final String meshId;
    private final int count;
    private final int amount;
    private final int tier;
    private final boolean flag;
    private final List<Drop> drops;
    private final Runnable canceler;
    private boolean canceled;

    private ScriptEvent(String name, GameEvents.Where where, String input, String result, String family, String hammerId,
                        String meshId, int count, int amount, int tier, boolean flag, List<Drop> drops, Runnable canceler) {
        this.name = name;
        this.where = where;
        this.input = input;
        this.result = result;
        this.family = family;
        this.hammerId = hammerId;
        this.meshId = meshId;
        this.count = count;
        this.amount = amount;
        this.tier = tier;
        this.flag = flag;
        this.drops = drops;
        this.canceler = canceler;
    }

    static ScriptEvent of(GameEvents.Sifted e) {
        return new ScriptEvent("sifted", e.where, e.input, "", "", "", e.meshId, e.load, 0, e.meshTier, false, e.drops, e::cancel);
    }

    static ScriptEvent of(GameEvents.Crushed e) {
        return new ScriptEvent("crushed", e.where, e.input, "", "", e.hammerId, "", 0, 0, e.hammerTier, e.byMill, e.drops, null);
    }

    static ScriptEvent of(GameEvents.BarrelFinished e) {
        return new ScriptEvent("barrelFinished", e.where(), "", e.result(), e.family(), "", "", e.count(), 0, 0, false,
                new ArrayList<>(), null);
    }

    static ScriptEvent of(GameEvents.Melted e) {
        return new ScriptEvent("melted", e.where(), e.input(), e.contentId(), "", "", "", 0, e.amount(), 0, e.isMelt(),
                new ArrayList<>(), null);
    }

    static ScriptEvent of(GameEvents.Cast e) {
        return new ScriptEvent("cast", e.where(), "", e.result(), "", "", e.meltId(), e.count(), 0, 0, false, new ArrayList<>(), null);
    }

    static ScriptEvent of(GameEvents.DriveWound e) {
        return new ScriptEvent("driveWound", e.where(), "", "", "", "", "", e.turns(), 0, 0, false, new ArrayList<>(), null);
    }

    /** The event's name: sifted, crushed, barrelFinished, melted, cast or driveWound. */
    public String name() {
        return name;
    }

    /** The world (a ServerLevel). */
    public Object level() {
        return where.level();
    }

    public int x() {
        return where.x();
    }

    public int y() {
        return where.y();
    }

    public int z() {
        return where.z();
    }

    /** The player who caused it (a ServerPlayer), or null for a machine. */
    public Object player() {
        return where.player();
    }

    /** The block or item that went in (sifted, crushed, melted). */
    public String input() {
        return input;
    }

    /** What came out (barrelFinished, cast), or the fluid or melt id (melted). */
    public String result() {
        return result;
    }

    /** barrelFinished: composting, soaking, mixing, steeping or growing. */
    public String family() {
        return family;
    }

    public String hammerId() {
        return hammerId;
    }

    /** sifted: the fitted mesh. cast: the melt id. */
    public String meshId() {
        return meshId;
    }

    /** sifted: blocks in the load. barrelFinished and cast: how many results. driveWound: the new total of turns. */
    public int count() {
        return count;
    }

    /** melted: mB made. */
    public int amount() {
        return amount;
    }

    /** sifted: mesh tier. crushed: hammer tier. */
    public int tier() {
        return tier;
    }

    /** crushed: made by an auto hammer. melted: the result is a melt rather than a fluid. */
    public boolean flag() {
        return flag;
    }

    /** The drops of sifted and crushed, as a live list of {@code {item(), count()}}. */
    public List<Drop> drops() {
        return drops;
    }

    /** Adds a drop (sifted, crushed). */
    public void addDrop(String item, int count) {
        if (count > 0 && item != null && !item.isBlank()) {
            drops.add(new Drop(item.trim(), count));
        }
    }

    /** Removes every drop of this item. */
    public void removeDrops(String item) {
        drops.removeIf(d -> d.item().equals(item));
    }

    public void clearDrops() {
        drops.clear();
    }

    /** Cancels the event where the game allows it (sifted: the load stays in the sieve). Other events ignore it. */
    public void cancel() {
        if (canceler != null) {
            canceler.run();
            canceled = true;
        }
    }

    public boolean canceled() {
        return canceled;
    }
}
