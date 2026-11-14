package com.itmecasey.primamateria.sieve;

import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.blockentity.SieveBlockEntity;
import com.itmecasey.primamateria.data.Meshes;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.recipe.PrimaRecipes;
import com.itmecasey.primamateria.sift.Drop;
import com.itmecasey.primamateria.sift.DropRoller;
import com.itmecasey.primamateria.sift.SiftMath;
import com.itmecasey.primamateria.sift.SiftState;
import com.itmecasey.primamateria.util.Transfer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Everything that happens to a sieve when it is used: fitting a mesh, putting a block in, shaking (alone or together with the
 * sieves next to it), handing out the drops. Runs on the server only.
 */
public final class SieveLogic {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");

    private SieveLogic() {
    }

    // ---- helpers ----

    public static String itemId(ItemStack stack) {
        return Transfer.itemId(stack);
    }

    public static ItemStack stackOf(String id, int count) {
        return Transfer.stackOf(id, count);
    }

    public static boolean siftable(ServerLevel level, ItemStack stack) {
        return PrimaRecipes.siftsFor(level.recipeAccess(), stack).siftable();
    }

    /** The sieves linked to the one at {@code pos}: same height, within the link radius, nearest first. Empty when linking is off. */
    public static List<SieveBlockEntity> linked(ServerLevel level, BlockPos pos) {
        List<SieveBlockEntity> out = new ArrayList<>();
        if (!Settings.getBool("sieve.link")) {
            return out;
        }
        for (int[] o : SiftMath.linkOffsets(Settings.getInt("sieve.link_radius"))) {
            if (level.getBlockEntity(pos.offset(o[0], 0, o[1])) instanceof SieveBlockEntity be) {
                out.add(be);
            }
        }
        return out;
    }

    // ---- use ----

    /** One use of the sieve at {@code pos} with {@code stack} in hand (empty for an empty hand). */
    public static InteractionResult interact(ServerLevel level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof SieveBlockEntity be)) {
            return InteractionResult.PASS;
        }
        SiftState state = be.state();
        if (!stack.isEmpty()) {
            Meshes.Mesh mesh = Meshes.get(itemId(stack));
            if (mesh != null && !state.filled()) {
                fitMesh(level, pos, be, player, stack, mesh);
                return InteractionResult.SUCCESS;
            }
            if (state.canAccept() && siftable(level, stack)) {
                putIn(level, pos, be, player, stack);
                return InteractionResult.SUCCESS;
            }
        }
        if (state.filled()) {
            shakeTogether(level, pos, be, player, stack);
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty() && player.isShiftKeyDown() && state.hasMesh()) {
            takeMesh(level, pos, be, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void fitMesh(ServerLevel level, BlockPos pos, SieveBlockEntity be, Player player, ItemStack stack, Meshes.Mesh mesh) {
        SiftState state = be.state();
        ItemStack old = state.hasMesh() ? stackOf(state.meshId(), 1) : ItemStack.EMPTY;
        state.setMesh(mesh.item(), mesh.tier(), mesh.speed());
        giveBack(player, old);
        consume(player, stack);
        be.changed();
    }

    private static void takeMesh(ServerLevel level, BlockPos pos, SieveBlockEntity be, Player player) {
        ItemStack back = stackOf(be.state().meshId(), 1);
        be.state().setMesh("", 0, 1.0);
        giveBack(player, back);
        be.changed();
    }

    /** Puts one block from the stack in the sieve at {@code pos}, and in every empty linked sieve with a mesh while the stack lasts. */
    private static void putIn(ServerLevel level, BlockPos pos, SieveBlockEntity origin, Player player, ItemStack stack) {
        if (insert(level, origin, stack, player)) {
            consume(player, stack);
        }
        refillLinked(level, pos, player, stack);
    }

    private static void refillLinked(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        for (SieveBlockEntity other : linked(level, pos)) {
            if (stack.isEmpty() || !other.state().canAccept()) {
                continue;
            }
            if (insert(level, other, stack, player)) {
                consume(player, stack);
            }
        }
    }

    /**
     * Starts sifting one block from {@code source} (it is not shrunk here): checks the sieve takes it, rolls the drops now so the
     * block's finds are known from the start, and saves.
     */
    public static boolean insert(ServerLevel level, SieveBlockEntity be, ItemStack source, @Nullable Player player) {
        SiftState state = be.state();
        if (!state.canAccept()) {
            return false;
        }
        PrimaRecipes.Sifts sifts = PrimaRecipes.siftsFor(level.recipeAccess(), source);
        if (!sifts.siftable()) {
            return false;
        }
        long seed = level.getRandom().nextLong();
        DropRoller.Rolled rolled = DropRoller.roll(sifts.drops(), sifts.residue(), state.meshTier(),
                Settings.getDouble("sieve.drop_multiplier"), new Random(seed));
        boolean started = state.start(itemId(source), rolled, Settings.getInt("sieve.shakes_per_block"), seed);
        if (started) {
            be.changed();
        }
        return started;
    }

    // ---- shaking ----

    /** Shakes the sieve and every filled sieve linked to it; sieves that finish are refilled from the hand stack. */
    private static void shakeTogether(ServerLevel level, BlockPos pos, SieveBlockEntity origin, Player player, ItemStack hand) {
        long now = level.getGameTime();
        List<SieveBlockEntity> group = new ArrayList<>();
        group.add(origin);
        group.addAll(linked(level, pos));
        for (SieveBlockEntity be : group) {
            if (be == origin || be.state().filled()) {
                if (be.state().ready(now, SiftMath.HAND_MIN_TICKS)) {
                    shakeOne(level, be, player, hand, now, be == origin);
                }
            } else if (!hand.isEmpty() && be.state().canAccept() && insert(level, be, hand, player)) {
                consume(player, hand);
            }
        }
    }

    /** One shake of one sieve, with the refill from the hand when the block finishes. */
    private static void shakeOne(ServerLevel level, SieveBlockEntity be, @Nullable Player player, ItemStack hand, long now, boolean sound) {
        SiftState state = be.state();
        BlockPos pos = be.getBlockPos();
        SiftState.Shake result = state.shake(now);
        be.setMoving(true);
        if (result == SiftState.Shake.DONE) {
            finish(level, be, player);
            if (player != null && !hand.isEmpty() && be.state().canAccept() && insert(level, be, hand, player)) {
                consume(player, hand);
            }
        }
        be.changed();
    }

    /** A drive shakes only its own sieve, no matter the link setting. Returns whether the sieve was shaken. */
    public static boolean shakeByDrive(ServerLevel level, SieveBlockEntity be, int minTicks) {
        long now = level.getGameTime();
        if (!be.state().ready(now, minTicks)) {
            return false;
        }
        shakeOne(level, be, null, ItemStack.EMPTY, now, true);
        return true;
    }

    // ---- finishing ----

    /** The block is done: tell the listeners, hand out the drops and the residue, empty the tray. */
    private static void finish(ServerLevel level, SieveBlockEntity be, @Nullable Player player) {
        SiftState state = be.state();
        BlockPos pos = be.getBlockPos();
        String input = state.block();
        List<GameEvents.Drop> asked = new ArrayList<>();
        for (Drop d : state.drops()) {
            asked.add(new GameEvents.Drop(d.item(), d.count()));
        }
        GameEvents.Sifted event = GameEvents.SIFTED.fire(new GameEvents.Sifted(
                new GameEvents.Where(level, pos.getX(), pos.getY(), pos.getZ(), player), input, state.meshId(), state.meshTier(), asked));
        List<Drop> residue = state.residue();
        state.clearBlock();
        if (event.canceled()) {
            deliver(level, pos, player, List.of(new Drop(input, 1)), List.of());
            return;
        }
        List<Drop> drops = new ArrayList<>();
        for (GameEvents.Drop d : event.drops) {
            if (d.count() >= 1) {
                drops.add(new Drop(d.item(), d.count()));
            }
        }
        deliver(level, pos, player, drops, residue);
    }

    /**
     * Drops go into the container below the sieve when it takes them and otherwise hop off the tray toward the player;
     * residue falls straight down.
     */
    private static void deliver(ServerLevel level, BlockPos pos, @Nullable Player player, List<Drop> drops, List<Drop> residue) {
        Container below = Transfer.below(level, pos);
        Transfer.deliver(level, pos, below, player, drops);
        Transfer.deliver(level, pos, below, null, residue);
    }

    // ---- small effects ----

    private static void giveBack(@Nullable Player player, ItemStack stack) {
        Transfer.giveBack(player, stack);
    }

    private static void consume(Player player, ItemStack stack) {
        Transfer.consume(player, stack);
    }
}
