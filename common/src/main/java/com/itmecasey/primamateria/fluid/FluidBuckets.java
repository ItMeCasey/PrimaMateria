package com.itmecasey.primamateria.fluid;

import java.util.List;
import java.util.Optional;

/**
 * Which bucket items carry which fluid, for the buckets we know by id: the game's water and lava buckets and our four. Barrels and
 * crucibles keep their fluid as a plain id and an amount in mB; a bucket fills or empties 1000 mB. Brine and the three molten
 * metals are real fluids with real bucket items; the game side asks {@code FluidIds} first, which also finds any other mod's
 * bucket by its fluid, and falls back to this table.
 */
public final class FluidBuckets {
    public static final int BUCKET_AMOUNT = 1000;
    public static final String EMPTY_BUCKET = "minecraft:bucket";

    /** One bucket item and the fluid inside it. */
    public record Bucket(String item, String fluid) {
    }

    public static final List<Bucket> ALL = buckets();

    private static List<Bucket> buckets() {
        List<Bucket> all = new java.util.ArrayList<>(List.of(
                new Bucket("minecraft:water_bucket", "minecraft:water"),
                new Bucket("minecraft:lava_bucket", "minecraft:lava"),
                new Bucket("primamateria:brine_bucket", "primamateria:brine")));
        for (Metals.Metal metal : Metals.ALL) {
            all.add(new Bucket("primamateria:" + metal.fluid() + "_bucket", "primamateria:" + metal.fluid()));
        }
        return List.copyOf(all);
    }

    private FluidBuckets() {
    }

    /** The fluid a full bucket item carries. */
    public static Optional<String> fluidOf(String bucketItem) {
        return ALL.stream().filter(b -> b.item().equals(bucketItem)).map(Bucket::fluid).findFirst();
    }

    /** The full bucket item for a fluid. */
    public static Optional<String> bucketOf(String fluid) {
        return ALL.stream().filter(b -> b.fluid().equals(fluid)).map(Bucket::item).findFirst();
    }
}
