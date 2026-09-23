/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNodeSpec;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.UiScriptRuntimeError;

public record UiScriptRenderResult(UiNodeSpec root, UiScriptRuntimeError error) {
    public static UiScriptRenderResult ok(UiNodeSpec root) {
        return new UiScriptRenderResult(root, null);
    }

    public static UiScriptRenderResult failed(UiScriptRuntimeError error) {
        return new UiScriptRenderResult(null, error);
    }

    public boolean success() {
        return error == null;
    }
}
