/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.UiBoundsPatchSet;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.UiScriptColor;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.UiScriptPatchSet;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.UiScriptProps;

/**
 * Примитивы патчинга UI-скрипта: текст, изображения, формы и bounds-патчи.
 */
public final class IslandPatchOps {

    private IslandPatchOps() {
    }

    public static void putControlBounds(UiBoundsPatchSet patches,
                                        String key,
                                        float x,
                                        float y,
                                        float size) {
        putBounds(patches, key + ":hover", x, y, size, size);
        putBounds(patches, key, x, y + 4.0f, size, Math.max(0.0f, size - 4.0f));
    }

    public static void putSvgControlBounds(UiBoundsPatchSet patches,
                                           String key,
                                           float x,
                                           float y,
                                           float size) {
        float iconSize = Math.max(0.0f, size * 0.74f);
        putBounds(patches, key + ":hover", x, y, size, size);
        putBounds(patches, key, x + (size - iconSize) * 0.5f, y + (size - iconSize) * 0.5f, iconSize, iconSize);
    }

    public static void putBounds(UiBoundsPatchSet patches,
                                 String key,
                                 float x,
                                 float y,
                                 float width,
                                 float height) {
        patches.put(key, x, y, width, height);
    }

    public static void patchControl(UiScriptPatchSet patches,
                                    String key,
                                    String icon,
                                    String iconColor,
                                    String accentSoft,
                                    float expandedAlpha,
                                    float hover) {
        patchText(patches, key, icon, scaleHexAlpha(iconColor, expandedAlpha));
        patchShape(patches, key + ":hover", "fill", scaleHexAlpha(accentSoft, Math.max(0.0f, hover) * 0.3f * expandedAlpha));
    }

    public static void patchSvgControl(UiScriptPatchSet patches,
                                       String key,
                                       String asset,
                                       String iconColor,
                                       String accentSoft,
                                       float expandedAlpha,
                                       float hover) {
        patchImage(patches, key, asset, scaleHexAlpha(iconColor, expandedAlpha));
        patchShape(patches, key + ":hover", "fill", scaleHexAlpha(accentSoft, Math.max(0.0f, hover) * 0.3f * expandedAlpha));
    }

    public static void patchText(UiScriptPatchSet patches,
                                 String key,
                                 String text,
                                 String color) {
        patches.text(key, text, color);
    }

    public static void patchClippedText(UiScriptPatchSet patches,
                                        String key,
                                        float measuredWidth,
                                        float boxWidth,
                                        float scrollTime,
                                        float delay,
                                        float speed,
                                        float fadeWidth) {
        patches.clippedText(key, measuredWidth, boxWidth, scrollTime, delay, speed, fadeWidth);
    }

    public static void patchImage(UiScriptPatchSet patches,
                                  String key,
                                  String asset,
                                  String tint) {
        patches.image(key, asset, tint);
    }

    public static void patchShape(UiScriptPatchSet patches,
                                  String key,
                                  Object... pairs) {
        patches.props(key, pairs);
    }

    public static void patchShape(UiScriptPatchSet patches,
                                  String key,
                                  String prop,
                                  Object value) {
        patches.put(key, prop, value);
    }

    public static void patchShape(UiScriptPatchSet patches,
                                  String key,
                                  String propA,
                                  Object valueA,
                                  String propB,
                                  Object valueB) {
        patches.put(key, propA, valueA, propB, valueB);
    }

    public static String stringProp(UiScriptProps props, String key, String fallback) {
        return props.string(key, fallback);
    }

    public static float numberProp(UiScriptProps props, String key, float fallback) {
        return props.number(key, fallback);
    }

    public static boolean boolProp(UiScriptProps props, String key, boolean fallback) {
        return props.bool(key, fallback);
    }

    public static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public static String scaleHexAlpha(String hex, float amount) {
        return UiScriptColor.alpha(hex, amount);
    }
}
