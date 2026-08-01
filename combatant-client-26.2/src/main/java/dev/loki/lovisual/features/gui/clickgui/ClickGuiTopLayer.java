package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.clickgui.sections.settings.SettingsTabRuntime;
import dev.loki.lovisual.render.engine.core.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.RenderPhase;
import dev.loki.lovisual.render.engine.core.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.ViewportContext;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.SystemCursor;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class ClickGuiTopLayer {

    private ClickGuiTopLayer() {
    }

    static boolean shouldRenderAsTopLayer() {
        return Minecraft.getInstance() != null && (ClickGuiLifecycle.isLifecycleActive()
                || ClientScreen.current() instanceof ClickGuiScreen
                || ClientScreen.current() instanceof ClickGuiPickerScreen
                || ClientScreen.current() instanceof ClickGuiEditorScreen);
    }

    static void renderTopLayer(GuiGraphicsExtractor ctx, float tickDelta) {
        if (!shouldRenderAsTopLayer()) return;

        LoVisualRenderSystem.ensureFrameContext();
        ViewportContext.beginUnscaledLogical(ctx);
        try (RenderPhaseScope phaseScope = LoVisualRenderSystem.phase(RenderPhase.SCREEN_TOP, "2d:clickgui_top")) {
            SystemCursor.beginFrame(ctx);
            try {
                Renderer2D.COLOR.begin();
                if (ClientScreen.current() instanceof ClickGuiEditorScreen) {
                    renderEditor(Renderer2D.COLOR, TextRenderer.get(), ctx, tickDelta);
                } else {
                    ClickGuiRenderEngine.renderEngine(Renderer2D.COLOR, TextRenderer.get(), ctx, tickDelta);
                }
                Renderer2D.COLOR.render();
                if (ClientScreen.current() instanceof ClickGuiPickerScreen) {
                    ClickGuiPickerIconManager.processPickerIcons(ctx);
                }
            } finally {
                SystemCursor.endFrame();
            }
        }
        ViewportContext.end(ctx);
    }

    static void renderEditor(Renderer2D rendererIn, TextRenderer fallback, GuiGraphicsExtractor ctx, float tickDelta) {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().getWindow() == null) return;
        ClickGuiLifecycle.init();
        ClickGuiRenderer.renderer = rendererIn;
        int fbw = Minecraft.getInstance().getWindow().getWidth();
        int fbh = Minecraft.getInstance().getWindow().getHeight();
        int vw = Math.max(1, Math.round(HudScale.virtualWidth(fbw, fbh)));
        int vh = Math.max(1, Math.round(HudScale.virtualHeight(fbw, fbh)));
        ClickGuiRenderer.fbWidth = vw;
        ClickGuiRenderer.fbHeight = vh;
        SystemCursor.beginFrame(ctx);
        try {
            SettingsTabRuntime.renderEditor(rendererIn, fallback, ctx, tickDelta, vw, vh, ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY);
        } finally {
            SystemCursor.endFrame();
        }
    }

    static void renderModulesPreview(float x, float y, float w, float h) {
        if (ClickGuiRenderer.renderer == null) return;
        ClickGuiLifecycle.init();
        float pad = 8f;
        float areaX = x + pad;
        float areaY = y + pad;
        float areaW = Math.max(1f, w - pad * 2f);
        float areaH = Math.max(1f, h - pad * 2f);
        ClickGuiRenderer.MODULES_SECTION.layout(areaX, areaY, areaW, areaH);
        float mx = -10_000f;
        float my = -10_000f;
        ClickGuiRenderer.MODULES_SECTION.render(mx, my);
    }
}
