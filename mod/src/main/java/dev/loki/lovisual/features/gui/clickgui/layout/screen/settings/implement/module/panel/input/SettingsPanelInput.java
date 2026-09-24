package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderContext;
import org.lwjgl.glfw.GLFW;

final class SettingsPanelInput {
    private final SettingsPanelState state;
    private final SettingsPanelLayout layout;

    SettingsPanelInput(SettingsPanelState state, SettingsPanelLayout layout) {
        this.state = state;
        this.layout = layout;
    }

    boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    boolean isHintToggle(int keyCode, int modifiers) {
        return keyCode == GLFW.GLFW_KEY_H && (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

    boolean mouseClicked(float mx, float my, int button, float scale) {
        if (!state.isActive()) return false;
        if (state.openAnim <= 0.01f) return false;
        float panelScale = layout.panelScale(scale);
        if (button == 0 && inside(mx, my, state.closeX, state.closeY, state.closeW, state.closeH)) {
            state.close();
            return true;
        }
        if (!inside(mx, my, state.panelX, state.panelY, state.panelW, state.panelH)) {
            return state.hudContext || state.editorContext;
        }
        if (button == 0 && inside(mx, my, state.panelX, state.panelY, state.panelW, layout.dropdownHeaderH * panelScale)) {
            state.dragging = true;
            state.dragOffsetX = state.panelX - mx;
            state.dragOffsetY = state.panelY - my;
            return true;
        }
        if (state.hudContext && button == 0 && inside(mx, my, state.pillX, state.pillY, state.pillW, state.pillH)) {
            float half = state.pillW * 0.5f;
            state.hudPreviewMode = (mx < state.pillX + half) ? HudPreviewMode.ALL_ENABLED : HudPreviewMode.ONLY_CURRENT;
            state.applyHudPreviewMode();
            return true;
        }
        if (inside(mx, my, state.contentX, state.contentY, state.contentW, state.contentH)) {
            SettingHit target = null;
            for (SettingHit hit : state.hits) {
                if (inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) {
                    target = hit;
                    break;
                }
            }
            try (SettingRenderContext.Scope ignored = SettingRenderContext.push(state.renderSurface, target == null ? layout.settingScale(panelScale) : target.scale())) {
                for (Setting setting : state.settings) {
                    if (target != null && target.setting() == setting) continue;
                    setting.mouseClickedOutside(mx, my, button);
                }
                if (target != null) {
                    if (!target.setting().isAvailable()) return true;
                    target.setting().mouseClicked(mx, my, button);
                    return true;
                }
            }
        } else {
            try (SettingRenderContext.Scope ignored = SettingRenderContext.push(state.renderSurface, layout.settingScale(panelScale))) {
                for (Setting setting : state.settings) setting.mouseClickedOutside(mx, my, button);
            }
        }
        return true;
    }

    boolean mouseReleased(float mx, float my, int button) {
        if (!state.isActive()) return false;
        if (button == 0) state.dragging = false;
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(state.renderSurface, state.lastSettingScale)) {
            for (Setting setting : state.settings) {
                if (!setting.isAvailable()) continue;
                setting.mouseReleased(mx, my, button);
            }
        }
        return inside(mx, my, state.panelX, state.panelY, state.panelW, state.panelH);
    }

    boolean mouseScrolled(float mx, float my, double amount) {
        if (!state.isActive()) return false;
        if (!state.limitedHeight) return inside(mx, my, state.panelX, state.panelY, state.panelW, state.panelH);
        if (!inside(mx, my, state.panelX, state.panelY, state.panelW, state.panelH)) return state.hudContext || state.editorContext;
        for (SettingHit hit : state.hits) {
            if (!inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
            if (!hit.setting().isAvailable()) return true;
            try (SettingRenderContext.Scope ignored = SettingRenderContext.push(state.renderSurface, hit.scale())) {
                if (hit.setting().mouseScrolled(mx, my, amount)) return true;
            }
            break;
        }
        state.scroll += (float) (amount * 20f * Math.max(0.25f, state.lastSettingScale));
        return true;
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!state.isActive()) return false;
        if (isHintToggle(keyCode, modifiers)) {
            MainConfig config = MainConfig.get();
            if (state.hudContext) config.setClickGuiHudEditorHintsEnabled(!config.isClickGuiHudEditorHintsEnabled());
            else config.setClickGuiHintsEnabled(!config.isClickGuiHintsEnabled());
            return true;
        }
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(state.renderSurface, state.lastSettingScale)) {
            for (Setting setting : state.settings) {
                if (!setting.isAvailable()) continue;
                if (setting.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            state.close();
            return true;
        }
        return false;
    }

    boolean charTyped(char chr, int modifiers) {
        if (!state.isActive()) return false;
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(state.renderSurface, state.lastSettingScale)) {
            for (Setting setting : state.settings) {
                if (!setting.isAvailable()) continue;
                if (setting.charTyped(chr, modifiers)) return true;
            }
        }
        return false;
    }
}
