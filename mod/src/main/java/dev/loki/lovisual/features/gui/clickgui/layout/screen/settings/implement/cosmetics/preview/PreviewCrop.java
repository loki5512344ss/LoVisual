package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview;

import java.util.Locale;

/**
 * Which part of a picture a cosmetic card shows, as texture coordinates (0..1). The window always has
 * the card's preview aspect ({@link #ASPECT}), so the player only moves it and zooms.
 */
public record PreviewCrop(float u0, float v0, float u1, float v1) {
    /** Width over height of the card's preview area. */
    public static final float ASPECT = 98f / 57f;
    private static final float MIN_ZOOM = 0.08f;

    /** The largest window of the preview aspect, centred on the picture. */
    public static PreviewCrop centered(int imageW, int imageH) {
        float[] size = fullSize(imageW, imageH);
        return new PreviewCrop((1f - size[0]) / 2f, (1f - size[1]) / 2f, (1f + size[0]) / 2f, (1f + size[1]) / 2f);
    }

    public PreviewCrop moved(float du, float dv) {
        float w = u1 - u0;
        float h = v1 - v0;
        float nu = clamp(u0 + du, 0f, 1f - w);
        float nv = clamp(v0 + dv, 0f, 1f - h);
        return new PreviewCrop(nu, nv, nu + w, nv + h);
    }

    /** Scales the window around its centre; factor above 1 shows more of the picture. */
    public PreviewCrop zoomed(float factor, int imageW, int imageH) {
        float[] max = fullSize(imageW, imageH);
        float w = clamp((u1 - u0) * factor, max[0] * MIN_ZOOM, max[0]);
        float h = w * imageW / (ASPECT * imageH);
        float cu = clamp((u0 + u1) / 2f, w / 2f, 1f - w / 2f);
        float cv = clamp((v0 + v1) / 2f, h / 2f, 1f - h / 2f);
        return new PreviewCrop(cu - w / 2f, cv - h / 2f, cu + w / 2f, cv + h / 2f);
    }

    public String serialize() {
        return String.format(Locale.ROOT, "%.5f %.5f %.5f %.5f", u0, v0, u1, v1);
    }

    /** Parses {@link #serialize()} output; null for anything malformed or out of range. */
    public static PreviewCrop parse(String text) {
        if (text == null) return null;
        String[] parts = text.trim().split("\\s+");
        if (parts.length != 4) return null;
        try {
            float[] v = new float[4];
            for (int i = 0; i < 4; i++) v[i] = Float.parseFloat(parts[i]);
            boolean valid = v[0] >= 0f && v[1] >= 0f && v[2] <= 1f && v[3] <= 1f && v[0] < v[2] && v[1] < v[3];
            return valid ? new PreviewCrop(v[0], v[1], v[2], v[3]) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Width and height (in uv) of the biggest window with the preview aspect that fits the picture. */
    private static float[] fullSize(int imageW, int imageH) {
        float imageAspect = (float) imageW / Math.max(1, imageH);
        return imageAspect >= ASPECT
                ? new float[]{ASPECT / imageAspect, 1f}
                : new float[]{1f, imageAspect / ASPECT};
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
