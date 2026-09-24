package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.helpers.clip.ClipFunction;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class ItemizerRenderer {

    private ItemizerRenderer() {}

    static void drawCards(Itemizer host, Renderer2D renderer, List<Itemizer.Entry> visible, float x, float y, float scale, float globalAlpha) {
        if (renderer == null || visible == null || visible.isEmpty()) return;
        float entryW = ItemizerModel.baseEntryWidth(host) * scale;
        float entryH = ItemizerModel.baseEntryHeight(host) * scale;
        float gap = Itemizer.BASE_GAP * scale;
        for (int i = 0; i < visible.size(); i++) {
            Itemizer.Entry entry = visible.get(i);
            float cardX = x;
            float cardY = y;
            if (host.direction.get() == Itemizer.ItemizerDirection.HORIZONTAL) {
                cardX += i * (entryW + gap);
            } else {
                cardY += i * (entryH + gap);
            }
            float alpha = AnimationUtility.clamp01(globalAlpha * entry.anim);
            if (alpha <= 0.001f) continue;
            float radius = Math.max(3.0f * scale, Math.min(entryW, entryH) * 0.24f);
            drawGlassCard(host, renderer, cardX, cardY, entryW, entryH, radius, scale, alpha);
            drawClippedTimeStrip(host, renderer, entry, cardX, cardY, entryW, entryH, radius, scale, alpha);
            queueItem(host, entry, cardX, cardY, entryW, entryH, scale, alpha);
        }
    }

    static void drawGlassCard(Itemizer host, Renderer2D renderer, float x, float y, float width, float height, float radius, float scale, float alpha) {
        if (width <= 0.0f || height <= 0.0f || alpha <= 0.001f) return;
        float blurStrength = host.blur.get() ? (host.bgAlpha.get() / 255.0f) * alpha : 0.0f;
        HudRenderUtil.drawLiquidGlass(x, y, width, height, radius, Math.max(0.001f, scale), true, blurStrength, alpha);
        int veil = HudRenderUtil.glassSmallBackground(alpha);
        renderer.roundedRectCorners(x, y, width, height, radius, radius, radius, radius, 1.0f, veil);
    }

    static void drawClippedTimeStrip(Itemizer host, Renderer2D renderer, Itemizer.Entry entry, float x, float y, float width, float height, float cardRadius, float scale, float alpha) {
        boolean clipped = ClipFunction.pushRoundedRect(x, y, width, height, cardRadius);
        if (!clipped) return;
        try {
            drawTimeStrip(host, renderer, entry, x, y, width, height, cardRadius, scale, alpha);
        } finally {
            ClipFunction.pop();
        }
    }

    static void drawTimeStrip(Itemizer host, Renderer2D renderer, Itemizer.Entry entry, float x, float y, float width, float height, float cardRadius, float scale, float alpha) {
        if (renderer == null || entry == null || width <= 0.0f || height <= 0.0f || alpha <= 0.001f) return;
        float life = entry.totalLife <= 0.0f ? 1.0f : AnimationUtility.clamp01(entry.life / entry.totalLife);
        if (life <= 0.001f) return;
        float stripH = Math.max(1.45f * scale, Math.min(3.0f * scale, height * 0.13f));
        float bleed = Math.max(0.65f, 0.45f * scale);
        float stripDrawH = stripH + bleed;
        float stripY = y + height - stripH;
        float bottomR = Math.min(cardRadius, stripDrawH);
        int accent = ItemizerProps.entryAccent(host, entry.event);
        int trackTop = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(0xFF07090D, accent, 0.08f), 0.30f * alpha);
        int trackBottom = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(0xFF020305, accent, 0.18f), 0.48f * alpha);
        renderer.roundedRectCornersQuad(x, stripY, width, stripDrawH, 0.0f, 0.0f, bottomR, bottomR, 0.0f, trackTop, trackTop, trackBottom, trackBottom);
        float fillW = Math.max(0.0f, width * life);
        if (fillW <= 0.35f) return;
        float fillRLeft = Math.min(bottomR, fillW * 0.5f);
        float fillRRight = life >= 0.985f ? bottomR : Math.min(stripDrawH * 0.5f, fillW * 0.5f);
        int fillTopLeft = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFFFFFFFF, 0.28f), 0.92f * alpha);
        int fillTopRight = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFFFFFFFF, 0.10f), 0.84f * alpha);
        int fillBottomRight = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFF05070A, 0.22f), 0.78f * alpha);
        int fillBottomLeft = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(accent, 0xFFFFFFFF, 0.16f), 0.88f * alpha);
        renderer.roundedRectCornersQuad(x, stripY, fillW, stripDrawH, 0.0f, life >= 0.985f ? 0.0f : fillRRight, fillRRight, fillRLeft, 0.0f, fillTopLeft, fillTopRight, fillBottomRight, fillBottomLeft);
    }

    static void queueItem(Itemizer host, Itemizer.Entry entry, float cardX, float cardY, float cardW, float cardH, float scale, float alpha) {
        if (entry == null || entry.stack == null || entry.stack.isEmpty()) return;
        boolean compact = host.displayMode.get() == Itemizer.ItemizerDisplayMode.COMPACT;
        float iconSize = compact ? Math.min(16.0f * scale, cardH - 7.0f * scale) : Math.min(Math.min(16.0f * scale, cardW - 8.0f * scale), cardH - 8.0f * scale);
        if (iconSize <= 0.0f) return;
        float iconX = compact ? cardX + 4.5f * scale : cardX + (cardW - iconSize) * 0.5f;
        float iconY = cardY + (cardH - iconSize) * 0.5f - 0.5f * scale;
        int overlayFlags = compact ? Renderer2D.ITEM_OVERLAY_ALL : Renderer2D.ITEM_OVERLAY_NONE;
        host.itemTasks.add(new Itemizer.ItemRenderTask(entry.stack.copy(), iconX, iconY, Math.max(0.05f, iconSize / 16.0f), overlayFlags, alpha));
    }

    static void drawItem(Renderer2D renderer, ItemStack stack, float x, float y, float itemScale, int seed, int overlayFlags, float alpha) {
        if (renderer == null || stack == null || stack.isEmpty() || alpha <= 0.001f) return;
        double previousAlpha = renderer.getAlpha();
        renderer.setAlpha(previousAlpha * AnimationUtility.clamp01(alpha));
        try {
            renderer.item(stack, x, y, itemScale, seed, overlayFlags, null);
        } finally {
            renderer.setAlpha(previousAlpha);
        }
    }
}
