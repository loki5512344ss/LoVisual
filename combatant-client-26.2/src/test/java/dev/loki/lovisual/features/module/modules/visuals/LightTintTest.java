/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LightTintTest {

    private static final float EPS = 1e-4f;

    @Test
    void toLinearMapsBytesToUnitRange() {
        Vector3f red = Light.toLinear(0xFF0000);
        assertEquals(1.0f, red.x, EPS);
        assertEquals(0.0f, red.y, EPS);
        assertEquals(0.0f, red.z, EPS);
    }

    @Test
    void toLinearIgnoresAlpha() {
        Vector3f a = Light.toLinear(0x00FF0000);
        Vector3f b = Light.toLinear(0xAAFF0000);
        assertEquals(a.x, b.x, EPS);
        assertEquals(a.y, b.y, EPS);
        assertEquals(a.z, b.z, EPS);
    }

    @Test
    void tintMixZeroKeepsCurrent() {
        Vector3f target = Light.toLinear(0xFF0000);
        Vector3f out = Light.tintAmbient(new Vector3f(0.2f, 0.3f, 0.4f), target, 0.0f);
        assertEquals(0.2f, out.x, EPS);
        assertEquals(0.3f, out.y, EPS);
        assertEquals(0.4f, out.z, EPS);
    }

    @Test
    void tintMixOneEqualsTarget() {
        Vector3f target = Light.toLinear(0x102030);
        Vector3f out = Light.tintAmbient(new Vector3f(0.9f, 0.9f, 0.9f), target, 1.0f);
        assertEquals(target.x, out.x, EPS);
        assertEquals(target.y, out.y, EPS);
        assertEquals(target.z, out.z, EPS);
    }

    @Test
    void tintMixHalfIsAverage() {
        Vector3f target = Light.toLinear(0x000000);
        Vector3f out = Light.tintAmbient(new Vector3f(0.8f, 0.8f, 0.8f), target, 0.5f);
        assertEquals(0.4f, out.x, EPS);
    }

    @Test
    void nullCurrentBehavesAsWhite() {
        Vector3f out = Light.tintAmbient(null, new Vector3f(0.0f, 0.0f, 0.0f), 1.0f);
        assertEquals(0.0f, out.x, EPS);
    }

    @Test
    void mixClampedToUnitRange() {
        Vector3f target = Light.toLinear(0xFF0000);
        Vector3f high = Light.tintAmbient(new Vector3f(0.1f, 0.2f, 0.3f), target, 5.0f);
        assertEquals(target.x, high.x, EPS);
        Vector3f low = Light.tintAmbient(new Vector3f(0.1f, 0.2f, 0.3f), target, -1.0f);
        assertEquals(0.1f, low.x, EPS);
    }
}
