package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other.StatusRender;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

import java.util.List;

final class ModuleCardRenderer {
    private static final float PARALLAX_MAX_ANGLE = 4.6f;
    private static final float PARALLAX_DEPTH = 4.8f;
    private static final float PARALLAX_PERSPECTIVE = 0.92f;
    private static final float PARALLAX_SCALE_BOOST = 0.012f;
    private static final float PARALLAX_CONTENT_SHIFT = 0.0f;

    private final ModuleCardState state;
    private final ModuleCardLayout layout;

    ModuleCardRenderer(ModuleCardState state, ModuleCardLayout layout) {
        this.state = state;
        this.layout = layout;
    }

    void render(float menuX, float menuY, float menuW, float menuH,
                List<ModuleComponent.CardEntry> entries, float scale) {
        state.hits.clear();
        if (entries.isEmpty()) return;

        int[] offsets = layout.calculateOffsets(entries, scale);
        int column = 0;
        int maxScroll = 0;
        float columnWidth = 142f * scale;
        SettingsGuiPalette palette = SettingsGuiPalette.current();

        float scissorX = menuX + 35f * scale - 75f * scale;
        float scissorY = menuY + 35.5f * scale;
        float scissorW = menuW - 35f * scale + 150f * scale;
        float scissorBottom = menuY + menuH - 3.5f * scale;
        float scissorH = Math.max(1f, scissorBottom - scissorY);
        boolean clipped = ScissorFunction.pushRaw(scissorX, scissorY, scissorW, scissorH);

        for (int i = entries.size() - 1; i >= 0; i--) {
            ModuleComponent.CardEntry entry = entries.get(i);
            int componentHeight = Math.round(layout.getComponentHeight(entry, columnWidth + 40f * scale, scale) + 9f * scale);
            float x = menuX + 32f * scale + (column * (columnWidth + 48f * scale));
            float y = menuY + 35f * scale + offsets[column] - componentHeight + state.smoothedScroll;
            float w = columnWidth + 40f * scale;
            float h = componentHeight - 9f * scale;
            if (y + h > scissorY + 1.0f * scale && scissorBottom - 1.0f * scale > y) {
                CardHit hit = drawCard(entry, x, y, w, h, scale, palette);
                state.hits.add(hit);
            }
            offsets[column] -= componentHeight;
            maxScroll = Math.max(maxScroll, offsets[column]);
            column = (column + 1) % 2;
        }

        int clamped = ClickGuiMath.clamp(Math.round(maxScroll - (menuH * 0.5f + 35f * scale)), 0, maxScroll);
        layout.updateScrollbarMetrics(menuX, menuY, menuH, clamped, scale);
        if (state.draggingScrollbar) {
            layout.scrollToMouse(ClickGuiRenderer.getMouseY());
        }
        state.scroll = ClickGuiMath.clamp(state.scroll, -clamped, 0f);
        state.smoothedScroll = AnimationUtility.approach(state.smoothedScroll, state.scroll,
                state.draggingScrollbar ? 0.55f : 0.2f);
        state.smoothedScroll = AnimationUtility.snap(state.smoothedScroll, state.scroll,
                state.draggingScrollbar ? 0.01f : 0.05f);
        if (clipped) ScissorFunction.pop();
        renderScrollbar(menuX, menuY, menuH, clamped, scale, palette);
    }

    private CardHit drawCard(ModuleComponent.CardEntry entry, float x, float y, float w, float h,
                             float scale, SettingsGuiPalette palette) {
        float enabledAnim = state.stateAnimById.compute(entry.getId(),
                (k, v) -> AnimationUtility.approach(v == null ? 0f : v, entry.enabled() ? 1f : 0f, 0.16f));
        boolean hovered = ClickGuiMath.insideRect(ClickGuiRenderer.getMouseX(), ClickGuiRenderer.getMouseY(), x, y, w, h);
        float hoverAnim = state.hoverAnimById.compute(entry.getId(),
                (k, v) -> AnimationUtility.approach(v == null ? 0f : v, hovered ? 1f : 0f, 0.18f));
        Parallax parallax = computeParallax(ClickGuiRenderer.getMouseX(), ClickGuiRenderer.getMouseY(), x, y, w, h, hoverAnim);
        int brightnessOffset = Math.round(9f * enabledAnim);
        int alphaOffset = 150 + Math.round(105f * enabledAnim);
        String description = normalizeDescription(entry.description());
        float descHeight = layout.descriptionHeight(description, w, scale);
        int cardTopStrong = brightenRgb(palette.moduleCardTopStrong(), brightnessOffset);
        int cardBottomStrong = brightenRgb(palette.moduleCardBottomStrong(), brightnessOffset);
        BindRect bindRect;
        float statusX = x + w - 24f * scale;
        float statusY = y + descHeight + 31f * scale;

        ClickGuiRenderer.drawRoundedRectShadow(x, y + 1.5f * scale, w, h, 5.4f * scale, 9.0f * scale, 1.0f * scale,
                LayoutRender2D.alpha(0xFF000000, 0.22f + 0.10f * hoverAnim));
        try (var ignored = pushParallax(parallax, x, y, w, h)) {
            ClickGuiRenderer.drawBlur(x, y, w, h, 5f * scale, 0xFF000000, 200f / 255f);
            LayoutRender2D.roundedQuad(x, y, w, h, 5f * scale,
                    palette.moduleCardTop(), cardTopStrong, palette.moduleCardBottom(), cardBottomStrong);
            LayoutRender2D.rectQuad(x, y + descHeight + 25f * scale, w, 0.5f * scale,
                    palette.moduleDividerStart(), palette.moduleDividerEnd(),
                    palette.moduleDividerEnd(), palette.moduleDividerStart());
            Renderer2D.COLOR.radialGlowMasked(x, y, w, h, 5f * scale, 0f, 48f * scale,
                    x + w * 0.18f, y + h * 0.08f,
                    LayoutRender2D.alpha(palette.moduleCardTopStrong(), 0.12f + 0.10f * hoverAnim));

            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), "• " + entry.title(),
                    x + 8.8f * scale, y + 5.4f * scale, 13f * scale,
                    SettingsGuiPalette.withAlpha(palette.moduleTitleText(), alphaOffset), false);
            layout.drawDescription(description, x, y, w, scale, palette);
            if (!entry.hasSettings()) {
                layout.drawNoSettingsHint(x, y, descHeight, scale, palette);
            }
            bindRect = layout.drawBind(entry, x, y, w, descHeight, scale, palette);
            if (entry.toggleable()) {
                StatusRender status = state.statusRenderById.computeIfAbsent(entry.getId(), k -> new StatusRender());
                status.render(statusX, statusY, enabledAnim, scale);
            }
        }

        return new CardHit(entry.getId(), x, y, w, h,
                bindRect.hitX(), bindRect.hitY(), bindRect.hitW(), bindRect.hitH(),
                statusX, statusY, 16f * scale, 8f * scale,
                entry.hasSettings(), entry.toggleable());
    }

    private void renderScrollbar(float menuX, float menuY, float menuH, int clamped, float scale, SettingsGuiPalette palette) {
        layout.updateScrollbarMetrics(menuX, menuY, menuH, clamped, scale);
        if (!state.scrollbarVisible) return;
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        boolean hover = layout.isScrollbarHovered(mx, my);
        if (hover || state.draggingScrollbar) {
            SystemCursor.set(SystemCursor.CursorType.SCROLL);
        }
        LayoutRender2D.roundedQuad(state.scrollbarX, state.scrollbarY, state.scrollbarW, state.scrollbarH, 2f * scale,
                palette.moduleScrollTrackA(), palette.moduleScrollTrackB(),
                palette.moduleScrollTrackB(), palette.moduleScrollTrackA());
        LayoutRender2D.roundedQuad(state.scrollbarX, state.scrollbarThumbY, state.scrollbarW, state.scrollbarThumbH, 2f * scale,
                palette.moduleScrollHandleA(), palette.moduleScrollHandleB(),
                palette.moduleScrollHandleB(), palette.moduleScrollHandleA());
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return ClickGuiI18n.tr("clickgui.settings.module.description_missing", "Module description is missing.");
        }
        return description;
    }

    private static int brightenRgb(int color, int delta) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.min(255, ((color >>> 16) & 0xFF) + delta);
        int g = Math.min(255, ((color >>> 8) & 0xFF) + delta);
        int b = Math.min(255, (color & 0xFF) + delta);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static Parallax computeParallax(float mx, float my, float x, float y, float w, float h, float hover) {
        float ease = smooth(Math.max(0f, Math.min(1f, hover)));
        if (ease <= 0.001f || w <= 0f || h <= 0f) return Parallax.NONE;
        float nx = clamp((mx - x) / w * 2f - 1f, -1f, 1f);
        float ny = clamp((my - y) / h * 2f - 1f, -1f, 1f);
        return new Parallax(-nx * PARALLAX_MAX_ANGLE * ease, ny * PARALLAX_MAX_ANGLE * ease,
                1f + PARALLAX_SCALE_BOOST * ease, nx * PARALLAX_CONTENT_SHIFT * ease, ny * PARALLAX_CONTENT_SHIFT * ease, true);
    }

    private static RenderWarpStack.Scope pushParallax(Parallax p, float x, float y, float w, float h) {
        if (p == null || !p.active()) return Renderer2D.pushWarp(null);
        return Renderer2D.pushPerspectiveWarp(x, y, w, h, p.yawDeg(), p.pitchDeg(), 0f, PARALLAX_DEPTH, PARALLAX_PERSPECTIVE, p.scale());
    }

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
