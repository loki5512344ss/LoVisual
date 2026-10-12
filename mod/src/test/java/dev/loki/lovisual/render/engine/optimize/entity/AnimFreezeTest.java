package dev.loki.lovisual.render.engine.optimize.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link AnimFreeze} pure logic — the toggle snapshot and the
 * squared-distance gate. The capture/replay path needs a live render
 * pipeline (vertex buffers, Sodium or the raw-buffer mixin) and is
 * covered by an in-game check.
 */
public final class AnimFreezeTest {

    @AfterEach
    void reset() {
        AnimFreeze.reset();
    }

    @Test
    void offUntilConfigured() {
        assertFalse(AnimFreeze.enabled());
        AnimFreeze.configure(true, 24);
        assertTrue(AnimFreeze.enabled());
        assertEquals(24, AnimFreeze.distance());
        AnimFreeze.reset();
        assertFalse(AnimFreeze.enabled());
    }

    @Test
    void freezesRejectsNonRenderState() {
        // Not an EntityRenderState (and even a far pose) — never freezes.
        AnimFreeze.configure(true, 16);
        assertFalse(AnimFreeze.freezes(new Object(), farPose()));
    }

    @Test
    void beyondDistanceIsSquared() {
        assertTrue(AnimFreeze.beyondDistance(17f, 0f, 0f, 16f), "just past the gate");
        assertFalse(AnimFreeze.beyondDistance(16f, 0f, 0f, 16f), "exactly at the gate");
        assertFalse(AnimFreeze.beyondDistance(3f, 4f, 0f, 16f), "5 blocks away, well inside");
        // diagonal: 12²+12² = 288 < 16² = 256 is false… 12²+12²=288 > 256 is true
        assertTrue(AnimFreeze.beyondDistance(12f, 12f, 0f, 16f), "diagonal past the gate");
        assertFalse(AnimFreeze.beyondDistance(0f, 0f, 0f, 0f), "at the camera");
    }

    private static org.joml.Matrix4f farPose() {
        org.joml.Matrix4f pose = new org.joml.Matrix4f();
        pose.translate(100f, 64f, 100f);
        return pose;
    }
}
