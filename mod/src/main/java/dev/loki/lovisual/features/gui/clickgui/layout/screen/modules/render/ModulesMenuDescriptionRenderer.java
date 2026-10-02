package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.DescriptionLines;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.ModulesMenuStyle;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.List;

/**
 * Hover plaque above the module columns: shows the description of the module row under the
 * cursor and fades out when nothing is hovered.
 */
final class ModulesMenuDescriptionRenderer {
    private static final int MAX_LINES = 2;

    private ModulesMenuDescriptionRenderer() {
    }

    static void render(ModulesMenuScreen screen, float mouseX, float mouseY, float screenAnim, float scale) {
        ModulesMenuPanel.ModuleHit hit = hoveredHit(screen, mouseX, mouseY);
        boolean show = hit != null && !hit.description().isBlank();
        if (show) {
            screen.descTitle = hit.title();
            screen.descText = hit.description();
        }
        screen.descAnim = AnimationUtility.snap(
                AnimationUtility.approach(screen.descAnim, show ? 1.0f : 0.0f, AnimationUtility.deltaTime(), 9.0f),
                show ? 1.0f : 0.0f, 0.01f);
        float alpha = ModulesMenuLayout.easeOutCubic(screenAnim) * ModulesMenuLayout.easeOutCubic(screen.descAnim);
        if (alpha <= 0.001f || screen.descText == null) return;

        ModulesMenuChromeRenderer.ensureFonts();
        TextRenderer titleFont = ModulesMenuChromeRenderer.medium;
        TextRenderer bodyFont = ModulesMenuChromeRenderer.regular;
        float titleSize = 7.4f * scale;
        float bodySize = 6.2f * scale;
        float pad = 6.0f * scale;
        float w = Math.min(screen.areaW * 0.36f, 190.0f * scale);
        List<String> lines = DescriptionLines.wrap(screen.descText, w - pad * 2.0f,
                s -> ClickGuiRenderer.textWidth(bodyFont, s, bodySize), MAX_LINES);
        float titleH = ModulesMenuLayout.textHeight(titleFont, titleSize);
        float lineH = ModulesMenuLayout.textHeight(bodyFont, bodySize) + 1.5f * scale;
        float h = pad * 2.0f + titleH + 2.0f * scale + lines.size() * lineH;
        float x = screen.areaX + (screen.areaW - w) * 0.5f;
        float y = Math.max(screen.areaY + 6.0f * scale, panelsTop(screen) - h - 8.0f * scale) + (1.0f - alpha) * 6.0f * scale;

        float radius = 6.0f * scale;
        ModulesMenuGlassRenderer.drawLiquidGlass(x, y, w, h, radius, false, alpha >= 0.999f ? 1.0f : alpha, scale);
        int veil = ModulesMenuLayout.withAlpha(ModulesMenuStyle.panelBgGlassDark(), alpha * 0.35f);
        LayoutRender2D.roundedQuad(x, y, w, h, radius, veil, veil, veil, veil);
        ClickGuiRenderer.drawText(titleFont, screen.descTitle, x + pad, y + pad, titleSize,
                ModulesMenuLayout.withAlpha(ModulesMenuStyle.text(), alpha), false);
        float ty = y + pad + titleH + 2.0f * scale;
        int bodyColor = ModulesMenuLayout.withAlpha(ModulesMenuStyle.textMuted(), alpha);
        for (String line : lines) {
            ClickGuiRenderer.drawText(bodyFont, line, x + pad, ty, bodySize, bodyColor, false);
            ty += lineH;
        }
    }

    private static ModulesMenuPanel.ModuleHit hoveredHit(ModulesMenuScreen screen, float mx, float my) {
        if (!ModulesMenuLayout.isInteractive(screen.screenAnim, screen.openTarget)) return null;
        for (ModulesMenuPanel panel : screen.panels) {
            if (panel.selected != null || !ModulesMenuLayout.inside(mx, my, panel.x, panel.y, panel.w, panel.h)) continue;
            for (ModulesMenuPanel.ModuleHit hit : panel.hits) {
                if (ModulesMenuLayout.inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) return hit;
            }
        }
        return null;
    }

    private static float panelsTop(ModulesMenuScreen screen) {
        float top = Float.MAX_VALUE;
        for (ModulesMenuPanel panel : screen.panels) top = Math.min(top, panel.y);
        return top == Float.MAX_VALUE ? screen.areaY : top;
    }
}
