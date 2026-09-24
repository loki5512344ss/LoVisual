package dev.loki.lovisual.render.engine.guard.boundary;

/**
 * Central list of architecture boundary package rules.
 * <p>
 * This is used by debug audits and as documentation in code. The rules are deliberately string-based so they do not
 * introduce hard dependencies from high-level renderer code to GL/Sodium classes.
 */
public enum RenderBoundary {
    ;
    public static final String RHI_BACKEND_PACKAGE = "dev.loki.lovisual.render.engine.rhi";
    public static final String SODIUM_GL_BACKEND_PACKAGE = "dev.loki.lovisual.render.engine.rhi";
    public static final String SODIUM_BRIDGE_PACKAGE = "dev.loki.lovisual.render.sodium";
    public static final String PROFILER_PACKAGE = "dev.loki.lovisual.render.engine.profiler";
    public static final String LEGACY_MSAA_PACKAGE = "dev.loki.lovisual.render.engine.msaa";

    public static boolean mayUseGl(String className) {
        return className.startsWith(SODIUM_GL_BACKEND_PACKAGE)
                || className.startsWith(PROFILER_PACKAGE)
                || className.startsWith(LEGACY_MSAA_PACKAGE);
    }

    public static boolean mayUseSodiumInternals(String className) {
        return className.startsWith(SODIUM_BRIDGE_PACKAGE)
                || className.startsWith("dev.loki.lovisual.render.sodium.terrain")
                || className.startsWith("dev.loki.lovisual.render.engine.visuals.lighting")
                || className.startsWith("dev.loki.lovisual.render.helpers");
    }
}
