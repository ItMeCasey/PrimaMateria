package com.itmecasey.primamateria.fluid;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The four real fluids of the mod: brine and the molten copper, iron and gold. Each has a source fluid {@code primamateria:<id>},
 * a flowing fluid {@code primamateria:flowing_<id>}, a liquid block {@code primamateria:<id>} and a bucket item (the existing
 * {@code <id>_bucket}, built by {@link ModContent}). The fluid classes and their fluid types depend on the loader's API, so the loader
 * module supplies a {@link Factory}; everything else is shared and lives here.
 *
 * <p>Brine is water's own fluid class with another look (it acts as water: swims, extinguishes, flows like water). The molten metals
 * are lava's own fluid class (they burn, set things on fire, spread fire to flammable blocks, make stone where they meet water),
 * except that they never turn into new sources the way water does.
 */
public final class ModFluids {
    /** Builds the loader's fluid classes: the source or the flowing form of one of {@link Ids#FLUIDS}. */
    public interface Factory {
        Fluid create(String id, boolean flowing);

        /** The bucket item for the source fluid of {@code id} (Forge wants the fluid handed over lazily, NeoForge directly). */
        Item bucket(String id, Item.Properties properties);
    }

    private static final String[] FACTORIES = {
            "com.itmecasey.primamateria.neoforge.NeoForgeFluids",
            "com.itmecasey.primamateria.forge.ForgeFluids",
    };

    private static final Map<String, Fluid> SOURCES = new LinkedHashMap<>();
    private static final Map<String, Fluid> FLOWING = new LinkedHashMap<>();
    private static final Map<String, LiquidBlock> BLOCKS = new LinkedHashMap<>();

    private static final Factory FACTORY = loadFactory();

    static {
        Factory factory = FACTORY;
        for (String id : Ids.FLUIDS) {
            Fluid source = factory.create(id, false);
            SOURCES.put(id, source);
            FLOWING.put(id, factory.create(id, true));
            BLOCKS.put(id, new PrimaLiquidBlock((FlowingFluid) source, blockProps(id)));
        }
    }

    private ModFluids() {
    }

    private static Factory loadFactory() {
        for (String name : FACTORIES) {
            try {
                return (Factory) Class.forName(name).getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                // the other loader's class: not in this jar
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot start the fluid factory " + name, e);
            }
        }
        throw new IllegalStateException("No loader fluid factory found");
    }

    private static BlockBehaviour.Properties blockProps(String id) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(Ids.MODID, id)))
                .replaceable().noCollision().strength(100.0F).pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                .sound(SoundType.EMPTY);
        if (isMolten(id)) {
            int light = FluidLook.glow("primamateria:" + id);
            return p.mapColor(MapColor.FIRE).randomTicks().lightLevel(state -> light);
        }
        return p.mapColor(MapColor.COLOR_CYAN);
    }

    /** Whether the fluid is one of the three molten metals (hot, lava-like) and not brine. */
    public static boolean isMolten(String id) {
        return id.startsWith("molten_");
    }

    /** The source fluid of an id from {@link Ids#FLUIDS}. */
    public static Fluid source(String id) {
        return SOURCES.get(id);
    }

    /** The flowing fluid of an id from {@link Ids#FLUIDS}. */
    public static Fluid flowing(String id) {
        return FLOWING.get(id);
    }

    /** The liquid block of an id from {@link Ids#FLUIDS}. */
    public static LiquidBlock block(String id) {
        return BLOCKS.get(id);
    }

    /** Builds the real bucket item of a fluid; {@link ModContent} calls this while it builds its items. */
    public static Item makeBucket(String id, Item.Properties properties) {
        return FACTORY.bucket(id, properties);
    }

    /** The bucket item of an id from {@link Ids#FLUIDS}. */
    public static Item bucket(String id) {
        return ModContent.item(id + "_bucket");
    }

    /** The fluid ids in registration order. */
    public static List<String> ids() {
        return Ids.FLUIDS;
    }

    /** Registers every source and flowing fluid under its id ({@code <id>} and {@code flowing_<id>}). */
    public static void registerFluids(BiConsumer<String, Fluid> out) {
        for (String id : Ids.FLUIDS) {
            out.accept(id, SOURCES.get(id));
            out.accept("flowing_" + id, FLOWING.get(id));
        }
    }

    /** Registers the four liquid blocks under the fluid ids. */
    public static void registerBlocks(BiConsumer<String, net.minecraft.world.level.block.Block> out) {
        BLOCKS.forEach(out);
    }
}
