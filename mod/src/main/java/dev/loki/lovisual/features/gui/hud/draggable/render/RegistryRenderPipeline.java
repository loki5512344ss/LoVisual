/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.render;

import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;
import dev.loki.lovisual.features.gui.hud.anchor.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.anchor.WidgetAnchorSide;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.interaction.RegistryDragController;
import dev.loki.lovisual.features.gui.hud.draggable.interaction.RegistryDragSession;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryAnchorLayout;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.features.gui.hud.draggable.render.RegistryOverlayRenderer;
import dev.loki.lovisual.features.gui.hud.draggable.render.RegistryRenderPipeline;
import dev.loki.lovisual.features.gui.hud.draggable.render.RegistryResizeHandler;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.draggable.interaction.RegistryDragController;
import dev.loki.lovisual.features.gui.hud.draggable.interaction.RegistryDragSession;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryAnchorLayout;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.profiler.config.ProfilerPhase;
import dev.loki.lovisual.render.engine.profiler.render.RenderProfiler2D;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Пакетный проход движка рендера: отбор и отрисовка всех виджетов фазы + drag-оверлей.
 */
public final class RegistryRenderPipeline {

    private RegistryRenderPipeline() {
    }

    public static boolean hasEngineWork(HudPhase phase, boolean foreground) {
        if (!Lifecycle.canRunHud()) return false;
        if (foreground && RegistryState.dragging != null) return true;

        for (DraggableHudElement w : RegistryState.WIDGETS) {
            if (RegistryWidgetStore.isRenderableCandidate(w, phase)) return true;
        }
        return false;
    }

    public static void renderAllEngine(HudPhase phase,
                                       Renderer2D renderer,
                                       TextRenderer textRenderer,
                                       net.minecraft.client.gui.GuiGraphicsExtractor ctx,
                                       float tickDelta) {
        if (!Lifecycle.canRunHud()) {
            RegistryDragSession.cancelDragging();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        boolean allowOnlyModuleList = false;

        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        int screenW = Math.max(1, Math.round(HudScale.virtualWidth(fbw, fbh)));
        int screenH = Math.max(1, Math.round(HudScale.virtualHeight(fbw, fbh)));
        RegistryResizeHandler.handleScreenResize(screenW, screenH);

        List<DraggableHudElement> renderList = new ArrayList<>();
        for (DraggableHudElement w : RegistryState.WIDGETS) {
            boolean singleEditorPreview = RegistryState.editorWidgetId != null;
            if (singleEditorPreview && (RegistryState.editorWidgetId.isEmpty() || !RegistryState.editorWidgetId.equals(w.getId()))) {
                continue;
            }
            boolean bypassEnabled = singleEditorPreview && RegistryState.forceVisible;
            if (!bypassEnabled && !w.isEnabled()) {
                continue;
            }
            if (!w.usesEngineRenderer()) {
                continue;
            }
            if (w.getHudPhase() != phase) {
                continue;
            }
            if (w.getRenderSpace() != HudRenderSpace.UNSCALED_LOGICAL) {
                continue;
            }
            RegistryAnchorLayout.ensurePositionInitialized(w, screenW, screenH);
            if (allowOnlyModuleList && !"module_list".equals(w.getId())) {
                continue;
            }
            renderList.add(w);
        }

        for (DraggableHudElement w : renderList) {
            if (w == RegistryState.dragging) continue;
            if (w.getParentId() == null || w.getParentSide() == WidgetAnchorSide.NONE) {
                RegistryAnchorLayout.applyScreenAnchor(w, screenW, screenH);
            }
        }
        for (DraggableHudElement w : renderList) {
            if (w == RegistryState.dragging) continue;
            if (w.getParentId() != null && w.getParentSide() != WidgetAnchorSide.NONE) {
                RegistryAnchorLayout.applyAnchors(w, screenW, screenH, new HashSet<>());
            }
        }

        for (DraggableHudElement w : renderList) {
            RegistryWidgetStore.noteRendered(w);
            try (ProfilerPhase.Scope widgetScope =
                         ProfilerPhase.scope("2d:widget:" + w.getId());
                 RenderProfiler2D.Section ignored = RenderProfiler2D.section("widget:" + w.getId())) {
                w.renderEngine(renderer, textRenderer, ctx, tickDelta, screenW, screenH);
            }
        }
    }

    public static void renderAllEngineForeground(HudPhase phase,
                                                 Renderer2D renderer,
                                                 TextRenderer textRenderer,
                                                 net.minecraft.client.gui.GuiGraphicsExtractor ctx,
                                                 float tickDelta) {
        if (!Lifecycle.canRunHud()) {
            RegistryDragSession.cancelDragging();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        boolean allowOnlyModuleList = false;

        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        int screenW = Math.max(1, Math.round(HudScale.virtualWidth(fbw, fbh)));
        int screenH = Math.max(1, Math.round(HudScale.virtualHeight(fbw, fbh)));
        RegistryResizeHandler.handleScreenResize(screenW, screenH);

        for (DraggableHudElement w : RegistryState.WIDGETS) {
            boolean singleEditorPreview = RegistryState.editorWidgetId != null;
            if (singleEditorPreview && (RegistryState.editorWidgetId.isEmpty() || !RegistryState.editorWidgetId.equals(w.getId()))) {
                continue;
            }
            boolean bypassEnabled = singleEditorPreview && RegistryState.forceVisible;
            if (!bypassEnabled && !w.isEnabled()) {
                continue;
            }
            if (!w.usesEngineRenderer()) {
                continue;
            }
            if (w.getHudPhase() != phase) {
                continue;
            }
            if (w.getRenderSpace() != HudRenderSpace.UNSCALED_LOGICAL) {
                continue;
            }
            if (allowOnlyModuleList && !"module_list".equals(w.getId())) {
                continue;
            }
            try (ProfilerPhase.Scope widgetScope =
                         ProfilerPhase.scope("2d:widget_fg:" + w.getId());
                 RenderProfiler2D.Section ignored = RenderProfiler2D.section("widget_fg:" + w.getId())) {
                w.renderEngineForeground(renderer, textRenderer, ctx, tickDelta, screenW, screenH);
            }
        }

        RegistryOverlayRenderer.renderDragOverlay(renderer, textRenderer, screenW, screenH);
        BetterChatRenderer.renderCapturedTooltip(ctx);
        if (phase == HudPhase.AFTER_SUBTITLES) {
            float uiScale = HudScale.scale(fbw, fbh);
            RegistryDragController.handleDragging(mc, screenW, screenH, uiScale);
        }
    }
}
