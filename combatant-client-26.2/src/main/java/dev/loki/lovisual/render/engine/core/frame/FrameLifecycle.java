/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.core.frame;

/**
 * Backend-visible lifecycle state for one Minecraft-presented frame.
 */
public enum FrameLifecycle {
    IDLE,
    RECORDING,
    SUBMITTED,
    PRESENTED
}
