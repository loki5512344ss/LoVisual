/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface;

public interface IMsaaTexture {
    void lovisual$setSamples(int samples);

    int lovisual$getSamples();

    boolean lovisual$isMsaa();

    int lovisual$getGlId();
}
