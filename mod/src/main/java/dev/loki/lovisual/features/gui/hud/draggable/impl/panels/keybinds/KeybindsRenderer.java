/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Util;

import java.util.LinkedHashMap;
import java.util.List;

final class KeybindsRenderer {

    private KeybindsRenderer() {}

    static void render(Keybinds host, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        boolean forceVisible = DraggableHudElementRegistry.isForceVisible();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || (mc.player == null && !forceVisible)) {
            host.setEmpty();
            return;
        }
        boolean chatPreview = ClientScreen.current() instanceof ChatScreen;
        List<Keybinds.Row> rows = KeybindsData.collectRows(host);
        List<Keybinds.AnimatedRow> animatedRows = KeybindsAnimator.animateRows(host, rows);
        boolean showExampleRow = rows.isEmpty() && (forceVisible || chatPreview);
        boolean showWidget = !rows.isEmpty() || showExampleRow;
        host.visibilityAnim = HudRenderUtil.animateVisibility(host.visibilityAnim, showWidget);
        float widgetScale = HudRenderUtil.visibilityScale(host.visibilityAnim);
        if (animatedRows.isEmpty() && !showExampleRow && host.visibilityAnim <= 0.0f) {
            host.setEmpty();
            return;
        }
        KeybindsPalette.updatePalette(host);
        TextRenderer headerIconRenderer = Fonts.renderer("Icons", FontInfo.Type.Regular, TextRenderer.get());
        TextRenderer headerTextRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, TextRenderer.get());
        TextRenderer rowTextRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, TextRenderer.get());
        if (headerIconRenderer == null) headerIconRenderer = textRenderer;
        if (headerTextRenderer == null) headerTextRenderer = textRenderer;
        if (rowTextRenderer == null) rowTextRenderer = textRenderer;
        float baseScale = HudScale.scale(screenW, screenH) * 1.1f * HudPanelLayoutModes.effectiveScale(host.scaleValue);
        float rowStep = Keybinds.ROW_STEP * baseScale;
        float fontScale = 0.98f * (host.hud.getFontSize() / 18.0f);
        headerIconRenderer.begin(fontScale * Keybinds.HEADER_ICON_SCALE, true, false);
        float headerIconH = (float) headerIconRenderer.getHeight(false);
        headerIconRenderer.end();
        headerTextRenderer.begin(fontScale, true, false);
        float headerTextH = (float) headerTextRenderer.getHeight(false);
        float titleWidth = (float) headerTextRenderer.getWidth("Keybinds", false);
        headerTextRenderer.end();
        rowTextRenderer.begin(fontScale, true, false);
        float rowTextH = (float) rowTextRenderer.getHeight(false);
        float maxWidth = Keybinds.MIN_WIDTH * baseScale;
        if (showExampleRow) {
            float pw = (float) rowTextRenderer.getWidth("KillAuraG", false) + (34.0f * baseScale);
            maxWidth = Math.max(maxWidth, pw);
        } else {
            for (Keybinds.AnimatedRow ar : animatedRows) {
                Keybinds.Row r = ar.row();
                float w = (float) rowTextRenderer.getWidth(r.name() + r.bindText(), false) + (34.0f * baseScale);
                maxWidth = Math.max(maxWidth, w);
            }
        }
        rowTextRenderer.end();
        rowTextRenderer.begin(fontScale * 0.92f, false, false);
        String activeCountText = Integer.toString(rows.size());
        float activeLabelWidth = (float) rowTextRenderer.getWidth("Active:", false);
        float activeCountWidth = (float) rowTextRenderer.getWidth(activeCountText, false);
        rowTextRenderer.end();
        float headerWidth = (Keybinds.TITLE_TEXT_X * baseScale) + titleWidth + activeLabelWidth + activeCountWidth + ((Keybinds.COUNT_LABEL_OFFSET + 12.0f) * baseScale);
        maxWidth = Math.max(maxWidth, headerWidth);
        float contentHeight = showExampleRow ? rowStep : KeybindsAnimator.totalAnimatedHeight(animatedRows, rowStep);
        float targetHeight = (showExampleRow || !animatedRows.isEmpty()) ? (Keybinds.CONTENT_START_Y * baseScale + contentHeight) : (Keybinds.HEADER_HEIGHT * baseScale);
        host.displayWidth = HudRenderUtil.animateDimension(host.displayWidth, maxWidth);
        host.displayHeight = HudRenderUtil.animateDimension(host.displayHeight, targetHeight);
        host.setSize(host.displayWidth, host.displayHeight);
        if (!showWidget && widgetScale <= 0.001f && animatedRows.isEmpty()) {
            host.setEmpty();
            return;
        }
        float drawScale = Math.max(0.0f, widgetScale);
        float drawWidth = host.displayWidth * drawScale;
        float drawHeight = host.displayHeight * drawScale;
        if (drawWidth <= 0.0f || drawHeight <= 0.0f) {
            host.setEmpty();
            return;
        }
        float drawX = host.getX() + ((host.displayWidth - drawWidth) * 0.5f);
        float drawY = host.getY() + ((host.displayHeight - drawHeight) * 0.5f);
        float drawBaseScale = baseScale * drawScale;
        float drawFontScale = fontScale * drawScale;
        int resolvedHeaderIconColor = host.uiCounter;
        if (host.headerIconPulse.get()) {
            int pulsed = HudTextEffects.animatedColor(host.uiCounter, HudTextEffects.Effect.PULSE, host.headerIconPulseSpeed.get(), (float) (Util.getMillis() / 1000.0), 0.0f);
            resolvedHeaderIconColor = HudRenderUtil.mixColor(host.uiCounter, pulsed, host.headerIconPulseIntensity.get() / 100.0f);
        }
        List<LinkedHashMap<String, Object>> panelRows = KeybindsData.buildPanelRows(host, animatedRows, showExampleRow);
        host.scriptedPanel.render(renderer, textRenderer, ctx, tickDelta, new ScriptedListHudPanel.Panel(
                ScriptedListHudPanel.KEYBINDS,
                new ScriptedListHudPanel.Palette(host.uiHeaderLeft, host.uiHeaderRight, host.uiBodyLeft, host.uiBodyRight, host.uiOutline, host.uiText, host.uiMuted, host.uiCounter, host.uiTitleText, host.uiDivider, host.uiBlurTint),
                drawX, drawY, drawWidth, drawHeight, drawScale, drawBaseScale, drawFontScale,
                (float) headerIconH * drawScale, (float) headerTextH * drawScale, (float) rowTextH * drawScale,
                activeLabelWidth * drawScale, activeCountWidth * drawScale, rows.size(),
                KeybindsPalette.hasEffect(host), Math.min(1.0f, (host.blurAlpha.get() / 255.0f) * (KeybindsPalette.isThemeMode(host) ? 1.15f : 1.0f)),
                resolvedHeaderIconColor, HudPanelLayoutModes.current(host.layoutMode), panelRows));
    }
}
