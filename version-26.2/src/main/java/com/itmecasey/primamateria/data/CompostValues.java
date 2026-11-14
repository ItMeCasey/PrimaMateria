package com.itmecasey.primamateria.data;

import com.google.gson.JsonObject;
import com.itmecasey.primamateria.sieve.SieveLogic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compost values loaded from {@code data/<namespace>/primamateria/compost/<id>.json}: an item or a tag, how many units it adds
 * to a barrel (1000 fill one), and an optional {@code mature_boost} (0 to 1) that shortens the maturing time once.
 *
 * <pre>
 * { "item": "minecraft:wheat", "units": 80 }
 * { "tag": "minecraft:saplings", "units": 100 }
 * { "item": "minecraft:bone_meal", "units": 40, "mature_boost": 0.5 }
 * </pre>
 */
public final class CompostValues {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");

    /** What one item adds to a barrel. */
    public record Value(int units, double boost) {
    }

    private record TagEntry(TagKey<Item> tag, Value value) {
    }

    private static volatile Map<String, Value> items = Map.of();
    private static volatile List<TagEntry> tags = List.of();

    /** The data folder {@code primamateria/compost}. */
    public static final DataFolders.Folder FOLDER = new DataFolders.Folder() {
        @Override
        public String path() {
            return "primamateria/compost";
        }

        @Override
        public void apply(Map<Identifier, JsonObject> files) {
            Map<String, Value> byItem = new HashMap<>();
            List<TagEntry> byTag = new ArrayList<>();
            for (Map.Entry<Identifier, JsonObject> e : files.entrySet()) {
                JsonObject json = e.getValue();
                try {
                    int units = json.get("units").getAsInt();
                    double boost = json.has("mature_boost") ? json.get("mature_boost").getAsDouble() : 0.0;
                    if (units < 1 || boost < 0 || boost > 1) {
                        LOGGER.warn("Compost file {} skipped: units must be 1 or more and mature_boost 0 to 1", e.getKey());
                        continue;
                    }
                    Value value = new Value(units, boost);
                    if (json.has("item")) {
                        byItem.put(json.get("item").getAsString(), value);
                    } else if (json.has("tag")) {
                        String tag = json.get("tag").getAsString();
                        Identifier id = Identifier.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
                        if (id == null) {
                            LOGGER.warn("Compost file {} skipped: bad tag {}", e.getKey(), tag);
                            continue;
                        }
                        byTag.add(new TagEntry(TagKey.create(Registries.ITEM, id), value));
                    } else {
                        LOGGER.warn("Compost file {} skipped: needs item or tag", e.getKey());
                    }
                } catch (RuntimeException ex) {
                    LOGGER.warn("Compost file {} skipped: {}", e.getKey(), ex.toString());
                }
            }
            items = Map.copyOf(byItem);
            tags = List.copyOf(byTag);
            LOGGER.info("Loaded {} compost values", byItem.size() + byTag.size());
        }
    };

    private CompostValues() {
    }

    /** The compost value of a stack, or null when the item cannot be composted. An item entry wins over a tag entry. */
    public static Value of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Value v = items.get(SieveLogic.itemId(stack));
        if (v != null) {
            return v;
        }
        for (TagEntry t : tags) {
            if (stack.is(t.tag())) {
                return t.value();
            }
        }
        return null;
    }

    public static int count() {
        return items.size() + tags.size();
    }
}
