package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderContext;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

final class SettingsPanelRenderer {
    private final SettingsPanelState state;
    private final SettingsPanelLayout layout;
    private final SettingsPanelChrome chrome;

    SettingsPanelRenderer(SettingsPanelState state, SettingsPanelLayout layout, SettingsPanelChrome chrome) {
        this.state = state;
        this.layout = layout;
        this.chrome = chrome;
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

    void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        float dt = AnimationUtility.deltaTime();
        float openTargetValue = state.openTarget ? 1f : 0f;
        state.openAnim = AnimationUtility.approach(state.openAnim, openTargetValue, dt, state.openTarget ? 12f : 13f);
        state.openAnim = AnimationUtility.snap(state.openAnim, openTargetValue, 0.01f);
        float pillTarget = state.hudPreviewMode == HudPreviewMode.ALL_ENABLED ? 0f : 1f;
        state.pillActiveAnim = AnimationUtility.approach(state.pillActiveAnim, pillTarget, dt, 14f);
        state.pillActiveAnim = AnimationUtility.snap(state.pillActiveAnim, pillTarget, 0.002f);
        if (!state.openTarget && state.openAnim <= 0.001f) {
            state.openAnim = 0f;
            if (state.hudContext) {
                state.resetHudPreview();
                state.hudContext = false;
            }
            state.editorContext = false;
            return;
        }
        float panelScale = layout.panelScale(scale);
        state.lastSettingScale = layout.settingScale(panelScale);
        layout.buildRows(panelScale);
        float baseW = layout.dropdownPanelW * panelScale;
        float baseX = menuX + menuW + 24f * panelScale;
        float baseY = menuY + 4f * panelScale;
        if (state.manualPos) {
            baseX = state.manualX;
            baseY = state.manualY;
        }
        state.hudExtraTop = state.hudContext ? 20f * panelScale : 0f;
        float targetH = AnimationUtility.clamp(
                layout.totalRowsHeight() + (layout.dropdownHeaderH + layout.dropdownSeparatorH) * panelScale + 5f * panelScale + state.hudExtraTop,
                0f, layout.dropdownPanelH * panelScale);
        state.panelHeight = AnimationUtility.approach(state.panelHeight, targetH, dt, 14f);
        state.panelHeight = AnimationUtility.snap(state.panelHeight, targetH, 0.25f);
        float distanceProgress = AnimationUtility.easeInOutCubic(state.openAnim);
        float fadeProgress = AnimationUtility.easeInOutCubic(state.openAnim);
        if (state.dragging) {
            state.manualPos = true;
            state.manualX = mx + state.dragOffsetX;
            state.manualY = my + state.dragOffsetY;
            state.panelX = state.manualX;
            state.panelY = state.manualY;
        } else {
            state.panelX = baseX;
            state.panelY = baseY;
        }
        state.panelW = baseW;
        state.panelH = state.panelHeight;
        state.limitedHeight = state.panelH >= (layout.dropdownPanelH * panelScale - 0.25f * panelScale);
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        try (RenderWarpStack.Scope lifecycleScope = layout.pushLifecycleWarp(distanceProgress)) {
            int bgTl = LayoutRender2D.alpha(palette.panelBgLeft(), fadeProgress);
            int bgTr = LayoutRender2D.alpha(palette.panelBgRight(), fadeProgress);
            int bgBr = LayoutRender2D.alpha(SettingsGuiPalette.darken(palette.panelBgRight(), 0.08f), fadeProgress);
            int bgBl = LayoutRender2D.alpha(SettingsGuiPalette.darken(palette.panelBgLeft(), 0.05f), fadeProgress);
            int stroke = LayoutRender2D.alpha(palette.panelStroke(), fadeProgress);
            int line = LayoutRender2D.alpha(palette.panelDivider(), 0.52f * fadeProgress);
            int text = LayoutRender2D.alpha(palette.panelText(), fadeProgress);
            int mutedBase = LayoutRender2D.alpha(palette.panelMuted(), fadeProgress);
            String header = state.title;
            state.closeW = 16f * panelScale;
            state.closeH = 14f * panelScale;
            state.closeX = state.panelX + state.panelW - state.closeW - 6f * panelScale;
            state.closeY = state.panelY + (layout.dropdownHeaderH * panelScale - state.closeH) * 0.5f;
            boolean closeHover = inside(mx, my, state.closeX, state.closeY, state.closeW, state.closeH);
            state.closeHoverAnim = AnimationUtility.approach(state.closeHoverAnim, closeHover ? 1f : 0f, dt, 10f);
            int muted = mixColor(mutedBase, text, state.closeHoverAnim * 0.7f);
            chrome.renderMatteChrome(header, panelScale, bgTl, bgTr, bgBr, bgBl, stroke, line, text, muted, palette);
            float contentStartY = state.panelY + (layout.dropdownHeaderH + layout.dropdownSeparatorH) * panelScale;
            if (state.hudContext) {
                chrome.renderHudModePill(panelScale, text, muted, palette);
                contentStartY += state.hudExtraTop;
            }
            state.contentPadLeft = 6f * panelScale;
            state.contentPadRight = 13f * panelScale;
            float contentClipInset = 2.2f * panelScale;
            state.contentX = state.panelX + state.contentPadLeft;
            state.contentY = contentStartY + contentClipInset;
            state.contentW = Math.max(1f, state.panelW - state.contentPadLeft - state.contentPadRight);
            state.contentH = Math.max(0f, state.panelY + state.panelH - state.contentY - 7.0f * panelScale);
            state.maxScroll = Math.max(0f, layout.totalRowsHeight() - state.contentH);
            state.scroll = AnimationUtility.clamp(state.scroll, -state.maxScroll, 0f);
            state.smoothedScroll = AnimationUtility.approach(state.smoothedScroll, state.scroll, dt, 14f);
            state.smoothedScroll = AnimationUtility.snap(state.smoothedScroll, state.scroll, 0.1f);
            state.hits.clear();
            float rowsReveal = fadeProgress;
            boolean clipped = ScissorFunction.pushRaw(state.contentX, state.contentY, state.contentW, state.contentH);
            float y = state.contentY + state.smoothedScroll;
            for (SettingRow row : state.rows) {
                float sh = row.height() * rowsReveal;
                float gap = row.gap() * rowsReveal;
                if (sh > 0.5f) {
                    state.hits.add(new SettingHit(row.setting(), state.contentX, y, state.contentW, sh, row.scale()));
                    if (y + sh >= state.contentY - 1.0f * panelScale && y <= state.contentY + state.contentH + 1.0f * panelScale) {
                        float slide = (1f - row.anim()) * 6f * panelScale;
                        boolean itemClip = ScissorFunction.pushRaw(state.contentX, y, state.contentW, sh);
                        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(state.renderSurface, row.scale())) {
                            row.setting().render(state.contentX, y + slide, state.contentW, mx, my);
                        }
                        ClickGuiRenderer.flushRenderer();
                        if (itemClip) ScissorFunction.pop();
                    }
                }
                y += sh + gap;
            }
            ClickGuiRenderer.flushRenderer();
            if (clipped) ScissorFunction.pop();
            chrome.renderScrollbar(panelScale, palette);
        }
        chrome.renderSettingTooltip(mx, my, panelScale, palette);
        chrome.renderHudEditorHints(menuX, menuY, menuW, menuH, panelScale, fadeProgress);
        chrome.renderSettingsPanelHints(menuX, menuY, menuW, menuH, panelScale, fadeProgress);
    }
}
