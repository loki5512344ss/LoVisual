package dev.loki.lovisual.features.module.modules.visuals.godrays;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class GodRaysSunTest {
    private static Matrix4f looking(float x, float y, float z) {
        return new Matrix4f().perspective((float) Math.toRadians(70), 16f / 9f, 0.05f, 256f)
                .lookAt(0f, 0f, 0f, x, y, z, 0f, 1f, 0f);
    }

    @Test
    void belowHorizonHasNoSun() {
        assertNull(GodRaysSun.project((float) Math.PI, new Matrix4f().identity()));
    }

    @Test
    void sunInFrontProjectsToScreen() {
        float angle = (float) Math.toRadians(30);
        GodRaysSun.Screen screen = GodRaysSun.project(angle, looking(-1f, 0.5f, 0f));
        assertNotNull(screen);
        assertEquals((float) Math.cos(angle), screen.elevation(), 1e-5f);
        assertTrue(screen.u() > 0.0f && screen.u() < 1.0f);
    }

    @Test
    void sunBehindCameraIsDropped() {
        assertNull(GodRaysSun.project((float) Math.toRadians(30), looking(1f, 0.5f, 0f)));
    }

    @Test
    void fadeWeakensWithRainAndLowSun() {
        GodRaysSun.Screen high = new GodRaysSun.Screen(0.5f, 0.5f, 0.9f);
        GodRaysSun.Screen low = new GodRaysSun.Screen(0.5f, 0.5f, 0.1f);
        assertEquals(1.0f, GodRaysSun.fade(high, 0.0f, false), 1e-5f);
        assertTrue(GodRaysSun.fade(high, 1.0f, false) < 0.2f);
        assertTrue(GodRaysSun.fade(low, 0.0f, false) < GodRaysSun.fade(high, 0.0f, false));
    }

    @Test
    void sunsetBoostOnlyHelpsLowSun() {
        GodRaysSun.Screen low = new GodRaysSun.Screen(0.5f, 0.5f, 0.2f);
        GodRaysSun.Screen high = new GodRaysSun.Screen(0.5f, 0.5f, 0.9f);
        assertTrue(GodRaysSun.fade(low, 0.0f, true) > GodRaysSun.fade(low, 0.0f, false));
        assertEquals(GodRaysSun.fade(high, 0.0f, true), GodRaysSun.fade(high, 0.0f, false), 1e-5f);
    }

    @Test
    void offScreenSunFadesOut() {
        GodRaysSun.Screen far = new GodRaysSun.Screen(2.0f, 0.5f, 0.9f);
        assertEquals(0.0f, GodRaysSun.fade(far, 0.0f, false), 1e-5f);
    }

    @Test
    void tintFollowsGlowOnlyWhenPresent() {
        assertEquals(1.0f, GodRaysSun.tintChannel(255, 0, 0), 1e-5f);
        assertTrue(GodRaysSun.tintChannel(255, 0, 255) < 0.2f);
        assertEquals(1.0f, GodRaysSun.tintChannel(255, 255, 255), 1e-5f);
    }
}
