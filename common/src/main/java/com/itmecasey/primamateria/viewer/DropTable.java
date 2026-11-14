package com.itmecasey.primamateria.viewer;

import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.data.RecipeLookup.Snapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The information page of a siftable block: one row per drop, one column per mesh tier holding the chance (a dash where the mesh
 * is too fine or too coarse), and the residue on the last rows. Values and names only. The same generator writes the wiki's drop
 * table page, so the viewers and the wiki never differ.
 */
public final class DropTable {
    /** One table for one input block. {@code tiers} are the mesh tiers in column order. */
    public record Table(String input, List<Integer> tiers, List<Row> rows) {
    }

    /** {@code cells} has one text per tier ("16%" or "-"); {@code label} is the count range ("1-2") or empty. */
    public record Row(String item, String label, List<String> cells, boolean residue) {
    }

    private DropTable() {
    }

    /** One table per siftable block, in the order the recipes name them. */
    public static List<Table> all(Snapshot s) {
        Map<String, List<RecipeLookup.Sifting>> byInput = new LinkedHashMap<>();
        for (RecipeLookup.Sifting r : s.sifting()) {
            byInput.computeIfAbsent(r.input(), k -> new ArrayList<>()).add(r);
        }
        for (RecipeLookup.Residue r : s.residue()) {
            byInput.computeIfAbsent(r.input(), k -> new ArrayList<>());
        }
        TreeSet<Integer> tierSet = new TreeSet<>(Cards.meshesByTier(s).keySet());
        List<Integer> tiers = new ArrayList<>(tierSet);
        List<Table> out = new ArrayList<>();
        for (Map.Entry<String, List<RecipeLookup.Sifting>> e : byInput.entrySet()) {
            List<Row> rows = new ArrayList<>();
            for (RecipeLookup.Sifting r : e.getValue()) {
                List<String> cells = new ArrayList<>();
                for (int tier : tiers) {
                    cells.add(Cards.allows(r, tier) ? Fmt.chance(r.chance()) : "-");
                }
                rows.add(new Row(r.result(), Fmt.range(r.min(), r.max()), cells, false));
            }
            for (RecipeLookup.Residue r : s.residue()) {
                if (r.input().equals(e.getKey())) {
                    List<String> cells = new ArrayList<>();
                    for (int tier : tiers) {
                        cells.add(Fmt.chance(r.chance()));
                    }
                    rows.add(new Row(r.result(), "", cells, true));
                }
            }
            out.add(new Table(e.getKey(), tiers, rows));
        }
        return out;
    }

    /** The table as a Markdown table (the wiki page). {@code name} turns an id into a display name. */
    public static String markdown(Table t, java.util.function.Function<String, String> name) {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(name.apply(t.input())).append("\n\n| Drop | Count |");
        for (int tier : t.tiers()) {
            sb.append(" T").append(tier).append(" |");
        }
        sb.append("\n|---|---|");
        for (int i = 0; i < t.tiers().size(); i++) {
            sb.append("---|");
        }
        sb.append('\n');
        for (Row r : t.rows()) {
            sb.append("| ").append(name.apply(r.item())).append(r.residue() ? " (residue)" : "").append(" | ").append(r.label())
                    .append(" |");
            for (String c : r.cells()) {
                sb.append(' ').append(c).append(" |");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
