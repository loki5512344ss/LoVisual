package dev.loki.lovisual.render.engine.renderer.ui.runtime.script.registry;

public enum UiTypeScriptStripper {
    ;

    public static String strip(String source) {

        String out = source != null ? source : "";
        out = out.replaceAll("(?m)^\\s*import\\s+type\\s+[^;]+;?\\s*$", "");
        out = out.replaceAll("(?m)^\\s*export\\s+type\\s+[^;]+;?\\s*$", "");
        out = out.replaceAll("(?m)^\\s*type\\s+[A-Za-z_$][\\w$]*\\s*=\\s*[^;]+;?\\s*$", "");
        out = out.replaceAll("\\s+as\\s+const\\b", "");
        out = out.replaceAll(":\\s*[A-Za-z_$][\\w$]*(?:<[^>]+>)?(?=\\s*[,)=;{])", "");
        return out;
    }
}
