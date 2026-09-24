package dev.loki.lovisual.render.engine.renderer.ui.runtime.style;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.bridge.UiBlendSpec;

final class UiStylePalette {
    private final float radius;
    private final Integer backgroundColor;
    private final Integer strokeColor;
    private final float strokeWidth;
    private final Integer shadowColor;
    private final float shadowBlur;
    private final float shadowInnerAlpha;
    private final boolean blur;
    private final boolean liquidGlass;
    private final boolean clip;
    private final boolean marquee;
    private final float blurQuality;
    private final float blurBrightness;
    private final float blurAlpha;
    private final UiBlendSpec blend;

    UiStylePalette(State state) {
        this.radius = state.radius;
        this.backgroundColor = state.backgroundColor;
        this.strokeColor = state.strokeColor;
        this.strokeWidth = state.strokeWidth;
        this.shadowColor = state.shadowColor;
        this.shadowBlur = state.shadowBlur;
        this.shadowInnerAlpha = state.shadowInnerAlpha;
        this.blur = state.blur;
        this.liquidGlass = state.liquidGlass;
        this.clip = state.clip;
        this.marquee = state.marquee;
        this.blurQuality = state.blurQuality;
        this.blurBrightness = state.blurBrightness;
        this.blurAlpha = state.blurAlpha;
        this.blend = state.blend;
    }

    float radius() { return radius; }
    Integer backgroundColor() { return backgroundColor; }
    Integer strokeColor() { return strokeColor; }
    float strokeWidth() { return strokeWidth; }
    Integer shadowColor() { return shadowColor; }
    float shadowBlur() { return shadowBlur; }
    float shadowInnerAlpha() { return shadowInnerAlpha; }
    boolean blur() { return blur; }
    boolean liquidGlass() { return liquidGlass; }
    boolean clip() { return clip; }
    boolean marquee() { return marquee; }
    float blurQuality() { return blurQuality; }
    float blurBrightness() { return blurBrightness; }
    float blurAlpha() { return blurAlpha; }
    UiBlendSpec blend() { return blend; }

    static final class State {
        float radius;
        Integer backgroundColor;
        Integer strokeColor;
        float strokeWidth = 1.0f;
        Integer shadowColor;
        float shadowBlur = 10.0f;
        float shadowInnerAlpha = 0.18f;
        boolean blur;
        boolean liquidGlass;
        boolean clip;
        boolean marquee;
        float blurQuality = 8.0f;
        float blurBrightness = 1.0f;
        float blurAlpha = 0.35f;
        UiBlendSpec blend = UiBlendSpec.TRANSLUCENT;
    }
}
