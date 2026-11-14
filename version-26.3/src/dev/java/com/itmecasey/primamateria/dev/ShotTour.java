package com.itmecasey.primamateria.dev;

import com.itmecasey.primamateria.Dev;
import com.itmecasey.primamateria.Settings;
import com.itmecasey.primamateria.blockentity.SievePreviewBlockEntity;
import com.itmecasey.primamateria.view.DriveKind;
import com.itmecasey.primamateria.view.SieveView;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only camera tour of the sieve. Opens a fresh superflat world, builds sieves, plays made-up sifts through the preview
 * block entity and saves screenshots as {@code run/screenshots/primamateria_<name>.png}, then measures frame time with 20 sieves in
 * view for each animation level and quits. Does nothing unless {@code PRIMAMATERIA_SHOTS} is set. It never touches the real mouse or
 * keyboard: everything goes through the game's own objects.
 */
public final class ShotTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");
    private static final boolean ENABLED = System.getenv("PRIMAMATERIA_SHOTS") != null && Dev.ENABLED;
    private static final String WORLD = "primamateria_shots";
    private static final String[] TIERS = {"cloth", "copper", "iron", "diamond", "netherite"};

    private enum Stage { WAITING_FOR_MENU, LOADING_WORLD, TOURING, DONE }

    private record Step(int delay, Runnable action) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static Stage stage = Stage.WAITING_FOR_MENU;
    private static int idle;
    private static int wait;
    private static int ground;
    private static boolean pauseOnLostFocus;
    private static int fpsSamples;
    private static long fpsSum;
    private static boolean sampling;

    private ShotTour() {
    }

    public static void tick() {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.mouseHandler.isMouseGrabbed()) {
                minecraft.mouseHandler.releaseMouse();
            }
            if (sampling) {
                fpsSum += minecraft.getFps();
                fpsSamples++;
            }
            switch (stage) {
                case WAITING_FOR_MENU -> waitForMenu(minecraft);
                case LOADING_WORLD -> waitForWorld(minecraft);
                case TOURING -> runSteps();
                default -> {
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("PRIMAMATERIA-SHOTS failed", e);
            quit(minecraft);
        }
    }

    static void quit(Minecraft minecraft) {
        minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
        LOGGER.info("PRIMAMATERIA-SHOTS DONE");
        stage = Stage.DONE;
        minecraft.stop();
    }

    private static void waitForMenu(Minecraft minecraft) {
        if (minecraft.gui.overlay() != null || minecraft.level != null || minecraft.gui.screen() == null || ++idle < 20) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;
        LevelStorageSource source = minecraft.getLevelSource();
        if (source.levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                LOGGER.warn("PRIMAMATERIA-SHOTS could not delete the old world", e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        stage = Stage.LOADING_WORLD;
        idle = 0;
        LOGGER.info("PRIMAMATERIA-SHOTS creating world");
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                registries -> registries.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), new TitleScreen());
    }

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null
                || minecraft.getSingleplayerServer() == null || ++idle < 40) {
            return;
        }
        ground = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        LOGGER.info("PRIMAMATERIA-SHOTS in the world, ground at y={}", ground);
        plan();
        stage = Stage.TOURING;
    }

    private static void runSteps() {
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.poll();
        if (step == null) {
            return;
        }
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    // ---- the tour ----

    private static final List<SieveView.Find> FINDS = List.of(
            new SieveView.Find("minecraft:iron_nugget", 1, 0.2F, 0), new SieveView.Find("minecraft:gold_nugget", 1, 0.4F, 4),
            new SieveView.Find("minecraft:diamond", 1, 0.6F, 2), new SieveView.Find("minecraft:emerald", 1, 0.8F, 5));

    private static String meshId(int tier) {
        return "primamateria:" + TIERS[tier] + "_mesh";
    }

    private static String meshTexture(int tier) {
        return "primamateria:block/mesh_" + TIERS[tier];
    }

    private static SievePreviewBlockEntity be(BlockPos pos) {
        return Minecraft.getInstance().level.getBlockEntity(pos) instanceof SievePreviewBlockEntity s ? s : null;
    }

    private static void place(BlockPos pos, int slot, boolean moving) {
        server("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " primamateria:sieve[mesh=" + slot + ",moving=" + moving + "]");
    }

    private static void sift(BlockPos pos, int tier, DriveKind kind, int load, int shakes) {
        sift(pos, tier, kind, load, shakes, "minecraft:gravel");
    }

    private static void sift(BlockPos pos, int tier, DriveKind kind, int load, int shakes, String block) {
        SievePreviewBlockEntity s = be(pos);
        if (s == null) {
            throw new IllegalStateException("no sieve at " + pos);
        }
        s.startSift(meshId(tier), meshTexture(tier), block, load, kind, shakes, pos.asLong(), FINDS);
    }

    private static void show(int x, int y, Object view) {
        if (Minecraft.getInstance().level.getBlockEntity(new BlockPos(x, y, 10)) instanceof com.itmecasey.primamateria.blockentity.MachinePreviewBlockEntity m) {
            m.show(view);
        } else {
            throw new IllegalStateException("no machine at " + x);
        }
    }

    private static void fixed(BlockPos pos, int tier, String block, int load, float progress, int shakes) {
        SievePreviewBlockEntity s = be(pos);
        if (s == null) {
            throw new IllegalStateException("no sieve at " + pos);
        }
        s.show(new SieveView(meshId(tier), meshTexture(tier), block, load, progress, shakes, 0L, DriveKind.HAND, false, 7L, FINDS));
    }

    private static void look(BlockPos p, double side, double height, double back) {
        moveTo(new Vec3(p.getX() + 0.5 + side, p.getY() + height, p.getZ() + 0.5 - back), new Vec3(p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5));
    }

    private static void plan() {
        int y = ground;
        if ("fluids".equals(System.getenv("PRIMAMATERIA_SHOTS"))) {
            FluidTour.plan(y);
            return;
        }
        BlockPos s0 = new BlockPos(0, y, 0);
        then(0, () -> {
            server("gamerule advance_time false", "gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule spawn_monsters false",
                    "difficulty peaceful", "time set noon", "weather clear", "kill @e[type=!player,type=!item]", "clear @p");
            player().getAbilities().flying = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            Settings.set("client.animations", "full");
        });
        then(4, () -> {
            server("fill -8 " + (y - 1) + " -8 24 " + (y - 1) + " 24 minecraft:smooth_stone");
            place(s0, 1, false);
        });
        // 1. the sieve at rest, from two sides
        then(8, () -> look(s0, 1.5, 2.0, 1.9));
        shoot("sieve_idle", 25);
        then(2, () -> look(s0, -1.6, 2.2, -1.8));
        shoot("sieve_idle_other_side", 20);
        // 2. every mesh tier, resting
        for (int t = 0; t < 5; t++) {
            int tier = t;
            then(2, () -> place(s0, tier + 1, false));
            then(6, () -> look(s0, 1.1, 2.3, 1.0));
            shoot("mesh_" + TIERS[tier], 18);
        }
        // 3. loaded and waiting, then shaking by hand
        then(2, () -> place(s0, 3, false));
        then(6, () -> fixed(s0, 2, "minecraft:gravel", 3, 0.1F, 0));
        then(3, () -> look(s0, 1.5, 2.0, 1.9));
        shoot("sieve_loaded", 20);
        then(2, () -> place(s0, 3, true));
        then(6, () -> sift(s0, 2, DriveKind.HAND, 3, 400));
        then(4, () -> look(s0, 1.5, 2.0, 1.9));
        shoot("sieve_shake_a", 34);
        shoot("sieve_shake_b", 6);
        then(60, () -> {
        });
        shoot("sieve_shake_c", 2);
        then(2, () -> look(s0, 0.2, 2.7, 0.2));
        shoot("sieve_shake_top", 12);
        // 4. thin layer with finds showing
        then(3, () -> fixed(s0, 2, "minecraft:gravel", 3, 0.85F, 40));
        then(3, () -> look(s0, 0.3, 2.4, 0.9));
        shoot("sieve_finds", 20);
        then(2, () -> fixed(s0, 2, "minecraft:sand", 4, 0.0F, 0));
        shoot("sieve_full_load_sand", 12);
        then(2, () -> server("setblock 0 " + y + " 0 minecraft:air"));
        // 4b. other materials: none of them is in our tables; the layer and fines come from the block's own texture
        String[] others = {"minecraft:red_sand", "minecraft:soul_sand", "minecraft:hay_block", "minecraft:bookshelf", "minecraft:grass_block"};
        then(4, () -> {
            for (int i = 0; i < 5; i++) {
                place(new BlockPos(-8 + i * 2, y, -4), 2, true);
            }
        });
        then(10, () -> {
            for (int i = 0; i < 5; i++) {
                sift(new BlockPos(-8 + i * 2, y, -4), 1, DriveKind.HAND, 3, 100000, others[i]);
            }
        });
        then(2, () -> moveTo(new Vec3(-3.5, y + 2.6, -7.4), new Vec3(-3.5, y + 0.7, -3.5)));
        shoot("sieve_materials_a", 40);
        shoot("sieve_materials_b", 7);
        then(2, () -> {
            for (int i = 0; i < 5; i++) {
                place(new BlockPos(-8 + i * 2, y, -4), 0, false);
                server("setblock " + (-8 + i * 2) + " " + y + " -4 minecraft:air");
            }
        });
        // 4b2. animations off: the still layer comes from the fill block model, tinted by the loaded block's color
        then(2, () -> Settings.set("client.animations", "off"));
        then(2, () -> {
            server("setblock 0 " + y + " 14 primamateria:sieve[mesh=2,fill=0]", "setblock 2 " + y + " 14 primamateria:sieve[mesh=2,fill=0]");
        });
        then(8, () -> {
            fixed(new BlockPos(0, y, 14), 1, "minecraft:red_sand", 3, 0.1F, 0);
            fixed(new BlockPos(2, y, 14), 1, "minecraft:soul_sand", 2, 0.1F, 0);
            server("setblock 0 " + y + " 14 primamateria:sieve[mesh=2,fill=3]", "setblock 2 " + y + " 14 primamateria:sieve[mesh=2,fill=2]");
        });
        then(2, () -> moveTo(new Vec3(1.5, y + 3.7, 12.6), new Vec3(1.5, y + 0.9, 14.5)));
        shoot("sieve_off_fill_tint", 25);
        then(2, () -> {
            Settings.set("client.animations", "full");
            server("setblock 0 " + y + " 14 minecraft:air", "setblock 2 " + y + " 14 minecraft:air");
        });
        // 4c. barrels, crucible, casting tray, drives and the auto hammer, each with a made-up state
        String[] machines = {"wooden_barrel", "wooden_barrel", "wooden_barrel", "clay_barrel", "crucible", "casting_tray", "casting_tray",
                "windup_drive", "furnace_drive", "powered_drive", "auto_hammer"};
        then(4, () -> {
            for (int i = 0; i < machines.length; i++) {
                server("setblock " + (-12 + i * 2) + " " + y + " 10 primamateria:" + machines[i] + "[facing=north]");
            }
        });
        then(12, () -> {
            long now = Minecraft.getInstance().level.getGameTime();
            show(-12, y, new com.itmecasey.primamateria.view.BarrelView(false, com.itmecasey.primamateria.view.BarrelView.Contents.FLUID, "minecraft:water", 700,
                    0x3F76E4, com.itmecasey.primamateria.view.BarrelView.Stage.IDLE, 0F, "", "", List.of()));
            show(-10, y, new com.itmecasey.primamateria.view.BarrelView(false, com.itmecasey.primamateria.view.BarrelView.Contents.COMPOST, "", 550,
                    0x7A5B34, com.itmecasey.primamateria.view.BarrelView.Stage.COMPOSTING, 0.5F, "", "", List.of()));
            show(-8, y, new com.itmecasey.primamateria.view.BarrelView(false, com.itmecasey.primamateria.view.BarrelView.Contents.FLUID, "primamateria:brine", 800,
                    0x45979C, com.itmecasey.primamateria.view.BarrelView.Stage.GROWING, 0.5F, "", "", List.of(
                    new com.itmecasey.primamateria.view.BarrelView.Crystal("primamateria:block/crystal_diamond", 0.9F, 0),
                    new com.itmecasey.primamateria.view.BarrelView.Crystal("primamateria:block/crystal_diamond", 0.5F, 1),
                    new com.itmecasey.primamateria.view.BarrelView.Crystal("primamateria:block/crystal_diamond", 0.2F, 3))));
            show(-6, y, new com.itmecasey.primamateria.view.BarrelView(true, com.itmecasey.primamateria.view.BarrelView.Contents.FLUID, "minecraft:lava", 600,
                    0xFFFFFF, com.itmecasey.primamateria.view.BarrelView.Stage.MIXING, 0.4F, "minecraft:cobblestone", "", List.of()));
            show(-4, y, new com.itmecasey.primamateria.view.CrucibleView("primamateria:molten_iron", 2600, 0xD8742A, 12, 2, "primamateria:iron_dust", 3, 8));
            show(-2, y, new com.itmecasey.primamateria.view.TrayView("primamateria:molten_iron", 0xD8742A, com.itmecasey.primamateria.view.TrayView.Stage.FILLING, 0.7F, ""));
            show(0, y, new com.itmecasey.primamateria.view.TrayView("primamateria:molten_iron", 0xD8742A, com.itmecasey.primamateria.view.TrayView.Stage.COOLING, 0.8F, "minecraft:iron_ingot"));
            show(2, y, new com.itmecasey.primamateria.view.DriveView(DriveKind.WINDUP, 0.8F, true, false, now));
            show(4, y, new com.itmecasey.primamateria.view.DriveView(DriveKind.FURNACE, 0.7F, true, false, now));
            show(6, y, new com.itmecasey.primamateria.view.DriveView(DriveKind.POWERED, 0.6F, true, false, now));
            show(8, y, new com.itmecasey.primamateria.view.AutoHammerView("primamateria:iron_hammer", "minecraft:cobblestone", 3, 0.4F, DriveKind.WINDUP, now));
        });
        then(2, () -> moveTo(new Vec3(-9.0, y + 3.3, 7.6), new Vec3(-9.0, y + 0.8, 10.5)));
        shoot("machines_barrels", 30);
        then(2, () -> moveTo(new Vec3(-2.0, y + 3.3, 7.6), new Vec3(-2.0, y + 0.8, 10.5)));
        shoot("machines_crucible_tray", 20);
        then(2, () -> moveTo(new Vec3(5.5, y + 2.6, 6.6), new Vec3(5.5, y + 0.6, 10.5)));
        shoot("machines_drives", 20);
        then(2, () -> moveTo(new Vec3(3.5, y + 3.2, 6.0), new Vec3(3.5, y + 0.6, 10.5)));
        shoot("machines_drives_b", 5);
        then(2, () -> {
            for (int i = 0; i < machines.length; i++) {
                server("setblock " + (-12 + i * 2) + " " + y + " 10 minecraft:air");
            }
        });
        // 5. twenty sieves in view, all moving; frame time per animation level
        then(4, () -> {
            for (int i = 0; i < 20; i++) {
                place(hall(y, i), 1 + (i % 5), true);
            }
        });
        then(10, () -> {
            for (int i = 0; i < 20; i++) {
                BlockPos p = hall(y, i);
                DriveKind kind = DriveKind.values()[1 + (i % 4)];
                sift(p, i % 5, kind, 1 + (i % 4), 100000);
            }
        });
        then(2, () -> moveTo(new Vec3(5.5, y + 5.5, -4.5), new Vec3(5.5, y + 0.4, 5.5)));
        shoot("sieve_hall", 40);
        measure("full");
        then(2, () -> Settings.set("client.animations", "reduced"));
        shoot("sieve_hall_reduced", 20);
        measure("reduced");
        then(2, () -> Settings.set("client.animations", "off"));
        shoot("sieve_hall_off", 20);
        measure("off");
        then(2, () -> Settings.set("client.animations", "full"));
        then(20, () -> quit(Minecraft.getInstance()));
    }

    /** The grid of 20 sieves: 5 columns, 4 rows, two blocks apart, starting at (2, y, 2). */
    private static BlockPos hall(int y, int i) {
        return new BlockPos(2 + (i % 5) * 2, y, 2 + (i / 5) * 2);
    }

    private static void measure(String mode) {
        then(40, () -> {
            fpsSum = 0;
            fpsSamples = 0;
            sampling = true;
            com.itmecasey.primamateria.client.SieveStats.reset();
        });
        then(120, () -> {
            sampling = false;
            long calls = Math.max(1, com.itmecasey.primamateria.client.SieveStats.calls);
            LOGGER.info("PRIMAMATERIA-SHOTS fps mode={} sieves=20 average_fps={} samples={} renderer_us_per_sieve={} drawn={}", mode,
                    fpsSamples == 0 ? 0 : fpsSum / fpsSamples, fpsSamples,
                    String.format(java.util.Locale.ROOT, "%.1f", com.itmecasey.primamateria.client.SieveStats.nanos / 1000.0 / calls),
                    com.itmecasey.primamateria.client.SieveStats.drawn);
        });
    }

    // ---- helpers ----

    static void moveTo(Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        server(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y - 1.62, eye.z, yaw, pitch));
        LocalPlayer player = player();
        player.snapTo(eye.x, eye.y - 1.62, eye.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    static void shoot(String name, int delay) {
        then(delay, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.hud.getChat().clearMessages(false);
            minecraft.gui.toastManager().clear();
        });
        then(2, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            LOGGER.info("PRIMAMATERIA-SHOTS {}", name);
            Screenshot.grab(minecraft.gameDirectory, "primamateria_" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> LOGGER.info("PRIMAMATERIA-SHOTS saved {}", name));
        });
    }

    static void then(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    static void onServer(java.util.function.Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> action.accept(server));
        }
    }

    static void server(String... commands) {
        onServer(server -> {
            for (String command : commands) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            }
        });
    }

    static {
        if (ENABLED) {
            LOGGER.info("PRIMAMATERIA-SHOTS enabled");
        }
    }
}
