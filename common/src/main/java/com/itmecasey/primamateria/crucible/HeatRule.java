package com.itmecasey.primamateria.crucible;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * One heat source, from {@code data/<namespace>/primamateria/heat/<id>.json}: a block (or a block tag) with optional block state
 * values it must have, and the heat it gives to a crucible above it. Exactly one of {@code block} and {@code tag} is set.
 *
 * @param block full block id, or empty
 * @param tag   block tag id without the {@code #}, or empty
 * @param state property values that must all match (as text, for example {@code lit=true}); may be empty
 * @param heat  1 or more
 */
public record HeatRule(String block, String tag, Map<String, String> state, int heat) {
    public HeatRule {
        state = Map.copyOf(state);
        if (heat < 1) {
            throw new IllegalArgumentException("heat must be at least 1");
        }
        if (block.isEmpty() == tag.isEmpty()) {
            throw new IllegalArgumentException("set exactly one of block and tag");
        }
    }

    /** The highest heat among the rules {@code matches} accepts, or 0 when none does. */
    public static int best(List<HeatRule> rules, Predicate<HeatRule> matches) {
        int best = 0;
        for (HeatRule r : rules) {
            if (r.heat() > best && matches.test(r)) {
                best = r.heat();
            }
        }
        return best;
    }
}
