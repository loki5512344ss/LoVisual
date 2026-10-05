package dev.loki.lovisual.features.module.modules.visuals.wings.model.anim;

import dev.loki.lovisual.util.bedrock.anim.Channel;
import dev.loki.lovisual.util.bedrock.anim.Clip;
import dev.loki.lovisual.util.bedrock.geo.GeoBone;
import dev.loki.lovisual.util.bedrock.geo.GeoParser;

import java.util.Map;

/** The first clip of a Bedrock {@code .animation.json}, applied to the bones of a wing model. */
public final class BedrockClip implements WingAnimator {
    private final Clip clip;

    private BedrockClip(Clip clip) {
        this.clip = clip;
    }

    public float length() {
        return clip.length();
    }

    public static BedrockClip parse(String json) {
        Map<String, Clip> clips = Clip.parseAll(json);
        return new BedrockClip(clips.isEmpty() ? null : clips.values().iterator().next());
    }

    @Override
    public void apply(GeoParser.Model model, float seconds, float flight) {
        model.resetPose();
        if (clip == null) return;
        float length = Math.max(clip.length(), 0.001f);
        float time = clip.loops() ? seconds % length : Math.min(seconds, length);
        float[] v = new float[3];
        for (Map.Entry<String, Clip.Tracks> entry : clip.bones().entrySet()) {
            GeoBone bone = model.bones().get(entry.getKey());
            if (bone == null) continue;
            Clip.Tracks tracks = entry.getValue();
            Channel rotation = tracks.rotation();
            if (rotation != null) {
                rotation.sample(time, v, 0);
                bone.rot[0] = bone.baseRot[0] + (float) Math.toRadians(-v[0]);
                bone.rot[1] = bone.baseRot[1] + (float) Math.toRadians(-v[1]);
                bone.rot[2] = bone.baseRot[2] + (float) Math.toRadians(v[2]);
            }
            Channel position = tracks.position();
            if (position != null) {
                position.sample(time, v, 0);
                bone.offset[0] = -v[0];
                bone.offset[1] = v[1];
                bone.offset[2] = v[2];
            }
            Channel scale = tracks.scale();
            if (scale != null) {
                scale.sample(time, bone.scale, 0);
            }
        }
    }
}
