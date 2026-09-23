/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.input.focus;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;

public final class UiFocusManager {
    private UiNode focused;

    public UiNode focused() {
        return focused;
    }

    public void focus(UiNode node) {
        if (focused == node) return;
        if (focused != null) focused.state().setFocused(false);
        focused = node;
        if (focused != null) focused.state().setFocused(true);
    }
}
