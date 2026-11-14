package com.itmecasey.primamateria;

import java.util.List;

/**
 * Every id the mod uses, as plain path strings under the {@code primamateria} namespace. This file is frozen: nobody renames an
 * id, and a new id is added here first. The lists drive registration, the self test and the data checks.
 */
public final class Ids {
    public static final String MODID = "primamateria";

    // ---- blocks (each also has a block item with the same id) ----
    public static final String SIEVE = "sieve";
    public static final String WOODEN_BARREL = "wooden_barrel";
    public static final String CLAY_BARREL = "clay_barrel";
    public static final String CRUCIBLE = "crucible";
    public static final String CASTING_TRAY = "casting_tray";
    public static final String WINDUP_DRIVE = "windup_drive";
    public static final String FURNACE_DRIVE = "furnace_drive";
    public static final String POWERED_DRIVE = "powered_drive";
    public static final String AUTO_HAMMER = "auto_hammer";
    public static final String STONE_MEAL = "stone_meal";
    public static final String SCORIA = "scoria";
    public static final String PALE_SAND = "pale_sand";

    public static final List<String> BLOCKS = List.of(SIEVE, WOODEN_BARREL, CLAY_BARREL, CRUCIBLE, CASTING_TRAY, WINDUP_DRIVE,
            FURNACE_DRIVE, POWERED_DRIVE, AUTO_HAMMER, STONE_MEAL, SCORIA, PALE_SAND);

    /** Blocks that get a block entity (workstream A registers the types under the block's id). */
    public static final List<String> BLOCK_ENTITIES = List.of(SIEVE, WOODEN_BARREL, CLAY_BARREL, CRUCIBLE, CASTING_TRAY,
            WINDUP_DRIVE, FURNACE_DRIVE, POWERED_DRIVE, AUTO_HAMMER);

    // ---- items ----
    /** Hammers in tier order: index + 1 is the hammer tier. */
    public static final List<String> HAMMERS = List.of("wooden_hammer", "stone_hammer", "copper_hammer", "iron_hammer",
            "diamond_hammer", "netherite_hammer");
    /** Hammer durability, same order as {@link #HAMMERS}. */
    public static final List<Integer> HAMMER_DURABILITY = List.of(96, 192, 288, 512, 2048, 2560);
    /** Meshes in tier order: index + 1 is the mesh tier. */
    public static final List<String> MESHES = List.of("cloth_mesh", "copper_mesh", "iron_mesh", "diamond_mesh", "netherite_mesh");

    public static final String PLANT_FIBER = "plant_fiber";
    public static final String ROCK_CHIP = "rock_chip";
    public static final String COPPER_DUST = "copper_dust";
    public static final String IRON_DUST = "iron_dust";
    public static final String GOLD_DUST = "gold_dust";
    public static final String SALT_DUST = "salt_dust";
    public static final List<String> SEEDS = List.of("quartz_seed", "amethyst_seed", "lapis_seed", "diamond_seed", "emerald_seed");
    public static final String RAW_CRUCIBLE = "raw_crucible";
    public static final String RAW_CASTING_TRAY = "raw_casting_tray";
    public static final String RAW_CLAY_BARREL = "raw_clay_barrel";
    public static final String BRINE_BUCKET = "brine_bucket";

    public static final List<String> MATERIALS = List.of(PLANT_FIBER, ROCK_CHIP, COPPER_DUST, IRON_DUST, GOLD_DUST,
            SALT_DUST, "quartz_seed", "amethyst_seed", "lapis_seed", "diamond_seed", "emerald_seed", RAW_CRUCIBLE,
            RAW_CASTING_TRAY, RAW_CLAY_BARREL, BRINE_BUCKET, "molten_copper_bucket", "molten_iron_bucket", "molten_gold_bucket");

    /** Every item that is not a block item: hammers, meshes, materials. */
    public static final List<String> ITEMS = concat(HAMMERS, MESHES, MATERIALS);

    // ---- recipe types (workstream A registers them) ----
    public static final String CRUSHING = "crushing";
    public static final String SIFTING = "sifting";
    public static final String SIFTING_RESIDUE = "sifting_residue";
    public static final String COMPOSTING = "composting";
    public static final String SOAKING = "soaking";
    public static final String MIXING = "mixing";
    public static final String STEEPING = "steeping";
    public static final String GROWING = "growing";
    public static final String MELTING = "melting";
    public static final String CASTING = "casting";
    public static final List<String> RECIPE_TYPES = List.of(CRUSHING, SIFTING, SIFTING_RESIDUE, COMPOSTING, SOAKING, MIXING,
            STEEPING, GROWING, MELTING, CASTING);

    // ---- data folders under data/<namespace>/primamateria/ ----
    public static final String DATA_MESH = "mesh";
    public static final String DATA_HEAT = "heat";
    public static final String DATA_COMPOST = "compost";

    // ---- fluids ----
    /** The four fluids the mod adds (workstream A registers each with a flowing form {@code flowing_<id>} and a bucket item). */
    public static final String BRINE = "brine";
    public static final List<String> FLUIDS = List.of(BRINE, "molten_copper", "molten_iron", "molten_gold");

    // ---- sounds (registered by ModContent; workstream C supplies the files) ----
    public static final List<String> SOUNDS = List.of("sieve.load", "sieve.shake.coarse", "sieve.shake.fine", "sieve.shake.soil",
            "sieve.reveal", "sieve.finish", "sieve.mesh", "drive.crank", "drive.tick", "auto_hammer.strike", "crucible.pour",
            "casting_tray.cool", "barrel.finish");

    // ---- particles (workstream C registers them) ----
    public static final List<String> PARTICLES = List.of("fines", "sift_dust", "pour");

    // ---- other ----
    public static final String WORLD_PRESET = "void_island";
    public static final String START_ISLAND = "start_island";
    public static final String CREATIVE_TAB = "main";

    private Ids() {
    }

    /** {@code primamateria:<path>}. */
    public static String full(String path) {
        return MODID + ":" + path;
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (List<String> l : lists) {
            out.addAll(l);
        }
        return List.copyOf(out);
    }
}
