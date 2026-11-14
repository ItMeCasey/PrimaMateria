package com.itmecasey.primamateria.item;

import com.itmecasey.primamateria.ModContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A hammer. It is a tool that mines stone, sand, gravel and leaves (the blocks of {@link #MINEABLE}); what a block crushes
 * into when broken with it is the crushing recipes' business ({@code hammer.HammerLogic}). The tier is 1 (wood) to 6 (netherite).
 */
public class HammerItem extends Item {
    /** The blocks a hammer mines at tool speed (data/primamateria/tags/block/mineable/hammer.json). */
    public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, ModContent.id("mineable/hammer"));

    private final int tier;

    public HammerItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    /** The hammer tier of a stack, or 0 when it is not a hammer. */
    public static int tierOf(ItemStack stack) {
        return stack.getItem() instanceof HammerItem hammer ? hammer.tier : 0;
    }
}
