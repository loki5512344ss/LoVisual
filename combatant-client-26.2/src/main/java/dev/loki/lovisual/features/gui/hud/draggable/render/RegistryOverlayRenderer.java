/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.render;

import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.WidgetAnchorSide;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryAnchorLayout;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistrySnapSupport;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryState;
import dev.loki.lovisual.features.gui.hud.draggable.registry.RegistryWidgetStore;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;

/** Оверлей перетаскивания: координаты, snap-линии и провода родительских связей. */
public final class RegistryOverlayRenderer {

    private RegistryOverlayRenderer() {
    }

    public static void renderDragOverlay(Renderer2D renderer,
                                         TextRenderer fallbackTextRenderer,
                                         int screenW,
                                         int screenH) {
        renderLinkWires(renderer, screenW, screenH);

        if (RegistryState.snapPreviewActive) {
            if (RegistryState.snapPreviewHasX) {
                renderer.line(RegistryState.snapPreviewLineX, 0f, RegistryState.snapPreviewLineX, screenH, RegistryState.SNAP_PREVIEW_LINE);
            }
            if (RegistryState.snapPreviewHasY) {
                renderer.line(0f, RegistryState.snapPreviewLineY, screenW, RegistryState.snapPreviewLineY, RegistryState.SNAP_PREVIEW_LINE);
            }
        }

        if (RegistryState.dragging == null) return;
        TextRenderer coordRenderer = Fonts.renderer("Comfortaa", dev.loki.lovisual.render.engine.text.FontInfo.Type.Regular,
                fallbackTextRenderer != null ? fallbackTextRenderer : TextRenderer.get());
        if (coordRenderer == null) return;

        String coords = "x: " + Math.round(RegistryState.dragging.getX()) + " y: " + Math.round(RegistryState.dragging.getY());
        coordRenderer.begin(RegistryState.DRAG_COORD_TEXT_SCALE, false, false);
        try {
            float textW = (float) coordRenderer.getWidth(coords, true);
            float textH = (float) coordRenderer.getHeight(true);
            float textX = RegistryState.dragging.getX();
            float textY = RegistryState.dragging.getY() - textH - RegistryState.DRAG_COORD_MARGIN;
            if (textY < RegistryState.DRAG_COORD_MARGIN) {
                textY = RegistryState.dragging.getY() + RegistryState.dragging.getHeight() + RegistryState.DRAG_COORD_MARGIN;
            }
            textX = RegistryAnchorLayout.clamp(textX, RegistryState.DRAG_COORD_MARGIN,
                    Math.max(RegistryState.DRAG_COORD_MARGIN, screenW - textW - RegistryState.DRAG_COORD_MARGIN));
            textY = RegistryAnchorLayout.clamp(textY, RegistryState.DRAG_COORD_MARGIN,
                    Math.max(RegistryState.DRAG_COORD_MARGIN, screenH - textH - RegistryState.DRAG_COORD_MARGIN));
            coordRenderer.render(coords, textX, textY, new dev.loki.lovisual.render.engine.color.RenderColor(0xFFFFFFFF), true);
        } finally {
            coordRenderer.end();
        }
    }

    private static void renderLinkWires(Renderer2D renderer,
                                        int screenW,
                                        int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || !(ClientScreen.current() instanceof net.minecraft.client.gui.screens.ChatScreen))
            return;

        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        float uiScale = HudScale.scale(fbw, fbh);
        float mx = HudScale.toVirtual((float) mc.mouseHandler.xpos(), uiScale);
        float my = HudScale.toVirtual((float) mc.mouseHandler.ypos(), uiScale);

        RegistryState.LinkWireColors colors = linkWireColors();
        for (DraggableHudElement child : RegistryState.WIDGETS) {
            if (!RegistryWidgetStore.isWidgetVisibleForLinkOverlay(child)) continue;
            if (child == RegistryState.dragging) continue;
            if (child.getParentId() == null || child.getParentSide() == WidgetAnchorSide.NONE) continue;

            DraggableHudElement parent = RegistryWidgetStore.findById(child.getParentId());
            if (!RegistryWidgetStore.isWidgetVisibleForLinkOverlay(parent)) continue;
            if (!parent.contains(mx, my) && !child.contains(mx, my)) continue;
            renderLinkWire(renderer, parent, child, child.getParentSide(),
                    RegistryState.LINK_WIRE_THICKNESS, colors.wireStart(), colors.wireEnd());
        }

        if (RegistryState.dragging == null || RegistryWidgetStore.isWidgetAnchorModifierDown()) return;
        RegistryState.WidgetAnchorCandidate candidate =
                RegistrySnapSupport.findWidgetAnchorCandidate(RegistryState.dragging, screenW, screenH);
        if (candidate == null) return;
        renderLinkWire(renderer, candidate.parent(), RegistryState.dragging, candidate.side(),
                RegistryState.LINK_PREVIEW_THICKNESS, colors.previewStart(), colors.previewEnd());
    }

    private static RegistryState.LinkWireColors linkWireColors() {
        Themes.Theme theme = Theme.theme();
        if (RegistryState.cachedLinkTheme == theme && RegistryState.cachedLinkColors != null) {
            return RegistryState.cachedLinkColors;
        }

        int wireStart = HudRenderUtil.setAlpha(
                HudRenderUtil.mixColor(theme.textMuted(), theme.strokeSoft(), 0.45f), 0x86);
        int wireEnd = HudRenderUtil.setAlpha(
                HudRenderUtil.mixColor(theme.accentSoft(), theme.windowStroke(), 0.24f), 0x78);
        int previewStart = HudRenderUtil.setAlpha(
                HudRenderUtil.mixColor(theme.textPrimary(), theme.accentSoft(), 0.38f), 0xC8);
        int previewEnd = HudRenderUtil.setAlpha(
                HudRenderUtil.mixColor(theme.accent(), theme.accentSoft(), 0.28f), 0xD8);

        RegistryState.cachedLinkTheme = theme;
        RegistryState.cachedLinkColors = new RegistryState.LinkWireColors(wireStart, wireEnd, previewStart, previewEnd);
        return RegistryState.cachedLinkColors;
    }

    private static void renderLinkWire(Renderer2D renderer,
                                       DraggableHudElement parent,
                                       DraggableHudElement child,
                                       WidgetAnchorSide side,
                                       double thickness,
                                       int startArgb,
                                       int endArgb) {
        if (parent == null || child == null || side == null || side == WidgetAnchorSide.NONE) return;
        if (parent.getWidth() <= 0f || parent.getHeight() <= 0f || child.getWidth() <= 0f || child.getHeight() <= 0f) {
            return;
        }

        double parentX = 0.0;
        double parentY = 0.0;
        double childX = 0.0;
        double childY = 0.0;
        double parentCx = 0.0;
        double parentCy = 0.0;
        double childCx = 0.0;
        double childCy = 0.0;

        switch (side) {
            case TOP -> {
                parentX = parent.getX() + parent.getWidth() * 0.5;
                parentY = parent.getY();
                childX = child.getX() + child.getWidth() * 0.5;
                childY = child.getY() + child.getHeight();
                double c = Math.max(14.0, Math.abs(childY - parentY) * 0.45);
                parentCx = parentX;
                parentCy = parentY - c;
                childCx = childX;
                childCy = childY + c;
            }
            case BOTTOM -> {
                parentX = parent.getX() + parent.getWidth() * 0.5;
                parentY = parent.getY() + parent.getHeight();
                childX = child.getX() + child.getWidth() * 0.5;
                childY = child.getY();
                double c = Math.max(14.0, Math.abs(childY - parentY) * 0.45);
                parentCx = parentX;
                parentCy = parentY + c;
                childCx = childX;
                childCy = childY - c;
            }
            case LEFT -> {
                parentX = parent.getX();
                parentY = parent.getY() + parent.getHeight() * 0.5;
                childX = child.getX() + child.getWidth();
                childY = child.getY() + child.getHeight() * 0.5;
                double c = Math.max(14.0, Math.abs(childX - parentX) * 0.45);
                parentCx = parentX - c;
                parentCy = parentY;
                childCx = childX + c;
                childCy = childY;
            }
            case RIGHT -> {
                parentX = parent.getX() + parent.getWidth();
                parentY = parent.getY() + parent.getHeight() * 0.5;
                childX = child.getX();
                childY = child.getY() + child.getHeight() * 0.5;
                double c = Math.max(14.0, Math.abs(childX - parentX) * 0.45);
                parentCx = parentX + c;
                parentCy = parentY;
                childCx = childX - c;
                childCy = childY;
            }
            case NONE -> {
                return;
            }
        }

        renderer.bezierConnectorGradient(childX, childY, childCx, childCy, parentCx, parentCy, parentX, parentY,
                18, thickness, startArgb, endArgb);
    }
}
