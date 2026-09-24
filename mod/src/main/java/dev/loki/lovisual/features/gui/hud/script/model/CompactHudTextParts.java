package dev.loki.lovisual.features.gui.hud.script;

import java.util.LinkedHashMap;

public final class CompactHudTextParts {
    private CompactHudTextParts() {}

    public static class Divider {
        public boolean visible;
        public float x; public float y; public float width; public float height; public int color;

        public Divider set(float x, float y, float w, float h, int c) {
            this.visible = true; this.x = x; this.y = y; this.width = w; this.height = h; this.color = c; return this;
        }

        void writeTo(LinkedHashMap<String, Object> p) {
            p.put("dividerVisible", visible); p.put("dividerX", x); p.put("dividerY", y);
            p.put("dividerW", width); p.put("dividerH", height);
            p.put("dividerColor", CompactHudCodec.color(color));
        }

        long mix(long h) {
            h = CompactHudCodec.mix(h, visible); h = CompactHudCodec.mix(h, x);
            h = CompactHudCodec.mix(h, y); h = CompactHudCodec.mix(h, width);
            h = CompactHudCodec.mix(h, height); return h;
        }
        long mixStructure(long h) { return CompactHudCodec.mix(h, visible); }
        long mixLayout(long h) {
            h = CompactHudCodec.mix(h, x); h = CompactHudCodec.mix(h, y);
            h = CompactHudCodec.mix(h, width); h = CompactHudCodec.mix(h, height); return h;
        }
    }

    public static class TextRun {
        public boolean visible;
        public String text = "";
        public String font = "";
        public float scale;
        public float x; public float y; public float width;
        public int color;

        public TextRun set(String text, String font, float scale, float x, float y, float w, int c) {
            this.visible = text != null && !text.isEmpty();
            this.text = text != null ? text : ""; this.font = font != null ? font : "";
            this.scale = scale; this.x = x; this.y = y; this.width = w; this.color = c; return this;
        }

        public TextRun hidden() {
            this.visible = false; this.text = ""; this.font = ""; this.scale = 1.0f;
            this.x = 0; this.y = 0; this.width = 0; this.color = 0; return this;
        }

        void writeTo(LinkedHashMap<String, Object> p, String prefix) {
            p.put(prefix + "Visible", visible); p.put(prefix + "Text", text);
            p.put(prefix + "Font", font); p.put(prefix + "Scale", scale);
            p.put(prefix + "X", x); p.put(prefix + "Y", y);
            p.put(prefix + "W", width); p.put(prefix + "Color", CompactHudCodec.color(color));
        }

        long mixStructure(long h) {
            h = CompactHudCodec.mix(h, visible); h = CompactHudCodec.mix(h, font); h = CompactHudCodec.mix(h, scale); return h;
        }
        long mixLayout(long h, float rw) { return CompactHudCodec.mix(h, rw); }
        long mixDynamic(long h) {
            h = CompactHudCodec.mix(h, visible); h = CompactHudCodec.mix(h, text); h = CompactHudCodec.mix(h, color); return h;
        }
    }

    public static class Animation {
        public String labelEffect = "NONE";
        public int labelEffectSpeed;
        public float effectTime;
        public boolean digitAnimation;
        public String previousValue = "";
        public float digitProgress = 1.0f;
        public float digitOffset;

        public Animation setLabelEffect(String e, int s, float t) {
            this.labelEffect = e != null ? e : "NONE"; this.labelEffectSpeed = s; this.effectTime = t; return this;
        }

        public Animation setDigitAnimation(boolean en, String prev, float pr, float off) {
            this.digitAnimation = en; this.previousValue = prev != null ? prev : "";
            this.digitProgress = pr; this.digitOffset = off; return this;
        }

        void writeTo(LinkedHashMap<String, Object> p) {
            p.put("labelEffect", labelEffect); p.put("labelEffectSpeed", labelEffectSpeed);
            p.put("effectTime", effectTime); p.put("digitAnimation", digitAnimation);
            p.put("previousValue", previousValue); p.put("digitProgress", digitProgress);
            p.put("digitOffset", digitOffset);
        }

        long mixStructure(long h) {
            h = CompactHudCodec.mix(h, labelEffect); h = CompactHudCodec.mix(h, labelEffectSpeed); h = CompactHudCodec.mix(h, digitAnimation); return h;
        }
        long mixDynamic(long h) {
            h = CompactHudCodec.mix(h, effectTime); h = CompactHudCodec.mix(h, previousValue);
            h = CompactHudCodec.mix(h, digitProgress); h = CompactHudCodec.mix(h, digitOffset); return h;
        }
    }
}
