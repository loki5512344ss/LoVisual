package dev.loki.lovisual.features.gui.hud.script;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.features.gui.hud.script.CompactHudStatModel;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;

public final class ScriptedCompactHudStatRenderer {
    public static final ScriptedCompactHudStatRenderer INSTANCE = new ScriptedCompactHudStatRenderer();

    private static final String LAYOUT_ID = "lovisual:api/hud/draggable/compact_stat";

    private final UiScriptModuleHandle moduleHandle = HudScriptLayouts.handle(LAYOUT_ID);
    private final CachedUiScriptRuntime scriptRuntime = new CachedUiScriptRuntime(HudScriptLayouts.runtimeReporter());

    private ScriptedCompactHudStatRenderer() {
    }

    public boolean render(CompactHudStatModel model,
                          Renderer2D renderer,
                          TextRenderer textRenderer,
                          GuiGraphicsExtractor ctx,
                          float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (model == null || !model.visible()) return false;
        if (moduleHandle.isRuntimeBlocked()) return false;
        if (mc == null || mc.getResourceManager() == null) return false;

        HudScriptLayouts.pollReloadCombo(mc);
        if (moduleHandle.consumeChanged()) {
            resetRuntime();
        }

        UiScriptModule module = ensureModule();
        if (module == null) return false;

        TextRenderer resolvedText = textRenderer != null ? textRenderer : TextRenderer.get();
        long treeSignature = model.structuralSignature();
        long layoutSignature = model.layoutSignature(treeSignature);
        UiRuntime runtime = scriptRuntime.updatePersistent(
                moduleHandle,
                module,
                model.getId(),
                treeSignature,
                layoutSignature,
                model.width(),
                model.height(),
                resolvedText,
                model.rootX(),
                model.rootY(),
                model.width(),
                model.height(),
                model::toTemplateProps,
                model::runtimePatches,
                model::runtimeBoundsPatches
        );
        if (runtime == null) return false;
        runtime.render(new UiRenderContext(renderer, resolvedText, ctx, tickDelta, UiProjectionMode.UNSCALED_LOGICAL));
        return true;
    }

    private UiScriptModule ensureModule() {
        Minecraft mc = Minecraft.getInstance();
        if (!moduleHandle.ensureLoaded(mc.getResourceManager())) {
            HudScriptLayouts.reportLoadError(moduleHandle);
            return null;
        }
        moduleHandle.consumeChanged();
        return moduleHandle.module();
    }

    private void resetRuntime() {
        scriptRuntime.reset();
    }
}
