package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.engine;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptRenderResult;

public interface UiScriptEngine extends AutoCloseable {
    UiScriptRenderResult render(UiScriptModule module, UiScriptRenderContext context);

    @Override
    void close();
}
