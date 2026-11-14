package com.itmecasey.primamateria.viewer;

import com.itmecasey.primamateria.view.AutoHammerView;
import com.itmecasey.primamateria.view.BarrelView;
import com.itmecasey.primamateria.view.CrucibleView;
import com.itmecasey.primamateria.view.DriveView;
import com.itmecasey.primamateria.view.SieveView;
import com.itmecasey.primamateria.view.TrayView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The look-at lines of SMWTI and Jade, worked out from a view record: a name and a value per line, state only (contents, progress,
 * tier), never advice. {@code name} turns an id into a display name and {@code label} a label key into its text; the game side
 * passes the real lookups, tests pass identity functions. A line is {@code {label, value}}; a {@code bar} line also has a 0 to 1
 * fraction (negative when there is no bar).
 */
public final class InfoLines {
    /** {@code fraction} is 0 to 1 for a progress bar, or -1 for a plain text line. */
    public record Line(String label, String value, double fraction) {
        public static Line text(String label, String value) {
            return new Line(label, value, -1);
        }

        public static Line bar(String label, String value, double fraction) {
            return new Line(label, value, Math.max(0, Math.min(1, fraction)));
        }
    }

    private InfoLines() {
    }

    private static String pct(double f) {
        return Math.round(Math.max(0, Math.min(1, f)) * 100) + "%";
    }

    public static List<Line> sieve(SieveView v, Function<String, String> name) {
        List<Line> out = new ArrayList<>();
        out.add(Line.text("mesh", v.meshId().isEmpty() ? "-" : name.apply(v.meshId())));
        if (v.filled()) {
            out.add(Line.text("load", name.apply(v.block())));
            out.add(Line.bar("progress", pct(v.progress()), v.progress()));
        }
        if (v.drive() != null && v.drive() != com.itmecasey.primamateria.view.DriveKind.NONE) {
            out.add(Line.text("drive", v.drive().name().toLowerCase(java.util.Locale.ROOT)));
        }
        if (v.blocked()) {
            out.add(Line.text("blocked", "yes"));
        }
        return out;
    }

    public static List<Line> barrel(BarrelView v, Function<String, String> name) {
        List<Line> out = new ArrayList<>();
        switch (v.contents()) {
            case EMPTY -> out.add(Line.text("contents", "-"));
            case FLUID -> out.add(Line.bar("contents", name.apply(v.contentId()) + " " + v.amount() + " mB",
                    v.amount() / (double) BarrelView.CAPACITY));
            case COMPOST -> out.add(Line.bar("contents", "compost " + v.amount() + " u", v.amount() / (double) BarrelView.CAPACITY));
        }
        if (v.stage() != BarrelView.Stage.IDLE) {
            out.add(Line.bar("stage", v.stage().name().toLowerCase(java.util.Locale.ROOT) + " " + pct(v.stageProgress()),
                    v.stageProgress()));
        }
        if (!v.result().isEmpty()) {
            out.add(Line.text("result", name.apply(v.result())));
        }
        for (BarrelView.Crystal c : v.crystals()) {
            out.add(Line.bar("crystal", (c.slot() + 1) + " " + pct(c.progress()), c.progress()));
        }
        if (v.heatproof()) {
            out.add(Line.text("type", "clay"));
        }
        return out;
    }

    public static List<Line> crucible(CrucibleView v, Function<String, String> name) {
        List<Line> out = new ArrayList<>();
        out.add(v.amount() <= 0 ? Line.text("contents", "-")
                : Line.bar("contents", name.apply(v.contentId()) + " " + v.amount() + " mB", v.amount() / (double) CrucibleView.CAPACITY));
        if (v.solids() > 0) {
            out.add(Line.text("solids", v.solids() + " " + name.apply(v.solidItem())));
        }
        out.add(Line.text("heat", String.valueOf(v.heat())));
        return out;
    }

    public static List<Line> tray(TrayView v, Function<String, String> name) {
        List<Line> out = new ArrayList<>();
        if (v.stage() == TrayView.Stage.EMPTY) {
            out.add(Line.text("contents", "-"));
            return out;
        }
        out.add(Line.text("contents", name.apply(v.fluidId())));
        out.add(Line.bar("stage", v.stage().name().toLowerCase(java.util.Locale.ROOT) + " " + pct(v.progress()), v.progress()));
        if (!v.result().isEmpty()) {
            out.add(Line.text("result", name.apply(v.result())));
        }
        return out;
    }

    public static List<Line> drive(DriveView v) {
        List<Line> out = new ArrayList<>();
        out.add(Line.bar("charge", pct(v.charge()), v.charge()));
        if (v.running()) {
            out.add(Line.text("running", "yes"));
        }
        if (v.blocked()) {
            out.add(Line.text("blocked", "yes"));
        }
        return out;
    }

    public static List<Line> autoHammer(AutoHammerView v, Function<String, String> name) {
        List<Line> out = new ArrayList<>();
        out.add(Line.text("hammer", v.hammerId().isEmpty() ? "-" : name.apply(v.hammerId())));
        out.add(Line.text("queue", String.valueOf(v.queued())));
        if (!v.inputBlock().isEmpty()) {
            out.add(Line.bar("progress", name.apply(v.inputBlock()) + " " + pct(v.progress()), v.progress()));
        }
        return out;
    }

    /** The lines of any view record, or an empty list for something else. */
    public static List<Line> of(Object view, Function<String, String> name) {
        if (view instanceof SieveView v) {
            return sieve(v, name);
        } else if (view instanceof BarrelView v) {
            return barrel(v, name);
        } else if (view instanceof CrucibleView v) {
            return crucible(v, name);
        } else if (view instanceof TrayView v) {
            return tray(v, name);
        } else if (view instanceof DriveView v) {
            return drive(v);
        } else if (view instanceof AutoHammerView v) {
            return autoHammer(v, name);
        }
        return List.of();
    }
}
