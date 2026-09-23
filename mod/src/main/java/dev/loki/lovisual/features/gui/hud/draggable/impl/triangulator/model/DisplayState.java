/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.model;

public record DisplayState(String statusText,
                           String primaryLine,
                           String secondaryLine,
                           String confidenceText,
                           int statusColor) {
}
