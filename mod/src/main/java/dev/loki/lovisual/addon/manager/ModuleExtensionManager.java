package dev.loki.lovisual.addon.manager;

import dev.loki.lovisual.api.v0.module.LoVisualModuleExtension;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import dev.loki.lovisual.addon.context.ModuleExtensionContextImpl;
import dev.loki.lovisual.addon.core.AddonManager;

public enum ModuleExtensionManager {
    ;

    private static final Map<String, List<RegisteredExtension>> EXTENSIONS = new LinkedHashMap<>();

    public static boolean register(String addonId, String moduleId, LoVisualModuleExtension extension) {
        if (extension == null || moduleId == null || moduleId.isBlank()) return false;
        Module module = ModuleManager.get(moduleId);
        if (module == null) {
            DebugLog.warn("[Addons] Module extension target missing: addon=%s module=%s", addonId, moduleId);
            return false;
        }
        String normalized = normalize(moduleId);
        RegisteredExtension registered = new RegisteredExtension(addonId, extension);
        EXTENSIONS.computeIfAbsent(normalized, ignored -> new ArrayList<>()).add(registered);
        try {
            extension.onRegister(new ModuleExtensionContextImpl(addonId, module));
        } catch (Throwable t) {
            DebugLog.error("[Addons] Module extension registration failed: %s", t, normalized);
            return false;
        }
        return true;
    }

    public static int count() {
        int count = 0;
        for (List<RegisteredExtension> list : EXTENSIONS.values()) {
            count += list.size();
        }
        return count;
    }

    public static boolean beforeEnable(Module module) {
        return all(module, extension -> extension.beforeEnable(module));
    }

    public static void afterEnable(Module module) {
        each(module, extension -> extension.afterEnable(module));
    }

    public static boolean beforeDisable(Module module) {
        return all(module, extension -> extension.beforeDisable(module));
    }

    public static void afterDisable(Module module) {
        each(module, extension -> extension.afterDisable(module));
    }

    public static boolean beforeTick(Module module) {
        return all(module, extension -> extension.beforeTick(module));
    }

    public static void afterTick(Module module) {
        each(module, extension -> extension.afterTick(module));
    }

    public static boolean beforeFrame(Module module, float frameDeltaTicks) {
        return all(module, extension -> extension.beforeFrame(module, frameDeltaTicks));
    }

    public static void afterFrame(Module module, float frameDeltaTicks) {
        each(module, extension -> extension.afterFrame(module, frameDeltaTicks));
    }

    public static boolean beforeHudRender(Module module,
                                          Renderer2D renderer,
                                          TextRenderer textRenderer,
                                          GuiGraphicsExtractor ctx,
                                          float tickDelta) {
        return all(module, extension -> extension.beforeHudRender(module, renderer, textRenderer, ctx, tickDelta));
    }

    public static void afterHudRender(Module module,
                                      Renderer2D renderer,
                                      TextRenderer textRenderer,
                                      GuiGraphicsExtractor ctx,
                                      float tickDelta) {
        each(module, extension -> extension.afterHudRender(module, renderer, textRenderer, ctx, tickDelta));
    }

    public static boolean beforeWorldRender(Module module,
                                            Renderer3D renderer,
                                            Renderer3D depthRenderer,
                                            float tickDelta) {
        return all(module, extension -> extension.beforeWorldRender(module, renderer, depthRenderer, tickDelta));
    }

    public static void afterWorldRender(Module module,
                                        Renderer3D renderer,
                                        Renderer3D depthRenderer,
                                        float tickDelta) {
        each(module, extension -> extension.afterWorldRender(module, renderer, depthRenderer, tickDelta));
    }

    private static boolean all(Module module, ExtensionBooleanCall call) {
        List<RegisteredExtension> extensions = extensions(module);
        if (extensions.isEmpty()) return true;
        boolean proceed = true;
        for (RegisteredExtension registered : extensions) {
            if (!AddonManager.isActive(registered.addonId())) continue;
            try {
                proceed &= call.invoke(registered.extension());
            } catch (Throwable t) {
                DebugLog.error("[Addons] Module extension hook failed: addon=%s module=%s",
                        t, registered.addonId(), module == null ? "" : module.name());
            }
        }
        return proceed;
    }

    private static void each(Module module, ExtensionVoidCall call) {
        List<RegisteredExtension> extensions = extensions(module);
        if (extensions.isEmpty()) return;
        for (RegisteredExtension registered : extensions) {
            if (!AddonManager.isActive(registered.addonId())) continue;
            try {
                call.invoke(registered.extension());
            } catch (Throwable t) {
                DebugLog.error("[Addons] Module extension hook failed: addon=%s module=%s",
                        t, registered.addonId(), module == null ? "" : module.name());
            }
        }
    }

    private static List<RegisteredExtension> extensions(Module module) {
        if (module == null) return List.of();
        List<RegisteredExtension> list = EXTENSIONS.get(normalize(module.name()));
        return list == null ? List.of() : List.copyOf(list);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface ExtensionBooleanCall {
        boolean invoke(LoVisualModuleExtension extension);
    }

    @FunctionalInterface
    private interface ExtensionVoidCall {
        void invoke(LoVisualModuleExtension extension);
    }

    private record RegisteredExtension(String addonId, LoVisualModuleExtension extension) {
    }
}
