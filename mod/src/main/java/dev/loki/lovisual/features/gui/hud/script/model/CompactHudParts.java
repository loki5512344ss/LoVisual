package dev.loki.lovisual.features.gui.hud.script;

import java.util.LinkedHashMap;

public final class CompactHudParts {
    private CompactHudParts() {}

    public static class Background {
        public String effect = "None";
        public boolean theme;
        public float blurAlpha;
        public int primary;
        public int secondary;
        public int stroke;
        public float strokeWidth;
        public float softness;

        public Background set(String effect, boolean theme, float blurAlpha, int primary, int secondary, int stroke, float strokeWidth, float softness) {
            this.effect = effect != null ? effect : "None";
            this.theme = theme;
            this.blurAlpha = blurAlpha;
            this.primary = primary;
            this.secondary = secondary;
            this.stroke = stroke;
            this.strokeWidth = strokeWidth;
            this.softness = softness;
            return this;
        }

        void writeTo(LinkedHashMap<String, Object> p) {
            p.put("backgroundEffect", effect);
            p.put("backgroundTheme", theme);
            p.put("blurAlpha", blurAlpha);
            p.put("bgPrimary", CompactHudCodec.color(primary));
            p.put("bgSecondary", CompactHudCodec.color(secondary));
            p.put("stroke", CompactHudCodec.color(stroke));
            p.put("strokeWidth", strokeWidth);
            p.put("softness", softness);
        }

        long mix(long h) {
            h = CompactHudCodec.mix(h, effect);
            h = CompactHudCodec.mix(h, theme);
            h = CompactHudCodec.mix(h, blurAlpha);
            h = CompactHudCodec.mix(h, primary);
            h = CompactHudCodec.mix(h, secondary);
            h = CompactHudCodec.mix(h, stroke);
            h = CompactHudCodec.mix(h, strokeWidth);
            h = CompactHudCodec.mix(h, softness);
            return h;
        }

        long mixStructure(long h) { return CompactHudCodec.mix(h, effect); }

        long mixLayout(long h) {
            h = CompactHudCodec.mix(h, strokeWidth);
            h = CompactHudCodec.mix(h, softness);
            return h;
        }
    }

    public static class Icon {
        public boolean visible;
        public String kind = "";
        public String id = "";
        public String glyph = "";
        public String font = "";
        public float x;
        public float y;
        public float width;
        public float height;
        public float scale;
        public int color;

        public Icon texture(String id, float x, float y, float w, float h, int c) {
            return set(true, "texture", id, "", "", x, y, w, h, 1.0f, c);
        }

        public Icon glyph(String g, String f, float x, float y, float w, float h, float s, int c) {
            return set(true, "glyph", "", g, f, x, y, w, h, s, c);
        }

        public Icon hidden() { return set(false, "", "", "", "", 0, 0, 0, 0, 1.0f, 0); }

        private Icon set(boolean v, String k, String i, String g, String f, float x, float y, float w, float h, float s, int c) {
            this.visible = v; this.kind = k != null ? k : ""; this.id = i != null ? i : "";
            this.glyph = g != null ? g : ""; this.font = f != null ? f : "";
            this.x = x; this.y = y; this.width = w; this.height = h; this.scale = s; this.color = c;
            return this;
        }

        void writeTo(LinkedHashMap<String, Object> p) {
            p.put("iconVisible", visible); p.put("iconKind", kind); p.put("iconId", id);
            p.put("iconGlyph", glyph); p.put("iconFont", font); p.put("iconX", x);
            p.put("iconY", y); p.put("iconW", width); p.put("iconH", height);
            p.put("iconScale", scale); p.put("iconColor", CompactHudCodec.color(color));
        }

        long mix(long h) {
            h = CompactHudCodec.mix(h, visible); h = CompactHudCodec.mix(h, kind);
            h = CompactHudCodec.mix(h, id); h = CompactHudCodec.mix(h, glyph);
            h = CompactHudCodec.mix(h, font); h = CompactHudCodec.mix(h, x);
            h = CompactHudCodec.mix(h, y); h = CompactHudCodec.mix(h, width);
            h = CompactHudCodec.mix(h, height); h = CompactHudCodec.mix(h, scale);
            return h;
        }

        long mixStructure(long h) {
            h = CompactHudCodec.mix(h, visible); h = CompactHudCodec.mix(h, kind);
            h = CompactHudCodec.mix(h, id); h = CompactHudCodec.mix(h, glyph);
            h = CompactHudCodec.mix(h, font); return h;
        }

        long mixLayout(long h) {
            h = CompactHudCodec.mix(h, x); h = CompactHudCodec.mix(h, y);
            h = CompactHudCodec.mix(h, width); h = CompactHudCodec.mix(h, height);
            h = CompactHudCodec.mix(h, scale); return h;
        }
    }
}
