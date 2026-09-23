/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.reconcile.match;

public record UiKeyPath(String value) {
    public UiKeyPath child(String keyOrIndex) {
        String child = keyOrIndex == null || keyOrIndex.isBlank() ? "?" : keyOrIndex;
        return new UiKeyPath(value == null || value.isBlank() ? child : value + "/" + child);
    }
}
