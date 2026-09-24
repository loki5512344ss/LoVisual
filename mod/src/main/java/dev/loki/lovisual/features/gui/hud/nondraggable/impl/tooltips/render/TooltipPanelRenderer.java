package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tooltips;

import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2ic;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TooltipPanelRenderer {

    private TooltipPanelRenderer() {
    }

    static String hex(int argb) {
        return String.format("#%08X", argb);
    }

    static void drawFallback(float x, float y, float width, float height,
                             int bg, int tl, int tr, int br, int bl, int glow) {
        float scale = ViewportContext.getScaleFactor();
        float xb = x * scale;
        float yb = y * scale;
        float wb = width * scale;
        float hb = height * scale;
        float stroke = BetterTooltips.TOOLTIP_STROKE_WIDTH * scale;
        ViewportContext.unscaledProjection();
        Renderer2D.COLOR.roundedRectGlow(xb, yb, wb, hb, 0.0f, 0.0f,
                BetterTooltips.TOOLTIP_GLOW_SIZE * scale, glow);
        Renderer2D.COLOR.quad(xb, yb, wb, hb, bg);
        drawStroke(Renderer2D.COLOR, xb, yb, wb, hb, stroke, tl, tr, br, bl);
        ViewportContext.scaledProjection();
    }

    static void drawStroke(Renderer2D r, float x, float y, float w, float h, float t,
                           int ctl, int ctr, int cbr, int cbl) {
        if (r == null || t <= 0 || w <= 0 || h <= 0) return;
        float th = Math.min(t, Math.min(w, h) * 0.5f);
        r.quadGradient(x, y, w, th, ctl, ctr, ctr, ctl);
        r.quadGradient(x, y + h - th, w, th, cbl, cbr, cbr, cbl);
        float inner = Math.max(0, h - th * 2);
        if (inner > 0) {
            r.quadGradient(x, y + th, th, inner, ctl, ctl, cbl, cbl);
            r.quadGradient(x + w - th, y + th, th, inner, ctr, ctr, cbr, cbr);
        }
    }

    static boolean renderScripted(Minecraft mc, Renderer2D renderer, TextRenderer tr,
                                  float x, float y, float w, float h,
                                  int bg, int tl, int trc, int br, int bl, int glow,
                                  float strokeW, float glowSize, float alpha,
                                  UiScriptModuleHandle handle, CachedUiScriptRuntime runtime,
                                  GuiGraphicsExtractor ctx) {
        if (mc == null || renderer == null || tr == null || handle.isRuntimeBlocked()) return false;
        HudScriptLayouts.pollReloadCombo(mc);
        if (handle.consumeChanged()) runtime.reset();
        UiScriptModule module = ensureModule(mc, handle);
        if (module == null) return false;
        float fb = ViewportContext.getScaleFactor();
        float dx = x * fb;
        float dy = y * fb;
        float dw = w * fb;
        float dh = h * fb;
        float sw = strokeW * fb;
        float gs = glowSize * fb;
        Map<String, Object> props = props(dw, dh, bg, tl, trc, br, bl, glow, sw, gs);
        long sig = CachedUiScriptRuntime.signature(props);
        long layoutSig = CachedUiScriptRuntime.mix(CachedUiScriptRuntime.mix(sig, dx), dy);
        UiRuntime baked = runtime.bake(handle, module, "tooltip_panel", sig, layoutSig,
                dw, dh, tr, dx, dy, dw, dh, () -> props);
        if (baked == null) return false;
        ViewportContext.unscaledProjection();
        boolean started = !Renderer2D.isBatching();
        if (started) renderer.begin();
        baked.render(new UiRenderContext(renderer, tr, ctx, 0.0f, UiProjectionMode.RAW_FRAMEBUFFER, alpha));
        if (started) renderer.render();
        ViewportContext.scaledProjection();
        return true;
    }

    static Map<String, Object> props(float dw, float dh, int bg, int tl, int tr, int br, int bl, int glow, float sw, float gs) {
        LinkedHashMap<String, Object> p = new LinkedHashMap<>(12);
        p.put("width", dw);
        p.put("height", dh);
        p.put("bg", hex(bg));
        p.put("strokeTopLeft", hex(tl));
        p.put("strokeTopRight", hex(tr));
        p.put("strokeBottomRight", hex(br));
        p.put("strokeBottomLeft", hex(bl));
        p.put("strokeWidth", sw);
        p.put("glow", hex(glow));
        p.put("glowSize", gs);
        return p;
    }

    private static UiScriptModule ensureModule(Minecraft mc, UiScriptModuleHandle handle) {
        if (mc == null || mc.getResourceManager() == null) return null;
        if (!handle.ensureLoaded(mc.getResourceManager())) {
            HudScriptLayouts.reportLoadError(handle);
            return null;
        }
        handle.consumeChanged();
        return handle.module();
    }

    static void renderTooltipInternal(BetterTooltips instance, List<TooltipLine> lines, ClientTooltipPositioner pos, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        TextRenderer tr = Fonts.renderer(instance.tooltipFontValue(), FontInfo.Type.Regular, TextRenderer.get());
        if (tr == null) return;
        PreviewLayout preview = ShulkerPreviewCache.buildLayout();
        List<TooltipLine> finalLines = TooltipState.prepareLines(lines, preview != null);
        float scale = BetterTooltips.TOOLTIP_TEXT_SCALE * TooltipState.getScaleOverride() * instance.tooltipTextSizeValue();
        tr.begin(scale, false, false);
        int maxW = 0;
        for (TooltipLine l : finalLines) maxW = Math.max(maxW, (int) Math.ceil(tr.getWidth(l.text(), false)));
        int lh = (int) Math.ceil(tr.getHeight(false));
        tr.end();
        if (finalLines.isEmpty()) finalLines = List.of(new TooltipLine(" ", Theme.theme().textPrimary()));
        int textH = finalLines.size() * lh + (finalLines.size() > 1 ? BetterTooltips.TOOLTIP_TITLE_GAP : 0) + Math.max(0, finalLines.size() - 2) * BetterTooltips.TOOLTIP_LINE_GAP;
        int pw = preview != null ? preview.width() : 0;
        int ph = preview != null ? preview.height() : 0;
        int contentW = Math.max(maxW, pw);
        int totalW = contentW + BetterTooltips.TOOLTIP_PAD_X * 2;
        int totalH = BetterTooltips.TOOLTIP_PAD_Y * 2 + textH + (preview != null ? BetterTooltips.TOOLTIP_PREVIEW_GAP + ph : 0);
        int x = mouseX, y = mouseY;
        if (pos != null) {
            Vector2ic p = pos.positionTooltip(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mouseX, mouseY, totalW, totalH);
            x = p.x(); y = p.y();
        }
        Themes.Theme theme = Theme.theme();
        int bg = HudRenderUtil.setAlpha(theme.windowBg(), BetterTooltips.TOOLTIP_BG_ALPHA);
        int edge = HudRenderUtil.mixColor(theme.accentSoft(), theme.accent(), 0.34f);
        int glow = HudRenderUtil.setAlpha(edge, BetterTooltips.TOOLTIP_GLOW_ALPHA);
        int tl = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(theme.accentSoft(), theme.accent(), 0.14f), BetterTooltips.TOOLTIP_STROKE_ALPHA);
        int trc = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(theme.accentSoft(), theme.accent(), 0.82f), BetterTooltips.TOOLTIP_STROKE_ALPHA);
        int br = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(theme.accentSoft(), theme.accent(), 0.58f), BetterTooltips.TOOLTIP_STROKE_ALPHA);
        int bl = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(theme.accentSoft(), theme.accent(), 0.24f), BetterTooltips.TOOLTIP_STROKE_ALPHA);
        GuiGraphicsExtractor ctx = TooltipState.getCtx();
        ViewportContext.beginScaled(ctx);
        boolean rendered = renderScripted(mc, Renderer2D.COLOR, tr, x, y, totalW, totalH, bg, tl, trc, br, bl, glow, BetterTooltips.TOOLTIP_STROKE_WIDTH, BetterTooltips.TOOLTIP_GLOW_SIZE, 1.0f, instance.tooltipPanelHandle(), instance.tooltipRuntime(), ctx);
        if (!rendered) drawFallback(x, y, totalW, totalH, bg, tl, trc, br, bl, glow);
        int tx = x + BetterTooltips.TOOLTIP_PAD_X, ty = y + BetterTooltips.TOOLTIP_PAD_Y;
        tr.begin(scale, false, false);
        for (int i = 0; i < finalLines.size(); i++) {
            TooltipLine l = finalLines.get(i);
            tr.render(l.text(), tx, ty, new RenderColor(l.color()), true);
            ty += lh;
            if (i == 0 && finalLines.size() > 1) ty += BetterTooltips.TOOLTIP_TITLE_GAP; else if (i + 1 < finalLines.size()) ty += BetterTooltips.TOOLTIP_LINE_GAP;
        }
        tr.end();
        ViewportContext.end(ctx);
        if (preview != null) {
            int cw = totalW - BetterTooltips.TOOLTIP_PAD_X * 2;
            int px = x + BetterTooltips.TOOLTIP_PAD_X + Math.max(0, (cw - preview.width()) / 2);
            int py = y + BetterTooltips.TOOLTIP_PAD_Y + textH + BetterTooltips.TOOLTIP_PREVIEW_GAP;
            ShulkerPreviewRenderer.render(preview, px, py, ctx);
        }
    }
}
