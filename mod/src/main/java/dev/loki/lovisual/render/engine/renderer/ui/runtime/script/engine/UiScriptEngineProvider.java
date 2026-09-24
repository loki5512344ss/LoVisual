package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.engine;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.engine.JavetUiScriptEngine;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.engine.UiScriptEngine;

public enum UiScriptEngineProvider {
    ;

    public static UiScriptEngine javet() {
        return new JavetUiScriptEngine();
    }
}
