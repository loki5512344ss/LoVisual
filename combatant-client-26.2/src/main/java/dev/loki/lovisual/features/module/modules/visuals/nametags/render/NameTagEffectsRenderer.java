/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.nametags.render;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.material.EffectIconRenderer;
import dev.loki.lovisual.util.player.effect.tracker.StatusEffectTracker;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTags;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;
import dev.loki.lovisual.features.module.modules.visuals.nametags.render.NameTagRenderer;

public final class NameTagEffectsRenderer {

    private static final int ICON_SIZE = 24;
    private static final int EFFECT_LINE_HEIGHT = 26;
    private static final int EFFECT_LEVEL_COLOR = 0xFFBFC6D1;
    private static final int EFFECT_ROW_GAP = 10;
    private static final int EFFECT_ROW_ICON_GAP = 8;
    private static final int EFFECT_ROW_LEVEL_GAP = 3;
    private static final int EFFECT_ROW_TIME_GAP = 2;
    private static final float EFFECT_ROW_SCALE = 1.15f;
    private static final float SMALL_TEXT_SCALE = 0.82f;
    private static final int EFFECT_LEVEL_STACK_COLOR = 0xFFFF4444;
    private static final int EFFECT_TIME_COLOR = 0xFFCCCCCC;

    private NameTagEffectsRenderer() {
    }

    static int scaleRow(int value) {
        return Math.round(value * EFFECT_ROW_SCALE);
    }

    static int computeEnchantLift(NameTags self, TextRenderer textRenderer, int maxLines) {
        if (!self.toggles.get("Show armor row")) return 0;
        if (maxLines <= 0) return 6;
        int lineHeight = (int) Math.round(textRenderer.getHeight(true) * SMALL_TEXT_SCALE) + 1;
        return 6 + maxLines * lineHeight;
    }

    static void drawRowIcons(GuiGraphicsExtractor ctx, List<MobEffectInstance> effects, int centerX, int itemsY, int lift) {
        if (effects.isEmpty()) return;
        int count = effects.size();
        int iconSize = scaleRow(ICON_SIZE);
        int gap = scaleRow(EFFECT_ROW_ICON_GAP);
        int rowGap = scaleRow(EFFECT_ROW_GAP);
        int rowW = count * iconSize + Math.max(0, count - 1) * gap;
        int startX = centerX - rowW / 2;
        int y = itemsY - iconSize - rowGap - lift;

        for (int i = 0; i < effects.size(); i++) {
            int x = startX + i * (iconSize + gap);
            EffectIconRenderer.draw(ctx, effects.get(i), x, y, iconSize);
        }
    }

    static void drawRowIconOverlays(NameTags self, TextRenderer baseRenderer, TextRenderer levelRenderer,
                                    TextRenderer timeRenderer, List<MobEffectInstance> effects, int entityId,
                                    int centerX, int itemsY, int lift) {
        if (effects.isEmpty()) return;

        TextRenderer levelTr = levelRenderer == null ? baseRenderer : levelRenderer;
        TextRenderer timeTr = timeRenderer == null ? baseRenderer : timeRenderer;

        float rowTextScale = SMALL_TEXT_SCALE * EFFECT_ROW_SCALE;
        boolean startedLevel = NameTagRenderer.beginTextRenderer(levelTr, rowTextScale);
        boolean startedTime = timeTr != levelTr && NameTagRenderer.beginTextRenderer(timeTr, rowTextScale);

        int levelHeight = (int) levelTr.getHeight(true);
        int timeHeight = (int) timeTr.getHeight(true);
        int levelGap = scaleRow(EFFECT_ROW_LEVEL_GAP);
        int timeGap = scaleRow(EFFECT_ROW_TIME_GAP);

        int count = effects.size();
        int iconSize = scaleRow(ICON_SIZE);
        int gap = scaleRow(EFFECT_ROW_ICON_GAP);
        int rowGap = scaleRow(EFFECT_ROW_GAP);
        int rowW = count * iconSize + Math.max(0, count - 1) * gap;
        int startX = centerX - rowW / 2;
        int iconY = itemsY - iconSize - rowGap - lift;

        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance inst = effects.get(i);
            int level = inst.getAmplifier() + 1;
            String dur = formatDuration(entityId, inst);

            int iconX = startX + i * (iconSize + gap);

            if (!dur.isEmpty()) {
                int timeW = (int) timeTr.getWidth(dur, true);
                int timeX = iconX + (iconSize - timeW) / 2;
                int timeY = (level > 1)
                        ? (iconY - levelHeight - levelGap - timeHeight - timeGap)
                        : (iconY - timeHeight - levelGap);
                timeTr.render(dur, timeX, timeY, new RenderColor(EFFECT_TIME_COLOR), true);
            }

            if (level > 1) {
                String lvl = roman(level);
                int levelW = (int) levelTr.getWidth(lvl, true);
                int levelX = iconX + (iconSize - levelW) / 2;
                int levelY = iconY - levelHeight - levelGap;
                levelTr.render(lvl, levelX, levelY, new RenderColor(EFFECT_LEVEL_STACK_COLOR), true);
            }
        }

        NameTagRenderer.endTextRenderer(levelTr, startedLevel);
        NameTagRenderer.endTextRenderer(timeTr, startedTime);
    }

    static String formatDuration(int entityId, MobEffectInstance inst) {
        if (StatusEffectTracker.shouldHideDuration(entityId, inst.getEffect())) return "";
        int ticks = inst.getDuration();
        if (ticks < 0 || ticks >= 32767) return "";
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (seconds < 10) return minutes + ":0" + seconds;
        return minutes + ":" + seconds;
    }

    private static String roman(int n) {
        if (n <= 0) return String.valueOf(n);
        int value = n;
        StringBuilder sb = new StringBuilder();
        int[] vals = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] syms = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        for (int i = 0; i < vals.length; i++) {
            while (value >= vals[i]) {
                value -= vals[i];
                sb.append(syms[i]);
            }
        }
        return sb.toString();
    }
}
