package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.barrel.BarrelLogic;
import com.itmecasey.primamateria.blockentity.BarrelBlockEntity;
import com.itmecasey.primamateria.blockentity.CastingTrayBlockEntity;
import com.itmecasey.primamateria.blockentity.CrucibleBlockEntity;
import com.itmecasey.primamateria.fluid.FluidIds;
import com.itmecasey.primamateria.fluid.FluidTanks;
import com.itmecasey.primamateria.fluid.MachineTank;
import com.itmecasey.primamateria.fluid.ModFluids;
import com.itmecasey.primamateria.machine.CrucibleLogic;
import com.itmecasey.primamateria.view.TrayView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.resources.Identifier;

/**
 * Server checks of the four real fluids: what is registered, that a bucket places a real source block and picks it up again, how they
 * flow, burn and catch fire, how real buckets and the machines work together, and the machines' tanks as a pipe uses them. The
 * loader dev classes add the checks of each loader's own fluid handler on the same machines ({@link #machines}).
 */
public final class FluidSelfTest {
    /** Where the machines of this test stand. */
    public record Machines(BlockPos crucible, BlockPos wooden, BlockPos clay, BlockPos tray) {
    }

    private static Machines placed;

    private FluidSelfTest() {
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(pos, state, 2);
    }

    /** A clean stretch of the test world: air above a stone floor, from {@code base} 26 blocks east and 12 south, 8 blocks high. */
    private static void clear(ServerLevel level, BlockPos base) {
        for (int x = -1; x <= 26; x++) {
            for (int z = -1; z <= 12; z++) {
                for (int y = 0; y <= 8; y++) {
                    BlockPos pos = base.offset(x, y, z);
                    level.setBlock(pos, y == 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    /** The machines (a crucible, a wooden barrel, a clay barrel and a casting tray, each empty), placed once per run. */
    public static Machines machines(ServerLevel level) {
        if (placed == null) {
            BlockPos base = SelfTest.area(level, 6);
            clear(level, base);
            BlockPos row = base.offset(0, 1, 0);
            placed = new Machines(row, row.offset(2, 0, 0), row.offset(4, 0, 0), row.offset(6, 0, 0));
            put(level, placed.crucible(), ModContent.block(Ids.CRUCIBLE).defaultBlockState());
            put(level, placed.wooden(), ModContent.block(Ids.WOODEN_BARREL).defaultBlockState());
            put(level, placed.clay(), ModContent.block(Ids.CLAY_BARREL).defaultBlockState());
            put(level, placed.tray(), ModContent.block(Ids.CASTING_TRAY).defaultBlockState());
        }
        return placed;
    }

    public static void run(MinecraftServer server, ServerLevel level) {
        placed = null;
        Machines m = machines(level);
        registries();
        buckets(level);
        behavior(level);
        withMachines(server, level, m);
        tanks(level, m);
    }

    // ---- registries ----

    private static void registries() {
        for (String id : Ids.FLUIDS) {
            Identifier key = ModContent.id(id);
            Fluid source = BuiltInRegistries.FLUID.getValue(key);
            Fluid flowing = BuiltInRegistries.FLUID.getValue(ModContent.id("flowing_" + id));
            SelfTest.verdict("fluid_source_registered_" + id, source != Fluids.EMPTY && source == ModFluids.source(id)
                    && source instanceof FlowingFluid && ((FlowingFluid) source).isSource(source.defaultFluidState()), String.valueOf(BuiltInRegistries.FLUID.getKey(source)));
            SelfTest.verdict("fluid_flowing_registered_" + id, flowing != Fluids.EMPTY && flowing == ModFluids.flowing(id)
                    && !((FlowingFluid) flowing).isSource(flowing.defaultFluidState().setValue(FlowingFluid.LEVEL, 7)),
                    String.valueOf(BuiltInRegistries.FLUID.getKey(flowing)));
            SelfTest.verdict("fluid_links_" + id, ((FlowingFluid) source).getFlowing() == flowing && ((FlowingFluid) flowing).getSource() == source
                    && source.isSame(flowing) && flowing.isSame(source), "source and flowing point at each other");
            net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.getValue(key);
            SelfTest.verdict("fluid_block_registered_" + id, block instanceof LiquidBlock && block == ModFluids.block(id)
                    && block.defaultBlockState().getFluidState().getType() == source, String.valueOf(BuiltInRegistries.BLOCK.getKey(block)));
            Item bucket = BuiltInRegistries.ITEM.getValue(ModContent.id(id + "_bucket"));
            SelfTest.verdict("fluid_bucket_registered_" + id, bucket instanceof BucketItem b && b.getContent() == source && bucket == ModFluids.bucket(id)
                    && source.getBucket() == bucket && !bucket.getDefaultInstance().isEmpty(), String.valueOf(BuiltInRegistries.ITEM.getKey(bucket)));
            SelfTest.verdict("fluid_legacy_block_is_the_liquid_block_" + id, source.defaultFluidState().createLegacyBlock().getBlock() == block,
                    "source state makes " + block);
        }
        SelfTest.verdict("fluid_ids_by_registry", FluidIds.idOf(Fluids.FLOWING_LAVA).equals("minecraft:lava")
                        && FluidIds.idOf(ModFluids.flowing("molten_iron")).equals("primamateria:molten_iron")
                        && FluidIds.idOf(Fluids.EMPTY).isEmpty() && FluidIds.fluidOf("primamateria:brine").orElse(null) == ModFluids.source("brine")
                        && FluidIds.fluidOf("nomod:nothing").isEmpty(),
                "source and flowing forms share an id; unknown ids are empty");
        SelfTest.verdict("fluid_buckets_found_by_item", FluidIds.carriedBy(new ItemStack(Items.WATER_BUCKET)).orElse("").equals("minecraft:water")
                        && FluidIds.carriedBy(new ItemStack(ModFluids.bucket("molten_gold"))).orElse("").equals("primamateria:molten_gold")
                        && FluidIds.carriedBy(new ItemStack(Items.BUCKET)).isEmpty() && FluidIds.carriedBy(ItemStack.EMPTY).isEmpty()
                        && FluidIds.bucketOf("minecraft:lava").orElse("").equals("minecraft:lava_bucket")
                        && FluidIds.bucketOf("primamateria:brine").orElse("").equals("primamateria:brine_bucket"),
                "water, lava, brine and the molten buckets resolve both ways");
    }

    // ---- placing and picking up ----

    private static void buckets(ServerLevel level) {
        BlockPos w = SelfTest.area(level, 6).offset(0, 1, 8);
        int i = 0;
        for (String id : Ids.FLUIDS) {
            BlockPos pos = w.offset(3 * i++, 0, 0);
            BucketItem bucket = (BucketItem) ModFluids.bucket(id);
            boolean placed = bucket.emptyContents(null, level, pos, null);
            BlockState state = level.getBlockState(pos);
            FluidState fluid = level.getFluidState(pos);
            SelfTest.verdict("bucket_places_a_source_block_" + id, placed && state.is(ModFluids.block(id)) && fluid.isSource()
                    && fluid.getType() == ModFluids.source(id), String.valueOf(state.getBlock()));
            ItemStack back = state.getBlock() instanceof BucketPickup pickup ? pickup.pickupBlock(null, level, pos, state) : ItemStack.EMPTY;
            SelfTest.verdict("bucket_scoops_the_source_back_" + id, back.is(bucket) && level.getBlockState(pos).isAir(),
                    back + ", block now " + level.getBlockState(pos).getBlock());
            SelfTest.verdict("dispenser_knows_the_bucket_" + id, net.minecraft.world.level.block.DispenserBlock.DISPENSER_REGISTRY.containsKey(bucket), "behavior registered");
        }
        // a dispenser places a molten bucket in front of it and keeps the empty bucket
        BlockPos dispenser = SelfTest.area(level, 6).offset(20, 1, 10);
        put(level, dispenser, Blocks.DISPENSER.defaultBlockState());
        net.minecraft.world.level.block.entity.DispenserBlockEntity box = (net.minecraft.world.level.block.entity.DispenserBlockEntity) level.getBlockEntity(dispenser);
        box.setItem(0, new ItemStack(ModFluids.bucket("molten_iron")));
        level.getBlockState(dispenser).tick(level, dispenser, level.getRandom());
        BlockPos front = dispenser.north();
        SelfTest.verdict("dispenser_places_a_molten_bucket", level.getBlockState(front).is(ModFluids.block("molten_iron")) && level.getFluidState(front).isSource()
                && box.getItem(0).is(Items.BUCKET), level.getBlockState(front).getBlock() + ", slot holds " + box.getItem(0));
        level.setBlock(front, Blocks.AIR.defaultBlockState(), 2);
    }

    // ---- how they flow, burn and catch fire ----

    private static void behavior(ServerLevel level) {
        BlockPos w = SelfTest.area(level, 6).offset(0, 1, 4);
        // lava's fire rule only works near a player: the test turns that limit off for a moment
        var radius = net.minecraft.world.level.gamerules.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER;
        int oldRadius = level.getGameRules().get(radius);
        level.getGameRules().set(radius, -1, level.getServer());
        int i = 0;
        for (String id : Ids.FLUIDS) {
            BlockPos pos = w.offset(6 + 4 * i++, 0, 0);
            level.setBlock(pos, ModFluids.block(id).defaultBlockState(), 3);
            FluidState source = level.getFluidState(pos);
            source.tick(level, pos, level.getBlockState(pos));
            int flowing = 0;
            StringBuilder sides = new StringBuilder();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                FluidState next = level.getFluidState(pos.relative(d));
                if (next.getType() == ModFluids.flowing(id) && !next.isSource()) {
                    flowing++;
                }
                sides.append(d.getName().charAt(0)).append('=').append(level.getBlockState(pos.relative(d)).getBlock()).append(' ');
            }
            SelfTest.verdict("fluid_spreads_on_the_floor_" + id, flowing == 4, flowing + " of 4 sides flow: " + sides);
            boolean molten = ModFluids.isMolten(id);
            SelfTest.verdict("fluid_ticks_randomly_only_when_molten_" + id, source.isRandomlyTicking() == molten, "random tick " + source.isRandomlyTicking());

            // a flammable block beside the fluid: lava's own random tick sets fire above it, water's never does
            BlockPos planks = pos.offset(0, 0, 1);
            level.setBlock(planks, Blocks.OAK_PLANKS.defaultBlockState(), 2);
            BlockPos above = planks.above();
            RandomSource random = RandomSource.create(7);
            int fires = 0;
            for (int n = 0; n < 600 && fires == 0; n++) {
                level.setBlock(pos, ModFluids.block(id).defaultBlockState(), 2);
                level.getFluidState(pos).randomTick(level, pos, random);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (level.getBlockState(pos.offset(dx, 1, dz)).is(Blocks.FIRE) || level.getBlockState(above.offset(dx, 0, dz)).is(Blocks.FIRE)) {
                            fires++;
                        }
                    }
                }
            }
            SelfTest.verdict("fluid_sets_flammables_alight_only_when_molten_" + id, (fires > 0) == molten, fires + " fires");
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 0; dy <= 3; dy++) {
                        level.setBlock(pos.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        level.getGameRules().set(radius, oldRadius, level.getServer());
    }

    // ---- real buckets with the machines ----

    private static void withMachines(MinecraftServer server, ServerLevel level, Machines m) {
        ServerPlayer player = SelfTest.testPlayer(server, level, "fluidtest");
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(m.crucible());
        BarrelBlockEntity wooden = (BarrelBlockEntity) level.getBlockEntity(m.wooden());
        BarrelBlockEntity clay = (BarrelBlockEntity) level.getBlockEntity(m.clay());
        crucible.state().restoreFluid("", 0);
        wooden.state().restoreFluid("", 0);
        clay.state().restoreFluid("", 0);

        // a barrel: the wooden one refuses a molten bucket, the clay one takes it and gives it back
        Item copperBucket = ModFluids.bucket("molten_copper");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(copperBucket));
        InteractionResult refused = BarrelLogic.interact(level, m.wooden(), player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("wooden_barrel_refuses_a_molten_bucket", refused == InteractionResult.FAIL && wooden.state().amount() == 0
                && player.getMainHandItem().is(copperBucket), String.valueOf(refused));
        BarrelLogic.interact(level, m.clay(), player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("clay_barrel_takes_a_molten_bucket", clay.state().amount() == 1000 && clay.state().fluid().equals("primamateria:molten_copper")
                && player.getMainHandItem().is(Items.BUCKET), clay.state().fluid() + " " + clay.state().amount());
        BarrelLogic.interact(level, m.clay(), player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("empty_bucket_scoops_molten_copper_from_the_clay_barrel", clay.state().amount() == 0 && player.getMainHandItem().is(copperBucket),
                String.valueOf(player.getMainHandItem()));

        // the round trip: bucket into the crucible, out into a bucket, onto the ground as a real block, scooped, back into the crucible
        CrucibleLogic.useCrucible(level, m.crucible(), player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("molten_bucket_fills_the_crucible", crucible.state().amount() == 1000 && player.getMainHandItem().is(Items.BUCKET), "1000 mB in");
        CrucibleLogic.useCrucible(level, m.crucible(), player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("crucible_empties_into_a_molten_bucket", crucible.state().amount() == 0 && player.getMainHandItem().is(copperBucket), "bucket full");
        BlockPos spot = m.crucible().offset(0, 0, 10);
        boolean placed = ((BucketItem) player.getMainHandItem().getItem()).emptyContents(player, level, spot, null);
        SelfTest.verdict("that_bucket_places_a_real_source_block", placed && level.getBlockState(spot).is(ModFluids.block("molten_copper"))
                && level.getFluidState(spot).isSource(), String.valueOf(level.getBlockState(spot).getBlock()));
        BlockState liquid = level.getBlockState(spot);
        ItemStack scooped = ((BucketPickup) liquid.getBlock()).pickupBlock(player, level, spot, liquid);
        SelfTest.verdict("the_source_block_scoops_back_to_a_molten_bucket", scooped.is(copperBucket) && level.getBlockState(spot).isAir(), String.valueOf(scooped));
        player.setItemInHand(InteractionHand.MAIN_HAND, scooped);
        CrucibleLogic.useCrucible(level, m.crucible(), player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("and_back_into_the_crucible", crucible.state().amount() == 1000 && crucible.state().fluid().equals("primamateria:molten_copper"),
                crucible.state().fluid() + " " + crucible.state().amount());
        crucible.state().restoreFluid("", 0);

        // brine moves through the barrel as a real bucket too
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModFluids.bucket("brine")));
        BarrelLogic.interact(level, m.wooden(), player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("brine_bucket_fills_the_wooden_barrel", wooden.state().fluid().equals("primamateria:brine") && wooden.state().amount() == 1000
                && player.getMainHandItem().is(Items.BUCKET), wooden.state().fluid());
        BarrelLogic.interact(level, m.wooden(), player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        SelfTest.verdict("and_it_scoops_back_as_the_real_brine_bucket", wooden.state().amount() == 0
                && player.getMainHandItem().is(ModFluids.bucket("brine")), String.valueOf(player.getMainHandItem()));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    // ---- the tanks the way a pipe uses them ----

    private static void tanks(ServerLevel level, Machines m) {
        CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(m.crucible());
        CastingTrayBlockEntity tray = (CastingTrayBlockEntity) level.getBlockEntity(m.tray());
        crucible.state().restoreFluid("", 0);
        String copper = "primamateria:molten_copper";
        MachineTank tank = FluidTanks.of(crucible);
        SelfTest.verdict("machines_offer_a_tank", tank != null && FluidTanks.of(level.getBlockEntity(m.wooden())) != null
                && FluidTanks.of(level.getBlockEntity(m.clay())) != null && FluidTanks.of(tray) != null
                && FluidTanks.of(level.getBlockEntity(m.crucible().offset(0, 0, 8))) == null, "crucible, both barrels, tray");
        int in = 0;
        for (int n = 0; n < 20; n++) {
            in += tank.insert(copper, 333, false);
        }
        SelfTest.verdict("pipe_fills_the_crucible_to_four_buckets", in == 4000 && crucible.state().amount() == 4000
                && crucible.state().fluid().equals(copper), in + " mB in");
        SelfTest.verdict("pipe_cannot_overfill_the_crucible", tank.insert(copper, 1, false) == 0 && tank.insert(copper, 1, true) == 0, "full");
        SelfTest.verdict("pipe_cannot_mix_a_second_kind", tank.insert("minecraft:lava", 100, false) == 0 && tank.insert("minecraft:water", 100, false) == 0,
                "lava and water refused");
        int out = 0;
        for (int n = 0; n < 20; n++) {
            out += tank.extract("", 700, false);
        }
        SelfTest.verdict("pipe_drains_the_crucible", out == 4000 && crucible.state().amount() == 0 && tank.fluid().isEmpty(), out + " mB out");
        SelfTest.verdict("simulate_changes_nothing", tank.insert(copper, 1000, true) == 1000 && crucible.state().amount() == 0, "still empty");

        MachineTank trayTank = FluidTanks.of(tray);
        int cast = trayTank.insert(copper, 1000, false);
        SelfTest.verdict("pipe_starts_a_cast_in_the_tray", cast == 100 && tray.state().stage() == TrayView.Stage.FILLING
                && tray.state().fluid().equals(copper), cast + " mB, " + tray.state().stage());
        SelfTest.verdict("a_busy_tray_takes_no_more", trayTank.insert(copper, 1000, false) == 0, "busy");
        tray.state().reset();
    }
}
