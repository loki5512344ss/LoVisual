/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.engine.uniform.impl.post.PostFXUniforms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.loki.lovisual.features.module.modules.visuals.fx.PostFX;

/**
 * Пресеты PostFX: Cinematic должен быть визуально заметным,
 * Vanilla — нейтральным (это причина «модуль ничего не делал»).
 */
public final class PostFxPresetTest {

    private static float[] preset(String name) {
        float[] out = new float[PostFXUniforms.FLOAT_COUNT];
        PostFX.applyPresetTo(name, out);
        return out;
    }

    @Test
    void vanillaPresetIsNeutral() {
        float[] vanilla = preset("Vanilla");
        assertEquals(0.0f, vanilla[0], 1e-6);  // exposure
        assertEquals(1.0f, vanilla[1], 1e-6);  // gamma
        assertEquals(1.0f, vanilla[2], 1e-6);  // contrast
        assertEquals(1.0f, vanilla[3], 1e-6);  // saturation
        assertEquals(0.0f, vanilla[7], 1e-6);  // LUT strength
        assertEquals(0.0f, vanilla[15], 1e-6); // tone map off
    }

    @Test
    void cinematicPresetIsVisible() {
        float[] cinematic = preset("Cinematic");
        assertNotEquals(1.0f, cinematic[2], 1e-4);             // contrast изменён
        assertTrue(cinematic[3] > 1.05f);                      // насыщенность выше
        assertTrue(cinematic[7] > 0.2f);                       // тёплый LUT заметен
        assertEquals(1.0f, cinematic[15], 1e-6);               // filmic tone map
        assertTrue(cinematic[19] > 0.0f);                      // холодные тени
        assertTrue(cinematic[27] > 0.0f);                      // тёплые света
        assertTrue(cinematic[8] > 0.1f);                       // виньетка заметна
    }

    @Test
    void cinematicDiffersFromVanillaOnGrade() {
        float[] cinematic = preset("Cinematic");
        float[] vanilla = preset("Vanilla");
        assertNotEquals(vanilla[2], cinematic[2]);
        assertNotEquals(vanilla[3], cinematic[3]);
        assertNotEquals(vanilla[7], cinematic[7]);
    }

    @Test
    void unknownPresetFallsBackToSoft() {
        float[] unknown = preset("Mystery");
        float[] soft = preset("Soft");
        for (int i = 0; i < 16; i++) {
            assertEquals(soft[i], unknown[i], 1e-6);
        }
    }

    @Test
    void nullPresetFallsBackToCinematic() {
        float[] byNull = preset(null);
        float[] cinematic = preset("Cinematic");
        for (int i = 0; i < 16; i++) {
            assertEquals(cinematic[i], byNull[i], 1e-6);
        }
    }
}
