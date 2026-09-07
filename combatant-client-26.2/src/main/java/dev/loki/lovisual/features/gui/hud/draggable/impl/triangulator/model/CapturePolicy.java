/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.model;

/** Tunables the tracker needs for a single tick. */
public record CapturePolicy(int minObservationTicks,
                            double stabilityThreshold,
                            double minPairAngleDeg,
                            boolean autoReset,
                            double autoResetDistance,
                            int maxCachedThrows,
                            long cacheLifetimeMs) {
}
