package dev.loki.lovisual.features.gui.hud.nondraggable.impl.hotbar;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.player.Animations;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.mixins.accessors.inventory.PlayerInventoryAccessor;
import dev.loki.lovisual.render.engine.math.ColorMath;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the vanilla-replacement hotbar: base panel(s), offhand divider, glass /
 * blur backdrop, sliding selection stroke and every item icon. Owns the fixed
 * pixel metrics (radius, dividers, slot pitch) so the module facade stays free
 * of drawing concerns; per-frame selection state lives in {@link HotbarSelection}.
 */
final class HotbarRenderer {

    private static final int BG_ALPHA = 0xAA;
    private static final float RADIUS = 6.0f;
    private static final float HOTBAR_SOFTNESS = 0.0f;
    private static final float BLUR_QUALITY = 16.0f;
    private static final float DIVIDER_WIDTH_PX = 1.05f;
    private static final float DIVIDER_HEIGHT = 11.0f;
    private static final float SELECT_STROKE_PX = 1.45f;
    private static final float SELECT_STROKE_SOFTNESS = 0.85f;
    private static final float SELECT_FILL_ALPHA = 0.16f;
    private static final float SLOT_STEP = 20.0f;

    private HotbarRenderer() {
    }

    /** Paint the panel backdrop (glass / blur / flat base) plus the sliding selection box. */
    static void renderBackground(Renderer2D r2d, Minecraft mc) {
        LocalPlayer player = mc.player;
        CustomHotbar bm = CustomHotbar.get();
        if (player == null || bm == null || !bm.isHudHotbarEnabled()) return;

        float s = (float) mc.getWindow().getGuiScale();
        float cx = mc.getWindow().getGuiScaledWidth() / 2f;
        float baseY = mc.getWindow().getGuiScaledHeight() - 22f + hotbarRaiseOffset();
        boolean hasOffhand = !player.getOffhandItem().isEmpty();
        boolean merged = bm.getHotbarOffHandMode() == CustomHotbar.OffHandMode.MERGED;

        int bgRgb = bm.getHotbarBgRgb();
        int bgAlpha = bm.getHotbarBgAlpha();
        boolean blur = bm.isHotbarBlurEnabled();
        boolean glass = bm.isHotbarGlassEnabled();
        int bgColor = glass ? HudRenderUtil.glassPanelBackground(1.0f) : ColorMath.premultiplyAlpha(bgRgb, bgAlpha);
        int accentAlpha = bm.getHotbarAccentAlpha();
        int dividerColor = ColorMath.premultiplyAlpha(bm.getHotbarDividerRgb(), accentAlpha);
        HotbarSelectionGradient.SelectionGradient selectionGradient = HotbarSelectionGradient.resolve(bm, accentAlpha);

        float[][] panels = panelsFor(cx, baseY, s, hasOffhand, merged);
        float r = RADIUS * s;
        for (float[] p : panels) {
            if (glass) drawGlass(p[0], p[1], p[2], p[3], r);
            else if (blur) drawBlur(p[0], p[1], p[2], p[3], r, bgRgb);
            drawBase(r2d, p[0], p[1], p[2], p[3], bgColor, s);
        }
        if (hasOffhand && merged) {
            float divX = (cx - 91f) * s;
            float divY = (baseY + (20f - DIVIDER_HEIGHT) * 0.5f) * s;
            r2d.quad(divX, divY, DIVIDER_WIDTH_PX, DIVIDER_HEIGHT * s, dividerColor);
        }

        HotbarSelection.Frame frame = HotbarSelection.update(getSelectedSlot(player));
        float selX = (cx - 88f + frame.visualSlot() * SLOT_STEP) * s;
        float selY = (baseY + 1.5f) * s;
        drawSelectionStroke(r2d, selX, selY, 17f * s, 17f * s, s,
                selectionGradient.startArgb(), selectionGradient.endArgb(), selectionGradient.angleDeg());
    }

    /** Panels (x, y, w, h in device pixels) covering the hotbar for the current off-hand mode. */
    private static float[][] panelsFor(float cx, float baseY, float s, boolean hasOffhand, boolean merged) {
        float y = baseY * s;
        float h = 20f * s;
        if (!hasOffhand) return new float[][]{{(cx - 90f) * s, y, 180f * s, h}};
        if (merged) return new float[][]{{(cx - 111f) * s, y, 201f * s, h}};
        return new float[][]{{(cx - 90f) * s, y, 180f * s, h}, {(cx - 112.5f) * s, y, 20f * s, h}};
    }

    /** Draw every item icon (off-hand + 9 hotbar slots) with vanilla bobbing. */
    static void renderItems(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, Minecraft mc) {
        LocalPlayer player = mc.player;
        CustomHotbar bm = CustomHotbar.get();
        if (player == null || bm == null || !bm.isHudHotbarEnabled()) return;

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        float baseY = mc.getWindow().getGuiScaledHeight() - 22 + hotbarRaiseOffset();
        HotbarSelection.Frame frame = HotbarSelection.update(getSelectedSlot(player));

        int seed = 1;
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            int x = (bm.getHotbarOffHandMode() == CustomHotbar.OffHandMode.MERGED) ? (cx - 109) : (cx - 111);
            renderHotbarItem(ctx, tickCounter, offhand, x, baseY + HotbarSelection.ITEM_Y_OFFSET, seed++);
        }
        for (int slot = 0; slot < 9; slot++) {
            double x = cx - 90.0 + slot * SLOT_STEP + 2.0;
            double y = baseY + HotbarSelection.animatedItemYOffset(slot, frame);
            renderHotbarItem(ctx, tickCounter, player.getInventory().getItem(slot), x, y, seed++);
        }
    }

    /** Vanilla-equivalent item draw with pop / bob handling. */
    private static void renderHotbarItem(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, ItemStack stack, double x, double y, int seed) {
        if (stack.isEmpty()) return;
        float f = (float) stack.getPopTime() - tickCounter.getGameTimeDeltaPartialTick(false);
        if (f > 0.0F) {
            float g = 1.0F + f / 5.0F;
            Renderer2D.COLOR.itemPivot(stack, x, y, 1.0f / g, (g + 1.0f) / 2.0f, 8.0f, 12.0f, seed, Renderer2D.ITEM_OVERLAY_NONE, null);
            Renderer2D.COLOR.itemOverlay(stack, x, y, 1.0f, Renderer2D.ITEM_OVERLAY_ALL, null);
        } else {
            Renderer2D.COLOR.item(stack, x, y, 1.0f, seed, Renderer2D.ITEM_OVERLAY_ALL, null);
        }
    }

    private static void drawBase(Renderer2D r2d, float x, float y, float w, float h, int argb, float scale) {
        if (RADIUS <= 0.0f) {
            r2d.quad(x, y, w, h, argb);
            return;
        }
        r2d.roundedRect(x, y, w, h, RADIUS * scale, HOTBAR_SOFTNESS * scale, argb);
    }

    /** Animated rectangular gradient + rotating angular stroke + soft halo around the selected slot. */
    private static void drawSelectionStroke(Renderer2D r2d, float x, float y, float w, float h, float scale,
                                            int startArgb, int endArgb, float gradientAngleDeg) {
        if (r2d == null) return;
        float radius = Math.max(3.0f * scale, Math.min(w, h) * 0.22f);
        float thickness = Math.max(1.15f, SELECT_STROKE_PX * Math.min(scale, 1.35f));
        float softness = SELECT_STROKE_SOFTNESS * Math.min(scale, 1.25f);
        int fillStart = HudRenderUtil.scaleAlpha(startArgb, SELECT_FILL_ALPHA);
        int fillEnd = HudRenderUtil.scaleAlpha(endArgb, SELECT_FILL_ALPHA * 0.82f);
        r2d.roundedRectGradient(x, y, w, h, radius, softness, fillStart, fillEnd, gradientAngleDeg);

        float t = (Util.getMillis() % 2200L) / 2200.0f;
        r2d.roundedRectStrokeAngularGradient(x, y, w, h, radius, softness, thickness, startArgb, endArgb, t);
        int halo = HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(startArgb, endArgb, 0.5f), 0.22f);
        r2d.roundedRectStroke(x, y, w, h, radius, softness + 0.4f, thickness + 0.55f, halo);
    }

    private static void drawBlur(float x, float y, float w, float h, float radius, int tintRgb) {
        CustomHotbar bm = CustomHotbar.get();
        float alpha = bm != null ? bm.getHotbarBlurAlpha() / 255f : 0.5f;
        Renderer2D.COLOR.blurRect(x, y, w, h, radius, BLUR_QUALITY, 1.0f, alpha, 0xFFFFFF);
    }

    private static void drawGlass(float x, float y, float w, float h, float radius) {
        CustomHotbar bm = CustomHotbar.get();
        float blurStrength = bm != null ? bm.getHotbarBlurAlpha() / 255f : 0.5f;
        float glassScale = RADIUS <= 0.0f ? 1.0f : radius / RADIUS;
        HudRenderUtil.drawLiquidGlass(x, y, w, h, radius, glassScale, true, blurStrength, 1.0f);
    }

    private static int getSelectedSlot(LocalPlayer player) {
        return ((PlayerInventoryAccessor) player.getInventory()).lovisual$getSelectedSlot();
    }

    /**
     * Vertical GUI-pixel offset applied to the whole hotbar while the
     * {@link Animations} module slides it into place. The animation itself is
     * advanced once per frame inside the module; this only reads the settled value.
     */
    private static float hotbarRaiseOffset() {
        Animations animations = Modules.get(Animations.class);
        return animations != null ? animations.getHotbarRaiseOffset() : 0.0f;
    }
}
