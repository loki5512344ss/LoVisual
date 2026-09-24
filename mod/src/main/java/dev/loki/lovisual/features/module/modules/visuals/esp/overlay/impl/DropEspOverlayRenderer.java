package dev.loki.lovisual.features.module.modules.visuals.esp.overlay;

import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.material.MatteHudStyle;
import dev.loki.lovisual.render.helpers.clip.ScreenSpaceOverlay2D;

import java.util.List;

/**
 * Screen-space label/overlay rendering subsystem for {@link DropESP}. Owns the collected entry
 * models, their render-order comparators, the matte label layout and the per-entry frame/plate/icon/
 * text drawing passes. All module settings that influenced the old inlined code are passed in as
 * plain arguments so behaviour stays identical to the original implementation.
 */
final class DropEspOverlayRenderer {
    private static final double MATTE_LABEL_PAD_X = 3.5;
    private static final double MATTE_LABEL_PAD_Y = 2.0;
    private static final double MATTE_ICON_SIZE = 14.0;
    private static final double MATTE_ICON_GAP = 4.0;
    private static final float MATTE_ICON_SCALE = (float) (MATTE_ICON_SIZE / 16.0);

    private DropEspOverlayRenderer() {
    }

    static int compareDropRenderOrder(DropOverlayEntry a, DropOverlayEntry b) {
        int rarity = Integer.compare(a.sortPriority(), b.sortPriority());
        if (rarity != 0) return rarity;
        return Double.compare(b.distSq(), a.distSq());
    }

    static int compareDropRenderOrder(DropVanillaEntry a, DropVanillaEntry b) {
        int rarity = Integer.compare(a.sortPriority(), b.sortPriority());
        if (rarity != 0) return rarity;
        return Double.compare(b.distSq(), a.distSq());
    }

    static void renderOverlayEntryPass(Renderer2D renderer,
                                       TextRenderer labelRenderer,
                                       DropOverlayEntry entry,
                                       boolean matteMode,
                                       double textScale,
                                       boolean frame,
                                       boolean textShadow) {
        ScreenSpaceOverlay2D.ScreenRect rect = entry.rect();
        if (frame) {
            if (matteMode) {
                MatteHudStyle.drawFrame(renderer, rect.minX(), rect.minY(), rect.width(), rect.height(), entry.color(), 1.0f);
            } else {
                ScreenSpaceOverlay2D.drawFrame(renderer, rect, entry.color());
            }
        }

        if (matteMode) {
            DropLabelEntry label = entry.matteLabel();
            if (label == null) return;
            MatteHudStyle.drawPlate(renderer, label.x(), label.y(), label.width(), label.height(), 2.0f, 1.0f);
            if (label.icon()) {
                renderer.item(label.stack(), label.iconX(), label.iconY(), MATTE_ICON_SCALE, 0, Renderer2D.ITEM_OVERLAY_NONE, null);
            }
            renderSingleDropLabel(labelRenderer, label, textScale);
        } else {
            ScreenSpaceOverlay2D.LabelEntry label = entry.label();
            if (label == null) return;
            if (textShadow) {
                ScreenSpaceOverlay2D.renderLabelBackplate(renderer, label);
            }
            renderSingleLabel(labelRenderer, label, false, textScale);
        }

        Renderer2D.flushBatch(Renderer2D.FlushReason.EXPLICIT);
    }

    static void renderSingleDropLabel(TextRenderer labelRenderer, DropLabelEntry label, double textScale) {
        boolean renderStarted = false;
        if (!labelRenderer.isBuilding()) {
            labelRenderer.begin(textScale);
            renderStarted = true;
        }
        try {
            labelRenderer.render(label.text(), label.textX(), label.textY(), new RenderColor(label.color()), false);
        } finally {
            if (renderStarted) labelRenderer.end();
        }
    }

    static void renderSingleLabel(TextRenderer labelRenderer,
                                  ScreenSpaceOverlay2D.LabelEntry label,
                                  boolean shadow,
                                  double textScale) {
        boolean renderStarted = false;
        if (!labelRenderer.isBuilding()) {
            labelRenderer.begin(textScale);
            renderStarted = true;
        }
        try {
            ScreenSpaceOverlay2D.renderLabels(labelRenderer, List.of(label), shadow);
        } finally {
            if (renderStarted) labelRenderer.end();
        }
    }

    static DropLabelEntry createMatteDropLabel(TextRenderer labelRenderer,
                                               ItemStack stack,
                                               String text,
                                               int color,
                                               ScreenSpaceOverlay2D.ScreenRect rect,
                                               boolean icon) {
        double textWidth = labelRenderer.getWidth(text, false);
        double textHeight = labelRenderer.getHeight(false);
        double iconBlock = icon ? MATTE_ICON_SIZE + MATTE_ICON_GAP : 0.0;
        double width = textWidth + iconBlock + MATTE_LABEL_PAD_X * 2.0;
        double height = Math.max(textHeight + MATTE_LABEL_PAD_Y * 2.0, icon ? MATTE_ICON_SIZE + MATTE_LABEL_PAD_Y * 2.0 : 0.0);
        double x = rect.minX() + (rect.width() - width) * 0.5;
        double y = rect.minY() - height - 4.0;
        x = Math.floor(x + 0.5);
        y = Math.floor(y + 0.5);
        double iconX = x + MATTE_LABEL_PAD_X;
        double iconY = y + (height - MATTE_ICON_SIZE) * 0.5;
        double textX = x + MATTE_LABEL_PAD_X + iconBlock;
        double textY = y + (height - textHeight) * 0.5;
        return new DropLabelEntry(stack, text, x, y, width, height, icon, iconX, iconY, textX, textY, color);
    }

    record DropOverlayEntry(int sortPriority, double distSq, ScreenSpaceOverlay2D.ScreenRect rect, int color,
                            DropLabelEntry matteLabel, ScreenSpaceOverlay2D.LabelEntry label) {
    }

    record DropVanillaEntry(int sortPriority, double distSq, ScreenSpaceOverlay2D.LabelEntry label) {
    }

    record DropLabelEntry(ItemStack stack, String text, double x, double y, double width, double height,
                          boolean icon, double iconX, double iconY, double textX, double textY, int color) {
    }
}
