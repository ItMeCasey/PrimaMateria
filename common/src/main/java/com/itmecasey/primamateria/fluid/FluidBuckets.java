package com.itmecasey.primamateria.fluid;

import java.util.List;
import java.util.Optional;

/**
 * Which bucket items carry which fluid. Barrels and crucibles keep their fluid as a plain id and an amount in mB; a bucket fills
 * or empties 1000 mB. Water and lava are the game's; brine and the three molten metals are ours (internal to our machines in
 * the first release: they have buckets but no placeable world fluid).
 */
public final class FluidBuckets {
    public static final int BUCKET_AMOUNT = 1000;
    public static final String EMPTY_BUCKET = "minecraft:bucket";

    /** One bucket item and the fluid inside it. */
    public record Bucket(String item, String fluid) {
    }

    public static final List<Bucket> ALL = List.of(
            new Bucket("minecraft:water_bucket", "minecraft:water"),
            new Bucket("minecraft:lava_bucket", "minecraft:lava"),
            new Bucket("primamateria:brine_bucket", "primamateria:brine"),
            new Bucket("primamateria:molten_copper_bucket", "primamateria:molten_copper"),
            new Bucket("primamateria:molten_iron_bucket", "primamateria:molten_iron"),
            new Bucket("primamateria:molten_gold_bucket", "primamateria:molten_gold"));

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
