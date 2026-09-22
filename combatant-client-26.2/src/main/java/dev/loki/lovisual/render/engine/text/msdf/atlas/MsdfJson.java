/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.msdf.atlas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

/**
 * Толерантное чтение полей msdf-bmfont JSON: любое отсутствие/несоответствие
 * типа отдаёт fallback вместо исключения.
 */
final class MsdfJson {
    private MsdfJson() {
    }

    static float getFloat(JsonObject obj, String key, float fallback) {
        if (obj == null || key == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static int getInt(JsonObject obj, String key, int fallback) {
        if (obj == null || key == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static String getString(JsonObject obj, String key, String fallback) {
        if (obj == null || key == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static int readUnicode(JsonElement element) {
        if (element == null) return -1;
        try {
            if (element.isJsonPrimitive()) {
                var prim = element.getAsJsonPrimitive();
                if (prim.isNumber()) return prim.getAsInt();
                if (prim.isString()) {
                    String str = prim.getAsString();
                    if (str.isEmpty()) return -1;
                    if (str.length() == 1) return str.codePointAt(0);
                    if (str.startsWith("0x") || str.startsWith("0X")) {
                        return Integer.parseInt(str.substring(2), 16);
                    }
                    return Integer.parseInt(str);
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    static @Nullable MsdfGlyph.Bounds readBounds(JsonObject obj) {
        if (obj == null) return null;
        float left = getFloat(obj, "left", 0f);
        float right = getFloat(obj, "right", 0f);
        float top = getFloat(obj, "top", 0f);
        float bottom = getFloat(obj, "bottom", 0f);
        return new MsdfGlyph.Bounds(left, bottom, right, top);
    }
}
