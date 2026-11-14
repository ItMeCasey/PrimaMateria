package com.itmecasey.primamateria.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.primamateria.crucible.HeatRule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The heat sources loaded from {@code data/<namespace>/primamateria/heat/<id>.json}:
 * <pre>
 * { "block": "minecraft:campfire", "state": { "lit": "true" }, "heat": 2 }
 * { "tag": "minecraft:campfires", "heat": 2 }
 * </pre>
 * A crucible takes the highest heat among the rules that match the block under it. Replaced after every datapack reload.
 */
public final class HeatSources {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");
    private static volatile List<HeatRule> rules = List.of();

    /** The data folder {@code primamateria/heat}. */
    public static final DataFolders.Folder FOLDER = new DataFolders.Folder() {
        @Override
        public String path() {
            return "primamateria/heat";
        }

        @Override
        public void apply(Map<Identifier, JsonObject> files) {
            List<HeatRule> loaded = new ArrayList<>();
            for (Map.Entry<Identifier, JsonObject> e : files.entrySet()) {
                try {
                    loaded.add(parse(e.getValue()));
                } catch (RuntimeException ex) {
                    LOGGER.warn("Heat file {} skipped: {}", e.getKey(), ex.getMessage());
                }
            }
            rules = List.copyOf(loaded);
            LOGGER.info("Loaded {} heat sources", loaded.size());
        }
    };

    private HeatSources() {
    }

    public static List<HeatRule> all() {
        return rules;
    }

    /** Parses one heat file; throws with a short reason when it is unusable. */
    static HeatRule parse(JsonObject json) {
        String block = json.has("block") ? json.get("block").getAsString() : "";
        String tag = json.has("tag") ? json.get("tag").getAsString() : "";
        if (tag.startsWith("#")) {
            tag = tag.substring(1);
        }
        if (!json.has("heat")) {
            throw new IllegalArgumentException("needs heat");
        }
        Map<String, String> state = new LinkedHashMap<>();
        if (json.has("state") && json.get("state").isJsonObject()) {
            for (Map.Entry<String, JsonElement> s : json.getAsJsonObject("state").entrySet()) {
                state.put(s.getKey(), s.getValue().getAsString());
            }
        }
        return new HeatRule(block, tag, state, json.get("heat").getAsInt());
    }

    /** The heat a crucible gets from the block at {@code pos} (the block under it): 0 when it is no heat source. */
    public static int heatAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return HeatRule.best(rules, r -> matches(r, state));
    }

    static boolean matches(HeatRule rule, BlockState state) {
        if (!rule.block().isEmpty()) {
            if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(rule.block())) {
                return false;
            }
        } else {
            Identifier tag = Identifier.tryParse(rule.tag());
            if (tag == null || !state.is(TagKey.create(Registries.BLOCK, tag))) {
                return false;
            }
        }
        for (Map.Entry<String, String> want : rule.state().entrySet()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(want.getKey());
            if (property == null || !valueName(state, property).equals(want.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
