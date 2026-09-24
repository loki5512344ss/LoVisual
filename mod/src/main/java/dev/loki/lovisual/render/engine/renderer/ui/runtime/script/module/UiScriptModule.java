package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiProps;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleId;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.UiScriptSourceKind;

public record UiScriptModule(UiScriptModuleId id, UiScriptSourceKind sourceKind, String source, UiProps metadata) {
    public UiScriptModule(UiScriptModuleId id, String source, UiProps metadata) {
        this(id, UiScriptSourceKind.fromPath(id != null ? id.path() : ""), source, metadata);
    }

    public UiScriptModule {
        id = id != null ? id : new UiScriptModuleId("lovisual", "main");
        sourceKind = sourceKind != null ? sourceKind : UiScriptSourceKind.fromPath(id.path());
        source = source != null ? source : "";
        metadata = metadata != null ? metadata : UiProps.EMPTY;
    }

    public UiScriptModuleId getId() {
        return id;
    }
}
