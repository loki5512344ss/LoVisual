package dev.loki.lovisual.render.engine.text.font.info;

/**
 * Optional text clip/fade region. Scissor stays external; this spec controls glyph alpha/animation.
 */
public record TextClipSpec(float left,
                           float right,
                           float fadeLeft,
                           float fadeRight,
                           boolean enabled) {
    public static final TextClipSpec NONE = new TextClipSpec(0, 0, 0, 0, false);

    public static TextClipSpec horizontal(float left, float right, float fadeLeft, float fadeRight) {
        return new TextClipSpec(left, right, Math.max(0.0f, fadeLeft), Math.max(0.0f, fadeRight), right > left);
    }
}
