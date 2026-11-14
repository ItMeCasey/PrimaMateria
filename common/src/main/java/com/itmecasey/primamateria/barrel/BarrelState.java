package com.itmecasey.primamateria.barrel;

import com.itmecasey.primamateria.sift.Drop;
import com.itmecasey.primamateria.view.BarrelView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The rules of one barrel, in plain Java: what it holds (one fluid or compost), what it is doing (composting, soaking, steeping,
 * growing crystals) and what waits to be taken. The block entity owns one and saves it as a string. Every method takes the
 * recipe data it needs as arguments, so nothing here reads config or the game.
 *
 * <p>Amounts: fluid in mB, compost in units, time in game ticks. A barrel holds either a fluid or compost, never both.
 */
public final class BarrelState {
    public static final int CAPACITY = BarrelView.CAPACITY;
    public static final int GROW_SLOTS = BarrelView.GROW_SLOTS;
    public static final String WATER = "minecraft:water";
    /** Fluids a wooden barrel refuses. */
    public static final Set<String> HOT = hot();

    private static Set<String> hot() {
        Set<String> out = new java.util.HashSet<>(Set.of("minecraft:lava"));
        for (com.itmecasey.primamateria.fluid.Metals.Metal metal : com.itmecasey.primamateria.fluid.Metals.ALL) {
            out.add("primamateria:" + metal.fluid());
        }
        return Set.copyOf(out);
    }

    /** A soaking recipe: {@code amount} mB of {@code fluid} turn one input into one {@code result} over {@code time} ticks. */
    public record SoakRule(String id, String fluid, int amount, String result, int time) {
    }

    /** A mixing recipe: pouring {@code added} on top of {@code base} (each {@code amount} mB) makes one {@code result}. */
    public record MixRule(String id, String base, String added, int amount, String result, boolean needsHeatproof) {
    }

    /** A steeping recipe: {@code count} inputs in at least {@code amount} mB of {@code fluid} turn it into {@code resultFluid}. */
    public record SteepRule(String id, String fluid, int amount, int count, String resultFluid, int time) {
    }

    /** A growing recipe: one seed in {@code amount} mB of {@code fluid} grows into {@code count} of {@code result}. */
    public record GrowRule(String id, String fluid, int amount, String result, int count, int time, String crystal) {
    }

    /** A recipe that finished. {@code family} is soaking, mixing, steeping, growing or composting. */
    public record Finished(String family, String recipeId, String result, int count) {
    }

    private static final class Soak {
        String id;
        String item;
        String result;
        int left;
        int total;
    }

    private static final class Steep {
        SteepRule rule;
        String item;
        int collected;
        int left;
        int total;
        double timeMult;

        boolean timing() {
            return total > 0;
        }
    }

    private static final class Crystal {
        String id;
        String texture;
        String result;
        int count;
        int left;
        int total;
    }

    private final boolean heatproof;
    private String fluid = "";
    private int amount;
    private int compost;
    private double compostBoost;
    private boolean boostUsed;
    private boolean maturing;
    private int maturingLeft;
    private int maturingTotal;
    private String compostResult = "";
    private String compostRecipe = "";
    private Soak soak;
    private Steep steep;
    private final List<Crystal> crystals = new ArrayList<>();
    private final List<Drop> pending = new ArrayList<>();
    private int dewTicks;

    public BarrelState(boolean heatproof) {
        this.heatproof = heatproof;
    }

    // ---- reading ----

    public boolean heatproof() {
        return heatproof;
    }

    public String fluid() {
        return amount > 0 ? fluid : "";
    }

    public int amount() {
        return amount;
    }

    public int compost() {
        return compost;
    }

    public boolean maturing() {
        return maturing;
    }

    public int crystalCount() {
        return crystals.size();
    }

    public List<Drop> pending() {
        return List.copyOf(pending);
    }

    public boolean hasResult() {
        return !pending.isEmpty();
    }

    public boolean empty() {
        return amount == 0 && compost == 0 && soak == null && steep == null && crystals.isEmpty() && pending.isEmpty();
    }

    /** True while a timer runs (the block entity ticks every tick only then). */
    public boolean busy() {
        return maturing || soak != null || (steep != null && steep.timing()) || !crystals.isEmpty();
    }

    /** The item in the soak slot (being soaked or steeped), or empty. */
    public String soakItem() {
        if (soak != null) {
            return soak.item;
        }
        return steep != null ? steep.item : "";
    }

    public static boolean isHot(String fluid) {
        return HOT.contains(fluid);
    }

    // ---- fluid ----

    /** Whether {@code f} may be poured in at all: no compost, no other fluid, and hot fluids only in a heatproof barrel. */
    public boolean canFill(String f) {
        if (f == null || f.isEmpty() || compost > 0 || maturing) {
            return false;
        }
        if (isHot(f) && !heatproof) {
            return false;
        }
        return amount == 0 || fluid.equals(f);
    }

    /** Pours in up to {@code mb}; returns how much went in (0 when refused). */
    public int fill(String f, int mb) {
        if (mb <= 0 || !canFill(f)) {
            return 0;
        }
        int accepted = Math.min(mb, CAPACITY - amount);
        if (accepted <= 0) {
            return 0;
        }
        fluid = f;
        amount += accepted;
        return accepted;
    }

    /** Puts the fluid back to a saved id and amount (a rolled back transaction); touches nothing else. */
    public void restoreFluid(String f, int mb) {
        amount = Math.max(0, Math.min(CAPACITY, mb));
        fluid = amount > 0 && f != null ? f : "";
    }

    /** A bucket takes fluid only while nothing is working in the barrel. */
    public boolean canDrain(int mb) {
        return compost == 0 && amount >= mb && mb > 0 && soak == null && steep == null && crystals.isEmpty();
    }

    /** Takes {@code mb} out; returns the fluid id, or null when it cannot be taken. */
    public String drain(int mb) {
        if (!canDrain(mb)) {
            return null;
        }
        String f = fluid;
        amount -= mb;
        if (amount == 0) {
            fluid = "";
        }
        return f;
    }

    /**
     * Pouring {@code added} (a bucket's {@code addedAmount} mB) on top of the fluid that is in: the first matching rule is
     * carried out. The base fluid is used up and one result waits to be taken; the caller empties the bucket.
     */
    public Finished mix(List<MixRule> rules, String added, int addedAmount) {
        if (amount == 0 || soak != null || steep != null || !crystals.isEmpty() || compost > 0 || !pending.isEmpty()) {
            return null;
        }
        for (MixRule r : rules) {
            if (r.base().equals(fluid) && r.added().equals(added) && amount >= r.amount() && addedAmount >= r.amount()
                    && (!r.needsHeatproof() || heatproof)) {
                amount -= r.amount();
                if (amount == 0) {
                    fluid = "";
                }
                pending.add(new Drop(r.result(), 1));
                return new Finished("mixing", r.id(), r.result(), 1);
            }
        }
        return null;
    }

    // ---- compost ----

    /** Whether an item with compost value could go in right now. */
    public boolean canCompost(double boost) {
        if (amount > 0 || !pending.isEmpty() || soak != null || steep != null) {
            return false;
        }
        if (maturing) {
            return boost > 0 && !boostUsed;
        }
        return compost < CAPACITY;
    }

    /**
     * Adds compost. At {@link #CAPACITY} the barrel starts maturing for {@code maturingTicks} (shortened by the best boost an
     * item brought, once). An item with a boost can also be added while maturing: it shortens what is left, once.
     */
    public boolean addCompost(int units, double boost, String resultItem, String recipeId, int maturingTicks) {
        if (!canCompost(boost)) {
            return false;
        }
        if (maturing) {
            maturingLeft = Math.max(1, (int) Math.ceil(maturingLeft * (1.0 - boost)));
            boostUsed = true;
            return true;
        }
        compost = Math.min(CAPACITY, compost + Math.max(0, units));
        compostBoost = Math.max(compostBoost, boost);
        compostResult = resultItem;
        compostRecipe = recipeId;
        if (compost >= CAPACITY) {
            maturing = true;
            maturingTotal = Math.max(1, (int) Math.round(maturingTicks * (1.0 - compostBoost)));
            maturingLeft = maturingTotal;
            boostUsed = compostBoost > 0;
        }
        return true;
    }

    // ---- soaking, steeping, growing ----

    public boolean canSoak(SoakRule rule) {
        return compost == 0 && soak == null && steep == null && pending.isEmpty() && fluid().equals(rule.fluid())
                && amount >= rule.amount();
    }

    public boolean startSoak(SoakRule rule, String inputItem, double timeMult) {
        if (!canSoak(rule)) {
            return false;
        }
        amount -= rule.amount();
        if (amount == 0) {
            fluid = "";
        }
        soak = new Soak();
        soak.id = rule.id();
        soak.item = inputItem;
        soak.result = rule.result();
        soak.total = Math.max(1, (int) Math.round(rule.time() * timeMult));
        soak.left = soak.total;
        return true;
    }

    public boolean canSteep(SteepRule rule) {
        if (compost > 0 || soak != null || !crystals.isEmpty() || !pending.isEmpty() || !fluid().equals(rule.fluid())
                || amount < rule.amount()) {
            return false;
        }
        return steep == null || (steep.rule.id().equals(rule.id()) && !steep.timing() && steep.collected < rule.count());
    }

    /** Adds one input to a steep; with the last one the timer starts. */
    public boolean addSteepItem(SteepRule rule, String inputItem, double timeMult) {
        if (!canSteep(rule)) {
            return false;
        }
        if (steep == null) {
            steep = new Steep();
            steep.rule = rule;
            steep.timeMult = timeMult;
        }
        steep.item = inputItem;
        steep.collected++;
        if (steep.collected >= rule.count()) {
            steep.total = Math.max(1, (int) Math.round(rule.time() * timeMult));
            steep.left = steep.total;
        }
        return true;
    }

    public boolean canPlant(GrowRule rule) {
        return compost == 0 && steep == null && crystals.size() < GROW_SLOTS && fluid().equals(rule.fluid()) && amount >= rule.amount();
    }

    public boolean plantSeed(GrowRule rule, double timeMult) {
        if (!canPlant(rule)) {
            return false;
        }
        amount -= rule.amount();
        if (amount == 0) {
            fluid = "";
        }
        Crystal c = new Crystal();
        c.id = rule.id();
        c.texture = rule.crystal();
        c.result = rule.result();
        c.count = rule.count();
        c.total = Math.max(1, (int) Math.round(rule.time() * timeMult));
        c.left = c.total;
        crystals.add(c);
        return true;
    }

    // ---- time ----

    /** Advances every timer by {@code dt} ticks and returns the recipes that finished. */
    public List<Finished> tick(int dt) {
        List<Finished> done = new ArrayList<>();
        if (maturing) {
            maturingLeft -= dt;
            if (maturingLeft <= 0) {
                maturing = false;
                compost = 0;
                compostBoost = 0;
                boostUsed = false;
                pending.add(new Drop(compostResult, 1));
                done.add(new Finished("composting", compostRecipe, compostResult, 1));
            }
        }
        if (soak != null) {
            soak.left -= dt;
            if (soak.left <= 0) {
                pending.add(new Drop(soak.result, 1));
                done.add(new Finished("soaking", soak.id, soak.result, 1));
                soak = null;
            }
        }
        if (steep != null && steep.timing()) {
            steep.left -= dt;
            if (steep.left <= 0) {
                fluid = steep.rule.resultFluid();
                done.add(new Finished("steeping", steep.rule.id(), steep.rule.resultFluid(), amount));
                steep = null;
            }
        }
        java.util.Iterator<Crystal> it = crystals.iterator();
        while (it.hasNext()) {
            Crystal c = it.next();
            c.left -= dt;
            if (c.left <= 0) {
                pending.add(new Drop(c.result, c.count));
                done.add(new Finished("growing", c.id, c.result, c.count));
                it.remove();
            }
        }
        return done;
    }

    /**
     * Rain and dew: with an open sky a barrel gains water. {@code dt} ticks passed; {@code ticksPerMb} of 0 switches it off.
     * Returns the mB that came in.
     */
    public int dew(int dt, int ticksPerMb, boolean raining, int rainMultiplier) {
        if (ticksPerMb <= 0 || amount >= CAPACITY || !canFill(WATER)) {
            dewTicks = 0;
            return 0;
        }
        dewTicks += dt * (raining ? Math.max(1, rainMultiplier) : 1);
        int mb = dewTicks / ticksPerMb;
        dewTicks %= ticksPerMb;
        return mb > 0 ? fill(WATER, mb) : 0;
    }

    // ---- results ----

    /** The first finished result, or null. */
    public Drop peekResult() {
        return pending.isEmpty() ? null : pending.get(0);
    }

    /** Takes up to {@code max} of the first result. Returns what was taken, or null when nothing waits. */
    public Drop takeResult(int max) {
        if (pending.isEmpty() || max < 1) {
            return null;
        }
        Drop first = pending.get(0);
        int n = Math.min(max, first.count());
        if (n >= first.count()) {
            pending.remove(0);
        } else {
            pending.set(0, new Drop(first.item(), first.count() - n));
        }
        return new Drop(first.item(), n);
    }

    /** Takes the whole first result. */
    public Drop takeResult() {
        return takeResult(Integer.MAX_VALUE);
    }

    // ---- for the renderer ----

    public BarrelView.Contents contents() {
        if (compost > 0) {
            return BarrelView.Contents.COMPOST;
        }
        return amount > 0 ? BarrelView.Contents.FLUID : BarrelView.Contents.EMPTY;
    }

    public BarrelView.Stage stage() {
        if (!pending.isEmpty()) {
            return BarrelView.Stage.DONE;
        }
        if (maturing) {
            return BarrelView.Stage.MATURING;
        }
        if (soak != null) {
            return BarrelView.Stage.SOAKING;
        }
        if (steep != null) {
            return BarrelView.Stage.STEEPING;
        }
        if (!crystals.isEmpty()) {
            return BarrelView.Stage.GROWING;
        }
        return compost > 0 ? BarrelView.Stage.COMPOSTING : BarrelView.Stage.IDLE;
    }

    /** 0 to 1 within the current stage (compost fill while composting, the timer otherwise). */
    public float stageProgress() {
        return switch (stage()) {
            case COMPOSTING -> compost / (float) CAPACITY;
            case MATURING -> 1F - maturingLeft / (float) Math.max(1, maturingTotal);
            case SOAKING -> 1F - soak.left / (float) Math.max(1, soak.total);
            case STEEPING -> steep.timing() ? 1F - steep.left / (float) Math.max(1, steep.total) : steep.collected / (float) steep.rule.count();
            case GROWING -> {
                float best = 0;
                for (Crystal c : crystals) {
                    best = Math.max(best, 1F - c.left / (float) Math.max(1, c.total));
                }
                yield best;
            }
            case DONE -> 1F;
            default -> 0F;
        };
    }

    public List<BarrelView.Crystal> crystalViews() {
        List<BarrelView.Crystal> out = new ArrayList<>();
        for (int i = 0; i < crystals.size(); i++) {
            Crystal c = crystals.get(i);
            out.add(new BarrelView.Crystal(c.texture, 1F - c.left / (float) Math.max(1, c.total), i));
        }
        return out;
    }

    /** Surface tint as 0xRRGGBB: the fluid's color, or compost going from fresh green to ripe brown as it matures. */
    public int color() {
        if (compost > 0) {
            float t = maturing ? stageProgress() : 0F;
            return BarrelColors.mix(BarrelColors.COMPOST_FRESH, BarrelColors.COMPOST_RIPE, t);
        }
        return BarrelColors.fluid(fluid());
    }

    // ---- saving ----

    private static final String SEP = "\t";

    /** The whole state as one string (fields separated by tabs; items never contain tabs). */
    public String save() {
        List<String> f = new ArrayList<>();
        f.add(fluid);
        f.add(Integer.toString(amount));
        f.add(Integer.toString(compost));
        f.add(Double.toString(compostBoost));
        f.add(boostUsed ? "1" : "0");
        f.add(maturing ? "1" : "0");
        f.add(maturingLeft + "," + maturingTotal);
        f.add(compostResult + "," + compostRecipe);
        f.add(soak == null ? "" : String.join("|", soak.id, soak.item, soak.result, Integer.toString(soak.left), Integer.toString(soak.total)));
        f.add(steep == null ? "" : String.join("|", steep.rule.id(), steep.rule.fluid(), Integer.toString(steep.rule.amount()),
                Integer.toString(steep.rule.count()), steep.rule.resultFluid(), Integer.toString(steep.rule.time()), steep.item,
                Integer.toString(steep.collected), Integer.toString(steep.left), Integer.toString(steep.total), Double.toString(steep.timeMult)));
        StringBuilder cs = new StringBuilder();
        for (Crystal c : crystals) {
            if (cs.length() > 0) {
                cs.append(';');
            }
            cs.append(String.join("|", c.id, c.texture, c.result, Integer.toString(c.count), Integer.toString(c.left), Integer.toString(c.total)));
        }
        f.add(cs.toString());
        f.add(com.itmecasey.primamateria.sift.SiftState.encode(pending));
        f.add(Integer.toString(dewTicks));
        return String.join(SEP, f);
    }

    /** Restores a saved state. Damaged parts are skipped; the barrel never fails to load. */
    public void load(String text) {
        fluid = "";
        amount = 0;
        compost = 0;
        compostBoost = 0;
        boostUsed = false;
        maturing = false;
        maturingLeft = 0;
        maturingTotal = 0;
        compostResult = "";
        compostRecipe = "";
        soak = null;
        steep = null;
        crystals.clear();
        pending.clear();
        dewTicks = 0;
        if (text == null || text.isEmpty()) {
            return;
        }
        String[] f = text.split(SEP, -1);
        try {
            fluid = at(f, 0);
            amount = clamp(num(at(f, 1)), 0, CAPACITY);
            compost = clamp(num(at(f, 2)), 0, CAPACITY);
            compostBoost = dbl(at(f, 3));
            boostUsed = at(f, 4).equals("1");
            maturing = at(f, 5).equals("1");
            String[] m = at(f, 6).split(",", -1);
            maturingLeft = num(at(m, 0));
            maturingTotal = num(at(m, 1));
            String[] cr = at(f, 7).split(",", -1);
            compostResult = at(cr, 0);
            compostRecipe = at(cr, 1);
            String[] s = at(f, 8).split("\\|", -1);
            if (s.length >= 5 && !s[0].isEmpty()) {
                soak = new Soak();
                soak.id = s[0];
                soak.item = s[1];
                soak.result = s[2];
                soak.left = num(s[3]);
                soak.total = num(s[4]);
            }
            String[] t = at(f, 9).split("\\|", -1);
            if (t.length >= 11 && !t[0].isEmpty()) {
                steep = new Steep();
                steep.rule = new SteepRule(t[0], t[1], num(t[2]), num(t[3]), t[4], num(t[5]));
                steep.item = t[6];
                steep.collected = num(t[7]);
                steep.left = num(t[8]);
                steep.total = num(t[9]);
                steep.timeMult = dbl(t[10]);
            }
            for (String part : at(f, 10).split(";")) {
                String[] c = part.split("\\|", -1);
                if (c.length >= 6 && !c[0].isEmpty() && crystals.size() < GROW_SLOTS) {
                    Crystal k = new Crystal();
                    k.id = c[0];
                    k.texture = c[1];
                    k.result = c[2];
                    k.count = Math.max(1, num(c[3]));
                    k.left = num(c[4]);
                    k.total = num(c[5]);
                    crystals.add(k);
                }
            }
            pending.addAll(com.itmecasey.primamateria.sift.SiftState.decode(at(f, 11)));
            dewTicks = Math.max(0, num(at(f, 12)));
        } catch (RuntimeException e) {
            // keep what was read; a damaged barrel is better than a lost one
        }
        if (amount == 0) {
            fluid = "";
        }
        if (compost > 0 && amount > 0) {
            amount = 0;
            fluid = "";
        }
    }

    private static String at(String[] a, int i) {
        return i < a.length ? a[i] : "";
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double dbl(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
