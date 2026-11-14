package com.itmecasey.primamateria.data;

import java.util.List;

/**
 * A read-only picture of every recipe and data file of the mod, as plain records with string ids. Workstream A replaces it after
 * each datapack reload (server) and each sync (client); the recipe viewers and the script bridge of workstream D read it. An
 * ingredient is a full item or block id, or a tag written as {@code #namespace:path}.
 */
public final class RecipeLookup {
    private static volatile Snapshot current = Snapshot.EMPTY;

    private RecipeLookup() {
    }

    public static Snapshot get() {
        return current;
    }

    /** Called by workstream A only. */
    public static void set(Snapshot snapshot) {
        current = snapshot;
    }

    /** One result with a chance. */
    public record Chance(String item, int min, int max, double chance) {
    }

    public record Crushing(String id, String input, int hammerTier, List<Chance> results) {
    }

    /** {@code meshMax} is 0 when there is no upper limit. */
    public record Sifting(String id, String input, String result, int min, int max, double chance, int meshMin, int meshMax) {
    }

    public record Residue(String id, String input, String result, double chance) {
    }

    public record Composting(String id, String result, int time) {
    }

    public record Soaking(String id, String fluid, int amount, String input, String result, int time) {
    }

    public record Mixing(String id, String baseFluid, String addedFluid, int amount, String result, boolean needsHeatproof) {
    }

    public record Steeping(String id, String fluid, String input, int count, String resultFluid, int time) {
    }

    public record Growing(String id, String fluid, int amount, String seed, String result, int count, int time, String crystal) {
    }

    /** {@code isMelt} tells whether {@code result} is a melt id or a fluid id. */
    public record Melting(String id, String input, String result, boolean isMelt, int amount) {
    }

    public record Casting(String id, String melt, int amount, String result, int time) {
    }

    public record Mesh(String id, String item, int tier, double speed, String texture, int slot) {
    }

    /** {@code state} is empty or a {@code key=value,key=value} filter. */
    public record Heat(String id, String block, String state, int heat) {
    }

    public record Melt(String id, int color, int glow, String nameKey) {
    }

    public record Compost(String id, String item, int units, double matureBoost) {
    }

    public record Snapshot(List<Crushing> crushing, List<Sifting> sifting, List<Residue> residue, List<Composting> composting,
                           List<Soaking> soaking, List<Mixing> mixing, List<Steeping> steeping, List<Growing> growing,
                           List<Melting> melting, List<Casting> casting, List<Mesh> meshes, List<Heat> heat, List<Melt> melts,
                           List<Compost> compost) {
        public static final Snapshot EMPTY = new Snapshot(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        public Snapshot {
            crushing = List.copyOf(crushing);
            sifting = List.copyOf(sifting);
            residue = List.copyOf(residue);
            composting = List.copyOf(composting);
            soaking = List.copyOf(soaking);
            mixing = List.copyOf(mixing);
            steeping = List.copyOf(steeping);
            growing = List.copyOf(growing);
            melting = List.copyOf(melting);
            casting = List.copyOf(casting);
            meshes = List.copyOf(meshes);
            heat = List.copyOf(heat);
            melts = List.copyOf(melts);
            compost = List.copyOf(compost);
        }
    }
}
