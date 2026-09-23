/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.ModulesMenuStyle;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.ModuleComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.clip.ClipFunction;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.List;

final class ModulesMenuModuleRenderer {
    private ModulesMenuModuleRenderer() {}

    static void renderModulePage(ModulesMenuPanel panel, float mouseX, float mouseY, float alpha, float scale) {
        if (alpha <= 0.001f) return;
        float pageX = panel.x - panel.w * panel.swap;
        float listY = panel.y + (ModulesMenuScreen.HEADER_H + ModulesMenuScreen.SEPARATOR_H - 1.0f) * scale;
        float clipY = panel.y + (ModulesMenuScreen.HEADER_H + ModulesMenuScreen.SEPARATOR_H) * scale;
        float clipH = panel.h - (ModulesMenuScreen.HEADER_H + ModulesMenuScreen.SEPARATOR_H) * scale - 0.5f * scale;
        boolean clipped = ClipFunction.pushRaw(panel.x, clipY, panel.w, clipH);
        ClickGuiRenderer.flushRenderer();
        boolean panelShapeClip = ClipFunction.pushRoundedRect(panel.x, panel.y, panel.w, panel.h, ModulesMenuScreen.PANEL_RADIUS * scale);
        float listW = Math.max(1.0f, panel.w - 7.0f * scale);
        float y = listY - panel.modulesSmoothScroll;
        float total = 0.0f;
        try {
            List<ModuleComponent.CardEntry> entries = ModulesMenuResolver.buildCards(panel.category);
            for (ModuleComponent.CardEntry entry : entries) {
                if (!ClickGuiSearch.matches(entry.title(), entry.searchAliases())) continue;
                if (y + ModulesMenuScreen.MODULE_ROW_H * scale >= clipY && y <= clipY + clipH) {
                    renderModuleRow(panel, entry, pageX, y, listW, mouseX, mouseY, alpha, scale);
                }
                y += ModulesMenuScreen.MODULE_ROW_H * scale;
                total += ModulesMenuScreen.MODULE_ROW_H * scale;
            }
        } finally {
            ClickGuiRenderer.flushRenderer();
            if (panelShapeClip) ClipFunction.pop();
        }
        panel.maxModulesScroll = Math.max(0.0f, total - clipH);
        panel.modulesScroll = AnimationUtility.clamp(panel.modulesScroll, 0.0f, panel.maxModulesScroll);
        panel.modulesSmoothScroll = AnimationUtility.approach(panel.modulesSmoothScroll, panel.modulesScroll, 0.22f);
        panel.modulesSmoothScroll = AnimationUtility.snap(panel.modulesSmoothScroll, panel.modulesScroll, 0.05f);
        if (clipped) ClipFunction.pop();
        ModulesMenuGlassRenderer.renderScrollbar(panel, panel.modulesSmoothScroll, panel.maxModulesScroll, clipY, clipH, alpha, mouseX, mouseY, scale);
    }

    static void renderModuleRow(ModulesMenuPanel panel, ModuleComponent.CardEntry entry,
                                float x, float y, float rowW, float mouseX, float mouseY, float alpha, float scale) {
        float h = ModulesMenuScreen.MODULE_ROW_H * scale;
        boolean hover = ModulesMenuLayout.inside(mouseX, mouseY, x, y, rowW, h);
        float enabled = panel.enabledAnim(entry.getId(), entry.enabled());
        float hoverAnim = panel.hoverAnim(entry.getId(), hover);
        renderModuleRowHover(panel, x, y, rowW, h, mouseX, mouseY, alpha, hoverAnim, scale);
        String label = panel.bindingId != null && panel.bindingId.equals(entry.getId())
                ? ModulesMenuChromeRenderer.bindingLabel(entry) : entry.title();
        boolean moduleListEdit = ModulesMenuLayout.isModuleListEditHeld();
        String moduleListLabel = entry.shownInModuleList() ? "[ON]" : "[OFF]";
        float moduleListTextSize = 7.2f * scale;
        float moduleListTextW = moduleListEdit ? ClickGuiRenderer.textWidth(ModulesMenuChromeRenderer.semiboldForRow(), moduleListLabel, moduleListTextSize) : 0.0f;
        float moduleListW = moduleListEdit ? moduleListTextW + 3.0f * scale : 0.0f;
        float checkReserve = entry.enabled() ? 18.0f * scale : 6.0f * scale;
        float moduleListX = x + rowW - checkReserve - moduleListW - 3.0f * scale;
        float moduleListY = y + ModulesMenuLayout.middle(ModulesMenuLayout.textHeight(ModulesMenuChromeRenderer.semiboldForRow(), moduleListTextSize), h) - 0.5f * scale;
        int nameColor = ModulesMenuLayout.mix(ModulesMenuLayout.withAlpha(ModulesMenuStyle.textMuted(), alpha), ModulesMenuLayout.withAlpha(ModulesMenuStyle.text(), alpha), 0.25f + 0.75f * enabled + 0.20f * hoverAnim);
        float nameX = x + (ModulesMenuScreen.TEXT_LEFT_PADDING + 2.0f * enabled) * scale;
        float nameY = y + ModulesMenuLayout.middle(ModulesMenuLayout.textHeight(ModulesMenuChromeRenderer.regularForRow(), 8.0f * scale), h) - 0.5f * scale;
        float nameMaxW = moduleListEdit ? Math.max(12.0f * scale, moduleListX - nameX - 5.0f * scale)
                : Math.max(12.0f * scale, rowW - (nameX - x) - 18.0f * scale);
        String visibleLabel = ClickGuiRenderer.fitText(ModulesMenuChromeRenderer.regularForRow(), label, 8.0f * scale, nameMaxW);
        ClickGuiRenderer.drawText(ModulesMenuChromeRenderer.regularForRow(), visibleLabel, nameX, nameY, 8.0f * scale, nameColor, false);
        if (moduleListEdit) {
            int listColor = entry.shownInModuleList() ? ModulesMenuStyle.MODULE_LIST_ON : ModulesMenuStyle.MODULE_LIST_OFF;
            ClickGuiRenderer.drawText(ModulesMenuChromeRenderer.semiboldForRow(), moduleListLabel, moduleListX, moduleListY, moduleListTextSize, ModulesMenuLayout.withAlpha(listColor, alpha), false);
        }
        int divider = ModulesMenuLayout.withAlpha(ModulesMenuStyle.text(), alpha * 0.02f);
        LayoutRender2D.rect(x + 3.0f * scale, y + h, Math.max(1f, rowW - 6.0f * scale), 0.5f * scale, divider);
        if (enabled > 0.001f) {
            float check = 6.0f * scale;
            float checkX = x + rowW - 15.0f * scale - 2.0f * scale * enabled;
            float checkY = y + 7.0f * scale;
            Renderer2D.COLOR.svg("check", checkX, checkY, check, check,
                    SvgRenderOptions.overrideColor(ModulesMenuLayout.withAlpha(ModulesMenuStyle.text(), alpha * (0.10f + 0.90f * enabled))));
        }
        panel.hits.add(new ModulesMenuPanel.ModuleHit(entry.getId(), x, y, rowW, h, entry.hasSettings(), entry.toggleable()));
    }

    static void renderModuleRowHover(ModulesMenuPanel panel, float x, float y, float w, float h,
                                     float mouseX, float mouseY, float alpha, float hoverAnim, float scale) {
        if (hoverAnim <= 0.001f || alpha <= 0.001f) return;
        float rx = x + 3.0f * scale;
        float ry = y + 2.0f * scale;
        float rw = w - 6.0f * scale;
        float rh = h - 4.0f * scale;
        if (rw <= 0.5f || rh <= 0.5f) return;
        float radius = 5.0f * scale;
        int hoverBg = ModulesMenuLayout.withAlpha(ModulesMenuStyle.rowHover(), alpha * hoverAnim);
        int glow = ModulesMenuStyle.rowHoverGlow(alpha * hoverAnim);
        float cx = AnimationUtility.clamp(mouseX, rx, rx + rw);
        float cy = AnimationUtility.clamp(mouseY, ry, ry + rh);
        float glowRadius = Math.max(rw, rh) * 0.82f;
        boolean ownsPanelClip = false;
        boolean panelClip = ClipFunction.isShapeClipActive();
        if (!panelClip) {
            panelClip = ClipFunction.pushRoundedRect(panel.x, panel.y, panel.w, panel.h, ModulesMenuScreen.PANEL_RADIUS * scale);
            ownsPanelClip = panelClip;
        }
        if (!panelClip) {
            DebugLog.warnOnChange("modules.hover.parent.clip.failed", panel.category.title(),
                    "ModulesMenuScreen: hover clipped draw skipped because parent rounded glass clip failed. panel=%s panelBounds=[%.2f, %.2f, %.2f, %.2f]",
                    panel.category.title(), panel.x, panel.y, panel.w, panel.h);
            return;
        }
        try {
            if (((glow >>> 24) & 0xFF) > 0) {
                Renderer2D.COLOR.radialGlowMasked(rx, ry, rw, rh, radius, 0.0f, glowRadius, cx, cy, glow);
            }
            if (((hoverBg >>> 24) & 0xFF) > 0) {
                LayoutRender2D.roundedQuad(rx, ry, rw, rh, radius, hoverBg, hoverBg, hoverBg, hoverBg);
            }
        } finally {
            Renderer2D.flushBatch(Renderer2D.FlushReason.SCISSOR);
            if (ownsPanelClip) ClipFunction.pop();
        }
    }
}
