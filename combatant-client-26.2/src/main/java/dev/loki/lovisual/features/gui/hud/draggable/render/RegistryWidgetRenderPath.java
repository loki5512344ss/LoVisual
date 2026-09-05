/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.render;

import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.WidgetAnchorSide;
import dev.loki.lovisual.features.gui.hud.draggable.interaction.RegistryDragSession;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryAnchorLayout;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.features.module.HudPhase;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.profiler.ProfilerPhase;
import dev.loki.lovisual.render.engine.profiler.RenderProfiler2D;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;

import java.util.HashSet;

/**
 * Путь отрисовки одиночного виджета по id (превью в редакторе и запросы движка).
 */
public final class RegistryWidgetRenderPath {

    private RegistryWidgetRenderPath() {
    }

    public static boolean hasEngineWidgetWork(String id) {
        if (!Lifecycle.canRunHud()) return false;
        if (id == null || id.isBlank()) return false;
        if (RegistryState.editorWidgetId != null && !RegistryState.editorWidgetId.equals(id)) return false;

        DraggableHudElement w = RegistryWidgetStore.findById(id);
        if (w == null) return false;
        if (!w.usesEngineRenderer()) return false;
        boolean bypassEnabled = RegistryState.forceVisible && RegistryState.editorWidgetId != null && RegistryState.editorWidgetId.equals(id);
        return bypassEnabled || w.isEnabled();
    }

    public static void renderEngineWidget(String id,
                                          Renderer2D renderer,
                                          TextRenderer textRenderer,
                                          net.minecraft.client.gui.GuiGraphicsExtractor ctx,
                                          float tickDelta) {
        if (!Lifecycle.canRunHud()) {
            RegistryDragSession.cancelDragging();
            return;
        }
        if (RegistryState.editorWidgetId != null && (!RegistryState.editorWidgetId.equals(id))) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        boolean isChat = ClientScreen.current() instanceof net.minecraft.client.gui.screens.ChatScreen;
        boolean isInventory = ClientScreen.current() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>;
        boolean allowOnlyModuleList = isInventory && ClientScreen.current() != null;
        if (RegistryState.forceVisible) {
            allowOnlyModuleList = false;
        }
        boolean shouldHideHud = !RegistryState.forceVisible && ClientScreen.current() != null && !isChat && !allowOnlyModuleList;
        if (shouldHideHud) {
            return;
        }

        DraggableHudElement w = RegistryWidgetStore.findById(id);
        if (w == null) return;
        if (!w.usesEngineRenderer()) return;
        boolean bypassEnabled = RegistryState.forceVisible && RegistryState.editorWidgetId != null && RegistryState.editorWidgetId.equals(id);
        if (!bypassEnabled && !w.isEnabled()) {
            return;
        }
        if (allowOnlyModuleList && !"module_list".equals(w.getId())) {
            return;
        }

        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        int screenW = Math.max(1, Math.round(HudScale.virtualWidth(fbw, fbh)));
        int screenH = Math.max(1, Math.round(HudScale.virtualHeight(fbw, fbh)));
        RegistryResizeHandler.handleScreenResize(screenW, screenH);

        RegistryAnchorLayout.ensurePositionInitialized(w, screenW, screenH);
        if (w != RegistryState.dragging) {
            if (w.getParentId() == null || w.getParentSide() == WidgetAnchorSide.NONE) {
                RegistryAnchorLayout.applyScreenAnchor(w, screenW, screenH);
            } else {
                RegistryAnchorLayout.applyAnchors(w, screenW, screenH, new HashSet<>());
            }
        }

        RegistryWidgetStore.noteRendered(w);
        try (ProfilerPhase.Scope widgetScope =
                     ProfilerPhase.scope("2d:widget:" + w.getId());
             RenderProfiler2D.Section ignored = RenderProfiler2D.section("widget:" + w.getId())) {
            w.renderEngine(renderer, textRenderer, ctx, tickDelta, screenW, screenH);
        }
        try (ProfilerPhase.Scope widgetScope =
                     ProfilerPhase.scope("2d:widget_fg:" + w.getId());
             RenderProfiler2D.Section ignored = RenderProfiler2D.section("widget_fg:" + w.getId())) {
            w.renderEngineForeground(renderer, textRenderer, ctx, tickDelta, screenW, screenH);
        }
    }
}
