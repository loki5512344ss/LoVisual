package dev.loki.lovisual.features.module.modules.visuals.wings;

/**
 * Static wing geometry for the Wings module: 8 silhouette shapes (fan of triangles from
 * the wing root, each point carries an edge-alpha multiplier) and the pose presets
 * (standing / sneaking / elytra-gliding). Ported from the WiNGS reference.
 */
public final class WingShapes {
    private WingShapes() {
    }

    /** Silhouette vertex: local x (outwards), y (up/down), alphaMul — edge fade of the fan triangle. */
    public record Point(float x, float y, float alphaMul) {
    }

    /** Pose-dependent anchoring, spread and flap parameters (all in wing-local units). */
    public record Pose(float preTranslateY, float preTranslateZ,
                       float anchorY, float anchorZ,
                       float pitchRotation, float rollRotation,
                       float openMultiplier, float scaleMultiplier,
                       float motionSpreadBoost, float flapAmplitude,
                       float sideOffset, float sideYOffset, float sideZOffset,
                       float sideRoll, float sidePitch, float flapSpeed) {
    }

    public static final Pose POSE_IDLE =
            new Pose(0f, 0f, 1.38f, 0.10f, 0f, 0f, 1f, 1f, 0.18f, 4.5f, 0.06f, 0f, 0.02f, -11f, -4f, 0.12f);
    public static final Pose POSE_SNEAK =
            new Pose(0f, 0f, 0.96f, 0.10f, 18f, 0f, 1f, 1f, 0.18f, 4.5f, 0.06f, 0f, 0.02f, -11f, -4f, 0.12f);

    /** Gliding pose: swept back with progress-scaled pitch toward the flight direction. */
    public static Pose glidePose(float pitch, float flightProgress) {
        return new Pose(0.34f, 0.46f, 0f, 0f, flightProgress * (-90f - pitch), 0f,
                0.76f, 0.92f, 0.10f, 0.58f, 0.05f, 0f, 0.06f, -5f, -2f, 0.13f);
    }

    public static final String[] TYPES = {
            "Angelic", "Dragon", "Butterfly", "Phoenix", "Crystal", "Mechanical", "Fairy", "Demon"
    };

    public static Point[] shape(String type) {
        return switch (type) {
            case "Dragon" -> DRAGON;
            case "Butterfly" -> BUTTERFLY;
            case "Phoenix" -> PHOENIX;
            case "Crystal" -> CRYSTAL;
            case "Mechanical" -> MECHANICAL;
            case "Fairy" -> FAIRY;
            case "Demon" -> DEMON;
            default -> ANGELIC;
        };
    }

    public static int[] ribs(Point[] shape) {
        int[] template = shape.length <= 10 ? new int[]{2, 4, 6, 8} : new int[]{2, 4, 7, 9, 11};
        int count = 0;
        for (int idx : template) {
            if (idx < shape.length) count++;
        }
        int[] out = new int[count];
        int i = 0;
        for (int idx : template) {
            if (idx < shape.length) out[i++] = idx;
        }
        return out;
    }

    private static final Point[] ANGELIC = {
            new Point(0.08f, 0.10f, 0.88f), new Point(0.28f, 0.34f, 0.78f), new Point(0.56f, 0.82f, 0.62f),
            new Point(0.86f, 0.30f, 0.52f), new Point(1.14f, 0.46f, 0.40f), new Point(1.24f, 0.04f, 0.30f),
            new Point(1.02f, -0.18f, 0.28f), new Point(1.18f, -0.64f, 0.22f), new Point(0.86f, -0.46f, 0.20f),
            new Point(0.80f, -0.98f, 0.14f), new Point(0.54f, -0.74f, 0.16f), new Point(0.30f, -1.16f, 0.12f),
            new Point(0.10f, -0.54f, 0.18f)
    };

    private static final Point[] DRAGON = {
            new Point(0.10f, 0.12f, 0.90f), new Point(0.22f, 0.40f, 0.80f), new Point(0.48f, 0.72f, 0.65f),
            new Point(0.80f, 0.60f, 0.55f), new Point(1.10f, 0.70f, 0.42f), new Point(1.30f, 0.30f, 0.35f),
            new Point(1.20f, -0.10f, 0.30f), new Point(1.05f, -0.50f, 0.25f), new Point(0.70f, -0.35f, 0.22f),
            new Point(0.50f, -0.70f, 0.18f), new Point(0.20f, -0.50f, 0.15f), new Point(0.05f, -0.30f, 0.20f)
    };

    private static final Point[] BUTTERFLY = {
            new Point(0.12f, 0.15f, 0.92f), new Point(0.30f, 0.50f, 0.85f), new Point(0.50f, 0.90f, 0.70f),
            new Point(0.70f, 0.80f, 0.60f), new Point(0.85f, 0.55f, 0.50f), new Point(0.75f, 0.20f, 0.45f),
            new Point(0.55f, -0.15f, 0.40f), new Point(0.40f, -0.60f, 0.30f), new Point(0.25f, -0.85f, 0.20f),
            new Point(0.12f, -0.65f, 0.25f), new Point(0.06f, -0.35f, 0.30f)
    };

    private static final Point[] PHOENIX = {
            new Point(0.10f, 0.14f, 0.90f), new Point(0.25f, 0.45f, 0.82f), new Point(0.52f, 0.78f, 0.68f),
            new Point(0.82f, 0.50f, 0.55f), new Point(1.15f, 0.55f, 0.42f), new Point(1.28f, 0.15f, 0.32f),
            new Point(1.20f, -0.25f, 0.28f), new Point(1.10f, -0.55f, 0.24f), new Point(1.25f, -0.85f, 0.18f),
            new Point(0.90f, -0.65f, 0.16f), new Point(0.60f, -0.90f, 0.14f), new Point(0.30f, -0.70f, 0.12f),
            new Point(0.08f, -0.40f, 0.16f)
    };

    private static final Point[] CRYSTAL = {
            new Point(0.15f, 0.10f, 0.85f), new Point(0.40f, 0.35f, 0.75f), new Point(0.70f, 0.60f, 0.60f),
            new Point(1.00f, 0.40f, 0.50f), new Point(0.85f, 0.10f, 0.45f), new Point(1.10f, -0.15f, 0.35f),
            new Point(0.90f, -0.45f, 0.30f), new Point(0.65f, -0.30f, 0.25f), new Point(0.45f, -0.60f, 0.20f),
            new Point(0.20f, -0.40f, 0.22f), new Point(0.08f, -0.15f, 0.30f)
    };

    private static final Point[] MECHANICAL = {
            new Point(0.08f, 0.08f, 0.90f), new Point(0.20f, 0.25f, 0.82f), new Point(0.45f, 0.40f, 0.70f),
            new Point(0.70f, 0.35f, 0.58f), new Point(0.95f, 0.25f, 0.48f), new Point(0.90f, 0.00f, 0.40f),
            new Point(1.10f, -0.15f, 0.32f), new Point(0.80f, -0.30f, 0.28f), new Point(0.55f, -0.20f, 0.25f),
            new Point(0.40f, -0.45f, 0.20f), new Point(0.15f, -0.30f, 0.22f), new Point(0.05f, -0.10f, 0.28f)
    };

    private static final Point[] FAIRY = {
            new Point(0.10f, 0.12f, 0.90f), new Point(0.25f, 0.38f, 0.82f), new Point(0.42f, 0.65f, 0.68f),
            new Point(0.55f, 0.70f, 0.58f), new Point(0.60f, 0.45f, 0.50f), new Point(0.50f, 0.15f, 0.42f),
            new Point(0.38f, -0.10f, 0.35f), new Point(0.30f, -0.35f, 0.28f), new Point(0.18f, -0.45f, 0.22f),
            new Point(0.08f, -0.25f, 0.26f)
    };

    private static final Point[] DEMON = {
            new Point(0.10f, 0.12f, 0.88f), new Point(0.25f, 0.38f, 0.80f), new Point(0.55f, 0.65f, 0.65f),
            new Point(0.85f, 0.50f, 0.52f), new Point(1.15f, 0.55f, 0.40f), new Point(1.25f, 0.20f, 0.32f),
            new Point(1.10f, -0.10f, 0.28f), new Point(1.30f, -0.45f, 0.22f), new Point(1.15f, -0.70f, 0.18f),
            new Point(0.85f, -0.55f, 0.16f), new Point(0.55f, -0.85f, 0.14f), new Point(0.25f, -0.65f, 0.12f),
            new Point(0.08f, -0.35f, 0.18f)
    };
}
