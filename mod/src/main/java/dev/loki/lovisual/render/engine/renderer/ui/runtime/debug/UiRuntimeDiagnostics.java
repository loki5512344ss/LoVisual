package dev.loki.lovisual.render.engine.renderer.ui.runtime.debug;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;

public final class UiRuntimeDiagnostics {
    private final UiPerfCounters counters = new UiPerfCounters();

    public UiPerfCounters counters() {
        return counters;
    }

    public String dump(UiNode root) {
        return UiTreeDumper.dump(root);
    }
}
