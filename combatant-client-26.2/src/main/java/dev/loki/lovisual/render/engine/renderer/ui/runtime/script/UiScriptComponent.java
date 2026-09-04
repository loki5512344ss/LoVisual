/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.script;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiNodeSpec;

public interface UiScriptComponent {
    UiNodeSpec render(UiScriptRenderContext context);

    default void dispose() {
    }
}
