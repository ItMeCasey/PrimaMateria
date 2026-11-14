package com.itmecasey.primamateria.hammer;

import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.crush.CrushMath;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.item.HammerItem;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.sift.Drop;
import com.itmecasey.primamateria.util.Transfer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Crushing: what a hammer (or an auto hammer) turns a block into. Server side. */
public final class HammerLogic {
    private HammerLogic() {
    }

    /**
     * Rolls the results of crushing one block of {@code input} with a hammer of {@code hammerTier}, fires the crushed event and
     * returns the final drops. Returns null when no recipe crushes the block or the hammer is too weak.
     */
    @Nullable
    public static List<Drop> crush(ServerLevel level, BlockPos pos, ItemStack input, String hammerId, int hammerTier, boolean byMill,
                                   @Nullable Player player) {
        PrimaRecipes.Crush crush = PrimaRecipes.crushFor(level.recipeAccess(), input);
        if (crush == null || hammerTier < crush.hammerTier()) {
            return null;
        }
        long seed = level.getRandom().nextLong();
        List<Drop> rolled = CrushMath.roll(crush.results(), hammerTier, Settings.getDouble("hammer.bonus_per_tier"), new Random(seed));
        List<GameEvents.Drop> asked = new ArrayList<>();
        for (Drop d : rolled) {
            asked.add(new GameEvents.Drop(d.item(), d.count()));
        }
        GameEvents.Crushed event = GameEvents.CRUSHED.fire(new GameEvents.Crushed(
                new GameEvents.Where(level, pos.getX(), pos.getY(), pos.getZ(), player), Transfer.itemId(input), hammerId, hammerTier, byMill, asked));
        List<Drop> out = new ArrayList<>();
        for (GameEvents.Drop d : event.drops) {
            if (d.count() >= 1) {
                out.add(new Drop(d.item(), d.count()));
            }
        }
        return out;
    }

    /**
     * Called from the loader's block break event. When the player holds a hammer of enough tier and the block has a crushing
     * recipe, the block is removed here and the crushed results drop in place of its normal drop; the caller then cancels the
     * original break. Returns true when it handled the break.
     */
    public static boolean onBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        ItemStack tool = player.getMainHandItem();
        int tier = HammerItem.tierOf(tool);
        if (tier == 0) {
            return false;
        }
        ItemStack block = new ItemStack(state.getBlock().asItem());
        if (block.isEmpty()) {
            return false;
        }
        List<Drop> drops = crush(level, pos, block, Transfer.itemId(tool), tier, false, player);
        if (drops == null) {
            return false;
        }
        level.destroyBlock(pos, false, player, 512);
        for (Drop d : drops) {
            for (ItemStack stack : Transfer.split(d)) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
        tool.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
        return true;
    }
}
