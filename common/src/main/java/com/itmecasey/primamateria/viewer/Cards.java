package com.itmecasey.primamateria.viewer;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.data.RecipeLookup;
import com.itmecasey.primamateria.data.RecipeLookup.Snapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The recipe cards of every viewer (BEI, JEI, REI, EMI), worked out once from a {@link Snapshot}. A card is plain data: slots with
 * ids, counts and values, no game classes, so the same cards come out of every viewer and the layout is unit tested. Cards carry
 * names and values only (counts, chances, amounts, times): no sentences.
 *
 * <p>An id is a full item id, a fluid id, a melt id, or a tag written {@code #namespace:path}.
 */
public final class Cards {
    public static final String MODID = Ids.MODID;

    /** The six categories. {@link #path} is the id under the mod's namespace. */
    public enum Category {
        SIFTING("sifting", 180, 82),
        CRUSHING("crushing", 180, 82),
        COMPOSTING("composting", 150, 44),
        BARREL("barrel", 170, 50),
        MELTING("melting", 150, 44),
        CASTING("casting", 150, 44),
        /** The information page of a siftable block: every drop with its chance at each mesh tier. */
        DROPS("drops", 180, 140);

        public final String path;
        public final int width;
        public final int height;

        Category(String path, int width, int height) {
            this.path = path;
            this.width = width;
            this.height = height;
        }

        /** The id path of the block or item that stands for the category (its tab icon and workstation). */
        public String station() {
            return switch (this) {
                case SIFTING, DROPS -> Ids.SIEVE;
                case CRUSHING -> Ids.HAMMERS.get(0);
                case COMPOSTING, BARREL -> Ids.WOODEN_BARREL;
                case MELTING -> Ids.CRUCIBLE;
                case CASTING -> Ids.CASTING_TRAY;
            };
        }

        /** The language key of the category's title. */
        public String titleKey() {
            return "viewer." + MODID + ".category." + path;
        }
    }

    /** What an entry is. A melt is a data-defined molten material with no bucket. */
    public enum Kind {
        ITEM, FLUID, MELT
    }

    /** One thing in a slot: {@code count} items, or {@code count} mB of a fluid or melt. */
    public record Entry(Kind kind, String id, int count) {
        public static Entry item(String id, int count) {
            return new Entry(Kind.ITEM, id, Math.max(1, count));
        }

        public static Entry fluid(String id, int mb) {
            return new Entry(Kind.FLUID, id, mb);
        }

        public static Entry melt(String id, int mb) {
            return new Entry(Kind.MELT, id, mb);
        }

        public boolean tag() {
            return id.startsWith("#");
        }
    }

    /** One output slot with the two small value lines under it ({@code top} is the chance, {@code bottom} the count range). */
    public record Output(Entry entry, String top, String bottom) {
    }

    /**
     * One card.
     *
     * @param category   the category
     * @param id         a stable id for the card (the recipe id, or a generated one)
     * @param inputs     input slots (an item, a tag, a fluid or a melt each)
     * @param catalysts  the stations and tools that make it work (mesh, hammers, heat sources, barrels), shown in one cycling slot
     * @param outputs    output slots
     * @param inputNotes the value text under each input slot, same order as {@code inputs} (may be empty strings)
     * @param values     value text lines for the whole card (time, tier)
     */
    public record Card(Category category, String id, List<Entry> inputs, List<Entry> catalysts, List<Output> outputs,
                       List<String> inputNotes, List<String> values) {
        public Card {
            inputs = List.copyOf(inputs);
            catalysts = List.copyOf(catalysts);
            outputs = List.copyOf(outputs);
            inputNotes = List.copyOf(inputNotes);
            values = List.copyOf(values);
        }
    }

    /** A place on the card, in pixels from its top left. */
    public record Spot(int x, int y) {
    }

    /** Where everything of a card goes. {@code text} lists the value lines as {@code (x, y, text, centered)}. */
    public record Layout(List<Spot> inputs, Spot catalyst, List<Spot> outputs, List<Text> texts) {
    }

    public record Text(int x, int y, String text, boolean centered) {
    }

    public static final int OUTPUT_COLUMNS = 5;
    public static final int CELL_WIDTH = 28;
    public static final int CELL_HEIGHT = 38;
    public static final int MAX_GRID_OUTPUTS = OUTPUT_COLUMNS * 2;
    public static final int SLOT = 18;

    private Cards() {
    }

    // ---- building cards ----

    /** Every card of every category, in category order. */
    public static List<Card> all(Snapshot s) {
        List<Card> out = new ArrayList<>();
        out.addAll(sifting(s));
        out.addAll(crushing(s));
        out.addAll(composting(s));
        out.addAll(barrel(s));
        out.addAll(melting(s));
        out.addAll(casting(s));
        out.addAll(drops(s));
        return out;
    }

    /** Mesh items by tier, from the mesh data files (the shipped five when the files are not loaded). */
    static Map<Integer, List<String>> meshesByTier(Snapshot s) {
        Map<Integer, List<String>> byTier = new TreeMap<>();
        for (RecipeLookup.Mesh m : s.meshes()) {
            byTier.computeIfAbsent(m.tier(), k -> new ArrayList<>()).add(m.item());
        }
        if (byTier.isEmpty()) {
            for (int i = 0; i < Ids.MESHES.size(); i++) {
                byTier.put(i + 1, List.of(Ids.full(Ids.MESHES.get(i))));
            }
        }
        return byTier;
    }

    /** Whether a drop can come out with a mesh of this tier. */
    static boolean allows(RecipeLookup.Sifting r, int tier) {
        return tier >= r.meshMin() && (r.meshMax() <= 0 || tier <= r.meshMax());
    }

    /**
     * One card per input block and run of mesh tiers that give the same drops, with the drops as outputs (more than ten outputs
     * continue on a second card) and the matching meshes as the catalyst. The residue of the input is the last output.
     */
    public static List<Card> sifting(Snapshot s) {
        Map<Integer, List<String>> meshes = meshesByTier(s);
        Map<String, List<RecipeLookup.Sifting>> byInput = new LinkedHashMap<>();
        for (RecipeLookup.Sifting r : s.sifting()) {
            byInput.computeIfAbsent(r.input(), k -> new ArrayList<>()).add(r);
        }
        for (RecipeLookup.Residue r : s.residue()) {
            byInput.computeIfAbsent(r.input(), k -> new ArrayList<>());
        }
        List<Card> cards = new ArrayList<>();
        for (Map.Entry<String, List<RecipeLookup.Sifting>> e : byInput.entrySet()) {
            String input = e.getKey();
            List<RecipeLookup.Residue> residue = new ArrayList<>();
            for (RecipeLookup.Residue r : s.residue()) {
                if (r.input().equals(input)) {
                    residue.add(r);
                }
            }
            int serial = 0;
            int runStart = -1;
            List<String> runKey = null;
            List<Integer> tiers = new ArrayList<>(meshes.keySet());
            for (int i = 0; i <= tiers.size(); i++) {
                List<String> key = null;
                if (i < tiers.size()) {
                    key = new ArrayList<>();
                    for (RecipeLookup.Sifting r : e.getValue()) {
                        if (allows(r, tiers.get(i))) {
                            key.add(r.id());
                        }
                    }
                }
                if (runKey != null && (key == null || !key.equals(runKey))) {
                    int from = tiers.get(runStart);
                    int to = tiers.get(i - 1);
                    List<RecipeLookup.Sifting> drops = new ArrayList<>();
                    for (RecipeLookup.Sifting r : e.getValue()) {
                        if (runKey.contains(r.id())) {
                            drops.add(r);
                        }
                    }
                    List<Entry> catalysts = new ArrayList<>();
                    for (int t = from; t <= to; t++) {
                        for (String mesh : meshes.getOrDefault(t, List.of())) {
                            catalysts.add(Entry.item(mesh, 1));
                        }
                    }
                    if (!drops.isEmpty() || !residue.isEmpty()) {
                        serial = addSiftingCards(cards, input, input.replace(':', '/').replace('#', '_') + "/" + from, serial,
                                drops, residue, catalysts, from, to);
                    }
                    runKey = null;
                }
                if (key != null && runKey == null) {
                    runKey = key;
                    runStart = i;
                }
            }
        }
        return cards;
    }

    private static int addSiftingCards(List<Card> cards, String input, String baseId, int serial, List<RecipeLookup.Sifting> drops,
                                       List<RecipeLookup.Residue> residue, List<Entry> catalysts, int from, int to) {
        List<Output> outputs = new ArrayList<>();
        for (RecipeLookup.Sifting r : drops) {
            outputs.add(new Output(Entry.item(r.result(), r.max()), Fmt.chance(r.chance()), Fmt.range(r.min(), r.max())));
        }
        for (RecipeLookup.Residue r : residue) {
            outputs.add(new Output(Entry.item(r.result(), 1), Fmt.chance(r.chance()), ""));
        }
        String tier = Fmt.tiers(from, to);
        int pages = Math.max(1, (outputs.size() + MAX_GRID_OUTPUTS - 1) / MAX_GRID_OUTPUTS);
        for (int p = 0; p < pages; p++) {
            List<Output> page = outputs.subList(p * MAX_GRID_OUTPUTS, Math.min(outputs.size(), (p + 1) * MAX_GRID_OUTPUTS));
            List<String> values = new ArrayList<>();
            values.add(tier);
            if (pages > 1) {
                values.add((p + 1) + "/" + pages);
            }
            cards.add(new Card(Category.SIFTING, baseId + "#" + serial++, List.of(Entry.item(input, 1)), catalysts, page, List.of(""),
                    values));
        }
        return serial;
    }

    /** One card per crushing recipe: the block, the hammers that are strong enough, the results with chances. */
    public static List<Card> crushing(Snapshot s) {
        List<Card> cards = new ArrayList<>();
        for (RecipeLookup.Crushing r : s.crushing()) {
            List<Output> outputs = new ArrayList<>();
            for (RecipeLookup.Chance c : r.results()) {
                outputs.add(new Output(Entry.item(c.item(), c.max()), Fmt.chance(c.chance()), Fmt.range(c.min(), c.max())));
            }
            List<Entry> hammers = new ArrayList<>();
            for (int i = Math.max(0, r.hammerTier() - 1); i < Ids.HAMMERS.size(); i++) {
                hammers.add(Entry.item(Ids.full(Ids.HAMMERS.get(i)), 1));
            }
            hammers.add(Entry.item(Ids.full(Ids.AUTO_HAMMER), 1));
            for (int p = 0; p * MAX_GRID_OUTPUTS < Math.max(1, outputs.size()); p++) {
                List<Output> page = outputs.subList(Math.min(outputs.size(), p * MAX_GRID_OUTPUTS),
                        Math.min(outputs.size(), (p + 1) * MAX_GRID_OUTPUTS));
                cards.add(new Card(Category.CRUSHING, r.id() + (p == 0 ? "" : "#" + p), List.of(Entry.item(r.input(), 1)), hammers,
                        page, List.of(""), List.of("T" + r.hammerTier())));
            }
        }
        return cards;
    }

    /** One card per compostable (with its units) and per composting recipe (the result and the time). */
    public static List<Card> composting(Snapshot s) {
        List<Card> cards = new ArrayList<>();
        List<Entry> barrels = List.of(Entry.item(Ids.full(Ids.WOODEN_BARREL), 1), Entry.item(Ids.full(Ids.CLAY_BARREL), 1));
        for (RecipeLookup.Composting r : s.composting()) {
            for (RecipeLookup.Compost c : s.compost()) {
                cards.add(new Card(Category.COMPOSTING, r.id() + "/" + c.id(), List.of(Entry.item(c.item(), 1)), barrels,
                        List.of(new Output(Entry.item(r.result(), 1), "", "")), List.of(c.units() + " u"), List.of(Fmt.seconds(r.time()))));
            }
        }
        return cards;
    }

    /** Soaking, mixing, steeping and growing: fluid slots, an item slot, a result and a time. */
    public static List<Card> barrel(Snapshot s) {
        List<Card> cards = new ArrayList<>();
        Entry wood = Entry.item(Ids.full(Ids.WOODEN_BARREL), 1);
        Entry clay = Entry.item(Ids.full(Ids.CLAY_BARREL), 1);
        for (RecipeLookup.Soaking r : s.soaking()) {
            cards.add(new Card(Category.BARREL, r.id(), List.of(Entry.fluid(r.fluid(), r.amount()), Entry.item(r.input(), 1)),
                    List.of(wood, clay), List.of(new Output(Entry.item(r.result(), 1), "", "")),
                    List.of(Fmt.mb(r.amount()), ""), List.of(Fmt.seconds(r.time()))));
        }
        for (RecipeLookup.Mixing r : s.mixing()) {
            cards.add(new Card(Category.BARREL, r.id(), List.of(Entry.fluid(r.baseFluid(), r.amount()), Entry.fluid(r.addedFluid(), r.amount())),
                    r.needsHeatproof() ? List.of(clay) : List.of(wood, clay), List.of(new Output(Entry.item(r.result(), 1), "", "")),
                    List.of(Fmt.mb(r.amount()), Fmt.mb(r.amount())), List.of()));
        }
        for (RecipeLookup.Steeping r : s.steeping()) {
            cards.add(new Card(Category.BARREL, r.id(), List.of(Entry.fluid(r.fluid(), 1000), Entry.item(r.input(), r.count())),
                    List.of(wood, clay), List.of(new Output(Entry.fluid(r.resultFluid(), 1000), "", "")),
                    List.of(Fmt.mb(1000), ""), List.of(Fmt.seconds(r.time()))));
        }
        for (RecipeLookup.Growing r : s.growing()) {
            cards.add(new Card(Category.BARREL, r.id(), List.of(Entry.fluid(r.fluid(), r.amount()), Entry.item(r.seed(), 1)),
                    List.of(wood, clay), List.of(new Output(Entry.item(r.result(), r.count()), "", "")),
                    List.of(Fmt.mb(r.amount()), ""), List.of(Fmt.seconds(r.time()))));
        }
        return cards;
    }

    /** One card per melting recipe: the solid, the crucible, the heat sources, the result and its amount. */
    public static List<Card> melting(Snapshot s) {
        List<Card> cards = new ArrayList<>();
        List<Entry> stations = new ArrayList<>();
        stations.add(Entry.item(Ids.full(Ids.CRUCIBLE), 1));
        for (RecipeLookup.Heat h : s.heat()) {
            if (!h.block().startsWith("#")) {
                stations.add(Entry.item(h.block(), 1));
            }
        }
        for (RecipeLookup.Melting r : s.melting()) {
            Entry out = r.isMelt() ? Entry.melt(r.result(), r.amount()) : Entry.fluid(r.result(), r.amount());
            cards.add(new Card(Category.MELTING, r.id(), List.of(Entry.item(r.input(), 1)), stations,
                    List.of(new Output(out, Fmt.mb(r.amount()), "")), List.of(""), List.of()));
        }
        return cards;
    }

    /** One card per casting recipe: the melt and its amount, the tray, the result and the time. */
    public static List<Card> casting(Snapshot s) {
        List<Card> cards = new ArrayList<>();
        for (RecipeLookup.Casting r : s.casting()) {
            cards.add(new Card(Category.CASTING, r.id(), List.of(Entry.melt(r.melt(), r.amount())),
                    List.of(Entry.item(Ids.full(Ids.CASTING_TRAY), 1)), List.of(new Output(Entry.item(r.result(), 1), "", "")),
                    List.of(Fmt.mb(r.amount())), List.of(Fmt.seconds(r.time()))));
        }
        return cards;
    }

    public static final int DROP_ROWS = 6;
    public static final int DROP_COLUMN_X = 60;

    /**
     * The information page of each siftable block: the block as the input, the meshes as the catalyst, one output per drop. The
     * {@code top} text of an output holds its chances per mesh tier joined by {@code |} ("-|-|16%|16%|16%"), {@code bottom} its
     * count range, and the first value is the list of tier names joined the same way.
     */
    public static List<Card> drops(Snapshot s) {
        List<Card> cards = new ArrayList<>();
        Map<Integer, List<String>> meshes = meshesByTier(s);
        List<Entry> catalysts = new ArrayList<>();
        meshes.values().forEach(l -> l.forEach(m -> catalysts.add(Entry.item(m, 1))));
        for (DropTable.Table t : DropTable.all(s)) {
            List<String> header = new ArrayList<>();
            t.tiers().forEach(tier -> header.add("T" + tier));
            int pages = Math.max(1, (t.rows().size() + DROP_ROWS - 1) / DROP_ROWS);
            for (int p = 0; p < pages; p++) {
                List<Output> outs = new ArrayList<>();
                for (DropTable.Row r : t.rows().subList(p * DROP_ROWS, Math.min(t.rows().size(), (p + 1) * DROP_ROWS))) {
                    outs.add(new Output(Entry.item(r.item(), 1), String.join("|", r.cells()), r.label()));
                }
                cards.add(new Card(Category.DROPS, "drops/" + t.input().replace(':', '/').replace('#', '_') + "#" + p,
                        List.of(Entry.item(t.input(), 1)), catalysts, outs, List.of(""), List.of(String.join("|", header))));
            }
        }
        return cards;
    }

    // ---- layout ----

    /** The place of everything on a card of this category. Every viewer uses it, so a card looks the same in all of them. */
    public static Layout layout(Card card) {
        List<Spot> inputs = new ArrayList<>();
        List<Spot> outputs = new ArrayList<>();
        List<Text> texts = new ArrayList<>();
        Spot catalyst;
        switch (card.category()) {
            case SIFTING, CRUSHING -> {
                inputs.add(new Spot(5, 6));
                catalyst = new Spot(5, 40);
                int y = 56;
                for (int i = 0; i < card.values().size(); i++) {
                    texts.add(new Text(5, y + i * 9, card.values().get(i), false));
                }
                for (int i = 0; i < card.outputs().size(); i++) {
                    int col = i % OUTPUT_COLUMNS;
                    int row = i / OUTPUT_COLUMNS;
                    int cx = 36 + col * CELL_WIDTH;
                    int cy = 4 + row * CELL_HEIGHT;
                    outputs.add(new Spot(cx + (CELL_WIDTH - SLOT) / 2, cy));
                    Output o = card.outputs().get(i);
                    if (!o.top().isEmpty()) {
                        texts.add(new Text(cx + CELL_WIDTH / 2, cy + SLOT + 1, o.top(), true));
                    }
                    if (!o.bottom().isEmpty()) {
                        texts.add(new Text(cx + CELL_WIDTH / 2, cy + SLOT + 10, o.bottom(), true));
                    }
                }
            }
            case DROPS -> {
                inputs.add(new Spot(4, 4));
                catalyst = new Spot(30, 4);
                String[] head = card.values().isEmpty() ? new String[0] : card.values().get(0).split("[|]");
                int col = Math.min(24, (card.category().width - DROP_COLUMN_X) / Math.max(1, head.length));
                for (int i = 0; i < head.length; i++) {
                    texts.add(new Text(DROP_COLUMN_X + i * col + col / 2, 9, head[i], true));
                }
                for (int i = 0; i < card.outputs().size(); i++) {
                    int y = 26 + i * 18;
                    outputs.add(new Spot(4, y));
                    Output o = card.outputs().get(i);
                    if (!o.bottom().isEmpty()) {
                        texts.add(new Text(24, y + 5, o.bottom(), false));
                    }
                    String[] cells = o.top().split("[|]");
                    for (int c = 0; c < cells.length; c++) {
                        texts.add(new Text(DROP_COLUMN_X + c * col + col / 2, y + 5, cells[c], true));
                    }
                }
            }
            case BARREL -> {
                inputs.add(new Spot(5, 6));
                inputs.add(new Spot(40, 6));
                catalyst = new Spot(70, 28);
                outputs.add(new Spot(120, 6));
                addNote(texts, card, 0, 5 + 9, 25);
                addNote(texts, card, 1, 40 + 9, 25);
                for (int i = 0; i < card.values().size(); i++) {
                    texts.add(new Text(129, 27 + i * 9, card.values().get(i), true));
                }
            }
            default -> {
                inputs.add(new Spot(5, 6));
                catalyst = new Spot(60, 22);
                outputs.add(new Spot(115, 6));
                addNote(texts, card, 0, 5 + 9, 25);
                Output o = card.outputs().get(0);
                int y = 25;
                if (!o.top().isEmpty()) {
                    texts.add(new Text(115 + 9, y, o.top(), true));
                    y += 9;
                }
                for (int i = 0; i < card.values().size(); i++) {
                    texts.add(new Text(115 + 9, y + i * 9, card.values().get(i), true));
                }
            }
        }
        return new Layout(inputs, catalyst, outputs, texts);
    }

    private static void addNote(List<Text> texts, Card card, int input, int x, int y) {
        if (input < card.inputNotes().size() && !card.inputNotes().get(input).isEmpty()) {
            texts.add(new Text(x, y, card.inputNotes().get(input), true));
        }
    }

    /** The color of the tint behind an entry's id for melts that have no data file: a stable gray-orange. */
    public static int fallbackColor(String id) {
        return 0x808080 | (id.hashCode() & 0x7F7F7F);
    }

    /** Lowercase path of an id, used to build lang keys and sprite names. */
    public static String path(String id) {
        int colon = id.indexOf(':');
        return (colon < 0 ? id : id.substring(colon + 1)).toLowerCase(Locale.ROOT);
    }
}
