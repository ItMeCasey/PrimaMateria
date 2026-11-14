package com.itmecasey.primamateria.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code primamateria_anim/<name>.json} (written by {@code scripts/export_anims.py}) into an {@link AnimModel}. Every
 * mistake fails with a message that names the bone, clip or field, so a bad export is found at load, not while drawing.
 *
 * <pre>
 * { "format": 1, "name": "sieve",
 *   "textures": { "wood": "minecraft:block/oak_planks" },
 *   "bones": [ { "name": "tray", "parent": "frame", "pivot": [8, 9, 8],
 *                "cubes": [ { "from": [..], "to": [..], "texture": "wood", "uv": { "north": [u0, v0, u1, v1], ... } } ] } ],
 *   "clips": { "shake": { "length": 0.6, "loop": "loop",
 *                "bones": { "tray": { "position": [ [kf..], [kf..], [kf..] ], "rotation": [..], "scale": [..] } } } } }
 * keyframe: { "t": 0.1, "v": 1.5, "i": "linear|step|catmullrom|bezier", "lt": 0, "lv": 0, "rt": 0, "rv": 0 }
 * </pre>
 */
public final class AnimJson {
    private static final String[] FACES = {"north", "south", "west", "east", "down", "up"};
    private static final String[] CHANNELS = {"position", "rotation", "scale"};

    private AnimJson() {
    }

    public static AnimModel parse(Reader reader) {
        return parse(JsonParser.parseReader(reader).getAsJsonObject());
    }

    public static AnimModel parse(String json) {
        return parse(JsonParser.parseString(json).getAsJsonObject());
    }

    public static AnimModel parse(JsonObject root) {
        if (!root.has("format") || root.get("format").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported animation format (expected format 1)");
        }
        String name = str(root, "name", "model");
        Map<String, String> textures = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : obj(root, "textures", "model " + name).entrySet()) {
            textures.put(e.getKey(), e.getValue().getAsString());
        }
        JsonArray bonesJson = arr(root, "bones", "model " + name);
        int n = bonesJson.size();
        String[] names = new String[n];
        int[] parent = new int[n];
        float[] pivot = new float[n * 3];
        AnimModel.Mesh[][] meshes = new AnimModel.Mesh[n][];
        for (int b = 0; b < n; b++) {
            JsonObject bone = bonesJson.get(b).getAsJsonObject();
            names[b] = str(bone, "name", "bone " + b);
            for (int k = 0; k < b; k++) {
                if (names[k].equals(names[b])) {
                    throw new IllegalArgumentException("Bone " + names[b] + " is defined twice");
                }
            }
            String parentName = str(bone, "parent", "bone " + names[b]);
            parent[b] = -1;
            if (!parentName.isEmpty()) {
                for (int k = 0; k < b; k++) {
                    if (names[k].equals(parentName)) {
                        parent[b] = k;
                    }
                }
                if (parent[b] < 0) {
                    throw new IllegalArgumentException("Bone " + names[b] + " has the parent " + parentName
                            + ", which is missing or comes later");
                }
            }
            float[] pv = floats(bone, "pivot", 3, "bone " + names[b]);
            System.arraycopy(pv, 0, pivot, b * 3, 3);
            meshes[b] = bake(bone, names[b], textures);
        }
        Map<String, AnimClip> clips = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : obj(root, "clips", "model " + name).entrySet()) {
            clips.put(e.getKey(), clip(e.getKey(), e.getValue().getAsJsonObject(), names));
        }
        return new AnimModel(name, names, parent, pivot, meshes, textures, clips);
    }

    private static AnimClip clip(String name, JsonObject json, String[] boneNames) {
        float length = json.has("length") ? json.get("length").getAsFloat() : 0F;
        AnimClip.Loop loop = AnimClip.Loop.valueOf(str(json, "loop", "clip " + name).toUpperCase());
        Track[] tracks = new Track[boneNames.length * AnimModel.POSE];
        for (Map.Entry<String, JsonElement> be : obj(json, "bones", "clip " + name).entrySet()) {
            int bone = -1;
            for (int i = 0; i < boneNames.length; i++) {
                if (boneNames[i].equals(be.getKey())) {
                    bone = i;
                }
            }
            if (bone < 0) {
                throw new IllegalArgumentException("Clip " + name + " animates the unknown bone " + be.getKey());
            }
            JsonObject channels = be.getValue().getAsJsonObject();
            for (int c = 0; c < CHANNELS.length; c++) {
                if (!channels.has(CHANNELS[c])) {
                    continue;
                }
                JsonArray axes = channels.getAsJsonArray(CHANNELS[c]);
                if (axes.size() != 3) {
                    throw new IllegalArgumentException("Clip " + name + ", bone " + be.getKey() + ", " + CHANNELS[c]
                            + " needs 3 axes");
                }
                for (int axis = 0; axis < 3; axis++) {
                    JsonArray keys = axes.get(axis).getAsJsonArray();
                    if (keys.isEmpty()) {
                        continue;
                    }
                    List<Track.Key> list = new ArrayList<>();
                    for (JsonElement ke : keys) {
                        list.add(key(ke.getAsJsonObject(), name + "/" + be.getKey() + "/" + CHANNELS[c]));
                    }
                    tracks[bone * AnimModel.POSE + c * 3 + axis] = new Track(list);
                }
            }
        }
        return new AnimClip(name, length, loop, tracks);
    }

    private static Track.Key key(JsonObject k, String where) {
        if (!k.has("t") || !k.has("v")) {
            throw new IllegalArgumentException("Keyframe in " + where + " needs t and v (expressions are not allowed)");
        }
        if (!k.get("t").isJsonPrimitive() || !k.get("t").getAsJsonPrimitive().isNumber()
                || !k.get("v").isJsonPrimitive() || !k.get("v").getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Keyframe in " + where + " must hold plain numbers");
        }
        String mode = k.has("i") ? k.get("i").getAsString() : "linear";
        byte m = switch (mode) {
            case "linear" -> Track.LINEAR;
            case "step" -> Track.STEP;
            case "catmullrom" -> Track.CATMULLROM;
            case "bezier" -> Track.BEZIER;
            default -> throw new IllegalArgumentException("Unknown interpolation " + mode + " in " + where);
        };
        return new Track.Key(k.get("t").getAsFloat(), k.get("v").getAsFloat(), m, num(k, "lt"), num(k, "lv"), num(k, "rt"),
                num(k, "rv"));
    }

    private static float num(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsFloat() : 0F;
    }

    /** Bakes the cubes of one bone into one quad list per texture key. */
    private static AnimModel.Mesh[] bake(JsonObject bone, String boneName, Map<String, String> textures) {
        Map<String, List<float[]>> byTexture = new LinkedHashMap<>();
        if (bone.has("cubes")) {
            int index = 0;
            for (JsonElement ce : bone.getAsJsonArray("cubes")) {
                JsonObject cube = ce.getAsJsonObject();
                String where = "bone " + boneName + " cube " + index++;
                String tex = str(cube, "texture", where);
                if (!textures.containsKey(tex)) {
                    throw new IllegalArgumentException("Texture key " + tex + " of " + where + " is not in textures");
                }
                float[] from = floats(cube, "from", 3, where);
                float[] to = floats(cube, "to", 3, where);
                JsonObject uv = obj(cube, "uv", where);
                List<float[]> list = byTexture.computeIfAbsent(tex, k -> new ArrayList<>());
                for (int f = 0; f < FACES.length; f++) {
                    float[] face = uv.has(FACES[f]) ? floats(uv, FACES[f], 4, where) : new float[] {0, 0, 16, 16};
                    list.add(face(f, from, to, face));
                }
            }
        }
        AnimModel.Mesh[] out = new AnimModel.Mesh[byTexture.size()];
        int i = 0;
        for (Map.Entry<String, List<float[]>> e : byTexture.entrySet()) {
            float[] all = new float[e.getValue().size() * 32];
            int at = 0;
            for (float[] q : e.getValue()) {
                System.arraycopy(q, 0, all, at, 32);
                at += 32;
            }
            out[i++] = new AnimModel.Mesh(e.getKey(), all);
        }
        return out;
    }

    /** One quad (4 vertices of x y z u v nx ny nz, in blocks) of a cube face, wound counter-clockwise seen from outside. */
    static float[] face(int face, float[] from, float[] to, float[] uv) {
        float x0 = from[0] / 16F;
        float y0 = from[1] / 16F;
        float z0 = from[2] / 16F;
        float x1 = to[0] / 16F;
        float y1 = to[1] / 16F;
        float z1 = to[2] / 16F;
        float u0 = uv[0] / 16F;
        float v0 = uv[1] / 16F;
        float u1 = uv[2] / 16F;
        float v1 = uv[3] / 16F;
        float[][] c;
        float nx = 0;
        float ny = 0;
        float nz = 0;
        switch (face) {
            case 0 -> { // north (-z)
                c = new float[][] {{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}};
                nz = -1;
            }
            case 1 -> { // south (+z)
                c = new float[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
                nz = 1;
            }
            case 2 -> { // west (-x)
                c = new float[][] {{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}};
                nx = -1;
            }
            case 3 -> { // east (+x)
                c = new float[][] {{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}};
                nx = 1;
            }
            case 4 -> { // down (-y)
                c = new float[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}};
                ny = -1;
            }
            default -> { // up (+y)
                c = new float[][] {{x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}, {x0, y1, z0}};
                ny = 1;
            }
        }
        float[][] t = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
        float[] out = new float[32];
        for (int i = 0; i < 4; i++) {
            int o = i * 8;
            out[o] = c[i][0];
            out[o + 1] = c[i][1];
            out[o + 2] = c[i][2];
            out[o + 3] = t[i][0];
            out[o + 4] = t[i][1];
            out[o + 5] = nx;
            out[o + 6] = ny;
            out[o + 7] = nz;
        }
        return out;
    }

    private static String str(JsonObject o, String key, String where) {
        if (!o.has(key)) {
            throw new IllegalArgumentException("Missing \"" + key + "\" in " + where);
        }
        return o.get(key).getAsString();
    }

    private static JsonObject obj(JsonObject o, String key, String where) {
        if (!o.has(key) || !o.get(key).isJsonObject()) {
            throw new IllegalArgumentException("Missing object \"" + key + "\" in " + where);
        }
        return o.getAsJsonObject(key);
    }

    private static JsonArray arr(JsonObject o, String key, String where) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            throw new IllegalArgumentException("Missing list \"" + key + "\" in " + where);
        }
        return o.getAsJsonArray(key);
    }

    private static float[] floats(JsonObject o, String key, int count, String where) {
        JsonArray a = arr(o, key, where);
        if (a.size() != count) {
            throw new IllegalArgumentException("\"" + key + "\" in " + where + " needs " + count + " numbers");
        }
        float[] out = new float[count];
        for (int i = 0; i < count; i++) {
            out[i] = a.get(i).getAsFloat();
        }
        return out;
    }
}
