package dev.loki.lovisual.render.engine.renderer.ui.runtime.animation;

public record UiAnimationSpec(String property,
                              float from,
                              float to,
                              long durationMs,
                              long delayMs,
                              UiEasing easing,
                              boolean loop,
                              boolean alternate) {
    public UiAnimationSpec {
        property = property != null ? property : "";
        durationMs = Math.max(1L, durationMs);
        delayMs = Math.max(0L, delayMs);
        easing = easing != null ? easing : UiEasing.EASE_OUT_CUBIC;
    }
}
