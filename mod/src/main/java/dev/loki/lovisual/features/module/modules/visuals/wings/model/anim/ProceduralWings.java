package dev.loki.lovisual.features.module.modules.visuals.wings.model.anim;

import dev.loki.lovisual.util.bedrock.geo.GeoBone;
import dev.loki.lovisual.util.bedrock.geo.GeoParser;

/**
 * Code-driven flaps for the three models that ship without an animation file. Angles are degrees added to the
 * pose stored in the model, positions are pixels, and the clock runs in degrees (seconds x speed).
 */
public final class ProceduralWings {
    private static final float ROOT_SCALE = 0.6f;
    private static final float DEGREES_PER_SECOND = 200.0f;

    private ProceduralWings() {
    }

    public static final WingAnimator ANGEL = (model, seconds, flight) -> {
        model.resetPose();
        float t = seconds * DEGREES_PER_SECOND;
        scaleRoots(model, ROOT_SCALE);
        float flap = cos(10 + t);
        float sway = sin(10 + t);
        pose(model, "Left_Wing", -18.2468f + flap * 10, -36.3088f + sway * 15, 18.01613f, -0.0288f, 0.20524f, 1.18197f);
        pose(model, "Right_Wing", -18.2468f + flap * 10, 36.3088f - sway * 15, -18.01609f, -0.0288f, 0.2052f, 1.18197f);
        pose(model, "bone6", 1.1552f - flap * 10, -0.543f - cos(t) * 25, 3.16854f, -0.90088f, 0.04987f, -0.43119f);
        pose(model, "bone4", 1.1552f - flap * 10, 0.543f + cos(t) * 25, -3.1685f, 0.9009f, 0.04987f, -0.43119f);
        pose(model, "bone7", flap * 5, -sway * 15, -sway * 15);
        pose(model, "bone5", flap * 5, sway * 15, sway * 15);
        pose(model, "bone3", -0.0082f + cos(t) * 10, 12.49978f, -7.57475f, -0.87628f, 1.75491f, 0.39042f);
        pose(model, "bone8", -0.0082f + cos(t) * 10, -12.4998f, 7.5748f, 0.8763f, 1.75491f, 0.39042f);
        pose(model, "bone2", 6.4724f - cos(60 + t) * 5, 5.1344f + sin(60 + t) * 5, -12.5338f);
        pose(model, "bone9", 6.4724f - cos(60 + t) * 5, -5.1344f - sin(60 + t) * 5, 12.5338f);
        pose(model, "bone10", 6.4876f + cos(200 + t) * 5, -0.97f + sin(300 + t) * 5, -6.9044f - sin(220 + t) * 7,
                -0.89933f, -0.02767f, -0.01524f);
        pose(model, "bone11", 6.4876f + cos(200 + t) * 5, 0.97f - sin(300 + t) * 5, 6.9044f + sin(220 + t) * 7,
                0.8993f, -0.0277f, -0.0152f);
    };

    public static final WingAnimator DEMON = (model, seconds, flight) -> {
        model.resetPose();
        float t = seconds * DEGREES_PER_SECOND;
        scaleRoots(model, ROOT_SCALE);
        pose(model, "Wing3", cos(t) * 15 + 10, 45 - cos(t) * 15, 0);
        pose(model, "Wing2", cos(t) * 15 + 10, -45 + cos(t) * 15, 0);
        pose(model, "bone8", 0, -cos(50 - t) * 20, 0);
        pose(model, "bone2", 0, cos(50 - t) * 20, 0);
        pose(model, "bone9", 0, -cos(80 - t) * 25, 0);
        pose(model, "bone3", 0, cos(80 - t) * 25, 0);
    };

    /** Standing and gliding flaps, blended by {@code flight}; the wings flap faster and the model grows when gliding. */
    public static final WingAnimator DRAGON = (model, seconds, flight) -> {
        model.resetPose();
        float f = Math.max(0.0f, Math.min(flight, 1.0f));
        float slow = seconds * 300.0f;
        float fast = seconds * 600.0f;
        scaleRoots(model, mix(f, 0.9f, 1.0f));

        float leftYaw = mix(f, -30 + cos(slow) * 20, -27.5839f + cos(fast) * 20);
        float rightYaw = mix(f, 30 - cos(slow) * 20, 27.5839f - cos(fast) * 20);
        float pitch = mix(f, cos(-60 + slow) * 10, 7.8279f + cos(-60 + fast) * 10);
        float roll = mix(f, 0, -12.46225f);
        pose(model, "left_wing", pitch, leftYaw, roll);
        pose(model, "right_wing", pitch, rightYaw, -roll);
        float tip = mix(f, cos(90 + slow), cos(90 + fast)) * 20;
        pose(model, "left_tip", 0, -tip, 0);
        pose(model, "right_tip", 0, tip, 0);

        float stretch = mix(f, 1.2f + cos(90 + slow) * 0.2f, 1.2f + cos(90 + fast) * 0.2f);
        stretch(model, "left_wing", stretch);
        stretch(model, "right_wing", stretch);
    };

    private static void pose(GeoParser.Model model, String name, float rx, float ry, float rz) {
        GeoBone bone = model.bones().get(name);
        if (bone == null) return;
        bone.rot[0] = bone.baseRot[0] + (float) Math.toRadians(-rx);
        bone.rot[1] = bone.baseRot[1] + (float) Math.toRadians(-ry);
        bone.rot[2] = bone.baseRot[2] + (float) Math.toRadians(rz);
    }

    private static void pose(GeoParser.Model model, String name, float rx, float ry, float rz, float ox, float oy, float oz) {
        pose(model, name, rx, ry, rz);
        GeoBone bone = model.bones().get(name);
        if (bone == null) return;
        bone.offset[0] = -ox;
        bone.offset[1] = oy;
        bone.offset[2] = oz;
    }

    private static void stretch(GeoParser.Model model, String name, float widthScale) {
        GeoBone bone = model.bones().get(name);
        if (bone == null) return;
        bone.scale[0] = widthScale;
        bone.scale[2] = 1.1f;
    }

    private static void scaleRoots(GeoParser.Model model, float factor) {
        for (GeoBone root : model.roots()) {
            root.scale[0] = factor;
            root.scale[1] = factor;
            root.scale[2] = factor;
        }
    }

    private static float mix(float t, float a, float b) {
        return a + (b - a) * t;
    }

    static float cos(float degrees) {
        return (float) Math.cos(Math.toRadians(degrees));
    }

    static float sin(float degrees) {
        return (float) Math.sin(Math.toRadians(degrees));
    }
}
