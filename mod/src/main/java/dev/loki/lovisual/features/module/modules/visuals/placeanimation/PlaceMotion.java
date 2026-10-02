package dev.loki.lovisual.features.module.modules.visuals.placeanimation;

import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.Set;

/** Pure maths of the place animation: easing curves and the hand-side slide direction. */
public final class PlaceMotion {
    public static final float FINAL_SCALE = 0.999f;

    private static final float SIDE_SHIFT = 0.1f;
    private static final float FORWARD_SHIFT = 0.2f;
    private static final float VERTICAL_SHIFT = 0.2f;
    private static final float SLIDE_BASE = easeBase(0.9f);
    private static final float TILT_BASE = easeBase(-0.08f);
    private static final float SCALE_BASE = easeBase(-0.7f);

    private PlaceMotion() {
    }

    /** Animation progress 0..1; the extra tick of length lets the last frame land exactly. */
    public static float progress(int age, float partial, int lifetime) {
        return Math.min(1.0f, (age + partial) / (lifetime + 1));
    }

    /** How far the block still is from its place: 1 at the start, 0 when landed (accelerates into place). */
    public static float slideRemaining(float progress) {
        return 1.0f - ease(progress, SLIDE_BASE);
    }

    /** Tilt in radians: starts at {@code maxTilt} and settles quickly. */
    public static float tilt(float progress, float maxTilt) {
        return (1.0f - ease(progress, TILT_BASE)) * maxTilt;
    }

    /** Block scale from {@code start} up to a hair under a full block (avoids z-fighting with the mesh). */
    public static float scale(float start, float progress) {
        return start + (FINAL_SCALE - start) * ease(progress, SCALE_BASE);
    }

    /**
     * Offset the block starts from, in world axes: back towards the player, to the hand's side, and up or
     * down depending on where the player looks. {@code rightSide} is the side of the hand that placed it.
     */
    public static Vector3f slideStart(float yawDegrees, float pitchDegrees, boolean rightSide, float distance) {
        double yaw = Math.toRadians(yawDegrees);
        float backX = (float) Math.sin(yaw);
        float backZ = (float) -Math.cos(yaw);
        float side = rightSide ? 1.0f : -1.0f;
        float vertical = pitchDegrees > 0.0f ? VERTICAL_SHIFT : -VERTICAL_SHIFT;
        return new Vector3f(
                (backZ * side * SIDE_SHIFT + backX * FORWARD_SHIFT) * distance,
                vertical * distance,
                (-backX * side * SIDE_SHIFT + backZ * FORWARD_SHIFT) * distance);
    }

    /** Drops the components that would push the block into a blocked neighbouring side. */
    public static Vector3f restrict(Vector3f offset, Set<Direction> blocked) {
        Vector3f out = new Vector3f(offset);
        if (out.x > 0.0f && blocked.contains(Direction.EAST) || out.x < 0.0f && blocked.contains(Direction.WEST)) out.x = 0.0f;
        if (out.y > 0.0f && blocked.contains(Direction.UP) || out.y < 0.0f && blocked.contains(Direction.DOWN)) out.y = 0.0f;
        if (out.z > 0.0f && blocked.contains(Direction.SOUTH) || out.z < 0.0f && blocked.contains(Direction.NORTH)) out.z = 0.0f;
        return out;
    }

    /** Exponential ease through (0,0) and (1,1); positive bases accelerate, negative bases decelerate. */
    static float ease(float t, float base) {
        return (float) (base * Math.pow(1.0 / base + 1.0, t) - base);
    }

    static float easeBase(float k) {
        return k > 0.0f ? (float) -Math.log(k) : (float) (Math.log(-k) - 1.0);
    }
}
