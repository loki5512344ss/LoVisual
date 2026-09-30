package dev.loki.lovisual.features.gui.hud.draggable.impl.scripted;

import java.util.LinkedHashMap;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Bakes and draws the triangulator HUD script layout for a panel.
 */
public final class ScriptedTriangulatorRenderer {

    private ScriptedTriangulatorRenderer() {
    }

    public static boolean render(UiScriptModuleHandle moduleHandle,
                                 CachedUiScriptRuntime runtime,
                                 Renderer2D renderer,
                                 TextRenderer textRenderer,
                                 GuiGraphicsExtractor ctx,
                                 float tickDelta,
                                 ScriptedTriangulatorHudPanel.Panel panel) {
        if (renderer == null || panel == null || panel.width() <= 0.0f || panel.height() <= 0.0f) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) return false;

        HudScriptLayouts.pollReloadCombo(mc);
        if (moduleHandle.consumeChanged()) {
            runtime.reset();
        }

        UiScriptModule module = ensureModule(moduleHandle, mc);
        if (module == null) return false;

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        LinkedHashMap<String, Object> props = panel.toProps();
        UiRuntime baked = runtime.bake(
                moduleHandle,
                module,
                "triangulator",
                panel.treeSignature(),
                panel.dataSignature(),
                panel.layoutSignature(),
                panel.width(),
                panel.height(),
                fallback,
                panel.x(),
                panel.y(),
                panel.width(),
                panel.height(),
                () -> props,
                panel::patches
        );
        if (baked == null) return false;
        baked.render(new UiRenderContext(renderer, fallback, ctx, tickDelta, UiProjectionMode.CURRENT));
        return true;
    }

    private static UiScriptModule ensureModule(UiScriptModuleHandle moduleHandle, Minecraft mc) {
        if (!moduleHandle.ensureLoaded(mc.getResourceManager())) {
            HudScriptLayouts.reportLoadError(moduleHandle);
            return null;
        }
        moduleHandle.consumeChanged();
        return moduleHandle.module();
    }
}
