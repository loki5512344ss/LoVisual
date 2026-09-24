package dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars;

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
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Scripted fill layer of the custom health bar: bakes the HUD script layout
 * {@code custom_health_bar} with live props and renders it above the glass
 * background. Owns the layout handle + cached runtime that the element used to
 * carry as instance fields.
 */
final class HealthScriptRenderer {

    private static final String LAYOUT_ID = "lovisual:api/hud/static/custom_health_bar";

    private final UiScriptModuleHandle moduleHandle = HudScriptLayouts.handle(LAYOUT_ID);
    private final CachedUiScriptRuntime scriptRuntime = new CachedUiScriptRuntime(HudScriptLayouts.runtimeReporter());

    void render(CustomHealthBar bm, Renderer2D r2d,
                float x, float y, float width, float height, float radius,
                float baseRatio, float absorbRatio) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || width <= 0.0f || height <= 0.0f) return;

        HudScriptLayouts.pollReloadCombo(mc);
        if (moduleHandle.consumeChanged()) {
            scriptRuntime.reset();
        }

        UiScriptModule loaded = ensureModule(mc);
        if (loaded == null) return;

        LinkedHashMap<String, Object> props = buildBarProps(bm, width, height, radius, baseRatio, absorbRatio);
        long signature = CachedUiScriptRuntime.signature(props);
        UiRuntime runtime = scriptRuntime.bake(
                moduleHandle,
                loaded,
                "custom_health_bar",
                signature,
                signature,
                signature,
                width,
                height,
                TextRenderer.get(),
                x,
                y,
                width,
                height,
                () -> props,
                null
        );
        if (runtime == null) return;
        runtime.render(new UiRenderContext(r2d, TextRenderer.get(), null, 0.0f, UiProjectionMode.CURRENT));
    }

    private LinkedHashMap<String, Object> buildBarProps(CustomHealthBar bm,
                                                        float width, float height, float radius,
                                                        float baseRatio, float absorbRatio) {
        float phase = (float) (Util.getMillis() / 1000.0);

        LinkedHashMap<String, Object> props = new LinkedHashMap<>();
        props.put("width", width);
        props.put("height", height);
        props.put("radius", radius);
        props.put("healthRatio", Mth.clamp(baseRatio, 0.0f, 1.0f));
        props.put("absorbRatio", Mth.clamp(absorbRatio, 0.0f, 1.0f));
        props.put("alpha", bm.getHudHealthAlphaFactor());
        props.put("phase", phase);
        props.put("colorMode", bm.isDynamicHealthColorMode() ? "dynamic" : "theme");
        props.put("themeAccent", hex(bm.themeAccentArgb()));
        props.put("themeAccentSoft", hex(bm.themeAccentSoftArgb()));
        props.put("themeTextPrimary", hex(bm.themeTextPrimaryArgb()));
        return props;
    }

    private UiScriptModule ensureModule(Minecraft mc) {
        if (mc == null || mc.getResourceManager() == null) return null;
        if (!moduleHandle.ensureLoaded(mc.getResourceManager())) {
            HudScriptLayouts.reportLoadError(moduleHandle);
            return null;
        }
        moduleHandle.consumeChanged();
        return moduleHandle.module();
    }

    private static String hex(int argb) {
        return String.format("#%08X", argb);
    }
}
