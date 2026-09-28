/*
 * Ported/adapted from Spatial GUI (https://github.com/tastytrash/Spatial-GUI), MIT License.
 * Copyright (c) tastytrash and Spatial GUI contributors.
 *
 * This file is part of LoVisual and is distributed under LoVisual's own GPLv3 license terms
 * for the modifications made here (see mod/LICENSE and mod/THIRD_PARTY_NOTICES.md); the MIT-licensed
 * portions ported from Spatial GUI remain available under the MIT terms above.
 */
package dev.loki.lovisual.render.spatialgui;

import org.joml.Vector3f;

/**
 * World-space description of the plane the captured GUI texture was last drawn onto, published every frame by
 * the SpatialGui module so the mouse-ray mixin can translate a window-space cursor position into a 2D GUI
 * coordinate (the equivalent of Spatial-GUI's {@code MouseHandlerUtil}/{@code CameraUtil} pair).
 */
public final class SpatialGuiPlane {
    private static volatile SpatialGuiPlane active;

    private final Vector3f center;
    private final Vector3f right;
    private final Vector3f up;
    private final float halfWidth;
    private final float halfHeight;
    private final int guiWidth;
    private final int guiHeight;

    public SpatialGuiPlane(Vector3f center, Vector3f right, Vector3f up, float halfWidth, float halfHeight, int guiWidth, int guiHeight) {
        this.center = center;
        this.right = right;
        this.up = up;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
    }

    public static void publish(SpatialGuiPlane plane) {
        active = plane;
    }

    public static void clear() {
        active = null;
    }

    public static SpatialGuiPlane current() {
        return active;
    }

    /**
     * Intersects a world-space ray with this plane and, if it hits within bounds, writes the equivalent
     * scaled-GUI-coordinate (top-left origin, matching {@code Screen} mouse coordinates) into {@code out}.
     */
    public boolean rayToGuiCoords(Vector3f origin, Vector3f direction, float[] out) {
        Vector3f normal = new Vector3f(right).cross(up, new Vector3f()).normalize();
        float denom = normal.dot(direction);
        if (Math.abs(denom) < 1.0e-6F) return false;

        Vector3f toCenter = new Vector3f(center).sub(origin);
        float t = toCenter.dot(normal) / denom;
        if (t <= 0.0F) return false;

        Vector3f hit = new Vector3f(direction).mul(t).add(origin).sub(center);
        float localX = hit.dot(right) / (right.length() * right.length());
        float localY = hit.dot(up) / (up.length() * up.length());
        if (localX < -halfWidth || localX > halfWidth || localY < -halfHeight || localY > halfHeight) {
            return false;
        }

        float u = (localX + halfWidth) / (2.0F * halfWidth);
        float v = 1.0F - (localY + halfHeight) / (2.0F * halfHeight);
        out[0] = u * guiWidth;
        out[1] = v * guiHeight;
        return true;
    }
}
