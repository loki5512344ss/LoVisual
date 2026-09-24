package dev.loki.lovisual.render.engine.renderer.ui.runtime.style;

import dev.loki.lovisual.render.engine.text.font.info.FontInfo;

final class UiStyleText {
    private final Integer textColor;
    private final String fontFamily;
    private final FontInfo.Type fontType;
    private final float textScale;
    private final boolean textShadow;
    private final String textEffect;
    private final int textEffectSpeed;
    private final String textBackend;
    private final float maxTextWidth;
    private final boolean ellipsis;
    private final String textAlign;
    private final String cursor;

    UiStyleText(State state) {
        this.textColor = state.textColor;
        this.fontFamily = state.fontFamily;
        this.fontType = state.fontType;
        this.textScale = state.textScale;
        this.textShadow = state.textShadow;
        this.textEffect = state.textEffect;
        this.textEffectSpeed = state.textEffectSpeed;
        this.textBackend = state.textBackend;
        this.maxTextWidth = state.maxTextWidth;
        this.ellipsis = state.ellipsis;
        this.textAlign = state.textAlign;
        this.cursor = state.cursor;
    }

    Integer textColor() { return textColor; }
    String fontFamily() { return fontFamily; }
    FontInfo.Type fontType() { return fontType; }
    float textScale() { return textScale; }
    boolean textShadow() { return textShadow; }
    String textEffect() { return textEffect; }
    int textEffectSpeed() { return textEffectSpeed; }
    String textBackend() { return textBackend; }
    float maxTextWidth() { return maxTextWidth; }
    boolean ellipsis() { return ellipsis; }
    String textAlign() { return textAlign; }
    String cursor() { return cursor; }

    static final class State {
        Integer textColor = 0xFFFFFFFF;
        String fontFamily;
        FontInfo.Type fontType = FontInfo.Type.Regular;
        float textScale = 1.0f;
        boolean textShadow;
        String textEffect = "";
        int textEffectSpeed = 18;
        String textBackend = "auto";
        float maxTextWidth;
        boolean ellipsis;
        String textAlign = "left";
        String cursor = "";
    }
}
