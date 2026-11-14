package com.itmecasey.primamateria.dev;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only client check of the recipe viewers. With the environment variable {@code PRIMAMATERIA_TOUR} set to {@code bei},
 * {@code jei} or {@code rei} (and {@code PRIMAMATERIA_DEMO} for the demo recipes), it opens a fresh superflat world, opens that
 * viewer's recipe screen for a few Prima Materia items through the viewer's own code and saves a screenshot of each into
 * {@code run/screenshots} as {@code primamateria_<viewer>_<name>.png}, then quits. It never touches the real mouse or keyboard.
 *
 * <p>With {@code PRIMAMATERIA_TOUR_SERVER} it joins a running dedicated server instead (no demo recipes, no integrated server),
 * so everything a viewer shows came from the server's recipe lookup payload.
 */
public final class IntegrationTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("PrimaMateria");
    private static final String VIEWER = System.getenv("PRIMAMATERIA_TOUR");
    private static final String WORLD = "primamateria_integration";
    /** With {@code PRIMAMATERIA_TOUR_SERVER=host:port} the tour joins that dedicated server instead of making a world. */
    private static final String SERVER = System.getenv("PRIMAMATERIA_TOUR_SERVER");

    private enum Stage { WAITING_FOR_MENU, LOADING_WORLD, TOURING, DONE }

    private record Step(int delay, Runnable action) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static Stage stage = Stage.WAITING_FOR_MENU;
    private static int idle;
    private static int wait;
    private static boolean pauseOnLostFocus;

    private IntegrationTour() {
    }

    public static boolean requested() {
        return VIEWER != null && !VIEWER.isBlank();
    }

    public static void tick() {
        if (!requested() || stage == Stage.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.mouseHandler.isMouseGrabbed()) {
                minecraft.mouseHandler.releaseMouse();
            }
            switch (stage) {
                case WAITING_FOR_MENU -> waitForMenu(minecraft);
                case LOADING_WORLD -> waitForWorld(minecraft);
                case TOURING -> runSteps();
                default -> {
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("PRIMAMATERIA-TOUR failed", e);
            quit(minecraft);
        }
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    private static void quit(Minecraft minecraft) {
        minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
        LOGGER.info("PRIMAMATERIA-TOUR DONE");
        stage = Stage.DONE;
        minecraft.stop();
    }

    private static void waitForMenu(Minecraft minecraft) {
        if (minecraft.gui.overlay() != null || minecraft.level != null || minecraft.gui.screen() == null || ++idle < 20) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;
        if (SERVER != null && !SERVER.isBlank()) {
            stage = Stage.LOADING_WORLD;
            idle = 0;
            LOGGER.info("PRIMAMATERIA-TOUR lookup before joining: {} sifting recipes", com.itmecasey.primamateria.data.RecipeLookup.get().sifting().size());
            LOGGER.info("PRIMAMATERIA-TOUR joining {} for {}", SERVER, VIEWER);
            net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new TitleScreen(), minecraft,
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(SERVER),
                    new net.minecraft.client.multiplayer.ServerData("sync", SERVER, net.minecraft.client.multiplayer.ServerData.Type.OTHER),
                    false, null);
            return;
        }
        LevelStorageSource source = minecraft.getLevelSource();
        if (source.levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                LOGGER.warn("PRIMAMATERIA-TOUR could not delete the old world", e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        stage = Stage.LOADING_WORLD;
        idle = 0;
        LOGGER.info("PRIMAMATERIA-TOUR creating world for {}", VIEWER);
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                registries -> registries.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)
                        .value().createWorldDimensions(), new TitleScreen());
    }

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null
                || (SERVER == null && minecraft.getSingleplayerServer() == null) || ++idle < 120) {
            return;
        }
        LOGGER.info("PRIMAMATERIA-TOUR in the world");
        if (SERVER != null) {
            var lookup = com.itmecasey.primamateria.data.RecipeLookup.get();
            LOGGER.info("PRIMAMATERIA-TOUR lookup after joining: {} sifting, {} crushing, {} melting recipes, singleplayer server: {}",
                    lookup.sifting().size(), lookup.crushing().size(), lookup.melting().size(), minecraft().getSingleplayerServer() != null);
        }
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
            quit(Minecraft.getInstance());
            return;
        }
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    private static void then(int delay, Runnable action) {
        STEPS.add(new Step(delay, action));
    }

    /** Opens the viewer's recipe screen for an item, by name, so this class needs no viewer on its compile path. */
    private static void open(String itemId, boolean uses) {
        try {
            switch (VIEWER) {
                case "bei" -> {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
                    Method m = Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("open", Item.class, boolean.class);
                    m.invoke(null, item, uses);
                }
                case "jei" -> Class.forName("com.itmecasey.primamateria.neoforge.compat.PrimaJeiPlugin")
                        .getMethod("tourShow", String.class, boolean.class).invoke(null, itemId, uses);
                case "rei" -> Class.forName("com.itmecasey.primamateria.neoforge.compat.PrimaReiPlugin")
                        .getMethod("tourShow", String.class, boolean.class).invoke(null, itemId, uses);
                default -> LOGGER.warn("PRIMAMATERIA-TOUR unknown viewer {}", VIEWER);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.error("PRIMAMATERIA-TOUR could not open {} for {}", VIEWER, itemId, e);
        }
    }

    private static void frame(String name, String itemId, boolean uses) {
        then(30, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if ("bei".equals(VIEWER)) {
                // BEI draws its popup over an open screen, so give it the inventory.
                minecraft.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
            }
        });
        then(20, () -> open(itemId, uses));
        then(60, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            LOGGER.info("PRIMAMATERIA-TOUR {}", name);
            Screenshot.grab(minecraft.gameDirectory, "primamateria_" + VIEWER + "_" + name + ".png",
                    minecraft.gameRenderer.mainRenderTarget(), 1, message -> LOGGER.info("PRIMAMATERIA-TOUR saved {}", name));
        });
        then(10, () -> Minecraft.getInstance().gui.setScreen(null));
    }

    private static void plan() {
        frame("sifting_iron_dust", "primamateria:iron_dust", false);
        frame("gravel_uses", "minecraft:gravel", true);
        frame("barrel_clay", "minecraft:clay", false);
        frame("melting_iron_dust", "primamateria:iron_dust", true);
        frame("casting_iron_ingot", "minecraft:iron_ingot", false);
        frame("crushing_plant_fiber", "primamateria:plant_fiber", false);
        frame("steeping_salt_brine", "primamateria:salt_dust", true);
        frame("melting_copper_dust", "primamateria:copper_dust", true);
        then(20, () -> {
        });
    }
}
