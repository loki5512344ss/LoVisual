package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime;

import java.util.Locale;

public enum UiScriptSourceKind {
    JAVASCRIPT("js"),
    TYPESCRIPT("ts");

    private final String extension;

    UiScriptSourceKind(String extension) {
        this.extension = extension;
    }

    public static UiScriptSourceKind fromPath(String path) {
        if (path == null) return JAVASCRIPT;
        String normalized = path.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".ts") ? TYPESCRIPT : JAVASCRIPT;
    }

    public String extension() {
        return extension;
    }
}
