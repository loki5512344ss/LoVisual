package dev.loki.lovisual.render.engine.renderer.style;

/**
 * Immutable material parameters for drawing one background plate.
 *
 * <p>Field order: style, softness, fresnelPower, fresnelAlpha, baseAlpha,
 * fresnelMix, distort, squircle, tint, strokeWidth, blurQuality.
 * Fields that a style does not use are carried as zeroes; see
 * {@link StyleSpecs#resolve(SurfaceStyle)}.
 */
public record StyleSpec(
        SurfaceStyle style,
        float softness,
        float fresnelPower,
        float fresnelAlpha,
        float baseAlpha,
        float fresnelMix,
        float distort,
        float squircle,
        int tint,
        float strokeWidth,
        float blurQuality) {
}
