package dev.loki.lovisual.features.gui.hud.script;

final class CompactHudCodec {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private CompactHudCodec() {}

    static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    static String alpha(String hex, float a) {
        int argb = parseColor(hex);
        int na = Math.round(((argb >>> 24) & 0xFF) * clamp01(a));
        return color((argb & 0x00FFFFFF) | (na << 24));
    }

    static float alpha01(int argb) {
        return ((argb >>> 24) & 0xFF) / 255.0f;
    }

    static int parseColor(String hex) {
        if (hex == null || hex.isBlank()) return 0xFFFFFFFF;
        String t = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            if (t.length() == 6) return 0xFF000000 | Integer.parseUnsignedInt(t, 16);
            if (t.length() == 8) return (int) Long.parseLong(t, 16);
        } catch (NumberFormatException ignored) {}
        return 0xFFFFFFFF;
    }

    static String color(int argb) {
        return colorString(argb);
    }

    static String colorString(int argb) {
        char[] out = new char[9];
        out[0] = '#';
        for (int i = 7; i >= 0; i--) out[8 - i] = HEX[(argb >>> (i * 4)) & 0xF];
        return new String(out);
    }

    static long mix(long h, boolean v) { return mix(h, v ? 1 : 0); }

    static long mix(long h, int v) { h ^= v; return h * 0x100000001b3L; }

    static long mix(long h, float v) { return mix(h, Float.floatToIntBits(v)); }

    static long mix(long h, String v) { return mix(h, v != null ? v.hashCode() : 0); }

    static long mixObject(long h, Object v) {
        if (v instanceof Boolean b) return mix(h, b);
        if (v instanceof Integer i) return mix(h, i);
        if (v instanceof Float f) return mix(h, f);
        if (v instanceof Number n) return mix(h, Float.floatToIntBits(n.floatValue()));
        return mix(h, v != null ? v.toString() : "");
    }
}
