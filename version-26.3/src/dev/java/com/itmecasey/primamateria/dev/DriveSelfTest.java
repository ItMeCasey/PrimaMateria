package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.blockentity.DriveBlockEntity;
import com.itmecasey.primamateria.blockentity.SieveBlockEntity;
import com.itmecasey.primamateria.event.GameEvents;
import com.itmecasey.primamateria.sieve.SieveLogic;
import com.itmecasey.primamateria.view.DriveKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Server checks of the three drives. Game time does not advance during the self test, so the drives are ticked by hand through
 * {@link DriveBlockEntity#tickOnce}, which keeps its own counters.
 */
public final class DriveSelfTest {
    private DriveSelfTest() {
    }

    /** Where the powered drive of the test stands; the loader dev classes use it to test the energy capability. */
    public static BlockPos poweredPos(ServerLevel level) {
        return SelfTest.area(level, 4).offset(0, 0, 12);
    }

    private record Rig(DriveBlockEntity drive, SieveBlockEntity sieve) {
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        for (BlockPos p : new BlockPos[] {pos, pos.above(), pos.east(), pos.below()}) {
            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        }
    }

    /** A drive with a sieve (cloth mesh) on top. */
    private static Rig rig(ServerLevel level, BlockPos pos, String driveId) {
        clear(level, pos);
        level.setBlockAndUpdate(pos, ModContent.block(driveId).defaultBlockState());
        level.setBlockAndUpdate(pos.above(), ModContent.block(Ids.SIEVE).defaultBlockState());
        SieveBlockEntity sieve = (SieveBlockEntity) level.getBlockEntity(pos.above());
        sieve.state().setMesh("primamateria:cloth_mesh", 1, 1.0);
        return new Rig((DriveBlockEntity) level.getBlockEntity(pos), sieve);
    }

    private static boolean fill(ServerLevel level, Rig r) {
        return SieveLogic.insert(level, r.sieve, new ItemStack(Items.GRAVEL), null);
    }

    /** Ticks the drive until the sieve is empty or {@code max} ticks passed; returns the ticks used. */
    private static int tickUntilDone(ServerLevel level, Rig r, int max) {
        int t = 0;
        while (t < max && r.sieve.state().filled()) {
            r.drive.tickOnce(level);
            t++;
        }
        return t;
    }

    private static void tick(ServerLevel level, Rig r, int ticks) {
        for (int i = 0; i < ticks; i++) {
            r.drive.tickOnce(level);
        }
    }

    private static int count(DriveBlockEntity be, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < DriveBlockEntity.BUFFER; i++) {
            if (be.getItem(i).is(item)) {
                n += be.getItem(i).getCount();
            }
        }
        return n;
    }

    private static int total(DriveBlockEntity be) {
        int n = 0;
        for (int i = 0; i < DriveBlockEntity.BUFFER; i++) {
            n += be.getItem(i).getCount();
        }
        return n;
    }

    private static void emptyBuffer(DriveBlockEntity be) {
        be.takeBuffer();
    }

    public static void run(MinecraftServer server, ServerLevel level) {
        BlockPos base = SelfTest.area(level, 4);
        GameEvents.SIFTED.clear();
        GameEvents.SIFTED.listen(e -> e.drops.add(new GameEvents.Drop("minecraft:stick", 3)));
        int[] wound = {0};
        GameEvents.DRIVE_WOUND.clear();
        GameEvents.DRIVE_WOUND.listen(e -> wound[0]++);
        try {
            windup(level, base, wound);
            furnace(level, base.offset(0, 0, 6));
            powered(level, base.offset(0, 0, 12));
        } finally {
            GameEvents.SIFTED.clear();
            GameEvents.DRIVE_WOUND.clear();
        }
    }

    private static void windup(ServerLevel level, BlockPos pos, int[] wound) {
        Rig r = rig(level, pos, Ids.WINDUP_DRIVE);
        SelfTest.verdict("windup_drive_has_the_right_kind", r.drive.kind() == DriveKind.WINDUP, String.valueOf(r.drive.kind()));
        SelfTest.verdict("drive_block_holds_a_nine_slot_buffer", r.drive.getContainerSize() == 9 && r.drive.freeSlots() == 9, "slots " + r.drive.getContainerSize());

        // faces: the machine above delivers through the top, hoppers and pipes empty from the bottom and the sides
        ItemStack probe = new ItemStack(Items.STICK);
        SelfTest.verdict("buffer_accepts_from_the_top_only", r.drive.canPlaceItemThroughFace(0, probe, Direction.UP)
                && !r.drive.canPlaceItemThroughFace(0, probe, Direction.DOWN) && !r.drive.canPlaceItemThroughFace(0, probe, Direction.EAST), "up yes, down and side no");
        SelfTest.verdict("buffer_empties_through_bottom_and_sides", r.drive.canTakeItemThroughFace(0, probe, Direction.DOWN)
                && r.drive.canTakeItemThroughFace(0, probe, Direction.NORTH) && !r.drive.canTakeItemThroughFace(0, probe, Direction.UP), "down and side yes, top no");
        SelfTest.verdict("windup_drive_has_no_fuel_slot", r.drive.getSlotsForFace(Direction.EAST).length == 9, "slots " + r.drive.getSlotsForFace(Direction.EAST).length);

        // winding by hand
        for (int i = 0; i < 10; i++) {
            r.drive.wind(level, null, true);
        }
        SelfTest.verdict("cranking_stores_turns", r.drive.state().turns() == 10, "turns " + r.drive.state().turns());
        for (int i = 0; i < 60; i++) {
            r.drive.wind(level, null, true);
        }
        SelfTest.verdict("turns_stop_at_the_maximum", r.drive.state().turns() == 48, "turns " + r.drive.state().turns());
        SelfTest.verdict("wound_event_fired_for_every_stored_turn", wound[0] == 48, "events " + wound[0]);
        r.drive.state().restore(10, 0, 0, 0, 0);

        // an idle drive does nothing, a loaded sieve gets sifted
        tick(level, r, 20);
        SelfTest.verdict("idle_drive_keeps_its_turns", r.drive.state().turns() == 10 && !r.drive.running(), "turns " + r.drive.state().turns());
        SelfTest.verdict("sieve_filled", fill(level, r), "gravel in");
        int ticks = tickUntilDone(level, r, 300);
        SelfTest.verdict("windup_drive_sifts_a_block", !r.sieve.state().filled() && ticks >= 50 && ticks <= 70, ticks + " ticks");
        SelfTest.verdict("windup_spends_one_turn_per_shake", r.drive.state().turns() == 2, "turns left " + r.drive.state().turns());
        SelfTest.verdict("output_reaches_the_drive_buffer", count(r.drive, Items.STICK) == 3, "sticks " + count(r.drive, Items.STICK));

        // a hopper under the drive takes from the buffer
        level.setBlockAndUpdate(pos.below(), Blocks.HOPPER.defaultBlockState());
        HopperBlockEntity hopper = (HopperBlockEntity) level.getBlockEntity(pos.below());
        int totalBefore = total(r.drive);
        boolean sucked = HopperBlockEntity.suckInItems(level, hopper);
        int inHopper = 0;
        for (int i = 0; i < hopper.getContainerSize(); i++) {
            inHopper += hopper.getItem(i).getCount();
        }
        SelfTest.verdict("hopper_takes_from_the_buffer", sucked && inHopper == 1 && total(r.drive) == totalBefore - 1, "hopper holds " + inHopper);
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        emptyBuffer(r.drive);

        // a full buffer stops the drive
        r.drive.state().restore(20, 0, 0, 0, 0);
        for (int i = 0; i < 7; i++) {
            r.drive.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        fill(level, r);
        tick(level, r, 40);
        SelfTest.verdict("full_buffer_stops_the_drive", r.sieve.state().shakes() == 0 && r.drive.blocked() && r.drive.state().turns() == 20,
                "shakes " + r.sieve.state().shakes() + ", free slots " + r.drive.freeSlots());
        r.drive.removeItemNoUpdate(0);
        tick(level, r, 40);
        SelfTest.verdict("room_in_the_buffer_lets_it_run_again", r.sieve.state().shakes() > 0 && !r.drive.blocked(), "shakes " + r.sieve.state().shakes());
        tickUntilDone(level, r, 300);
        emptyBuffer(r.drive);

        // redstone stops it
        r.drive.state().restore(20, 0, 0, 0, 0);
        fill(level, r);
        level.setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        tick(level, r, 40);
        SelfTest.verdict("redstone_signal_stops_the_drive", r.sieve.state().shakes() == 0, "shakes " + r.sieve.state().shakes());
        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        tick(level, r, 40);
        SelfTest.verdict("drive_runs_again_without_the_signal", r.sieve.state().shakes() > 0, "shakes " + r.sieve.state().shakes());
        tickUntilDone(level, r, 300);

        // an empty drive with a loaded sieve stands still
        r.drive.state().restore(0, 0, 0, 0, 0);
        fill(level, r);
        tick(level, r, 40);
        SelfTest.verdict("windup_without_turns_does_not_run", r.sieve.state().shakes() == 0, "shakes " + r.sieve.state().shakes());
        r.sieve.state().clearBlock();
    }

    private static void furnace(ServerLevel level, BlockPos pos) {
        Rig r = rig(level, pos, Ids.FURNACE_DRIVE);
        SelfTest.verdict("furnace_drive_has_a_fuel_slot", r.drive.getContainerSize() == 10 && r.drive.getSlotsForFace(Direction.EAST).length == 10,
                "slots " + r.drive.getContainerSize());
        ItemStack coal = new ItemStack(Items.COAL, 5);
        SelfTest.verdict("fuel_goes_in_from_the_side_not_the_top", r.drive.canPlaceItemThroughFace(9, coal, Direction.EAST)
                && !r.drive.canPlaceItemThroughFace(9, coal, Direction.UP) && !r.drive.canPlaceItemThroughFace(9, new ItemStack(Items.COBBLESTONE), Direction.EAST),
                "coal yes, cobblestone no");
        SelfTest.verdict("lava_bucket_is_not_fuel", !r.drive.canPlaceItem(9, new ItemStack(Items.LAVA_BUCKET)), "no remainder to lose");

        // no fuel, no work
        fill(level, r);
        tick(level, r, 40);
        SelfTest.verdict("furnace_without_fuel_does_not_run", r.sieve.state().shakes() == 0 && !r.drive.state().burning(), "shakes " + r.sieve.state().shakes());

        int taken = r.drive.insertFuel(coal);
        SelfTest.verdict("fuel_is_taken_by_hand", taken == 5 && r.drive.getItem(9).getCount() == 5, "taken " + taken);
        int ticks = tickUntilDone(level, r, 300);
        int burnt = 1600 - r.drive.state().burnLeft();
        SelfTest.verdict("furnace_drive_sifts_a_block", !r.sieve.state().filled() && ticks >= 25 && ticks <= 40, ticks + " ticks");
        SelfTest.verdict("one_fuel_item_is_burning", r.drive.getItem(9).getCount() == 4 && r.drive.state().burning() && burnt >= 20 && burnt <= 45,
                "fuel " + r.drive.getItem(9).getCount() + ", burnt " + burnt);
        SelfTest.verdict("furnace_is_lit_while_burning", level.getBlockState(pos).getValue(StateProps.LIT), "lit");
        int left = r.drive.state().burnLeft();
        tick(level, r, 100);
        SelfTest.verdict("furnace_burns_only_while_the_machine_has_work", r.drive.state().burnLeft() == left, "burn left " + left);
        SelfTest.verdict("furnace_output_reaches_the_buffer", count(r.drive, Items.STICK) == 3, "sticks " + count(r.drive, Items.STICK));
        emptyBuffer(r.drive);
    }

    private static void powered(ServerLevel level, BlockPos pos) {
        Rig r = rig(level, pos, Ids.POWERED_DRIVE);
        SelfTest.verdict("powered_drive_starts_empty", r.drive.state().energy() == 0 && level.getBlockState(pos).getValue(StateProps.CHARGE) == 0, "no energy");
        fill(level, r);
        tick(level, r, 30);
        SelfTest.verdict("powered_without_energy_does_not_run", r.sieve.state().shakes() == 0, "shakes " + r.sieve.state().shakes());
        SelfTest.verdict("energy_is_accepted", r.drive.receiveEnergy(500, false) == 500 && r.drive.state().energy() == 500, "energy " + r.drive.state().energy());
        SelfTest.verdict("charge_shows_in_the_block_state", level.getBlockState(pos).getValue(StateProps.CHARGE) == 1, "step " + level.getBlockState(pos).getValue(StateProps.CHARGE));
        int ticks = tickUntilDone(level, r, 300);
        SelfTest.verdict("powered_drive_sifts_a_block", !r.sieve.state().filled() && ticks >= 10 && ticks <= 25, ticks + " ticks");
        SelfTest.verdict("eight_shakes_cost_320_fe", r.drive.state().energy() == 180, "energy " + r.drive.state().energy());
        SelfTest.verdict("energy_is_capped", r.drive.receiveEnergy(50000, false) == 9820 && r.drive.state().energy() == 10000
                && level.getBlockState(pos).getValue(StateProps.CHARGE) == 5, "energy " + r.drive.state().energy());
        SelfTest.verdict("simulated_energy_changes_nothing", r.drive.receiveEnergy(100, true) == 0 && r.drive.state().energy() == 10000, "full");
        r.drive.state().restore(0, 0, 0, 30, 0);
        r.sieve.state().clearBlock();
        fill(level, r);
        tick(level, r, 30);
        SelfTest.verdict("not_enough_energy_for_a_shake_does_not_run", r.sieve.state().shakes() == 0 && r.drive.state().energy() == 30, "energy 30");
    }
}
