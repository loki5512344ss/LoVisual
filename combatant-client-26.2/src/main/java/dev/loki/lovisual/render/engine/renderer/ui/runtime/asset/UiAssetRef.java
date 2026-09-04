/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.asset;

public record UiAssetRef(UiAssetKind kind, String id, float intrinsicWidth, float intrinsicHeight) {
    public static UiAssetRef texture(String id, float width, float height) {
        return new UiAssetRef(UiAssetKind.TEXTURE, id, width, height);
    }

    public String getId() {
        return id;
    }
}
