package dev.loki.lovisual.render.engine.renderer;

/**
 * Culling flag bundle for {@link Renderer3DCulling}. Extracted from
 * {@code Renderer3DCullOptions}; Physically under {@code impl/} but declares the
 * parent package to preserve the {@code renderer} namespace.
 */
public record Renderer3DCullOptions(boolean frustum,
                                    boolean sectionVisibility,
                                    boolean visibleOnly,
                                    boolean lookAtTarget,
                                    boolean skipVisibilityWhenTranslucent,
                                    double minLookDot) {
    public static final Renderer3DCullOptions NONE =
            new Renderer3DCullOptions(false, false, false, false, false, 0.0);
    public static final Renderer3DCullOptions FRUSTUM_LOOK =
            new Renderer3DCullOptions(true, true, false, true, false, 0.05);
    public static final Renderer3DCullOptions SMART_TARGET =
            new Renderer3DCullOptions(true, true, true, true, true, 0.05);

    public Renderer3DCullOptions withoutSectionVisibility() {
        return new Renderer3DCullOptions(frustum, false, visibleOnly, lookAtTarget, skipVisibilityWhenTranslucent, minLookDot);
    }
}
