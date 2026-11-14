package com.itmecasey.primamateria.compat;

import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.viewer.Cards;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Turns the plain ids of the card model into game objects for the recipe viewers. Never throws on an unknown id: an id that is not
 * in the registries gives an empty result, so a datapack typo shows as a missing slot instead of a crash.
 */
public final class Stacks {
    private Stacks() {
    }

    /** One stack of the item with this id, or empty. */
    public static ItemStack stack(String id, int count) {
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(key);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, count));
    }

    /** Every stack an id stands for: the item itself, or every item of a {@code #tag}. */
    public static List<ItemStack> alternatives(String id, int count) {
        List<ItemStack> out = new ArrayList<>();
        if (id.startsWith("#")) {
            Identifier key = Identifier.tryParse(id.substring(1));
            if (key != null) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, key);
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    out.add(new ItemStack(holder.value(), Math.max(1, count)));
                }
            }
        } else {
            ItemStack s = stack(id, count);
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    /** The fluid with this id, or empty. */
    public static Fluid fluid(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(key);
    }

    /** The bucket of a fluid as the stand-in item for viewers that show only items; empty when there is none. */
    public static ItemStack bucket(String fluidId) {
        Fluid fluid = fluid(fluidId);
        if (fluid == Fluids.EMPTY) {
            return ItemStack.EMPTY;
        }
        Item bucket = fluid.getBucket();
        return bucket == Items.AIR ? ItemStack.EMPTY : new ItemStack(bucket);
    }

    /** What an entry shows as an item: an item or tag as itself, a fluid as its bucket, a melt as nothing. */
    public static List<ItemStack> itemsOf(Cards.Entry entry) {
        return switch (entry.kind()) {
            case ITEM -> alternatives(entry.id(), entry.count());
            case FLUID -> {
                ItemStack b = bucket(entry.id());
                yield b.isEmpty() ? List.of() : List.of(b);
            }
            case MELT -> List.of();
        };
    }

    /** The display name of an id (an item, a fluid, a melt or a tag) for lines of text. */
    public static String name(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        if (id.startsWith("#")) {
            return id;
        }
        ItemStack s = stack(id, 1);
        if (!s.isEmpty()) {
            return s.getHoverName().getString();
        }
        Identifier key = Identifier.tryParse(id);
        if (key != null) {
            if (BuiltInRegistries.FLUID.containsKey(key)) {
                return Component.translatable("fluid." + key.getNamespace() + "." + key.getPath()).getString();
            }
            Component melt = Component.translatable("melt." + key.getNamespace() + "." + key.getPath());
            String text = melt.getString();
            return text.startsWith("melt.") ? key.getPath() : text;
        }
        return id;
    }

    /** The color of a melt from its data file, or a stable gray-orange when it has none. */
    public static int meltColor(String meltId) {
        return Cards.fallbackColor(meltId);
    }
}
