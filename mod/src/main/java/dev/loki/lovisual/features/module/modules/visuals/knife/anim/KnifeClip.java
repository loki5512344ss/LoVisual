package dev.loki.lovisual.features.module.modules.visuals.knife.anim;

import dev.loki.lovisual.util.bedrock.anim.Channel;
import dev.loki.lovisual.util.bedrock.anim.Clip;

import java.util.Map;

/**
 * A clip bound to a model: bone names are resolved to indices once. A pose holds nine floats per bone:
 * rotation (degrees), position (pixels), scale.
 */
public final class KnifeClip {
    public static final int STRIDE = 9;
    public static final int POSITION = 3;
    public static final int SCALE = 6;

    private final Clip clip;
    private final int[] bones;
    private final Clip.Tracks[] tracks;

    public KnifeClip(Clip clip, Map<String, Integer> boneIndex) {
        this.clip = clip;
        int count = 0;
        int[] indices = new int[clip.bones().size()];
        Clip.Tracks[] resolved = new Clip.Tracks[indices.length];
        for (Map.Entry<String, Clip.Tracks> entry : clip.bones().entrySet()) {
            Integer index = boneIndex.get(entry.getKey());
            if (index == null) continue;
            indices[count] = index;
            resolved[count++] = entry.getValue();
        }
        this.bones = java.util.Arrays.copyOf(indices, count);
        this.tracks = java.util.Arrays.copyOf(resolved, count);
    }

    public Clip clip() {
        return clip;
    }

    public float length() {
        return clip.length();
    }

    /** Overwrites the channels this clip animates; every other bone keeps what {@code pose} already holds. */
    public void evaluate(float time, float[] pose) {
        for (int i = 0; i < bones.length; i++) {
            int at = bones[i] * STRIDE;
            if (at + STRIDE > pose.length) continue;
            Clip.Tracks t = tracks[i];
            Channel rotation = t.rotation();
            Channel position = t.position();
            Channel scale = t.scale();
            if (rotation != null) rotation.sample(time, pose, at);
            if (position != null) position.sample(time, pose, at + POSITION);
            if (scale != null) scale.sample(time, pose, at + SCALE);
        }
    }
}
