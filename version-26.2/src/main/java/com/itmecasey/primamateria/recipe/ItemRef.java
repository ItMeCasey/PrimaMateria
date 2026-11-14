package com.itmecasey.primamateria.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * A recipe result that is either an item id ({@code "minecraft:iron_ingot"}) or an item tag ({@code "#c:ingots/tin"}). A tag stands
 * for whichever item of that tag exists in the game: the lowest id wins, with {@code minecraft} first, so the pick is the same on
 * every run. A tag with no items (the mod that fills it is not installed) resolves to nothing and the recipe entry is skipped, so
 * data for other mods is inert without them.
 *
 * <p>Inside the recipe records a tag is held as the id {@code primamateria:tag/<namespace>/<path>} (a plain {@link Identifier});
 * {@link #CODEC} reads and writes the {@code #namespace:path} form.
 */
public final class ItemRef {
    private static final String TAG_PREFIX = "tag/";

    /** {@code namespace:path} or {@code #namespace:path}. */
    public static final Codec<Identifier> CODEC = Codec.STRING.comapFlatMap(ItemRef::parse, ItemRef::write);

    private ItemRef() {
    }

    private static DataResult<Identifier> parse(String text) {
        if (!text.startsWith("#")) {
            return Identifier.read(text);
        }
        Identifier tag = Identifier.tryParse(text.substring(1));
        if (tag == null) {
            return DataResult.error(() -> "Not a valid tag id: " + text);
        }
        return DataResult.success(Identifier.fromNamespaceAndPath("primamateria", TAG_PREFIX + tag.getNamespace() + "/" + tag.getPath()));
    }

    private static String write(Identifier id) {
        if (isTag(id)) {
            String rest = id.getPath().substring(TAG_PREFIX.length());
            int slash = rest.indexOf('/');
            return "#" + rest.substring(0, slash) + ":" + rest.substring(slash + 1);
        }
        return id.toString();
    }

    public static boolean isTag(Identifier id) {
        return id.getNamespace().equals("primamateria") && id.getPath().startsWith(TAG_PREFIX) && id.getPath().indexOf('/', TAG_PREFIX.length()) > 0;
    }

    /** The text form of a stored result: an item id or {@code #tag}. */
    public static String text(Identifier id) {
        return write(id);
    }

    /** The item this result stands for in this game, or null when the item (or every item of the tag) does not exist. */
    public static Identifier resolve(Identifier id) {
        if (!isTag(id)) {
            return BuiltInRegistries.ITEM.containsKey(id) ? id : null;
        }
        String rest = id.getPath().substring(TAG_PREFIX.length());
        int slash = rest.indexOf('/');
        Identifier tagId = Identifier.tryParse(rest.substring(0, slash) + ":" + rest.substring(slash + 1));
        if (tagId == null) {
            return null;
        }
        List<Identifier> found = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tagId))) {
            Identifier key = BuiltInRegistries.ITEM.getKey(holder.value());
            if (key != null && holder.value() != net.minecraft.world.item.Items.AIR) {
                found.add(key);
            }
        }
        return found.stream()
                .min(Comparator.comparing((Identifier k) -> !k.getNamespace().equals("minecraft")).thenComparing(Identifier::toString))
                .orElse(null);
    }

    /** The resolved item id as text, or null. */
    public static String resolveText(Identifier id) {
        Identifier item = resolve(id);
        return item == null ? null : item.toString();
    }
}
