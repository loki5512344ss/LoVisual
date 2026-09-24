package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderContext;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderSurface;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;

final class SettingsPanelLayout {
    final float dropdownPanelW = 115.0f;
    final float dropdownPanelH = 240.0f;
    final float dropdownHeaderH = 24.0f;
    final float dropdownSeparatorH = 4.0f;
    final float dropdownRadius = 10.0f;

    private final SettingsPanelState state;

    SettingsPanelLayout(SettingsPanelState state) {
        this.state = state;
    }

    void buildRows(float menuScale) {
        state.rows.clear();
        for (Setting setting : state.settings) {
            float rowScale = settingScale(setting, menuScale);
            try (SettingRenderContext.Scope ignored = SettingRenderContext.push(state.renderSurface, rowScale)) {
                float anim = setting.updateVisibilityAnim();
                boolean targetVisible = setting.isVisibilityTargetVisible();
                if (!targetVisible && anim <= 0.01f) continue;
                float h = setting.getHeight() * anim;
                state.rows.add(new SettingRow(setting, anim, h, 0f, rowScale));
            }
        }
    }

    float totalRowsHeight() {
        float out = 0f;
        for (SettingRow row : state.rows) out += row.height() + row.gap();
        return out;
    }

    float panelScale(float menuScale) {
        float base = Math.max(0.25f, menuScale);
        if (state.renderSurface != SettingRenderSurface.MODULES) return base;
        return AnimationUtility.clamp(base * 1.5f, 2.55f, 3.0f);
    }

    float settingScale(float menuScale) {
        return Math.max(0.25f, menuScale);
    }

    float settingScale(Setting setting, float menuScale) {
        return settingScale(menuScale);
    }

    RenderWarpStack.Scope pushLifecycleWarp(float progress) {
        if (progress >= 0.999f || state.dragging) return Renderer2D.pushWarp(null);
        return Renderer2D.pushPerspectiveWarp(
                state.panelX, state.panelY, state.panelW, state.panelH,
                0.0f, 0.0f, 0.0f, 3.6f, 0.80f, 0.92f + 0.08f * progress);
    }

    void updateHudModePillBounds(float scale) {
        state.pillX = state.panelX + 7f * scale;
        state.pillY = state.panelY + dropdownHeaderH * scale + 4f * scale;
        state.pillW = state.panelW - 14f * scale;
        state.pillH = 16f * scale;
    }
}
