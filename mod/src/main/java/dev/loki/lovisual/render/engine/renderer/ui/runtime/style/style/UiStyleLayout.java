package dev.loki.lovisual.render.engine.renderer.ui.runtime.style;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.token.UiAlign;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.token.UiJustify;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.token.UiOverflow;

final class UiStyleLayout {
    private final Float width;
    private final Float height;
    private final Float minWidth;
    private final Float minHeight;
    private final Float maxWidth;
    private final Float maxHeight;
    private final float paddingLeft;
    private final float paddingTop;
    private final float paddingRight;
    private final float paddingBottom;
    private final float marginLeft;
    private final float marginTop;
    private final float marginRight;
    private final float marginBottom;
    private final float gap;
    private final float grow;
    private final boolean absolute;
    private final Float offsetX;
    private final Float offsetY;
    private final UiAlign align;
    private final UiJustify justify;
    private final UiOverflow overflow;

    UiStyleLayout(State state) {
        this.width = state.width;
        this.height = state.height;
        this.minWidth = state.minWidth;
        this.minHeight = state.minHeight;
        this.maxWidth = state.maxWidth;
        this.maxHeight = state.maxHeight;
        this.paddingLeft = state.paddingLeft;
        this.paddingTop = state.paddingTop;
        this.paddingRight = state.paddingRight;
        this.paddingBottom = state.paddingBottom;
        this.marginLeft = state.marginLeft;
        this.marginTop = state.marginTop;
        this.marginRight = state.marginRight;
        this.marginBottom = state.marginBottom;
        this.gap = state.gap;
        this.grow = state.grow;
        this.absolute = state.absolute;
        this.offsetX = state.offsetX;
        this.offsetY = state.offsetY;
        this.align = state.align;
        this.justify = state.justify;
        this.overflow = state.overflow;
    }

    Float width() { return width; }
    Float height() { return height; }
    Float minWidth() { return minWidth; }
    Float minHeight() { return minHeight; }
    Float maxWidth() { return maxWidth; }
    Float maxHeight() { return maxHeight; }
    float paddingLeft() { return paddingLeft; }
    float paddingTop() { return paddingTop; }
    float paddingRight() { return paddingRight; }
    float paddingBottom() { return paddingBottom; }
    float paddingX() { return paddingLeft + paddingRight; }
    float paddingY() { return paddingTop + paddingBottom; }
    float marginLeft() { return marginLeft; }
    float marginTop() { return marginTop; }
    float marginRight() { return marginRight; }
    float marginBottom() { return marginBottom; }
    float marginX() { return marginLeft + marginRight; }
    float marginY() { return marginTop + marginBottom; }
    float gap() { return gap; }
    float grow() { return grow; }
    boolean absolute() { return absolute; }
    Float offsetX() { return offsetX; }
    Float offsetY() { return offsetY; }
    UiAlign align() { return align; }
    UiJustify justify() { return justify; }
    UiOverflow overflow() { return overflow; }

    float resolveWidth(float fallback) {
        return clamp(width != null ? width : fallback, minWidth, maxWidth);
    }

    float resolveHeight(float fallback) {
        return clamp(height != null ? height : fallback, minHeight, maxHeight);
    }

    private static float clamp(float value, Float min, Float max) {
        float out = value;
        if (min != null) out = Math.max(out, min);
        if (max != null) out = Math.min(out, max);
        return out;
    }

    static final class State {
        Float width;
        Float height;
        Float minWidth;
        Float minHeight;
        Float maxWidth;
        Float maxHeight;
        float paddingLeft;
        float paddingTop;
        float paddingRight;
        float paddingBottom;
        float marginLeft;
        float marginTop;
        float marginRight;
        float marginBottom;
        float gap;
        float grow;
        boolean absolute;
        Float offsetX;
        Float offsetY;
        UiAlign align = UiAlign.START;
        UiJustify justify = UiJustify.START;
        UiOverflow overflow = UiOverflow.VISIBLE;
    }
}
