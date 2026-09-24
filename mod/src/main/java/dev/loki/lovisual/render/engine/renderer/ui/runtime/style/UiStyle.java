package dev.loki.lovisual.render.engine.renderer.ui.runtime.style;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.token.UiAlign;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.token.UiJustify;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.token.UiOverflow;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.bridge.UiBlendSpec;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;

public final class UiStyle {
    public static final UiStyle DEFAULT = builder().build();

    private final UiStyleLayout layout;
    private final UiStylePalette palette;
    private final UiStyleText text;

    private UiStyle(Builder builder) {
        this.layout = new UiStyleLayout(builder.layout);
        this.palette = new UiStylePalette(builder.palette);
        this.text = new UiStyleText(builder.text);
    }

    public static Builder builder() { return new Builder(); }

    public Float width() { return layout.width(); }
    public Float height() { return layout.height(); }
    public Float minWidth() { return layout.minWidth(); }
    public Float minHeight() { return layout.minHeight(); }
    public Float maxWidth() { return layout.maxWidth(); }
    public Float maxHeight() { return layout.maxHeight(); }
    public float paddingLeft() { return layout.paddingLeft(); }
    public float paddingTop() { return layout.paddingTop(); }
    public float paddingRight() { return layout.paddingRight(); }
    public float paddingBottom() { return layout.paddingBottom(); }
    public float paddingX() { return layout.paddingX(); }
    public float paddingY() { return layout.paddingY(); }
    public float marginLeft() { return layout.marginLeft(); }
    public float marginTop() { return layout.marginTop(); }
    public float marginRight() { return layout.marginRight(); }
    public float marginBottom() { return layout.marginBottom(); }
    public float marginX() { return layout.marginX(); }
    public float marginY() { return layout.marginY(); }
    public float gap() { return layout.gap(); }
    public float grow() { return layout.grow(); }
    public boolean absolute() { return layout.absolute(); }
    public Float offsetX() { return layout.offsetX(); }
    public Float offsetY() { return layout.offsetY(); }
    public UiAlign align() { return layout.align(); }
    public UiJustify justify() { return layout.justify(); }
    public UiOverflow overflow() { return layout.overflow(); }
    public float radius() { return palette.radius(); }
    public Integer backgroundColor() { return palette.backgroundColor(); }
    public Integer strokeColor() { return palette.strokeColor(); }
    public float strokeWidth() { return palette.strokeWidth(); }
    public Integer shadowColor() { return palette.shadowColor(); }
    public float shadowBlur() { return palette.shadowBlur(); }
    public float shadowInnerAlpha() { return palette.shadowInnerAlpha(); }
    public boolean blur() { return palette.blur(); }
    public boolean liquidGlass() { return palette.liquidGlass(); }
    public boolean clip() { return palette.clip(); }
    public boolean marquee() { return palette.marquee(); }
    public float blurQuality() { return palette.blurQuality(); }
    public float blurBrightness() { return palette.blurBrightness(); }
    public float blurAlpha() { return palette.blurAlpha(); }
    public Integer textColor() { return text.textColor(); }
    public String fontFamily() { return text.fontFamily(); }
    public FontInfo.Type fontType() { return text.fontType(); }
    public float textScale() { return text.textScale(); }
    public boolean textShadow() { return text.textShadow(); }
    public String textEffect() { return text.textEffect(); }
    public int textEffectSpeed() { return text.textEffectSpeed(); }
    public String textBackend() { return text.textBackend(); }
    public float maxTextWidth() { return text.maxTextWidth(); }
    public boolean ellipsis() { return text.ellipsis(); }
    public String textAlign() { return text.textAlign(); }
    public String cursor() { return text.cursor(); }
    public UiBlendSpec blend() { return palette.blend(); }

    public float resolveWidth(float fallback) { return layout.resolveWidth(fallback); }
    public float resolveHeight(float fallback) { return layout.resolveHeight(fallback); }

    public static final class Builder {
        final UiStyleLayout.State layout = new UiStyleLayout.State();
        final UiStylePalette.State palette = new UiStylePalette.State();
        final UiStyleText.State text = new UiStyleText.State();

        public Builder width(float v) { layout.width = v; return this; }
        public Builder height(float v) { layout.height = v; return this; }
        public Builder minWidth(float v) { layout.minWidth = v; return this; }
        public Builder minHeight(float v) { layout.minHeight = v; return this; }
        public Builder maxWidth(float v) { layout.maxWidth = v; return this; }
        public Builder maxHeight(float v) { layout.maxHeight = v; return this; }
        public Builder padding(float p) { return padding(p, p, p, p); }
        public Builder padding(float h, float v) { return padding(h, v, h, v); }
        public Builder padding(float l, float t, float r, float b) {
            layout.paddingLeft = l; layout.paddingTop = t;
            layout.paddingRight = r; layout.paddingBottom = b; return this;
        }
        public Builder margin(float m) { return margin(m, m, m, m); }
        public Builder margin(float h, float v) { return margin(h, v, h, v); }
        public Builder margin(float l, float t, float r, float b) {
            layout.marginLeft = l; layout.marginTop = t;
            layout.marginRight = r; layout.marginBottom = b; return this;
        }
        public Builder marginLeft(float v) { layout.marginLeft = v; return this; }
        public Builder marginTop(float v) { layout.marginTop = v; return this; }
        public Builder marginRight(float v) { layout.marginRight = v; return this; }
        public Builder marginBottom(float v) { layout.marginBottom = v; return this; }
        public Builder gap(float v) { layout.gap = v; return this; }
        public Builder grow(float v) { layout.grow = Math.max(0.0f, v); return this; }
        public Builder absolute(boolean v) { layout.absolute = v; return this; }
        public Builder offsetX(float v) { layout.offsetX = v; return this; }
        public Builder offsetY(float v) { layout.offsetY = v; return this; }
        public Builder align(UiAlign v) {
            layout.align = v != null ? v : UiAlign.START; return this;
        }
        public Builder justify(UiJustify v) {
            layout.justify = v != null ? v : UiJustify.START; return this;
        }
        public Builder overflow(UiOverflow v) {
            layout.overflow = v != null ? v : UiOverflow.VISIBLE;
            if (layout.overflow.clips()) palette.clip = true; return this;
        }
        public Builder radius(float v) { palette.radius = v; return this; }
        public Builder backgroundColor(int v) { palette.backgroundColor = v; return this; }
        public Builder strokeColor(int v) { palette.strokeColor = v; return this; }
        public Builder strokeWidth(float v) { palette.strokeWidth = v; return this; }
        public Builder shadow(int c, float b, float a) {
            palette.shadowColor = c; palette.shadowBlur = Math.max(0.0f, b);
            palette.shadowInnerAlpha = Math.max(0.0f, Math.min(1.0f, a)); return this;
        }
        public Builder blur(boolean v) { palette.blur = v; return this; }
        public Builder liquidGlass(boolean v) { palette.liquidGlass = v; return this; }
        public Builder clip(boolean v) { palette.clip = v; return this; }
        public Builder marquee(boolean v) {
            palette.marquee = v; palette.clip = v || palette.clip; return this;
        }
        public Builder blur(float q, float br, float a) {
            palette.blur = true; palette.blurQuality = q;
            palette.blurBrightness = br; palette.blurAlpha = a; return this;
        }
        public Builder textColor(int v) { text.textColor = v; return this; }
        public Builder font(String f, FontInfo.Type t) {
            text.fontFamily = f; text.fontType = t != null ? t : FontInfo.Type.Regular; return this;
        }
        public Builder textScale(float v) { text.textScale = v; return this; }
        public Builder textShadow(boolean v) { text.textShadow = v; return this; }
        public Builder textEffect(String e, int s) {
            text.textEffect = e != null ? e : ""; text.textEffectSpeed = Math.max(1, s); return this;
        }
        public Builder textBackend(String v) {
            text.textBackend = v != null && !v.isBlank() ? v : "auto"; return this;
        }
        public Builder maxTextWidth(float v) { text.maxTextWidth = Math.max(0.0f, v); return this; }
        public Builder ellipsis(boolean v) { text.ellipsis = v; return this; }
        public Builder textAlign(String v) { text.textAlign = v != null ? v : "left"; return this; }
        public Builder cursor(String v) { text.cursor = v != null ? v : ""; return this; }
        public Builder blend(UiBlendSpec v) {
            palette.blend = v != null ? v : UiBlendSpec.TRANSLUCENT; return this;
        }

        public UiStyle build() { return new UiStyle(this); }
    }
}
