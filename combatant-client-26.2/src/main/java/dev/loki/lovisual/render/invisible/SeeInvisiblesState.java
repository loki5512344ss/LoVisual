/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.invisible;

/**
 * Marker injected into entity render states when InvisESP reveals
 * an invisible player this frame.
 */
public interface SeeInvisiblesState {
    public boolean lovisual$isSeeInvisRevealed();

    public void lovisual$setSeeInvisRevealed(boolean revealed);
}
