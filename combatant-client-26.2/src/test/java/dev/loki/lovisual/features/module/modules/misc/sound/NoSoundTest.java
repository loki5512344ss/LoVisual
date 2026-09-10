/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.misc.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import dev.loki.lovisual.features.module.modules.misc.sound.NoSound;

public final class NoSoundTest {

    @Test
    void resolveScaleConvertsPercent() {
        assertEquals(0.3f, NoSound.resolveScale(true, 30), 0.0001f);
        assertEquals(1.0f, NoSound.resolveScale(true, 100), 0.0001f);
        assertEquals(0.01f, NoSound.resolveScale(true, 1), 0.0001f);
    }

    @Test
    void unmatchedSoundKeepsOriginalVolume() {
        assertEquals(1.0f, NoSound.resolveScale(false, 30), 0.0001f);
        assertEquals(1.0f, NoSound.resolveScale(false, 100), 0.0001f);
    }

    @Test
    void scaleIsClampedToSaneRange() {
        assertEquals(1.0f, NoSound.resolveScale(true, 250), 0.0001f);
        assertEquals(0.0f, NoSound.resolveScale(true, -5), 0.0001f);
    }
}
