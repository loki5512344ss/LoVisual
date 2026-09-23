/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.combat.tracking;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.overlay.ReachDisplay;

/**
 * Tracks the reach (eye -> target hitbox distance) of the player's last attack
 * for the ReachDisplay HUD widget. Measurement math is pure and unit-tested;
 * state is a volatile snapshot written on the attack event.
 */
public final class ReachMeter {

    /** Vanilla survival attack range, m. Distances above are tinted as overreach. */
    public static final double VANILLA_REACH = 3.0;

    private static volatile double lastDistance = -1.0;
    private static volatile double sessionMax = -1.0;
    private static volatile long lastAttackAtMs;

    private ReachMeter() {
    }

    public static void record(double distance, long nowMs) {
        if (Double.isNaN(distance) || distance < 0.0) return;
        lastDistance = distance;
        lastAttackAtMs = nowMs;
        if (distance > sessionMax) {
            sessionMax = distance;
        }
    }

    public static double lastDistance() {
        return lastDistance;
    }

    public static double sessionMax() {
        return sessionMax;
    }

    public static long lastAttackAtMs() {
        return lastAttackAtMs;
    }

    public static void reset() {
        lastDistance = -1.0;
        sessionMax = -1.0;
        lastAttackAtMs = 0L;
    }

    /**
     * Distance from a point to the closest point of an AABB (per-axis clamp).
     * Returns 0 when the point is inside the box.
     */
    public static double distanceToBox(Vec3 point, AABB box) {
        if (point == null || box == null) return -1.0;
        double dx = axisDelta(point.x, box.minX, box.maxX);
        double dy = axisDelta(point.y, box.minY, box.maxY);
        double dz = axisDelta(point.z, box.minZ, box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double axisDelta(double value, double min, double max) {
        if (value < min) return min - value;
        if (value > max) return value - max;
        return 0.0;
    }

    /** Formats meters for display, e.g. "3.12 m"; "-" when no measurement yet. */
    public static String format(double distance) {
        if (distance < 0.0) return "-";
        return String.format(Locale.ROOT, "%.2f m", distance);
    }
}
