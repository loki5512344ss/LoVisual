package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;

final class MenuScreenLayout {

    void layout(MenuScreenState state, float areaX, float areaY, float areaW, float areaH) {
        state.menuW = state.menuWConst * state.s;
        state.menuH = state.menuHConst * state.s;
        state.menuX = areaX + (areaW - state.menuW) * 0.5f;
        state.menuY = areaY + (areaH - state.menuH) * 0.5f;
        state.shellX = state.menuX - 20f * state.s;
        state.shellY = state.menuY;
        state.shellW = state.menuW + 40f * state.s;
        state.shellH = state.menuH;
        state.categoryX = state.menuX - 20f * state.s;
        state.categoryY = state.menuY + 40f * state.s;
        state.searchX = state.menuX + 330f * state.s;
        state.searchY = state.menuY + 7.5f * state.s;
    }

    RenderWarpStack.Scope pushLifecycleWarp(MenuScreenState state, float progress) {
        if (progress >= 0.999f) {
            return Renderer2D.pushWarp(null);
        }
        return Renderer2D.pushPerspectiveWarp(
                state.shellX,
                state.shellY,
                state.shellW,
                state.shellH,
                0.0f,
                0.0f,
                0.0f,
                3.6f,
                0.80f,
                0.90f + 0.10f * progress
        );
    }
}
