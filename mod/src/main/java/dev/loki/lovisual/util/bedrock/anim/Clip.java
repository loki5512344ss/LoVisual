package dev.loki.lovisual.util.bedrock.anim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One Bedrock animation: its length, looping, per-bone channels and timed sound events. */
public final class Clip {
    /** The channels of one bone; any of them may be null. */
    public record Tracks(Channel rotation, Channel position, Channel scale) {
    }

    private final String name;
    private final float length;
    private final boolean loop;
    private final Map<String, Tracks> bones;
    private final float[] soundTimes;
    private final String[] soundNames;

    private Clip(String name, float length, boolean loop, Map<String, Tracks> bones, float[] soundTimes, String[] soundNames) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.bones = bones;
        this.soundTimes = soundTimes;
        this.soundNames = soundNames;
    }

    public String name() {
        return name;
    }

    public float length() {
        return length;
    }

    public boolean loops() {
        return loop;
    }

    public Map<String, Tracks> bones() {
        return bones;
    }

    public int soundCount() {
        return soundTimes.length;
    }

    public float soundTime(int index) {
        return soundTimes[index];
    }

    public String soundName(int index) {
        return soundNames[index];
    }

    /** Every clip of an animation file, keyed by name without its {@code animation.<model>.} prefix. */
    public static Map<String, Clip> parseAll(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject clips = root.has("animations") ? root.getAsJsonObject("animations") : root;
        Map<String, Clip> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : clips.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject clip = entry.getValue().getAsJsonObject();
            if (!clip.has("bones")) continue;
            String name = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
            out.put(name, parse(name, clip));
        }
        return out;
    }

    private static Clip parse(String name, JsonObject clip) {
        Map<String, Tracks> bones = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : clip.getAsJsonObject("bones").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject channels = entry.getValue().getAsJsonObject();
            bones.put(entry.getKey(), new Tracks(Channel.parse(channels.get("rotation")),
                    Channel.parse(channels.get("position")), Channel.parse(channels.get("scale"))));
        }
        float length = clip.has("animation_length") ? clip.get("animation_length").getAsFloat() : 0.0f;
        if (length <= 0.0f) length = lastKey(bones);
        boolean loop = !clip.has("loop") || !clip.get("loop").isJsonPrimitive() || !clip.get("loop").getAsJsonPrimitive().isBoolean()
                || clip.get("loop").getAsBoolean();

        List<float[]> sounds = new ArrayList<>();
        List<String> names = new ArrayList<>();
        if (clip.has("sound_effects") && clip.get("sound_effects").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : clip.getAsJsonObject("sound_effects").entrySet()) {
                JsonElement value = entry.getValue();
                if (!value.isJsonObject() || !value.getAsJsonObject().has("effect")) continue;
                try {
                    sounds.add(new float[]{Float.parseFloat(entry.getKey())});
                    names.add(value.getAsJsonObject().get("effect").getAsString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        float[] times = new float[sounds.size()];
        String[] effects = new String[times.length];
        Integer[] order = new Integer[times.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Float.compare(sounds.get(a)[0], sounds.get(b)[0]));
        for (int i = 0; i < order.length; i++) {
            times[i] = sounds.get(order[i])[0];
            effects[i] = names.get(order[i]);
        }
        return new Clip(name, length, loop, bones, times, effects);
    }

    private static float lastKey(Map<String, Tracks> bones) {
        float last = 0.0f;
        for (Tracks tracks : bones.values()) {
            for (Channel channel : new Channel[]{tracks.rotation(), tracks.position(), tracks.scale()}) {
                if (channel != null) last = Math.max(last, channel.lastTime());
            }
        }
        return last;
    }
}
