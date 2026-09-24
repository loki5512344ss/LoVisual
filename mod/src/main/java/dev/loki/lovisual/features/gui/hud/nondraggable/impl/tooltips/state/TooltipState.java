package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tooltips;

import dev.loki.lovisual.features.theme.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TooltipState {

    private static List<TooltipLine> lines;
    private static ClientTooltipPositioner positioner;
    private static int mouseX;
    private static int mouseY;
    private static ItemStack stack;
    private static GuiGraphicsExtractor ctx;
    private static float scaleOverride = 1.0f;
    private static ItemStack lastStack;

    private TooltipState() {}
    static void beginFrame() {
        lines = null;
        positioner = null;
        stack = null;
        ctx = null;
        lastStack = null;
        scaleOverride = 1.0f;
    }

    static boolean hasTooltip() {
        return lines != null && !lines.isEmpty();
    }

    static void captureOrdered(List<? extends FormattedCharSequence> src,
                               ClientTooltipPositioner pos, int x, int y) {
        if (src == null || src.isEmpty()) return;
        lines = convertOrdered(src);
        positioner = pos != null ? pos : DefaultTooltipPositioner.INSTANCE;
        mouseX = x;
        mouseY = y;
        stack = ItemStack.EMPTY;
    }

    static void captureItemOrdered(List<? extends FormattedCharSequence> src,
                                   ClientTooltipPositioner pos, int x, int y,
                                   ItemStack s, GuiGraphicsExtractor c) {
        if (src == null || src.isEmpty()) return;
        lines = convertOrdered(src);
        positioner = pos != null ? pos : DefaultTooltipPositioner.INSTANCE;
        mouseX = x;
        mouseY = y;
        stack = s;
        ctx = c;
    }

    static void setDrawContext(GuiGraphicsExtractor c) {
        ctx = c;
    }

    static void setLastStack(ItemStack s) {
        lastStack = s;
    }

    static ItemStack consumeLastStack() {
        ItemStack s = lastStack;
        lastStack = null;
        return s;
    }

    static void setScaleOverride(float scale) {
        scaleOverride = Math.max(0.5f, Math.min(1.5f, scale));
    }

    static void resetScaleOverride() {
        scaleOverride = 1.0f;
    }

    static List<TooltipLine> getLines() {
        return lines;
    }

    static ClientTooltipPositioner getPositioner() {
        return positioner;
    }

    static int getMouseX() {
        return mouseX;
    }

    static int getMouseY() {
        return mouseY;
    }

    static ItemStack getStack() {
        return stack;
    }

    static GuiGraphicsExtractor getCtx() {
        return ctx;
    }

    static float getScaleOverride() {
        return scaleOverride;
    }

    static List<TooltipLine> convertOrdered(List<? extends FormattedCharSequence> src) {
        List<TooltipLine> out = new ArrayList<>();
        int fallback = Theme.theme().textPrimary();
        for (FormattedCharSequence line : src) {
            StringBuilder sb = new StringBuilder();
            int[] color = new int[]{0};
            line.accept((idx, style, cp) -> {
                sb.appendCodePoint(cp);
                if (color[0] == 0 && style != null && style.getColor() != null) {
                    color[0] = 0xFF000000 | style.getColor().getValue();
                }
                return true;
            });
            if (sb.length() == 0) sb.append(' ');
            int c = color[0] != 0 ? color[0] : fallback;
            out.add(new TooltipLine(sb.toString(), c));
        }
        return out;
    }

    static List<TooltipLine> prepareLines(List<TooltipLine> in, boolean previewOpen) {
        if (in == null || in.isEmpty()) return in;
        boolean isShulker = ShulkerPreviewCache.isShulkerBox(stack);
        boolean showHint = shouldShowHint(isShulker);
        if (!previewOpen && !showHint) return in;
        List<TooltipLine> out = new ArrayList<>(in.size() + (showHint ? 1 : 0));
        for (TooltipLine line : in) {
            if (previewOpen && isShulker && (isShulkerMoreLine(line.text()) || isShulkerItemLine(line.text()))) {
                continue;
            }
            out.add(line);
        }
        if (showHint) {
            out.add(new TooltipLine(buildHint(), ShulkerPreviewCache.SHULKER_HINT_COLOR));
        }
        return out;
    }

    private static boolean shouldShowHint(boolean isShulker) {
        if (!isShulker) return false;
        BetterTooltips b = BetterTooltips.get();
        if (b == null || !b.isShulkerPreviewEnabled()) return false;
        if (!ShulkerPreviewCache.hasShulkerContents(stack)) return false;
        return !b.isShulkerPreviewHeld();
    }

    private static String buildHint() {
        String bind = BetterTooltips.get().getShulkerPreviewBind();
        return "<<" + bind + ">> показать содержимое";
    }

    private static boolean isShulkerMoreLine(String text) {
        if (text == null) return false;
        String t = text.trim();
        if (t.isEmpty()) return false;
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.startsWith("and ") || lower.startsWith("и ")) {
            return t.contains("...") || t.contains("…");
        }
        if (lower.startsWith("и еще") || lower.startsWith("и ещё")) {
            return t.contains("...") || t.contains("…");
        }
        return false;
    }

    private static boolean isShulkerItemLine(String text) {
        if (text == null) return false;
        String t = text.trim();
        if (t.isEmpty()) return false;
        int l = t.lastIndexOf('(');
        int r = t.lastIndexOf(')');
        if (l < 0 || r <= l) return false;
        for (int i = l + 1; i < r; i++) {
            char c = t.charAt(i);
            if (c >= '0' && c <= '9') return true;
        }
        return false;
    }
}

record TooltipLine(String text, int color) {
}
