/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.engine;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptRenderResult;

public interface UiScriptEngine extends AutoCloseable {
    UiScriptRenderResult render(UiScriptModule module, UiScriptRenderContext context);

    @Override
    void close();
}
