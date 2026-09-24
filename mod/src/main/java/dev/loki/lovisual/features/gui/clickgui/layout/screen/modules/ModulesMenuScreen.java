package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.ModulesMenuCategory;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.ModulesMenuStyle;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModulesMenuScreen {
    static final float INPUT_READY_PROGRESS = 0.72f;
    public static final float PANEL_W = 115.0f;
    public static final float PANEL_H = 240.0f;
    public static final float PANEL_GAP = 14.0f;
    public static final float HEADER_H = 24.0f;
    public static final float SEPARATOR_H = 4.0f;
    public static final float MODULE_ROW_H = 20.0f;
    public static final float PANEL_RADIUS = 10.0f;
    public static final float TEXT_LEFT_PADDING = 10.0f;
    public static final float LIQUID_GLASS_LOGICAL_SCALE = 3.0f;
    public static float ACTIVE_PORT_SCALE = LIQUID_GLASS_LOGICAL_SCALE;

    final List<ModulesMenuPanel> panels = new ArrayList<>();
    float areaX; float areaY; float areaW; float areaH;
    float scale = 1.0f; float screenAnim = 0.0f; boolean openTarget = false;
    float searchAnim = 0.0f; float searchX; float searchY; float searchW; float searchH;
    float searchBtnX; float searchBtnY; float searchBtnW; float searchBtnH; float searchBtnHoverAnim;

    public ModulesMenuScreen() {
        for (ModulesMenuCategory c : ModulesMenuCategory.values()) panels.add(new ModulesMenuPanel(c, c.ordinal()));
    }

    public static float computePortScale() { return ACTIVE_PORT_SCALE; }
    static boolean isModuleListEditHeld() { return ModulesMenuLayout.isModuleListEditHeld(); }
    static boolean inside(float mx, float my, float x, float y, float w, float h) { return ModulesMenuLayout.inside(mx, my, x, y, w, h); }

    public void open() { openTarget = true; }
    public void close() {
        openTarget = false; ClickGuiSearch.deactivate();
        if (!ClickGuiRenderer.isClosingForExit()) { screenAnim = 0.0f; ModulesMenuLayout.resetTransientState(panels); }
    }
    public boolean isVisible() { return openTarget || screenAnim > 0.001f; }

    public void layout(float x, float y, float w, float h) {
        this.areaX = x; this.areaY = y; this.areaW = Math.max(1.0f, w); this.areaH = Math.max(1.0f, h);
        ModulesMenuLayout.layout(this, panels, areaX, areaY, areaW, areaH);
    }

    public void render(float mouseX, float mouseY) {
        ModulesMenuChromeRenderer.ensureFonts();
        ModulesMenuStyle.syncTheme();
        float dt = AnimationUtility.deltaTime();
        float target = openTarget ? 1.0f : 0.0f;
        screenAnim = AnimationUtility.approach(screenAnim, target, dt, openTarget ? 4.8f : 7.5f);
        screenAnim = AnimationUtility.snap(screenAnim, target, 0.01f);
        if (screenAnim <= 0.001f && !openTarget) { ModulesMenuLayout.resetTransientState(panels); return; }
        searchAnim = AnimationUtility.approach(searchAnim, ClickGuiSearch.isActive() ? 1.0f : 0.0f, dt, 10.0f);
        searchAnim = AnimationUtility.snap(searchAnim, ClickGuiSearch.isActive() ? 1.0f : 0.0f, 0.01f);
        layout(areaX, areaY, areaW, areaH);
        for (ModulesMenuPanel p : panels) {
            if (p.isDraggingScrollbar()) p.updateScrollbarDrag(mouseX, mouseY);
            float a = ModulesMenuLayout.panelAlpha(p.anim);
            if (a <= 0.001f) continue;
            try (RenderWarpStack.Scope ignored = ModulesMenuLayout.pushPanelDropWarp(p)) {
                ModulesMenuGlassRenderer.renderPanelGlass(p, a, scale);
            }
        }
        for (ModulesMenuPanel p : panels) {
            float a = ModulesMenuLayout.panelAlpha(p.anim);
            if (a <= 0.001f) continue;
            try (RenderWarpStack.Scope ignored = ModulesMenuLayout.pushPanelDropWarp(p)) {
                ModulesMenuChromeRenderer.renderPanel(p, mouseX, mouseY, a, scale);
            }
        }
        ModulesMenuChromeRenderer.renderSearchButton(this, mouseX, mouseY, screenAnim, scale);
        ModulesMenuChromeRenderer.renderSearch(this, screenAnim, scale);
        ModulesMenuChromeRenderer.renderHints(this, screenAnim, scale);
        ClickGuiRenderer.flushRenderer();
    }

    public void renderGlassPass(float alphaFactor) {
        if (screenAnim <= 0.001f) return;
        float passAlpha = AnimationUtility.clamp(alphaFactor, 0.0f, 1.0f);
        for (ModulesMenuPanel p : panels) {
            float a = ModulesMenuLayout.panelAlpha(p.anim) * passAlpha;
            if (a <= 0.001f) continue;
            try (RenderWarpStack.Scope ignored = ModulesMenuLayout.pushPanelDropWarp(p)) {
                ModulesMenuGlassRenderer.renderPanelGlass(p, a, scale);
            }
        }
    }

    public boolean mousePressed(float mx, float my, int btn) {
        if (!ModulesMenuLayout.isInteractive(screenAnim, openTarget)) return false;
        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && inside(mx, my, searchBtnX, searchBtnY, searchBtnW, searchBtnH)) { ClickGuiSearch.setActive(!ClickGuiSearch.isActive()); return true; }
        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && inside(mx, my, searchX, searchY, searchW, searchH)) { ClickGuiSearch.setActive(true); return true; }
        for (ModulesMenuPanel p : panels) if (inside(mx, my, p.x, p.y, p.w, p.h)) return p.mousePressed(mx, my, btn);
        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && ClickGuiSearch.isActive()) { ClickGuiSearch.setActive(false); return true; }
        return false;
    }

    public void mouseReleased(float mx, float my, int btn) {
        if (!ModulesMenuLayout.isInteractive(screenAnim, openTarget)) return;
        for (ModulesMenuPanel p : panels) p.mouseReleased(mx, my, btn);
    }

    public boolean mouseScrolled(float mx, float my, double amt) {
        if (!ModulesMenuLayout.isInteractive(screenAnim, openTarget)) return false;
        for (ModulesMenuPanel p : panels) if (inside(mx, my, p.x, p.y, p.w, p.h)) { p.scroll(mx, my, amt); return true; }
        return false;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (!ModulesMenuLayout.isInteractive(screenAnim, openTarget)) return false;
        if (key == GLFW.GLFW_KEY_H && (mods & GLFW.GLFW_MOD_ALT) != 0) { MainConfig c = MainConfig.get(); c.setClickGuiHintsEnabled(!c.isClickGuiHintsEnabled()); return true; }
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && key == GLFW.GLFW_KEY_F) { ClickGuiSearch.setActive(!ClickGuiSearch.isActive()); return true; }
        if (ClickGuiSearch.isActive()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { ClickGuiSearch.setActive(false); return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE) { ClickGuiSearch.backspace(); return true; }
        }
        for (ModulesMenuPanel p : panels) if (p.keyPressed(key, scan, mods)) return true;
        return false;
    }

    public boolean charTyped(char chr, int mods) {
        if (!ModulesMenuLayout.isInteractive(screenAnim, openTarget)) return false;
        if (ClickGuiSearch.isActive()) { ClickGuiSearch.append(chr); return true; }
        for (ModulesMenuPanel p : panels) if (p.charTyped(chr, mods)) return true;
        return false;
    }
}
