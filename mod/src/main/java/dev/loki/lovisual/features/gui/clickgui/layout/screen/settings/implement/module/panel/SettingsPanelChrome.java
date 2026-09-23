package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.util.tooltip.SettingTooltip;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

/**
 * Отрисовка панели настроек: матовое хром-окно, кнопка-крестик,
 * HUD-пилюля режимов, скроллбар, тултип строки. Чистый draw — состояние
 * (геометрия/анимации) читает из {@link SettingsPanelComponent}.
 */
final class SettingsPanelChrome {
    private static final float DROPDOWN_RADIUS = 10.0f;

    private SettingsPanelChrome() {}

    static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    static int mixColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >>> 24) & 0xFF) * (1 - t) + ((to >>> 24) & 0xFF) * t);
        int r = (int) (((from >>> 16) & 0xFF) * (1 - t) + ((to >>> 16) & 0xFF) * t);
        int g = (int) (((from >>> 8) & 0xFF) * (1 - t) + ((to >>> 8) & 0xFF) * t);
        int b = (int) (((from) & 0xFF) * (1 - t) + ((to) & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static void renderSettingTooltip(SettingsPanelComponent p, float mx, float my, float panelScale) {
        for (SettingsPanelRows.SettingHit hit : p.rows.hits()) {
            if (inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) {
                SettingTooltip.render(hit.setting(), mx, my, hit.y() + hit.h(), panelScale, SettingsGuiPalette.current());
                return;
            }
        }
    }

    static void renderMatteChrome(SettingsPanelComponent p, String header, float scale, int bgTl, int bgTr,
                                  int bgBr, int bgBl, int stroke, int line, int text, int muted) {
        if (p.panelW <= 0.5f || p.panelH <= 0.5f || p.openAnim <= 0.001f) return;
        SettingsGuiPalette palette = SettingsGuiPalette.current();

        float radius = DROPDOWN_RADIUS * scale;
        float headerBottom = p.panelY + SettingsPanelComponent.DROPDOWN_HEADER_H * scale;
        int shadow = LayoutRender2D.alpha(palette.panelShadow(), 0.72f * p.openAnim);
        int blurTint = LayoutRender2D.alpha(palette.panelBlurTint(), p.openAnim);
        int topWashA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelBgLeft(), palette.menuCategorySelectedLeft(), 0.16f), p.openAnim);
        int topWashB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelBgRight(), palette.menuCategorySelectedRight(), 0.18f), p.openAnim);
        int bodyA = LayoutRender2D.alpha(SettingsGuiPalette.mix(bgTl, palette.menuWindowBgLeft(), 0.18f), p.openAnim);
        int bodyB = LayoutRender2D.alpha(SettingsGuiPalette.mix(bgTr, palette.menuWindowBgRight(), 0.18f), p.openAnim);
        int bodyC = LayoutRender2D.alpha(SettingsGuiPalette.darken(bgBr, 0.06f), p.openAnim);
        int accent = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedRight(), 0.18f), 0.55f * p.openAnim);
        int accentSoft = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillActive(), palette.panelBgRight(), 0.42f), 0.22f * p.openAnim);
        int strokeTop = LayoutRender2D.alpha(SettingsGuiPalette.mix(stroke, palette.moduleDividerEnd(), 0.22f), p.openAnim);
        int strokeBottom = LayoutRender2D.alpha(SettingsGuiPalette.mix(stroke, palette.panelBgRight(), 0.22f), 0.82f * p.openAnim);

        LayoutRender2D.roundedSoftShadow(p.panelX - 1.4f * scale, p.panelY + 1.2f * scale, p.panelW + 2.8f * scale, p.panelH + 2.4f * scale, radius + 1.6f * scale, 9.5f * scale, 0.028f, shadow);
        ClickGuiRenderer.drawBlur(p.panelX, p.panelY, p.panelW, p.panelH, radius, blurTint, (150f / 255f) * p.openAnim);
        LayoutRender2D.roundedQuad(p.panelX, p.panelY, p.panelW, p.panelH, radius, bodyA, bodyB, bodyC, bgBl);
        LayoutRender2D.roundedStrokeQuad(p.panelX, p.panelY, p.panelW, p.panelH, radius, 0.58f * scale, strokeTop, LayoutRender2D.alpha(palette.moduleDividerEnd(), p.openAnim * 0.72f), strokeBottom, LayoutRender2D.alpha(strokeTop, 0.82f));

        LayoutRender2D.roundedQuad(p.panelX + 1.0f * scale, p.panelY + 1.0f * scale, p.panelW - 2.0f * scale, SettingsPanelComponent.DROPDOWN_HEADER_H * scale - 1.0f * scale, Math.max(0f, radius - 1.0f * scale), topWashA, topWashB, LayoutRender2D.alpha(SettingsGuiPalette.darken(topWashB, 0.18f), 0.64f), LayoutRender2D.alpha(SettingsGuiPalette.darken(topWashA, 0.14f), 0.64f));
        LayoutRender2D.rectQuad(p.panelX + 8.0f * scale, headerBottom, p.panelW - 16.0f * scale, Math.max(0.45f, 0.55f * scale), LayoutRender2D.alpha(line, 0.55f), line, LayoutRender2D.alpha(line, 0.28f), LayoutRender2D.alpha(line, 0.28f));
        LayoutRender2D.rectQuad(p.panelX + 8.0f * scale, headerBottom + 0.6f * scale, p.panelW - 16.0f * scale, Math.max(0.45f, 0.65f * scale), accent, accentSoft, 0x00000000, 0x00000000);

        float markW = 2.4f * scale;
        float markH = 9.2f * scale;
        LayoutRender2D.roundedQuad(p.panelX + 8.0f * scale, p.panelY + 7.2f * scale, markW, markH, markW * 0.5f, accent, accentSoft, accentSoft, accent);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), header, p.panelX + 14.0f * scale, p.panelY + 7.0f * scale, 8.0f * scale, text, false);

        renderCloseButton(p, scale, text, muted, palette);
    }

    private static void renderCloseButton(SettingsPanelComponent p, float scale, int text, int muted, SettingsGuiPalette palette) {
        int bgA = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverLeft(), 0.16f + p.closeHoverAnim * 0.26f);
        int bgB = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverRight(), 0.18f + p.closeHoverAnim * 0.28f);
        int bgC = SettingsGuiPalette.mix(SettingsGuiPalette.darken(palette.panelBgRight(), 0.06f), palette.menuCategoryHoverRight(), 0.12f + p.closeHoverAnim * 0.20f);
        int strokeA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelStroke(), palette.moduleDividerStart(), 0.26f), p.openAnim * (0.72f + p.closeHoverAnim * 0.22f));
        int strokeB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.menuCategorySelectedRight(), 0.22f), p.openAnim * (0.78f + p.closeHoverAnim * 0.20f));
        float radius = p.closeH * 0.50f;

        ClickGuiRenderer.drawBlur(p.closeX, p.closeY, p.closeW, p.closeH, radius, palette.panelBlurTint(), (72f / 255f) * p.openAnim);
        LayoutRender2D.roundedSoftShadow(p.closeX, p.closeY + 0.4f * scale, p.closeW, p.closeH, radius, 4.0f * scale, 0.018f + p.closeHoverAnim * 0.018f, LayoutRender2D.alpha(palette.menuShadow(), p.openAnim * (0.38f + p.closeHoverAnim * 0.36f)));
        LayoutRender2D.roundedQuad(p.closeX, p.closeY, p.closeW, p.closeH, radius, LayoutRender2D.alpha(bgA, p.openAnim), LayoutRender2D.alpha(bgB, p.openAnim), LayoutRender2D.alpha(bgC, p.openAnim), LayoutRender2D.alpha(bgA, p.openAnim));
        LayoutRender2D.roundedStrokeQuad(p.closeX, p.closeY, p.closeW, p.closeH, radius, 0.42f * scale, strokeA, strokeB, LayoutRender2D.alpha(strokeB, 0.74f), LayoutRender2D.alpha(strokeA, 0.78f));

        float icon = 6.8f * scale;
        int iconColor = LayoutRender2D.alpha(SettingsGuiPalette.mix(muted, text, 0.42f + p.closeHoverAnim * 0.48f), p.openAnim);
        Renderer2D.COLOR.svg("x", p.closeX + (p.closeW - icon) * 0.5f, p.closeY + (p.closeH - icon) * 0.5f, icon, icon, SvgRenderOptions.overrideColor(iconColor));
        if (p.closeHoverAnim > 0.08f) SystemCursor.set(SystemCursor.CursorType.HAND);
    }

    static void updateHudModePillBounds(SettingsPanelComponent p, float scale) {
        p.pillX = p.panelX + 7f * scale;
        p.pillY = p.panelY + SettingsPanelComponent.DROPDOWN_HEADER_H * scale + 4f * scale;
        p.pillW = p.panelW - 14f * scale;
        p.pillH = 16f * scale;
    }

    static void renderHudModePill(SettingsPanelComponent p, float scale, int text, int muted, SettingsGuiPalette palette) {
        updateHudModePillBounds(p, scale);

        float pillActiveAnim = p.hudPreview.activeAnim();
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        float segmentW = p.pillW * 0.5f;
        boolean enabledHover = inside(mx, my, p.pillX, p.pillY, segmentW, p.pillH);
        boolean onlyHover = inside(mx, my, p.pillX + segmentW, p.pillY, segmentW, p.pillH);
        int baseA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgLeft(), 0.32f), p.openAnim);
        int baseB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgRight(), 0.40f), p.openAnim);
        int strokeA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelStroke(), palette.moduleDividerStart(), 0.32f), p.openAnim);
        int strokeB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.menuCategorySelectedRight(), 0.20f), p.openAnim);

        ClickGuiRenderer.drawBlur(p.pillX, p.pillY, p.pillW, p.pillH, 5.5f * scale, palette.panelBlurTint(), (105f / 255f) * p.openAnim);
        LayoutRender2D.roundedQuad(p.pillX, p.pillY, p.pillW, p.pillH, 5.5f * scale, baseA, baseB, SettingsGuiPalette.darken(baseB, 0.05f), baseA);
        LayoutRender2D.roundedStrokeQuad(p.pillX, p.pillY, p.pillW, p.pillH, 5.5f * scale, 0.45f * scale, strokeA, strokeB, LayoutRender2D.alpha(strokeB, 0.82f), LayoutRender2D.alpha(strokeA, 0.88f));

        float innerPad = 1.3f * scale;
        float activeW = segmentW - innerPad * 2f;
        float activeH = p.pillH - innerPad * 2f;
        float pillT = AnimationUtility.easeInOutCubic(pillActiveAnim);
        float activeX = p.pillX + innerPad + (segmentW * pillT);
        int activeA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedLeft(), 0.18f), p.openAnim);
        int activeB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedRight(), 0.24f), p.openAnim);
        int activeC = LayoutRender2D.alpha(SettingsGuiPalette.darken(activeB, 0.10f), p.openAnim);
        LayoutRender2D.roundedSoftShadow(activeX, p.pillY + innerPad, activeW, activeH, 4.4f * scale, 4.0f * scale, 0.018f, LayoutRender2D.alpha(palette.menuShadow(), p.openAnim * 0.55f));
        LayoutRender2D.roundedQuad(activeX, p.pillY + innerPad, activeW, activeH, 4.4f * scale, activeA, activeB, activeC, activeA);
        LayoutRender2D.roundedStrokeQuad(activeX, p.pillY + innerPad, activeW, activeH, 4.4f * scale, 0.28f * scale, LayoutRender2D.alpha(palette.moduleDividerStart(), p.openAnim * 0.86f), LayoutRender2D.alpha(palette.moduleDividerEnd(), p.openAnim * 0.92f), LayoutRender2D.alpha(palette.moduleDividerEnd(), p.openAnim * 0.72f), LayoutRender2D.alpha(palette.moduleDividerStart(), p.openAnim * 0.78f));

        float sepX = p.pillX + segmentW;
        LayoutRender2D.roundedQuad(sepX - 0.42f * scale, p.pillY + 4.0f * scale, 0.84f * scale, p.pillH - 8.0f * scale, 0.42f * scale,
                LayoutRender2D.alpha(palette.moduleDividerStart(), 0.35f * p.openAnim),
                LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.50f * p.openAnim),
                LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.42f * p.openAnim),
                LayoutRender2D.alpha(palette.moduleDividerStart(), 0.28f * p.openAnim));

        float ty = p.pillY + (p.pillH - ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), 6.2f * scale)) * 0.5f;
        int enabledColor = mixColor(mixColor(text, muted, pillT), text, enabledHover ? 0.10f : 0f);
        int onlyColor = mixColor(mixColor(muted, text, pillT), text, onlyHover ? 0.10f : 0f);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), "Enabled", p.pillX + 8.0f * scale, ty, 6.2f * scale, enabledColor, false);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), "Only this", p.pillX + segmentW + 8.0f * scale, ty, 6.2f * scale, onlyColor, false);
    }

    static void renderScrollbar(SettingsPanelComponent p, float scale, SettingsGuiPalette palette) {
        if (!p.limitedHeight || p.maxScroll <= 0f) return;

        float trackW = 2.5f * scale;
        float trackX = p.panelX + p.panelW - 5f * scale;
        float trackY = p.contentY + 5f * scale;
        float viewableH = Math.max(1f, p.contentH);
        float trackH = Math.max(8f * scale, viewableH - 10f * scale);

        float rowsH = Math.max(1f, p.rows.totalHeight());
        float handleH = Math.min(trackH, Math.max(20f * scale, (viewableH / rowsH) * trackH));
        float ratio = p.maxScroll <= 0f ? 0f : (-p.smoothedScroll / p.maxScroll);
        float handleY = trackY + (trackH - handleH) * ratio;

        int trTl = LayoutRender2D.alpha(palette.panelScrollTrackA(), p.openAnim);
        int trTr = LayoutRender2D.alpha(palette.panelScrollTrackB(), p.openAnim);
        int hdTl = LayoutRender2D.alpha(palette.panelScrollHandleA(), p.openAnim);
        int hdTr = LayoutRender2D.alpha(palette.panelScrollHandleB(), p.openAnim);

        LayoutRender2D.roundedQuad(trackX, trackY, trackW, trackH, 1.25f * scale, trTl, trTr, trTr, trTl);
        LayoutRender2D.roundedQuad(trackX, handleY, trackW, handleH, 1.25f * scale, hdTl, hdTr, hdTr, hdTl);
    }
}
