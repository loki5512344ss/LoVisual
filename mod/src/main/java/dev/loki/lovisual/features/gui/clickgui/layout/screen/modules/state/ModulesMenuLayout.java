package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.ModulesMenuCategory;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

final class ModulesMenuLayout {
    private ModulesMenuLayout() {}

    static float easeOutCubic(float t) {
        t = AnimationUtility.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    static float middle(float child, float parent) { return (parent - child) * 0.5f; }

    static float textHeight(TextRenderer renderer, float size) { return ClickGuiRenderer.textHeight(renderer, size); }

    static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    static int withAlpha(int color, float alpha) {
        float a = AnimationUtility.clamp(alpha, 0.0f, 1.0f);
        int ca = (color >>> 24) & 0xFF;
        int na = Math.round(ca * a);
        return (color & 0x00FFFFFF) | ((na & 0xFF) << 24);
    }

    static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static int mix(int from, int to, float t) {
        t = AnimationUtility.clamp(t, 0.0f, 1.0f);
        int a = Math.round(((from >>> 24) & 0xFF) * (1.0f - t) + ((to >>> 24) & 0xFF) * t);
        int r = Math.round(((from >>> 16) & 0xFF) * (1.0f - t) + ((to >>> 16) & 0xFF) * t);
        int g = Math.round(((from >>> 8) & 0xFF) * (1.0f - t) + ((to >>> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1.0f - t) + (to & 0xFF) * t);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    static boolean isModuleListEditHeld() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null || ClickGuiSearch.isActive()) return false;
        return GLFW.glfwGetKey(client.getWindow().handle(), GLFW.GLFW_KEY_E) == GLFW.GLFW_PRESS;
    }

    static float panelProgress(float screenAnim, int panelCount, int index) {
        if (panelCount == 0) return screenAnim;
        float stagger = 0.055f;
        float span = Math.max(0.20f, 1.0f - stagger * Math.max(0, panelCount - 1));
        return AnimationUtility.clamp((screenAnim - index * stagger) / span, 0.0f, 1.0f);
    }

    static float panelAlpha(float anim) { return easeOutCubic(anim); }

    static boolean isInteractive(float screenAnim, boolean openTarget) {
        return openTarget && screenAnim >= ModulesMenuScreen.INPUT_READY_PROGRESS;
    }

    static RenderWarpStack.Scope pushPanelDropWarp(ModulesMenuPanel panel) {
        float t = easeOutCubic(panel.anim);
        if (t >= 0.999f) return Renderer2D.pushWarp(null);
        float inv = 1.0f - t;
        return Renderer2D.pushPerspectiveWarp(panel.x, panel.y, panel.w, panel.h, 0.0f, -9.0f * inv, 0.0f, 3.4f, 0.82f, 0.94f + 0.06f * t);
    }

    static void resetTransientState(List<ModulesMenuPanel> panels) {
        for (ModulesMenuPanel p : panels) {
            p.selected = null; p.selectedTitle = null; p.selectedSettings.clear();
            p.settingsScroll = 0.0f; p.settingsSmoothScroll = 0.0f;
            p.modulesScroll = 0.0f; p.modulesSmoothScroll = 0.0f; p.swap = 0.0f;
            p.hits.clear(); p.settingHits.clear();
        }
    }

    static float computePortScale(float areaW, float areaH) {
        float designW = ModulesMenuScreen.PANEL_W * ModulesMenuCategory.values().length
                + ModulesMenuScreen.PANEL_GAP * (ModulesMenuCategory.values().length - 1);
        float sideMargin = Math.max(28.0f, areaW * 0.03f);
        float verticalReserve = Math.max(56.0f, areaH * 0.10f);
        float fitX = Math.max(1.0f, (areaW - sideMargin * 2.0f) / designW);
        float fitY = Math.max(1.0f, (areaH - verticalReserve) / (ModulesMenuScreen.PANEL_H + 30.0f));
        float fit = Math.min(fitX, fitY);
        return AnimationUtility.clamp(Math.min(ModulesMenuScreen.LIQUID_GLASS_LOGICAL_SCALE, fit), 2.55f, ModulesMenuScreen.LIQUID_GLASS_LOGICAL_SCALE);
    }

    static void layout(ModulesMenuScreen screen, List<ModulesMenuPanel> panels, float areaX, float areaY, float areaW, float areaH) {
        float scale = computePortScale(areaW, areaH);
        screen.scale = scale;
        ModulesMenuScreen.ACTIVE_PORT_SCALE = scale;
        float pw = ModulesMenuScreen.PANEL_W * scale;
        float ph = ModulesMenuScreen.PANEL_H * scale;
        float gap = ModulesMenuScreen.PANEL_GAP * scale;
        float total = (pw + gap) * panels.size() - gap;
        float outerPadX = 18.0f * scale;
        float outerPadTop = 6.0f * scale;
        float outerPadBottom = 26.0f * scale;
        float layoutW = Math.max(total, areaW - outerPadX * 2.0f);
        float startX = areaX + (layoutW - total) * 0.5f + (areaW - layoutW) * 0.5f;
        startX = Math.max(areaX + outerPadX, startX);
        float py = areaY + outerPadTop + Math.max(0.0f, (areaH - outerPadTop - outerPadBottom - ph) * 0.5f);
        for (int i = 0; i < panels.size(); i++) {
            ModulesMenuPanel panel = panels.get(i);
            float targetX = startX + i * (pw + gap);
            float panelT = panelProgress(screen.screenAnim, panels.size(), i);
            float eased = easeOutCubic(panelT);
            float drop = -(38.0f + i * 3.5f) * scale * (1.0f - eased);
            float settle = (float) Math.sin(panelT * Math.PI) * 4.5f * scale;
            panel.x = targetX; panel.y = py + drop + settle; panel.w = pw; panel.h = ph; panel.anim = panelT;
        }
        screen.searchW = 100.0f * scale; screen.searchH = 20.0f * scale;
        screen.searchX = areaX + areaW * 0.5f - screen.searchW * 0.5f;
        float panelBottom = py + ph;
        float visibleSearchY = panelBottom + 6.0f * scale;
        float maxVisibleSearchY = areaY + areaH - screen.searchH - 8.0f * scale;
        if (visibleSearchY > maxVisibleSearchY) visibleSearchY = maxVisibleSearchY;
        float hiddenSearchY = visibleSearchY + 12.0f * scale;
        screen.searchY = AnimationUtility.lerp(hiddenSearchY, visibleSearchY, screen.searchAnim);
        screen.searchBtnW = 22.0f * scale; screen.searchBtnH = 22.0f * scale;
        screen.searchBtnX = areaX + areaW - screen.searchBtnW - 14.0f * scale;
        screen.searchBtnY = areaY + 12.0f * scale;
    }
}
