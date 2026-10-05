package dev.loki.lovisual.features.module.modules.visuals.knife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loki.lovisual.features.module.modules.visuals.knife.anim.types.KnifeVariant;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * One entry of the knife catalog ({@code assets/lovisual/knife/index.json}). Recolours share the geometry
 * of {@code model}; {@code holyBones} get the holy shader; the transforms place the knife in a third person hand.
 */
public record KnifeSkin(String key, String name, String model, List<String> holyBones, List<KnifeVariant> variants,
                        Transform thirdRight, Transform thirdLeft) {

    /** Display-style placement in pixels and degrees. */
    public record Transform(float[] rotation, float[] translation, float[] scale) {
        public static final Transform NONE = new Transform(new float[3], new float[3], new float[]{1, 1, 1});
    }

    private static final String INDEX = "/assets/lovisual/knife/index.json";
    public static final List<KnifeSkin> ALL = load();
    public static final String[] KEYS = ALL.stream().map(KnifeSkin::key).toArray(String[]::new);

    public static KnifeSkin byKey(String key) {
        for (KnifeSkin skin : ALL) {
            if (skin.key.equals(key)) return skin;
        }
        return ALL.isEmpty() ? null : ALL.get(0);
    }

    public Identifier texture() {
        return Identifier.fromNamespaceAndPath("lovisual", "textures/knife/" + key + ".png");
    }

    static List<KnifeSkin> load() {
        try (InputStream in = KnifeSkin.class.getResourceAsStream(INDEX)) {
            return in == null ? List.of() : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    public static List<KnifeSkin> parse(String json) {
        List<KnifeSkin> out = new ArrayList<>();
        for (JsonElement entry : JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("melee")) {
            JsonObject data = entry.getAsJsonObject();
            String key = data.get("key").getAsString();
            JsonObject transforms = data.has("transforms") ? data.getAsJsonObject("transforms") : new JsonObject();
            out.add(new KnifeSkin(key, data.get("name").getAsString(),
                    data.has("model") ? data.get("model").getAsString() : key,
                    strings(data.get("holyBones")), variants(data.get("variants")),
                    transform(transforms.get("thirdperson_righthand")), transform(transforms.get("thirdperson_lefthand"))));
        }
        return out;
    }

    private static List<KnifeVariant> variants(JsonElement element) {
        List<KnifeVariant> out = new ArrayList<>();
        if (element == null || !element.isJsonArray()) return out;
        for (JsonElement entry : element.getAsJsonArray()) {
            JsonObject data = entry.getAsJsonObject();
            out.add(new KnifeVariant(data.has("suffix") ? data.get("suffix").getAsString() : "", strings(data.get("hidden"))));
        }
        return out;
    }

    private static Transform transform(JsonElement element) {
        if (element == null || !element.isJsonObject()) return Transform.NONE;
        JsonObject data = element.getAsJsonObject();
        return new Transform(vec(data.get("rotation"), 0), vec(data.get("translation"), 0), vec(data.get("scale"), 1));
    }

    private static float[] vec(JsonElement element, float fallback) {
        float[] out = {fallback, fallback, fallback};
        if (element == null || !element.isJsonArray()) return out;
        JsonArray array = element.getAsJsonArray();
        for (int i = 0; i < Math.min(3, array.size()); i++) out[i] = array.get(i).getAsFloat();
        return out;
    }

    private static List<String> strings(JsonElement element) {
        List<String> out = new ArrayList<>();
        if (element == null || !element.isJsonArray()) return out;
        for (JsonElement entry : element.getAsJsonArray()) out.add(entry.getAsString());
        return out;
    }
}
