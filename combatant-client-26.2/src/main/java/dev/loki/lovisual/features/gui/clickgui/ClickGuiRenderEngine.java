package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.module.ModuleManager;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.profiler.ProfilerPhase;
import dev.loki.lovisual.render.engine.profiler.RenderProfiler2D;
import dev.loki.lovisual.render.engine.profiler.TracyGpuProfiler;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.helpers.SystemCursor;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

final class ClickGuiRenderEngine {

    private ClickGuiRenderEngine() {
    }

    static void renderEngine(Renderer2D rendererIn, TextRenderer fallback, GuiGraphicsExtractor ctx, float tickDelta) {
        try (ProfilerPhase.Scope frameScope = ProfilerPhase.scope("2d:clickgui:render");
             RenderProfiler2D.Section ignoredFrame = RenderProfiler2D.section("render")) {
            boolean enabled = ModuleManager.isEnabled("clickgui");
            long now = System.nanoTime();
            boolean mainScreenActive = ClientScreen.current() instanceof ClickGuiScreen;
            boolean pickerScreenActive = ClientScreen.current() instanceof ClickGuiPickerScreen;

            if (!enabled && !ClickGuiRenderer.closingForExit && ClickGuiRenderer.lifecycleAnim <= 0.001f) {
                ClickGuiLifecycle.onClose();
                return;
            }

            if (ClientScreen.current() != null && !mainScreenActive && !pickerScreenActive) {
                if (ClickGuiRenderer.closingForExit) ClickGuiLifecycle.finishCloseImmediately();
                return;
            }
            // Screen renders ClickGUI directly; no extra gating needed here.

            ClickGuiLifecycle.init();
            if (enabled && ClickGuiRenderer.closingForExit) {
                ClickGuiRenderer.closingForExit = false;
                ClickGuiTabState.getActiveSection().onSelected();
            }
            ClickGuiLifecycle.updateLifecycleAnim(now, enabled && !ClickGuiRenderer.closingForExit);
            ClickGuiRenderer.renderer = rendererIn;
            ClickGuiRenderer.pickerIcons.clear();

            SystemCursor.beginFrame(ctx);
            try {
                if (enabled && !ClickGuiRenderer.closingForExit && !ClickGuiRenderer.cursorShown) {
                    GLFW.glfwSetInputMode(
                            Minecraft.getInstance().getWindow().handle(),
                            GLFW.GLFW_CURSOR,
                            GLFW.GLFW_CURSOR_NORMAL
                    );
                    SystemCursor.invalidate();
                    ClickGuiRenderer.cursorShown = true;
                }
                int fbw = Minecraft.getInstance().getWindow().getWidth();
                int fbh = Minecraft.getInstance().getWindow().getHeight();
                int vw = Math.max(1, Math.round(HudScale.virtualWidth(fbw, fbh)));
                int vh = Math.max(1, Math.round(HudScale.virtualHeight(fbw, fbh)));
                ClickGuiRenderer.fbWidth = vw;
                ClickGuiRenderer.fbHeight = vh;

                try (ProfilerPhase.Scope layoutScope = ProfilerPhase.scope("2d:clickgui:layout");
                     RenderProfiler2D.Section ignoredLayout = RenderProfiler2D.section("layout");
                     TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("2d:clickgui:layout")) {
                    ClickGuiTabBar.layout(vw, vh);
                    ClickGuiTabBar.updateTabAnim(now);
                }

                if (pickerScreenActive) {
                    try (ProfilerPhase.Scope pickerScope = ProfilerPhase.scope("2d:clickgui:modal");
                         RenderProfiler2D.Section ignoredPicker = RenderProfiler2D.section("modal");
                         TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("2d:clickgui:modal")) {
                        if (ClickGuiRenderer.protocolHeuristicsEditor != null) {
                            ClickGuiRenderer.protocolHeuristicsEditor.render(vw, vh);
                        } else {
                            ClickGuiEditorManager.renderPicker(vw, vh);
                        }
                    }
                    try (ProfilerPhase.Scope textEditorScope = ProfilerPhase.scope("2d:clickgui:text_editor");
                         RenderProfiler2D.Section ignoredTextEditor = RenderProfiler2D.section("text_editor");
                         TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("2d:clickgui:text_editor")) {
                        if (!ClickGuiRenderer.closingForExit) {
                            ClickGuiEditorManager.renderTextEditor(vw, vh);
                        }
                    }
                    return;
                }

                try (ProfilerPhase.Scope tabsScope = ProfilerPhase.scope("2d:clickgui:tabs");
                     RenderProfiler2D.Section ignoredTabs = RenderProfiler2D.section("tabs");
                     TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("2d:clickgui:tabs")) {
                    ClickGuiTabBar.drawTabs();
                }

                try (ProfilerPhase.Scope sectionScope = ProfilerPhase.scope("2d:clickgui:section");
                     RenderProfiler2D.Section ignoredSection = RenderProfiler2D.section("section");
                     TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("2d:clickgui:section")) {
                    ClickGuiTabState.getActiveSection().render(ClickGuiRenderer.mouseX, ClickGuiRenderer.mouseY);
                }
                try (ProfilerPhase.Scope textEditorScope = ProfilerPhase.scope("2d:clickgui:text_editor");
                     RenderProfiler2D.Section ignoredTextEditor = RenderProfiler2D.section("text_editor");
                     TracyGpuProfiler.Scope gpuScope = TracyGpuProfiler.beginZone("2d:clickgui:text_editor")) {
                    if (!ClickGuiRenderer.closingForExit) {
                        ClickGuiEditorManager.renderTextEditor(vw, vh);
                    }
                }

                if (ClickGuiRenderer.closingForExit && ClickGuiRenderer.lifecycleAnim <= 0.001f && ClickGuiLifecycle.sectionsClosed()) {
                    ClickGuiLifecycle.completeCloseAnimation();
                }
            } finally {
                SystemCursor.endFrame();
            }
        }
    }

    static int framebufferWidth() {
        return ClickGuiRenderer.fbWidth;
    }

    static int framebufferHeight() {
        return ClickGuiRenderer.fbHeight;
    }

    static float getAnimAlpha() {
        return 1f;
    }

    static float getMouseX() {
        return ClickGuiRenderer.mouseX;
    }

    static float getMouseY() {
        return ClickGuiRenderer.mouseY;
    }
}
