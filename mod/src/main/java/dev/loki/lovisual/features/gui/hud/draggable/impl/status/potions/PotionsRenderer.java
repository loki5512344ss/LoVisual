package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class PotionsRenderer {

    private PotionsRenderer() {}

    static void render(Potions host, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        boolean forceVisible = dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry.isForceVisible();
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || (mc.player == null && !forceVisible)) {
            host.setEmpty();
            return;
        }
        boolean chatPreview = dev.loki.lovisual.util.screen.ClientScreen.current() instanceof net.minecraft.client.gui.screens.ChatScreen;
        List<Potions.Row> rows = PotionsData.collectRows();
        List<Potions.AnimatedRow> animatedRows = PotionsAnimator.animateRows(host, rows);
        boolean showExampleRow = rows.isEmpty() && (forceVisible || chatPreview);
        boolean showWidget = !rows.isEmpty() || showExampleRow;
        host.visibilityAnim = HudRenderUtil.animateVisibility(host.visibilityAnim, showWidget);
        float widgetScale = HudRenderUtil.visibilityScale(host.visibilityAnim);
        if (animatedRows.isEmpty() && !showExampleRow && host.visibilityAnim <= 0.0f) {
            host.setEmpty();
            return;
        }
        PotionsPalette.updatePalette(host);
        TextRenderer headerIconRenderer = Fonts.renderer("IconsNur", FontInfo.Type.Regular, TextRenderer.get());
        TextRenderer headerTextRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, TextRenderer.get());
        TextRenderer rowTextRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, TextRenderer.get());
        if (headerTextRenderer == null) headerTextRenderer = textRenderer;
        if (rowTextRenderer == null) rowTextRenderer = textRenderer;
        if (headerIconRenderer == null) headerIconRenderer = textRenderer;
        float baseScale = HudScale.scale(screenW, screenH) * 1.1f * HudPanelLayoutModes.effectiveScale(host.scaleValue);
        float rowStep = Potions.ROW_STEP * baseScale;
        float fontScale = 0.98f * (host.hud.getFontSize() / 18.0f);
        headerIconRenderer.begin(fontScale, true, false);
        float headerIconH = (float) headerIconRenderer.getHeight(false);
        headerIconRenderer.end();
        headerTextRenderer.begin(fontScale, true, false);
        float headerTextH = (float) headerTextRenderer.getHeight(false);
        float titleWidth = (float) headerTextRenderer.getWidth("Potions", false);
        headerTextRenderer.end();
        rowTextRenderer.begin(fontScale, true, false);
        float rowTextH = (float) rowTextRenderer.getHeight(false);
        float maxWidth = Potions.MIN_WIDTH * baseScale;
        if (showExampleRow) {
            float previewWidth = (float) rowTextRenderer.getWidth("Example effect**:**", false) + (30.0f * baseScale);
            maxWidth = Math.max(maxWidth, previewWidth);
        } else {
            for (Potions.AnimatedRow animatedRow : animatedRows) {
                Potions.Row row = animatedRow.row();
                String label = row.name() + (row.amp().isEmpty() ? "" : " " + row.amp());
                float widthCandidate = (float) rowTextRenderer.getWidth(label + row.duration(), false) + (30.0f * baseScale);
                maxWidth = Math.max(maxWidth, widthCandidate);
            }
        }
        rowTextRenderer.end();
        rowTextRenderer.begin(fontScale * 0.92f, false, false);
        String activeCountText = Integer.toString(rows.size());
        float activeLabelWidth = (float) rowTextRenderer.getWidth("Active:", false);
        float activeCountWidth = (float) rowTextRenderer.getWidth(activeCountText, false);
        rowTextRenderer.end();
        float headerWidth = (Potions.TITLE_TEXT_X * baseScale) + titleWidth + activeLabelWidth + activeCountWidth + ((Potions.COUNT_LABEL_OFFSET + 12.0f) * baseScale);
        maxWidth = Math.max(maxWidth, headerWidth);
        float contentHeight = showExampleRow ? rowStep : PotionsAnimator.totalAnimatedHeight(animatedRows, rowStep);
        float targetHeight = (showExampleRow || !animatedRows.isEmpty()) ? (Potions.CONTENT_START_Y * baseScale + contentHeight) : (Potions.HEADER_HEIGHT * baseScale);
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
        float drawHeaderIconH = headerIconH * drawScale;
        float drawHeaderTextH = headerTextH * drawScale;
        float drawRowTextH = rowTextH * drawScale;
        int resolvedHeaderIconColor = host.uiCounter;
        if (host.headerIconPulse.get()) {
            int pulsedHeaderIconColor = HudTextEffects.animatedColor(host.uiCounter, HudTextEffects.Effect.PULSE, host.headerIconPulseSpeed.get(), (float) (Util.getMillis() / 1000.0), 0.0f);
            resolvedHeaderIconColor = HudRenderUtil.mixColor(host.uiCounter, pulsedHeaderIconColor, host.headerIconPulseIntensity.get() / 100.0f);
        }
        List<LinkedHashMap<String, Object>> panelRows = ScriptedListHudPanel.rows();
        rowTextRenderer.begin(drawFontScale, false, false);
        if (showExampleRow) {
            Identifier previewIcon = Potions.PREVIEW_ICONS[(int) ((System.currentTimeMillis() / 1000L) % Potions.PREVIEW_ICONS.length)];
            List<LinkedHashMap<String, Object>> parts = List.of(ScriptedListHudPanel.textPart("Example effect", PotionsPalette.withAlpha(host.uiText & 0x00FFFFFF, 255), 0.0f));
            LinkedHashMap<String, Object> row = ScriptedListHudPanel.row("preview", "texture", ScriptedListHudPanel.idString(previewIcon), parts, "**:**", PotionsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, 255), PotionsPalette.withAlpha(host.uiMuted & 0x00FFFFFF, 130), 1.0f);
            row.put("iconTint", ScriptedListHudPanel.hex(PotionsPalette.withAlpha(0x00FFFFFF, 255)));
            panelRows.add(row);
        } else {
            for (Potions.AnimatedRow animatedRow : animatedRows) {
                float anim = animatedRow.anim();
                if (anim <= 0.01f) continue;
                Potions.Row rowData = animatedRow.row();
                int rowBaseAlpha = PotionsAnimator.clamp255(Math.round(255.0f * anim));
                int blinkAlpha = PotionsAnimator.getBlinkAlpha(rowData, rowBaseAlpha, 100);
                boolean bad = PotionsAnimator.isBadEffect(rowData.effect());
                int nameColor = PotionsPalette.withAlpha(bad ? Potions.NEGATIVE_EFFECT_RGB : (host.uiText & 0x00FFFFFF), blinkAlpha);
                int dividerColor = PotionsPalette.withAlpha((bad ? Potions.NEGATIVE_EFFECT_RGB : (host.uiMuted & 0x00FFFFFF)), Math.max(24, blinkAlpha - 125));
                List<LinkedHashMap<String, Object>> parts = new ArrayList<>(2);
                parts.add(ScriptedListHudPanel.textPart(rowData.name(), nameColor, 0.0f));
                if (!rowData.amp().isEmpty()) {
                    float labelWidth = (float) rowTextRenderer.getWidth(rowData.name(), false);
                    parts.add(ScriptedListHudPanel.textPart(" " + rowData.amp(), PotionsPalette.withAlpha(host.uiAmpText & 0x00FFFFFF, blinkAlpha), labelWidth));
                }
                LinkedHashMap<String, Object> row = ScriptedListHudPanel.row(rowData.key(), "texture", ScriptedListHudPanel.idString(rowData.iconId()), parts, rowData.duration(), PotionsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, blinkAlpha), dividerColor, anim);
                row.put("iconTint", ScriptedListHudPanel.hex(PotionsPalette.withAlpha(0x00FFFFFF, blinkAlpha)));
                panelRows.add(row);
            }
        }
        rowTextRenderer.end();
        host.scriptedPanel.render(renderer, textRenderer, ctx, tickDelta, new ScriptedListHudPanel.Panel(ScriptedListHudPanel.POTIONS, new ScriptedListHudPanel.Palette(host.uiHeaderLeft, host.uiHeaderRight, host.uiBodyLeft, host.uiBodyRight, host.uiOutline, host.uiText, host.uiMuted, host.uiCounter, host.uiTitleText, host.uiDivider, host.uiBlurTint), drawX, drawY, drawWidth, drawHeight, drawScale, drawBaseScale, drawFontScale, drawHeaderIconH, drawHeaderTextH, drawRowTextH, activeLabelWidth * drawScale, activeCountWidth * drawScale, rows.size(), PotionsPalette.hasEffect(host), Math.min(1.0f, (host.blurAlpha.get() / 255.0f) * (PotionsPalette.isThemeMode(host) ? 1.15f : 1.0f)), resolvedHeaderIconColor, HudPanelLayoutModes.current(host.layoutMode), panelRows));
    }

    static void renderExampleRow(Potions host, Renderer2D renderer, TextRenderer rowTextRenderer, float fontScale, float rowTextH, float baseScale, float renderWidth, float centerY, float renderX) {
        int textColor = PotionsPalette.withAlpha(host.uiText & 0x00FFFFFF, 255);
        int timeColor = PotionsPalette.withAlpha(host.uiCounter & 0x00FFFFFF, 255);
        int dividerColor = PotionsPalette.withAlpha(host.uiMuted & 0x00FFFFFF, 130);
        float iconSize = Potions.ROW_ICON_SIZE * baseScale;
        float rowContentCenterY = centerY + (Potions.ROW_CONTENT_CENTER_OFFSET * baseScale);
        Identifier previewIcon = Potions.PREVIEW_ICONS[(int) ((System.currentTimeMillis() / 1000L) % Potions.PREVIEW_ICONS.length)];
        Renderer2D.TEXTURE.roundedTexRect(renderX + (Potions.ROW_ICON_X * baseScale), rowContentCenterY - (iconSize * 0.5f), iconSize, iconSize, Math.min(1.15f * baseScale, iconSize * 0.18f), 1.0f, PotionsPalette.withAlpha(0x00FFFFFF, 255), previewIcon);
        renderer.quad(renderX + (Potions.ROW_DIVIDER_X * baseScale), rowContentCenterY - ((Potions.ROW_DIVIDER_H * baseScale) * 0.5f), Math.max(0.5f, 0.5f * baseScale), Potions.ROW_DIVIDER_H * baseScale, dividerColor);
        rowTextRenderer.begin(fontScale, false, false);
        float textY = rowContentCenterY - (rowTextH * 0.5f);
        rowTextRenderer.render("Example effect", renderX + (Potions.ROW_TEXT_X * baseScale), textY, new RenderColor(textColor), false);
        float durationWidth = (float) rowTextRenderer.getWidth("**:**", false);
        rowTextRenderer.render("**:**", renderX + renderWidth - durationWidth - (Potions.ROW_TIME_RIGHT * baseScale), textY, new RenderColor(timeColor), false);
        rowTextRenderer.end();
    }
}
