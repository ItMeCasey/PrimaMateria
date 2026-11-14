package com.itmecasey.primamateria.neoforge.compat;

import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.compat.GameAccess;
import com.itmecasey.primamateria.compat.Stacks;
import com.itmecasey.primamateria.viewer.InfoLines;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade: the same state lines as SMWTI (sieve mesh, load and progress; barrel contents and stage; crucible contents and heat; tray
 * stage; drive charge; auto hammer), worked out from the block entity's view record. Names and values only. Loaded only when Jade
 * is installed (NeoForge builds).
 */
@WailaPlugin
public class PrimaJadePlugin implements IWailaPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("primamateria", "state");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        IBlockComponentProvider provider = new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                Object view = GameAccess.view(accessor.getBlockEntity());
                if (view == null) {
                    return;
                }
                for (InfoLines.Line l : InfoLines.of(view, Stacks::name)) {
                    tooltip.add(Component.translatable("viewer.primamateria.info." + l.label().replace(' ', '_'), l.value()));
                }
            }

            @Override
            public Identifier getUid() {
                return UID;
            }
        };
        Set<Class<?>> done = new HashSet<>();
        for (Block block : ModContent.BLOCKS.values()) {
            if (done.add(block.getClass())) {
                registration.registerBlockComponent(provider, block.getClass());
            }
        }
    }
}
