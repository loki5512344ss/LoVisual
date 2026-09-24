package dev.loki.lovisual.features.gui.hud.script;

import java.util.LinkedHashMap;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;

final class CompactHudLayout {
    private CompactHudLayout() {}

    static float reservedRootWidth(CompactHudStatModel m) {
        float s = m.scale() != 0 ? m.scale() : 1.0f;
        return switch (m.id()) {
            case "fps" -> (26 + 36 + 2 + 17 + 5) * s;
            case "ping" -> (26 + 40 + 2 + 16 + 5) * s;
            case "tps" -> (26 + 39 + 2 + 15 + 3.5f) * s;
            case "memory" -> (26 + 74 + 2 + 17 + 5 + 34 + 5) * s;
            case "speed_bps" -> (26 + 36 + 2 + 20 + 3.5f) * s;
            case "game_time" -> (28.5f + 45 + 2 + 18 + 5) * s;
            case "system_time" -> (28.5f + 66 + 2 + 18 + 5) * s;
            case "xyz" -> (bool(m, "showNether") ? 150 : 112) * s;
            default -> Math.max(m.width(), (26 + 42 + 2 + 18 + 5) * s);
        };
    }

    static float reservedValueWidth(CompactHudStatModel m) {
        float s = m.scale() != 0 ? m.scale() : 1.0f;
        float slot = switch (m.id()) {
            case "fps" -> 36; case "ping" -> 40; case "tps" -> 39; case "memory" -> 74;
            case "speed_bps" -> 36; case "game_time" -> 45; case "system_time" -> 66;
            case "xyz" -> 28; default -> 42;
        };
        return Math.max(m.value().width, slot * s);
    }

    static float reservedUnitWidth(CompactHudStatModel m) {
        float s = m.scale() != 0 ? m.scale() : 1.0f;
        float slot = switch (m.id()) {
            case "fps", "memory" -> 17; case "ping" -> 16; case "tps" -> 15; case "speed_bps" -> 20; default -> 18;
        };
        return Math.max(m.unit().width, slot * s);
    }

    static float reservedExtraWidth(CompactHudStatModel m) {
        float s = m.scale() != 0 ? m.scale() : 1.0f;
        float slot = switch (m.id()) {
            case "memory" -> 34; case "xyz" -> bool(m, "showNether") ? 34 : 0; default -> 0;
        };
        return Math.max(m.extra().width, slot * s);
    }

    static float visualWidth(CompactHudStatModel m) {
        if ("xyz".equals(m.id()) && bool(m, "showNether")) return Math.max(1.0f, m.width() - 4.0f * m.scale());
        return m.width();
    }

    static long mixDataShape(CompactHudStatModel m, long h) {
        for (var e : m.dataMap().entrySet()) {
            String k = e.getKey(); Object v = e.getValue();
            h = CompactHudCodec.mix(h, k);
            if (v instanceof Boolean b) h = CompactHudCodec.mix(h, b);
            else if ("labelColor".equals(k) || "valueColor".equals(k)) h = CompactHudCodec.mixObject(h, v);
            else if ("netherText".equals(k)) h = CompactHudCodec.mix(h, v != null && !v.toString().isEmpty());
            else if (v != null) h = CompactHudCodec.mix(h, v.getClass().getName());
            else h = CompactHudCodec.mix(h, 0);
        }
        return h;
    }

    static LinkedHashMap<String, LinkedHashMap<String, Object>> runtimePatches(CompactHudStatModel m) {
        var patches = new LinkedHashMap<String, LinkedHashMap<String, Object>>(12);
        putProp(patches, "compact-stat:" + m.id(), "renderBlurAlpha", m.background().blurAlpha);
        putProp(patches, "fill", "startColor", CompactHudCodec.color(m.background().primary));
        putProp(patches, "fill", "endColor", CompactHudCodec.color(m.background().secondary));
        float pa = Math.max(CompactHudCodec.alpha01(m.background().primary), CompactHudCodec.alpha01(m.background().secondary));
        float sa = Math.max(CompactHudCodec.alpha01(m.background().stroke), pa);
        putProp(patches, "fill", "stroke", CompactHudCodec.alpha(CompactHudCodec.color(m.background().stroke), sa));
        putProp(patches, "fill", "strokeStartColor", CompactHudCodec.alpha("#FFFFFFFF", 0.16f * sa));
        putProp(patches, "fill", "strokeEndColor", CompactHudCodec.alpha(CompactHudCodec.color(m.background().stroke), sa));
        putProp(patches, "top-glint", "startColor", CompactHudCodec.alpha("#FFFFFFFF", 0.065f * pa));
        putProp(patches, "top-glint", "endColor", "#00000000");
        putProp(patches, "icon:texture", "tint", CompactHudCodec.color(m.icon().color));
        putProp(patches, "icon:glyph", "color", CompactHudCodec.color(m.icon().color));
        putProp(patches, "divider", "fill", CompactHudCodec.color(m.divider().color));
        putText(patches, "value", m.value().text, CompactHudCodec.color(m.value().color));
        putText(patches, "unit", m.unit().text, CompactHudCodec.color(m.unit().color));
        putText(patches, "extra", m.extra().text, CompactHudCodec.color(m.extra().color));
        float pr = CompactHudCodec.clamp01(m.animation().digitProgress);
        float off = m.animation().digitOffset;
        putText(patches, "digit:current", m.value().text, CompactHudCodec.alpha(CompactHudCodec.color(m.value().color), pr), -off * (1 - pr) + 2.0f);
        putText(patches, "digit:prev", m.animation().previousValue, CompactHudCodec.alpha(CompactHudCodec.color(m.value().color), 1 - pr), off * pr + 2.0f);
        putText(patches, "x:v", str(m, "xText", "0"), str(m, "valueColor", CompactHudCodec.color(m.value().color)));
        putText(patches, "y:v", str(m, "yText", "0"), str(m, "valueColor", CompactHudCodec.color(m.value().color)));
        putText(patches, "z:v", str(m, "zText", "0"), str(m, "valueColor", CompactHudCodec.color(m.value().color)));
        putText(patches, "nether", str(m, "netherText", ""), CompactHudCodec.color(m.extra().color));
        return patches;
    }

    static LinkedHashMap<String, UiBounds> runtimeBounds(CompactHudStatModel m) {
        var patches = new LinkedHashMap<String, UiBounds>(24);
        float vw = visualWidth(m);
        putBounds(patches, "compact-stat:" + m.id(), m.rootX(), m.rootY(), vw, m.height());
        putBounds(patches, "clip", m.rootX(), m.rootY(), vw, m.height());
        putBounds(patches, "fill", m.rootX(), m.rootY(), vw, m.height());
        putBounds(patches, "top-glint", m.rootX() + 1, m.rootY() + 1, Math.max(0, vw - 2), Math.max(1, m.height() * 0.42f));
        if (m.icon().visible) {
            putBounds(patches, "icon:texture", m.rootX() + m.icon().x, m.rootY() + m.icon().y, m.icon().width, m.icon().height);
            putBounds(patches, "icon:glyph", m.rootX() + m.icon().x, m.rootY() + m.icon().y, m.icon().width, m.icon().height);
        }
        if (m.divider().visible) putBounds(patches, "divider", m.rootX() + m.divider().x, m.rootY() + m.divider().y, m.divider().width, m.divider().height);
        if ("xyz".equals(m.id())) putCoords(m, patches, vw);
        else if (m.animation().digitAnimation) {
            putBounds(patches, "digit:layer", m.rootX(), m.rootY(), m.width(), m.height());
            putBounds(patches, "digit:clip", m.rootX() + m.value().x, m.rootY() + m.value().y - 2, m.value().width, Math.max(8, m.height()));
            putBounds(patches, "digit:prev", m.rootX() + m.value().x, m.rootY() + m.value().y - 2, m.value().width, Math.max(8, m.height()));
            putBounds(patches, "digit:current", m.rootX() + m.value().x, m.rootY() + m.value().y - 2, m.value().width, Math.max(8, m.height()));
            putBounds(patches, "unit", m.unit().visible ? m.rootX() + m.unit().x : m.rootX(), m.unit().visible ? m.rootY() + m.unit().y : m.rootY(), m.unit().visible ? m.unit().width : 0, m.unit().visible ? Math.max(8, m.height()) : 0);
            putBounds(patches, "extra", m.extra().visible ? m.rootX() + m.extra().x : m.rootX(), m.extra().visible ? m.rootY() + m.extra().y : m.rootY(), m.extra().visible ? m.extra().width : 0, m.extra().visible ? Math.max(8, m.height()) : 0);
        } else {
            putBounds(patches, "text:layer", m.rootX(), m.rootY(), m.width(), m.height());
            putBounds(patches, "value", m.value().visible ? m.rootX() + m.value().x : m.rootX(), m.value().visible ? m.rootY() + m.value().y : m.rootY(), m.value().visible ? m.value().width : 0, m.value().visible ? Math.max(8, m.height()) : 0);
            putBounds(patches, "unit", m.unit().visible ? m.rootX() + m.unit().x : m.rootX(), m.unit().visible ? m.rootY() + m.unit().y : m.rootY(), m.unit().visible ? m.unit().width : 0, m.unit().visible ? Math.max(8, m.height()) : 0);
            putBounds(patches, "extra", m.extra().visible ? m.rootX() + m.extra().x : m.rootX(), m.extra().visible ? m.rootY() + m.extra().y : m.rootY(), m.extra().visible ? m.extra().width : 0, m.extra().visible ? Math.max(8, m.height()) : 0);
        }
        return patches;
    }

    private static void putCoords(CompactHudStatModel m, LinkedHashMap<String, UiBounds> p, float vw) {
        float rx = m.rootX() + m.value().x, ry = m.rootY() + m.value().y, rh = Math.max(8, m.height() - m.value().y);
        float pg = Math.max(1, m.scale() * 1.6f), g = Math.max(2, m.scale() * 3.2f);
        float lxw = num(m, "labelXW", 6 * m.scale()), lyw = num(m, "labelYW", lxw), lzw = num(m, "labelZW", lxw);
        float vxw = num(m, "valueXW", 28 * m.scale()), vyw = num(m, "valueYW", vxw), vzw = num(m, "valueZW", vxw);
        float ew = num(m, "xyzExtraW", m.extra().width);
        float c = rx;
        putBounds(p, "coords:row", rx, ry, Math.max(0, vw - m.value().x - 4), rh);
        putBounds(p, "x:l", c, ry, lxw, rh); c += lxw + pg;
        putBounds(p, "x:v", c, ry, vxw, rh); c += vxw + g;
        putBounds(p, "y:l", c, ry, lyw, rh); c += lyw + pg;
        putBounds(p, "y:v", c, ry, vyw, rh); c += vyw + g;
        putBounds(p, "z:l", c, ry, lzw, rh); c += lzw + pg;
        putBounds(p, "z:v", c, ry, vzw, rh); c += vzw + g;
        if (bool(m, "showNether")) putBounds(p, "nether", c, ry, ew, rh);
    }

    private static void putBounds(LinkedHashMap<String, UiBounds> p, String k, float x, float y, float w, float h) {
        p.put(k, new UiBounds(x, y, Math.max(0, w), Math.max(0, h)));
    }

    private static void putProp(LinkedHashMap<String, LinkedHashMap<String, Object>> p, String k, String prop, Object v) {
        var m = p.get(k); if (m == null) { m = new LinkedHashMap<>(2); p.put(k, m); } m.put(prop, v);
    }

    private static void putText(LinkedHashMap<String, LinkedHashMap<String, Object>> p, String k, String t, String c) { putText(p, k, t, c, Float.NaN); }

    private static void putText(LinkedHashMap<String, LinkedHashMap<String, Object>> p, String k, String t, String c, float off) {
        var m = new LinkedHashMap<String, Object>(3); m.put("text", t != null ? t : "");
        if (c != null) m.put("color", c); if (!Float.isNaN(off)) m.put("textOffsetY", off); p.put(k, m);
    }

    static boolean bool(CompactHudStatModel m, String k) { Object v = m.dataMap().get(k); return v instanceof Boolean b && b; }

    static float num(CompactHudStatModel m, String k, float fb) {
        Object v = m.dataMap().get(k);
        if (v instanceof Number n) return n.floatValue();
        if (v instanceof String s) try { return Float.parseFloat(s); } catch (NumberFormatException ignored) {}
        return fb;
    }

    static String str(CompactHudStatModel m, String k, String fb) { Object v = m.dataMap().get(k); return v != null ? String.valueOf(v) : fb; }
}
