package dev.loki.lovisual.util.bedrock.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One animated vector channel (rotation, position or scale of a bone): keyframes with separate values arriving
 * at and leaving each key, blended linearly or, where a key asks for it, along a Catmull-Rom spline.
 */
public final class Channel {
    private final float[] times;
    private final float[] pre;
    private final float[] post;
    private final boolean[] smooth;

    private Channel(float[] times, float[] pre, float[] post, boolean[] smooth) {
        this.times = times;
        this.pre = pre;
        this.post = post;
        this.smooth = smooth;
    }

    public float lastTime() {
        return times[times.length - 1];
    }

    /** Writes the three components at {@code time} into {@code out} starting at {@code at}. */
    public void sample(float time, float[] out, int at) {
        int last = times.length - 1;
        if (time <= times[0]) {
            copy(pre, 0, out, at);
            return;
        }
        if (time >= times[last]) {
            copy(post, last, out, at);
            return;
        }
        int i = 0;
        while (times[i + 1] <= time) i++;
        float span = times[i + 1] - times[i];
        if (span <= 0.0f) {
            copy(post, i, out, at);
            return;
        }
        float f = (time - times[i]) / span;
        if (smooth[i]) spline(i, f, out, at, last);
        else lerp(i, f, out, at);
    }

    private void lerp(int i, float f, float[] out, int at) {
        for (int a = 0; a < 3; a++) {
            float from = post[i * 3 + a];
            out[at + a] = from + (pre[(i + 1) * 3 + a] - from) * f;
        }
    }

    private void spline(int i, float f, float[] out, int at, int last) {
        int before = Math.max(i - 1, 0) * 3;
        int after = Math.min(i + 2, last) * 3;
        float f2 = f * f;
        float f3 = f2 * f;
        for (int a = 0; a < 3; a++) {
            float p0 = post[before + a];
            float p1 = post[i * 3 + a];
            float p2 = pre[(i + 1) * 3 + a];
            float p3 = pre[after + a];
            out[at + a] = 0.5f * (2.0f * p1 + (p2 - p0) * f
                    + (2.0f * p0 - 5.0f * p1 + 4.0f * p2 - p3) * f2
                    + (-p0 + 3.0f * p1 - 3.0f * p2 + p3) * f3);
        }
    }

    private static void copy(float[] from, int index, float[] out, int at) {
        System.arraycopy(from, index * 3, out, at, 3);
    }

    /** A channel is a constant vector or a map of time to key; null when absent or empty. */
    public static Channel parse(JsonElement element) {
        if (element == null) return null;
        if (element.isJsonArray()) {
            float[] v = vector(element);
            return new Channel(new float[]{0.0f}, v.clone(), v, new boolean[]{false});
        }
        if (!element.isJsonObject()) return null;
        List<Object[]> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            float time;
            try {
                time = Float.parseFloat(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }
            keys.add(new Object[]{time, entry.getValue()});
        }
        if (keys.isEmpty()) return null;
        keys.sort((a, b) -> Float.compare((Float) a[0], (Float) b[0]));
        int n = keys.size();
        float[] times = new float[n];
        float[] pre = new float[n * 3];
        float[] post = new float[n * 3];
        boolean[] smooth = new boolean[n];
        for (int i = 0; i < n; i++) {
            times[i] = (Float) keys.get(i)[0];
            JsonElement key = (JsonElement) keys.get(i)[1];
            float[] arriving;
            float[] leaving;
            if (key.isJsonObject()) {
                JsonObject object = key.getAsJsonObject();
                leaving = object.has("post") ? vector(object.get("post")) : object.has("vector") ? vector(object.get("vector")) : null;
                arriving = object.has("pre") ? vector(object.get("pre")) : null;
                if (leaving == null) leaving = arriving == null ? new float[3] : arriving;
                if (arriving == null) arriving = leaving;
                smooth[i] = object.has("lerp_mode") && "catmullrom".equals(object.get("lerp_mode").getAsString());
            } else {
                leaving = vector(key);
                arriving = leaving;
            }
            System.arraycopy(arriving, 0, pre, i * 3, 3);
            System.arraycopy(leaving, 0, post, i * 3, 3);
        }
        return new Channel(times, pre, post, smooth);
    }

    private static float[] vector(JsonElement value) {
        float[] out = new float[3];
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            for (int i = 0; i < Math.min(3, array.size()); i++) out[i] = number(array.get(i));
        } else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            for (String key : new String[]{"post", "vector", "pre"}) {
                if (object.has(key)) return vector(object.get(key));
            }
        } else {
            java.util.Arrays.fill(out, number(value));
        }
        return out;
    }

    private static float number(JsonElement element) {
        try {
            return element.getAsFloat();
        } catch (RuntimeException e) {
            return 0.0f;
        }
    }
}
