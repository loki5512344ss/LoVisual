/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.style;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiNodeSpec;

public final class UiStyleResolver {
    private final UiStyleCache cache;

    public UiStyleResolver(UiStyleCache cache) {
        this.cache = cache != null ? cache : new UiStyleCache();
    }

    public UiStyle resolve(UiNodeSpec spec) {
        if (spec == null) return UiStyle.DEFAULT;
        if (!spec.styleClass().isBlank()) {
            return cache.resolve(spec.styleClass());
        }
        return spec.style();
    }
}
