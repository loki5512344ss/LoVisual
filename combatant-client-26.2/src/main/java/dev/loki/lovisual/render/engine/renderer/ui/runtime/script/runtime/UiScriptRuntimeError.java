/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleId;

public record UiScriptRuntimeError(UiScriptModuleId moduleId, String phase, String message, Throwable cause) {
    public UiScriptRuntimeError {
        phase = phase != null ? phase : "";
        message = message != null ? message : "";
    }

    public String stackTrace() {
        if (cause == null) return "";
        StringWriter out = new StringWriter(2048);
        cause.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
