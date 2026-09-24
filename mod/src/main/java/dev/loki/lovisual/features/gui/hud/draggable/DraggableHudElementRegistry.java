package dev.loki.lovisual.features.gui.hud.draggable;

import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.features.gui.hud.draggable.render.RegistryRenderPipeline;
import dev.loki.lovisual.features.gui.hud.draggable.render.RegistryWidgetRenderPath;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.List;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;

/**
 * Реестр HUD-виджетов + drag-and-drop при открытом чате.
 * <p>
 * Публичный фасад: логика делегирована хелперам
 * ({@code registry/} — состояние, поиск, якоря и snap;
 * {@code interaction/} — drag-and-drop; {@code render/} — пайплайн рендера и оверлей).
 */
public enum DraggableHudElementRegistry {
    ;

    public static void register(DraggableHudElement widget) {
        RegistryWidgetStore.register(widget);
    }

    public static List<DraggableHudElement> getWidgets() {
        return RegistryWidgetStore.getWidgets();
    }

    public static DraggableHudElement getById(String id) {
        return RegistryWidgetStore.getById(id);
    }

    public static boolean isEnabled(String id) {
        return RegistryWidgetStore.isEnabled(id);
    }

    public static <T extends DraggableHudElement> T get(Class<T> type) {
        return RegistryWidgetStore.get(type);
    }

    public static <T extends DraggableHudElement> T widget(Class<T> type) {
        return get(type);
    }

    public static boolean isEnabled(Class<? extends DraggableHudElement> type) {
        return RegistryWidgetStore.isEnabled(type);
    }

    public static void tickAll() {
        RegistryWidgetStore.tickAll();
    }

    public static boolean isForceVisible() {
        return RegistryState.forceVisible;
    }

    public static void setForceVisible(boolean value) {
        RegistryState.forceVisible = value;
    }

    public static void setEditorWidgetId(String id) {
        RegistryState.editorWidgetId = id;
    }

    public static boolean hasEngineWork(HudPhase phase, boolean foreground) {
        return RegistryRenderPipeline.hasEngineWork(phase, foreground);
    }

    public static boolean hasEngineWidgetWork(String id) {
        return RegistryWidgetRenderPath.hasEngineWidgetWork(id);
    }

    public static void renderAllEngine(HudPhase phase,
                                       Renderer2D renderer,
                                       TextRenderer textRenderer,
                                       net.minecraft.client.gui.GuiGraphicsExtractor ctx,
                                       float tickDelta) {
        RegistryRenderPipeline.renderAllEngine(phase, renderer, textRenderer, ctx, tickDelta);
    }

    public static void renderAllEngineForeground(HudPhase phase,
                                                 Renderer2D renderer,
                                                 TextRenderer textRenderer,
                                                 net.minecraft.client.gui.GuiGraphicsExtractor ctx,
                                                 float tickDelta) {
        RegistryRenderPipeline.renderAllEngineForeground(phase, renderer, textRenderer, ctx, tickDelta);
    }

    public static void renderEngineWidget(String id,
                                          Renderer2D renderer,
                                          TextRenderer textRenderer,
                                          net.minecraft.client.gui.GuiGraphicsExtractor ctx,
                                          float tickDelta) {
        RegistryWidgetRenderPath.renderEngineWidget(id, renderer, textRenderer, ctx, tickDelta);
    }
}
