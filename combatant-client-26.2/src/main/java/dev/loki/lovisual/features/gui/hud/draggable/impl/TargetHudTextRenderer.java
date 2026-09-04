/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl;

import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import net.minecraft.util.Util;

import static dev.loki.lovisual.features.gui.hud.draggable.impl.TargetHud.*;

final class TargetHudTextRenderer {

    private TargetHudTextRenderer() {
    }

    static float measureText(TextRenderer renderer, String text, float scale) {
        renderer.begin(scale, false, false);
        try {
            return (float) renderer.getWidth(text, false);
        } finally {
            renderer.end();
        }
    }

    static float getTextHeight(TextRenderer renderer, float scale) {
        renderer.begin(scale, false, false);
        try {
            return (float) renderer.getHeight(false);
        } finally {
            renderer.end();
        }
    }

    static String trimText(TextRenderer renderer, String text, float maxWidth, float scale) {
        if (text == null || text.isEmpty()) return "";
        renderer.begin(scale, false, false);
        try {
            if (renderer.getWidth(text, false) <= maxWidth) return text;
            String value = text;
            while (!value.isEmpty()) {
                String candidate = value + "...";
                if (renderer.getWidth(candidate, false) <= maxWidth) {
                    return candidate;
                }
                value = value.substring(0, value.length() - 1);
            }
            return "";
        } finally {
            renderer.end();
        }
    }

    static void drawScrollingText(TextRenderer renderer,
                                  String text,
                                  float x,
                                  float y,
                                  float fullWidth,
                                  float viewWidth,
                                  float scale,
                                  float baseScale,
                                  float alpha,
                                  int color) {
        float speed = BASE_NAME_SCROLL_SPEED * baseScale;
        float gap = BASE_NAME_SCROLL_GAP * baseScale;
        if (speed <= 0.0f) {
            return;
        }

        float cycleDistance = fullWidth + gap;
        float cycleTime = NAME_SCROLL_PAUSE_SEC + (cycleDistance / speed);
        if (cycleTime <= 0.0f) {
            return;
        }

        float now = Util.getMillis() / 1000.0f;
        float t = now % cycleTime;
        float offset = 0.0f;
        if (t > NAME_SCROLL_PAUSE_SEC) {
            offset = -(t - NAME_SCROLL_PAUSE_SEC) * speed;
        }

        RenderColor renderColor = new RenderColor(HudRenderUtil.scaleAlpha(color, alpha));
        float fade = Math.min(viewWidth * 0.25f, BASE_NAME_FADE * baseScale);
        renderer.begin(scale, false, false);
        try {
            renderer.renderHorizontalFadeClipped(text, x + offset, y, renderColor, x, x + viewWidth, fade, fade, false);
            if (cycleDistance > viewWidth * 0.5f) {
                renderer.renderHorizontalFadeClipped(text, x + offset + cycleDistance, y, renderColor,
                        x, x + viewWidth, fade, fade, false);
            }
        } finally {
            renderer.end();
        }
    }
}
