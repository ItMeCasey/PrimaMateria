package com.itmecasey.primamateria.compat;

import com.itmecasey.primamateria.viewer.InfoLines;
import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.SmwtiPlugin;
import net.minecraft.network.chat.Component;

/**
 * The Prima Materia lines in Show Me What This Is: state only, as a name and a value (sieve mesh, load and progress; barrel
 * contents, stage and result; crucible contents and heat; casting tray stage; drive charge; auto hammer hammer and queue). The server
 * works the lines out from the block entity's view record, so they are the same ones Jade shows. SMWTI finds this class by its
 * annotation; nothing calls into it, and it is never loaded unless SMWTI is installed.
 */
@SmwtiPlugin
public class PrimaSmwtiPlugin implements ISmwtiPlugin {
    private static final int GOLD = 0xF2B33D;

    /** {@code Label: value}, with the label from the language file. */
    static Component line(InfoLines.Line line) {
        return Component.translatable("viewer.primamateria.info." + line.label().replace(' ', '_'), line.value());
    }

    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> {
            Object view = GameAccess.view(entity);
            if (view == null) {
                return;
            }
            for (InfoLines.Line l : InfoLines.of(view, Stacks::name)) {
                if (l.fraction() >= 0) {
                    tooltip.bar(line(l), l.fraction(), GOLD);
                } else {
                    tooltip.add(line(l));
                }
            }
        });
    }
}
