package com.itmecasey.primamateria;

import com.itmecasey.primamateria.block.CastingTrayBlock;
import com.itmecasey.primamateria.block.CrucibleBlock;
import com.itmecasey.primamateria.block.SieveBlock;
import com.itmecasey.primamateria.blockentity.CastingTrayBlockEntity;
import com.itmecasey.primamateria.blockentity.CrucibleBlockEntity;
import com.itmecasey.primamateria.blockentity.SieveBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Every block, item and sound the mod adds, built once and handed to the loader's registry event. The ids come from {@link Ids}
 * and are frozen.
 *
 * <p>Scaffold state: every block is a plain placeholder {@link Block} and every item a plain {@link Item}. Workstream A replaces
 * the constructor calls in {@link #makeBlock} and {@link #makeItem} with the real classes (and adds block entity types, recipe
 * types and the brine fluid in its own registration methods); the ids, the maps and the register methods stay as they are.
 */
public final class ModContent {
    public static final String MODID = Ids.MODID;

    /** Blocks by id path, in {@link Ids#BLOCKS} order. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    /** Items by id path: block items first, then {@link Ids#ITEMS}. */
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    /** Sound events by id path, in {@link Ids#SOUNDS} order. */
    public static final Map<String, SoundEvent> SOUNDS = new LinkedHashMap<>();

    static {
        for (String name : Ids.BLOCKS) {
            Block block = makeBlock(name, blockProps(name));
            BLOCKS.put(name, block);
            ITEMS.put(name, new BlockItem(block, itemProps(name).useBlockDescriptionPrefix()));
        }
        for (String name : Ids.ITEMS) {
            ITEMS.put(name, makeItem(name, itemProps(name)));
        }
        for (String name : Ids.SOUNDS) {
            SOUNDS.put(name, SoundEvent.createVariableRangeEvent(id(name)));
        }
    }

    /** The sieve's block entity type. Declared after the block map is filled. */
    public static final BlockEntityType<SieveBlockEntity> SIEVE_BE = new BlockEntityType<>(SieveBlockEntity::new, Set.of(block(Ids.SIEVE)));

    /** The crucible's and the casting tray's block entity types. */
    public static final BlockEntityType<CrucibleBlockEntity> CRUCIBLE_BE = new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(block(Ids.CRUCIBLE)));
    public static final BlockEntityType<CastingTrayBlockEntity> TRAY_BE = new BlockEntityType<>(CastingTrayBlockEntity::new, Set.of(block(Ids.CASTING_TRAY)));

    private ModContent() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static Block block(String name) {
        return BLOCKS.get(name);
    }

    public static Item item(String name) {
        return ITEMS.get(name);
    }

    public static SoundEvent sound(String name) {
        return SOUNDS.get(name);
    }

    private static Block makeBlock(String name, BlockBehaviour.Properties props) {
        if (name.equals(Ids.SIEVE)) {
            return new SieveBlock(props);
        }
        if (name.equals(Ids.CRUCIBLE)) {
            return new CrucibleBlock(props);
        }
        if (name.equals(Ids.CASTING_TRAY)) {
            return new CastingTrayBlock(props);
        }
        return new Block(props);
    }

    private static Item makeItem(String name, Item.Properties props) {
        int hammer = Ids.HAMMERS.indexOf(name);
        if (hammer >= 0) {
            return new Item(props.stacksTo(1).durability(Ids.HAMMER_DURABILITY.get(hammer)));
        }
        if (Ids.MESHES.contains(name) || name.equals(Ids.BRINE_BUCKET)) {
            return new Item(props.stacksTo(1));
        }
        return new Item(props);
    }

    private static BlockBehaviour.Properties blockProps(String name) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name)));
        return switch (name) {
            case Ids.STONE_MEAL, Ids.PALE_SAND -> p.mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.SAND);
            case Ids.SCORIA -> p.mapColor(MapColor.NETHER).strength(0.6F).sound(SoundType.GRAVEL);
            case Ids.SIEVE, Ids.WOODEN_BARREL, Ids.AUTO_HAMMER, Ids.WINDUP_DRIVE ->
                    p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).noOcclusion();
            case Ids.FURNACE_DRIVE, Ids.POWERED_DRIVE ->
                    p.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops();
            default -> p.mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion()
                    .requiresCorrectToolForDrops();
        };
    }

    private static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    public static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + MODID))
                .icon(() -> new ItemStack(item(Ids.SIEVE)))
                .displayItems((params, output) -> ITEMS.values().forEach(output::accept))
                .build();
    }

    public static void registerBlocks(BiConsumer<String, Block> out) {
        BLOCKS.forEach(out);
    }

    public static void registerItems(BiConsumer<String, Item> out) {
        ITEMS.forEach(out);
    }

    public static void registerBlockEntities(BiConsumer<String, BlockEntityType<?>> out) {
        out.accept(Ids.SIEVE, SIEVE_BE);
        out.accept(Ids.CRUCIBLE, CRUCIBLE_BE);
        out.accept(Ids.CASTING_TRAY, TRAY_BE);
    }

    public static void registerSounds(BiConsumer<String, SoundEvent> out) {
        SOUNDS.forEach(out);
    }
}
