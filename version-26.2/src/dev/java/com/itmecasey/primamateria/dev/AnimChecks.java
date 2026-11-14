package com.itmecasey.primamateria.dev;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itmecasey.primamateria.Ids;
import com.itmecasey.primamateria.ModAnim;
import com.itmecasey.primamateria.ModContent;
import com.itmecasey.primamateria.StateProps;
import com.itmecasey.primamateria.anim.AnimJson;
import com.itmecasey.primamateria.anim.AnimModel;
import com.itmecasey.primamateria.anim.SieveViewSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Headless checks for workstream C: everything about the sieve animation and the models that can be proven without a renderer.
 * The drawing itself is proven by the client tour (ShotTour) with frames.
 */
final class AnimChecks {
    interface Verdict {
        void check(String name, boolean ok, String detail);
    }

    private static final String ASSETS = "/assets/primamateria/";

    private AnimChecks() {
    }

    static void run(ServerLevel level, Verdict v) {
        sieveBlock(level, v);
        particles(v);
        animFile(v);
        models(v);
        sounds(v);
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream in = AnimChecks.class.getResourceAsStream(ASSETS + path)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static boolean exists(String path) {
        return AnimChecks.class.getResource(ASSETS + path) != null;
    }

    private static void sieveBlock(ServerLevel level, Verdict v) {
        Block sieve = ModContent.block(Ids.SIEVE);
        v.check("anim_sieve_state_props", sieve.getStateDefinition().getProperty("moving") == StateProps.MOVING
                && sieve.getStateDefinition().getProperty("mesh") == StateProps.MESH
                && sieve.getStateDefinition().getProperty("fill") == StateProps.FILL, "moving, mesh, fill");
        v.check("anim_sieve_be_type_registered", BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ModContent.id("sieve_preview")), "sieve_preview");
        BlockPos spawn = level.getRespawnData().pos();
        BlockPos pos = new BlockPos(spawn.getX() + 40, level.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ()) + 20, spawn.getZ());
        for (int mesh = 0; mesh <= 8; mesh++) {
            BlockState state = sieve.defaultBlockState().setValue(StateProps.MESH, mesh).setValue(StateProps.MOVING, mesh % 2 == 0)
                    .setValue(StateProps.FILL, mesh % 5);
            level.setBlockAndUpdate(pos, state);
            if (mesh == 0) {
                v.check("anim_sieve_has_view_source", level.getBlockEntity(pos) instanceof SieveViewSource, String.valueOf(level.getBlockEntity(pos)));
            }
            v.check("anim_sieve_state_" + mesh, level.getBlockState(pos).equals(state), state.toString());
        }
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        v.check("anim_sieve_shape_inside_block", ModAnimShapes.sieveWithinBlock(), "selection shape");
    }

    private static void particles(Verdict v) {
        for (String name : Ids.PARTICLES) {
            v.check("anim_particle_" + name, BuiltInRegistries.PARTICLE_TYPE.containsKey(ModContent.id(name)) && ModAnim.particle(name) != null, name);
            v.check("anim_particle_file_" + name, exists("particles/" + name + ".json"), name);
        }
    }

    private static void animFile(Verdict v) {
        try (InputStream in = AnimChecks.class.getResourceAsStream(ASSETS + "primamateria_anim/sieve.json")) {
            AnimModel m = AnimJson.parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            m.requireBones("frame", "tray", "strap_nw", "strap_ne", "strap_sw", "strap_se", "mesh", "layer", "finds", "chute");
            boolean clips = true;
            for (String c : List.of("idle", "load", "shake_hand", "shake_windup", "shake_furnace", "finish", "mesh_swap", "blocked")) {
                clips &= m.clip(c) != null;
            }
            v.check("anim_sieve_file_loads", clips, m.boneCount() + " bones, " + m.clips().size() + " clips");
        } catch (Exception e) {
            v.check("anim_sieve_file_loads", false, e.toString());
        }
    }

    /** Every blockstate and model we ship parses, every model it names exists, every element is inside the block. */
    private static void models(Verdict v) {
        String[] names = {"sieve", "wooden_barrel", "clay_barrel", "crucible", "auto_hammer", "powered_drive"};
        for (String name : names) {
            try {
                JsonObject state = json("blockstates/" + name + ".json");
                int refs = 0;
                boolean ok = true;
                for (String ref : models(state)) {
                    refs++;
                    ok &= exists("models/block/" + ref.substring(ref.indexOf('/') + 1) + ".json");
                }
                JsonObject model = json("models/block/" + name + ".json");
                int elements = checkElements(model);
                v.check("anim_blockstate_" + name, ok && refs > 0, refs + " model references");
                v.check("anim_model_bounds_" + name, elements > 0, elements + " elements inside the block");
            } catch (Exception e) {
                v.check("anim_model_" + name, false, e.toString());
            }
        }
        for (String part : List.of("sieve_frame", "sieve_tray", "sieve_mesh_1", "sieve_mesh_5", "sieve_fill_1", "sieve_fill_4",
                "powered_drive_charge_1", "powered_drive_charge_5")) {
            try {
                v.check("anim_model_" + part, checkElements(json("models/block/" + part + ".json")) > 0, part);
            } catch (Exception e) {
                v.check("anim_model_" + part, false, e.toString());
            }
        }
    }

    private static List<String> models(JsonObject state) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (state.has("variants")) {
            for (var e : state.getAsJsonObject("variants").entrySet()) {
                out.add(e.getValue().getAsJsonObject().get("model").getAsString());
            }
        }
        if (state.has("multipart")) {
            for (JsonElement part : state.getAsJsonArray("multipart")) {
                JsonElement apply = part.getAsJsonObject().get("apply");
                out.add((apply.isJsonArray() ? apply.getAsJsonArray().get(0) : apply).getAsJsonObject().get("model").getAsString());
            }
        }
        return out;
    }

    private static int checkElements(JsonObject model) {
        JsonArray elements = model.getAsJsonArray("elements");
        for (JsonElement e : elements) {
            JsonObject o = e.getAsJsonObject();
            for (String key : new String[] {"from", "to"}) {
                for (JsonElement n : o.getAsJsonArray(key)) {
                    double d = n.getAsDouble();
                    if (d < 0 || d > 16) {
                        throw new IllegalStateException("element outside the block");
                    }
                }
            }
        }
        return elements.size();
    }

    private static void sounds(Verdict v) {
        try {
            JsonObject sounds = json("sounds.json");
            for (String name : List.of("sieve.load", "sieve.shake.coarse", "sieve.shake.fine", "sieve.shake.soil", "sieve.reveal",
                    "sieve.finish", "sieve.mesh", "drive.crank", "drive.tick")) {
                boolean ok = sounds.has(name);
                int files = 0;
                if (ok) {
                    for (JsonElement s : sounds.getAsJsonObject(name).getAsJsonArray("sounds")) {
                        String id = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
                        files++;
                        ok &= exists("sounds/" + id.substring(id.indexOf(':') + 1) + ".ogg");
                    }
                }
                v.check("anim_sound_" + name, ok && files >= 3, files + " variations");
            }
        } catch (Exception e) {
            v.check("anim_sounds_json", false, e.toString());
        }
    }
}
