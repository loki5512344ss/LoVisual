package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.tooltip.ClickGuiHintOverlay;
import dev.loki.lovisual.features.gui.clickgui.util.tooltip.SettingTooltip;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import net.minecraft.client.Minecraft;

final class SettingsPanelChrome {
    private final SettingsPanelState state;
    private final SettingsPanelLayout layout;

    SettingsPanelChrome(SettingsPanelState state, SettingsPanelLayout layout) {
        this.state = state;
        this.layout = layout;
    }

    boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    int mixColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((from >>> 24) & 0xFF) * (1 - t) + ((to >>> 24) & 0xFF) * t);
        int r = (int) (((from >>> 16) & 0xFF) * (1 - t) + ((to >>> 16) & 0xFF) * t);
        int g = (int) (((from >>> 8) & 0xFF) * (1 - t) + ((to >>> 8) & 0xFF) * t);
        int b = (int) (((from) & 0xFF) * (1 - t) + ((to) & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    void renderMatteChrome(String header, float scale, int bgTl, int bgTr, int bgBr, int bgBl,
                           int stroke, int line, int text, int muted, SettingsGuiPalette palette) {
        if (state.panelW <= 0.5f || state.panelH <= 0.5f || state.openAnim <= 0.001f) return;
        float radius = layout.dropdownRadius * scale;
        float headerBottom = state.panelY + layout.dropdownHeaderH * scale;
        int shadow = LayoutRender2D.alpha(palette.panelShadow(), 0.72f * state.openAnim);
        int blurTint = LayoutRender2D.alpha(palette.panelBlurTint(), state.openAnim);
        int topWashA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelBgLeft(), palette.menuCategorySelectedLeft(), 0.16f), state.openAnim);
        int topWashB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelBgRight(), palette.menuCategorySelectedRight(), 0.18f), state.openAnim);
        int bodyA = LayoutRender2D.alpha(SettingsGuiPalette.mix(bgTl, palette.menuWindowBgLeft(), 0.18f), state.openAnim);
        int bodyB = LayoutRender2D.alpha(SettingsGuiPalette.mix(bgTr, palette.menuWindowBgRight(), 0.18f), state.openAnim);
        int bodyC = LayoutRender2D.alpha(SettingsGuiPalette.darken(bgBr, 0.06f), state.openAnim);
        int accent = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedRight(), 0.18f), 0.55f * state.openAnim);
        int accentSoft = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillActive(), palette.panelBgRight(), 0.42f), 0.22f * state.openAnim);
        int strokeTop = LayoutRender2D.alpha(SettingsGuiPalette.mix(stroke, palette.moduleDividerEnd(), 0.22f), state.openAnim);
        int strokeBottom = LayoutRender2D.alpha(SettingsGuiPalette.mix(stroke, palette.panelBgRight(), 0.22f), 0.82f * state.openAnim);
        LayoutRender2D.roundedSoftShadow(state.panelX - 1.4f * scale, state.panelY + 1.2f * scale, state.panelW + 2.8f * scale, state.panelH + 2.4f * scale, radius + 1.6f * scale, 9.5f * scale, 0.028f, shadow);
        ClickGuiRenderer.drawBlur(state.panelX, state.panelY, state.panelW, state.panelH, radius, blurTint, (150f / 255f) * state.openAnim);
        LayoutRender2D.roundedQuad(state.panelX, state.panelY, state.panelW, state.panelH, radius, bodyA, bodyB, bodyC, bgBl);
        LayoutRender2D.roundedStrokeQuad(state.panelX, state.panelY, state.panelW, state.panelH, radius, 0.58f * scale, strokeTop, LayoutRender2D.alpha(palette.moduleDividerEnd(), state.openAnim * 0.72f), strokeBottom, LayoutRender2D.alpha(strokeTop, 0.82f));
        LayoutRender2D.roundedQuad(state.panelX + 1.0f * scale, state.panelY + 1.0f * scale, state.panelW - 2.0f * scale, layout.dropdownHeaderH * scale - 1.0f * scale, Math.max(0f, radius - 1.0f * scale), topWashA, topWashB, LayoutRender2D.alpha(SettingsGuiPalette.darken(topWashB, 0.18f), 0.64f), LayoutRender2D.alpha(SettingsGuiPalette.darken(topWashA, 0.14f), 0.64f));
        LayoutRender2D.rectQuad(state.panelX + 8.0f * scale, headerBottom, state.panelW - 16.0f * scale, Math.max(0.45f, 0.55f * scale), LayoutRender2D.alpha(line, 0.55f), line, LayoutRender2D.alpha(line, 0.28f), LayoutRender2D.alpha(line, 0.28f));
        LayoutRender2D.rectQuad(state.panelX + 8.0f * scale, headerBottom + 0.6f * scale, state.panelW - 16.0f * scale, Math.max(0.45f, 0.65f * scale), accent, accentSoft, 0x00000000, 0x00000000);
        float markW = 2.4f * scale;
        float markH = 9.2f * scale;
        LayoutRender2D.roundedQuad(state.panelX + 8.0f * scale, state.panelY + 7.2f * scale, markW, markH, markW * 0.5f, accent, accentSoft, accentSoft, accent);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), header, state.panelX + 14.0f * scale, state.panelY + 7.0f * scale, 8.0f * scale, text, false);
        renderCloseButton(scale, text, muted, palette);
    }

    void renderCloseButton(float scale, int text, int muted, SettingsGuiPalette palette) {
        int bgA = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverLeft(), 0.16f + state.closeHoverAnim * 0.26f);
        int bgB = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverRight(), 0.18f + state.closeHoverAnim * 0.28f);
        int bgC = SettingsGuiPalette.mix(SettingsGuiPalette.darken(palette.panelBgRight(), 0.06f), palette.menuCategoryHoverRight(), 0.12f + state.closeHoverAnim * 0.20f);
        int strokeA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelStroke(), palette.moduleDividerStart(), 0.26f), state.openAnim * (0.72f + state.closeHoverAnim * 0.22f));
        int strokeB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.menuCategorySelectedRight(), 0.22f), state.openAnim * (0.78f + state.closeHoverAnim * 0.20f));
        float radius = state.closeH * 0.50f;
        ClickGuiRenderer.drawBlur(state.closeX, state.closeY, state.closeW, state.closeH, radius, palette.panelBlurTint(), (72f / 255f) * state.openAnim);
        LayoutRender2D.roundedSoftShadow(state.closeX, state.closeY + 0.4f * scale, state.closeW, state.closeH, radius, 4.0f * scale, 0.018f + state.closeHoverAnim * 0.018f, LayoutRender2D.alpha(palette.menuShadow(), state.openAnim * (0.38f + state.closeHoverAnim * 0.36f)));
        LayoutRender2D.roundedQuad(state.closeX, state.closeY, state.closeW, state.closeH, radius, LayoutRender2D.alpha(bgA, state.openAnim), LayoutRender2D.alpha(bgB, state.openAnim), LayoutRender2D.alpha(bgC, state.openAnim), LayoutRender2D.alpha(bgA, state.openAnim));
        LayoutRender2D.roundedStrokeQuad(state.closeX, state.closeY, state.closeW, state.closeH, radius, 0.42f * scale, strokeA, strokeB, LayoutRender2D.alpha(strokeB, 0.74f), LayoutRender2D.alpha(strokeA, 0.78f));
        float icon = 6.8f * scale;
        int iconColor = LayoutRender2D.alpha(SettingsGuiPalette.mix(muted, text, 0.42f + state.closeHoverAnim * 0.48f), state.openAnim);
        Renderer2D.COLOR.svg("x", state.closeX + (state.closeW - icon) * 0.5f, state.closeY + (state.closeH - icon) * 0.5f, icon, icon, SvgRenderOptions.overrideColor(iconColor));
        if (state.closeHoverAnim > 0.08f) SystemCursor.set(SystemCursor.CursorType.HAND);
    }

    void renderHudModePill(float scale, int text, int muted, SettingsGuiPalette palette) {
        layout.updateHudModePillBounds(scale);
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        float segmentW = state.pillW * 0.5f;
        boolean enabledHover = inside(mx, my, state.pillX, state.pillY, segmentW, state.pillH);
        boolean onlyHover = inside(mx, my, state.pillX + segmentW, state.pillY, segmentW, state.pillH);
        int baseA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgLeft(), 0.32f), state.openAnim);
        int baseB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgRight(), 0.40f), state.openAnim);
        int strokeA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelStroke(), palette.moduleDividerStart(), 0.32f), state.openAnim);
        int strokeB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.menuCategorySelectedRight(), 0.20f), state.openAnim);
        ClickGuiRenderer.drawBlur(state.pillX, state.pillY, state.pillW, state.pillH, 5.5f * scale, palette.panelBlurTint(), (105f / 255f) * state.openAnim);
        LayoutRender2D.roundedQuad(state.pillX, state.pillY, state.pillW, state.pillH, 5.5f * scale, baseA, baseB, SettingsGuiPalette.darken(baseB, 0.05f), baseA);
        LayoutRender2D.roundedStrokeQuad(state.pillX, state.pillY, state.pillW, state.pillH, 5.5f * scale, 0.45f * scale, strokeA, strokeB, LayoutRender2D.alpha(strokeB, 0.82f), LayoutRender2D.alpha(strokeA, 0.88f));
        float innerPad = 1.3f * scale;
        float activeW = segmentW - innerPad * 2f;
        float activeH = state.pillH - innerPad * 2f;
        float pillT = AnimationUtility.easeInOutCubic(state.pillActiveAnim);
        float activeX = state.pillX + innerPad + (segmentW * pillT);
        int activeA = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedLeft(), 0.18f), state.openAnim);
        int activeB = LayoutRender2D.alpha(SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedRight(), 0.24f), state.openAnim);
        int activeC = LayoutRender2D.alpha(SettingsGuiPalette.darken(activeB, 0.10f), state.openAnim);
        LayoutRender2D.roundedSoftShadow(activeX, state.pillY + innerPad, activeW, activeH, 4.4f * scale, 4.0f * scale, 0.018f, LayoutRender2D.alpha(palette.menuShadow(), state.openAnim * 0.55f));
        LayoutRender2D.roundedQuad(activeX, state.pillY + innerPad, activeW, activeH, 4.4f * scale, activeA, activeB, activeC, activeA);
        LayoutRender2D.roundedStrokeQuad(activeX, state.pillY + innerPad, activeW, activeH, 4.4f * scale, 0.28f * scale, LayoutRender2D.alpha(palette.moduleDividerStart(), state.openAnim * 0.86f), LayoutRender2D.alpha(palette.moduleDividerEnd(), state.openAnim * 0.92f), LayoutRender2D.alpha(palette.moduleDividerEnd(), state.openAnim * 0.72f), LayoutRender2D.alpha(palette.moduleDividerStart(), state.openAnim * 0.78f));
        float sepX = state.pillX + segmentW;
        LayoutRender2D.roundedQuad(sepX - 0.42f * scale, state.pillY + 4.0f * scale, 0.84f * scale, state.pillH - 8.0f * scale, 0.42f * scale, LayoutRender2D.alpha(palette.moduleDividerStart(), 0.35f * state.openAnim), LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.50f * state.openAnim), LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.42f * state.openAnim), LayoutRender2D.alpha(palette.moduleDividerStart(), 0.28f * state.openAnim));
        float ty = state.pillY + (state.pillH - ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), 6.2f * scale)) * 0.5f;
        int enabledColor = mixColor(mixColor(text, muted, pillT), text, enabledHover ? 0.10f : 0f);
        int onlyColor = mixColor(mixColor(muted, text, pillT), text, onlyHover ? 0.10f : 0f);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), "Enabled", state.pillX + 8.0f * scale, ty, 6.2f * scale, enabledColor, false);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), "Only this", state.pillX + segmentW + 8.0f * scale, ty, 6.2f * scale, onlyColor, false);
    }

    void renderScrollbar(float scale, SettingsGuiPalette palette) {
        if (!state.limitedHeight || state.maxScroll <= 0f) return;
        float trackW = 2.5f * scale;
        float trackX = state.panelX + state.panelW - 5f * scale;
        float trackY = state.contentY + 5f * scale;
        float viewableH = Math.max(1f, state.contentH);
        float trackH = Math.max(8f * scale, viewableH - 10f * scale);
        float rowsH = Math.max(1f, layout.totalRowsHeight());
        float handleH = Math.min(trackH, Math.max(20f * scale, (viewableH / rowsH) * trackH));
        float ratio = state.maxScroll <= 0f ? 0f : (-state.smoothedScroll / state.maxScroll);
        float handleY = trackY + (trackH - handleH) * ratio;
        int trTl = LayoutRender2D.alpha(palette.panelScrollTrackA(), state.openAnim);
        int trTr = LayoutRender2D.alpha(palette.panelScrollTrackB(), state.openAnim);
        int hdTl = LayoutRender2D.alpha(palette.panelScrollHandleA(), state.openAnim);
        int hdTr = LayoutRender2D.alpha(palette.panelScrollHandleB(), state.openAnim);
        LayoutRender2D.roundedQuad(trackX, trackY, trackW, trackH, 1.25f * scale, trTl, trTr, trTr, trTl);
        LayoutRender2D.roundedQuad(trackX, handleY, trackW, handleH, 1.25f * scale, hdTl, hdTr, hdTr, hdTl);
    }

    void renderSettingTooltip(float mx, float my, float panelScale, SettingsGuiPalette palette) {
        for (SettingHit hit : state.hits) {
            if (inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) {
                SettingTooltip.render(hit.setting(), mx, my, hit.y() + hit.h(), panelScale, palette);
                return;
            }
        }
    }

    void renderHudEditorHints(float fallbackX, float fallbackY, float fallbackW, float fallbackH, float scale, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        MainConfig config = MainConfig.get();
        if (!state.hudContext || !config.isClickGuiHudEditorHintsEnabled()) return;
        float areaX = 0f; float areaY = 0f;
        float areaW = Math.max(1f, fallbackX + fallbackW);
        float areaH = Math.max(1f, fallbackY + fallbackH);
        if (mc != null && mc.getWindow() != null) {
            int screenW = mc.getWindow().getWidth();
            int screenH = mc.getWindow().getHeight();
            areaW = Math.max(1f, HudScale.virtualWidth(screenW, screenH));
            areaH = Math.max(1f, HudScale.virtualHeight(screenW, screenH));
        }
        ClickGuiHintOverlay.renderBottomLeft(areaX, areaY, areaW, areaH, scale, alpha,
                ClickGuiI18n.tr("clickgui.hints.hud.alt", "Alt - drag without widget linking"),
                ClickGuiI18n.tr("clickgui.hints.hud.close", "Esc - close editor"),
                ClickGuiI18n.tr("clickgui.hints.hud.hide", "Alt+H - hide hints"));
    }

    void renderSettingsPanelHints(float fallbackX, float fallbackY, float fallbackW, float fallbackH, float scale, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        MainConfig config = MainConfig.get();
        if (state.hudContext || !state.isActive() || !config.isClickGuiHintsEnabled()) return;
        float areaX = 0f; float areaY = 0f;
        float areaW = Math.max(1f, fallbackX + fallbackW);
        float areaH = Math.max(1f, fallbackY + fallbackH);
        if (mc != null && mc.getWindow() != null) {
            int screenW = mc.getWindow().getWidth();
            int screenH = mc.getWindow().getHeight();
            areaW = Math.max(1f, HudScale.virtualWidth(screenW, screenH));
            areaH = Math.max(1f, HudScale.virtualHeight(screenW, screenH));
        }
        ClickGuiHintOverlay.renderBottomLeft(areaX, areaY, areaW, areaH, scale, alpha,
                ClickGuiI18n.tr("clickgui.hints.settings_panel.change", "LMB - change focused setting"),
                ClickGuiI18n.tr("clickgui.hints.settings_panel.drag", "Drag header - move this panel"),
                ClickGuiI18n.tr("clickgui.hints.settings_panel.scroll", "Wheel - scroll settings"),
                ClickGuiI18n.tr("clickgui.hints.settings_panel.back", "Esc - back to category"),
                ClickGuiI18n.tr("clickgui.hints.settings_panel.hide", "Alt+H - hide hints"));
    }
}
