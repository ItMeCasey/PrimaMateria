package com.itmecasey.primamateria.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The mod's gameplay events. Workstream A fires them on the server thread; workstream D (script bridge, recipe viewers) listens.
 * Plain Java, no loader event bus. A listener that throws is logged once per event and skipped, so it can never break gameplay.
 *
 * <p>World and player are passed as {@code Object} (a ServerLevel and a ServerPlayer or null) because this package has no game
 * classes; positions are block coordinates; ids are full strings such as {@code minecraft:gravel}.
 */
public final class GameEvents {
    public static final Channel<Sifted> SIFTED = new Channel<>("sifted");
    public static final Channel<Crushed> CRUSHED = new Channel<>("crushed");
    public static final Channel<BarrelFinished> BARREL_FINISHED = new Channel<>("barrelFinished");
    public static final Channel<Melted> MELTED = new Channel<>("melted");
    public static final Channel<Cast> CAST = new Channel<>("cast");
    public static final Channel<DriveWound> DRIVE_WOUND = new Channel<>("driveWound");

    private GameEvents() {
    }

    /** Where and by whom. {@code player} is null for machines. */
    public record Where(Object level, int x, int y, int z, Object player) {
    }

    /** One stack of a result: full item id and count. */
    public record Drop(String item, int count) {
    }

    /**
     * A sieve load is about to hand out its drops. Listeners may change {@link #drops} or cancel; a canceled load stays in the
     * sieve unprocessed.
     */
    public static final class Sifted {
        public final Where where;
        public final String input;
        public final int load;
        public final String meshId;
        public final int meshTier;
        public final List<Drop> drops;
        private boolean canceled;

        public Sifted(Where where, String input, int load, String meshId, int meshTier, List<Drop> drops) {
            this.where = where;
            this.input = input;
            this.load = load;
            this.meshId = meshId;
            this.meshTier = meshTier;
            this.drops = new ArrayList<>(drops);
        }

        public void cancel() {
            canceled = true;
        }

        public boolean canceled() {
            return canceled;
        }
    }

    /** A block was crushed by a hammer (by hand or in an auto hammer). Listeners may change {@link #drops}. */
    public static final class Crushed {
        public final Where where;
        public final String input;
        public final String hammerId;
        public final int hammerTier;
        public final boolean byMill;
        public final List<Drop> drops;

        public Crushed(Where where, String input, String hammerId, int hammerTier, boolean byMill, List<Drop> drops) {
            this.where = where;
            this.input = input;
            this.hammerId = hammerId;
            this.hammerTier = hammerTier;
            this.byMill = byMill;
            this.drops = new ArrayList<>(drops);
        }
    }

    /** A barrel finished a recipe. {@code family} is the recipe type path: composting, soaking, mixing, steeping or growing. */
    public record BarrelFinished(Where where, String family, String recipeId, String result, int count) {
    }

    /** A crucible melted one solid item into {@code amount} mB of {@code contentId}. */
    public record Melted(Where where, String input, String contentId, boolean isMelt, int amount) {
    }

    /** A casting tray finished cooling. */
    public record Cast(Where where, String meltId, String result, int count) {
    }

    /** A player cranked a windup drive; {@code turns} is the new total. */
    public record DriveWound(Where where, int turns) {
    }

    /** A list of listeners for one event type. */
    public static final class Channel<E> {
        private final String name;
        private final List<Consumer<? super E>> listeners = new CopyOnWriteArrayList<>();

        Channel(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public void listen(Consumer<? super E> listener) {
            listeners.add(listener);
        }

        public void remove(Consumer<? super E> listener) {
            listeners.remove(listener);
        }

        /** Removes every listener (script reload, tests). */
        public void clear() {
            listeners.clear();
        }

        public boolean hasListeners() {
            return !listeners.isEmpty();
        }

        /** Calls every listener in order and returns the event. */
        public E fire(E event) {
            for (Consumer<? super E> l : listeners) {
                try {
                    l.accept(event);
                } catch (RuntimeException | LinkageError e) {
                    System.err.println("[PrimaMateria] A listener of '" + name + "' failed: " + e);
                }
            }
            return event;
        }
    }
}
