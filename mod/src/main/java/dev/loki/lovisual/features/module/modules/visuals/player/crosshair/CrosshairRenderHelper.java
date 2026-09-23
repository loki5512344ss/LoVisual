/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.player;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.util.Mth;

final class CrosshairRenderHelper {

    private static final int OUTLINE_COLOR = 0xFF000000;
    private static final int RIPTIDE_BLOCK_COLOR = 0xFFFF3B3B;
    private static final float OUTLINE_EXPAND = 0.5f;
    private static final float GAP_SMOOTHING = 0.15f;
    private static final float RIPTIDE_CROSS_LENGTH_FACTOR = 0.65f;
    private static final float PINWHEEL_SPIN_DEGREES = 130f;

    private final Crosshair host;
    private float animatedGap;

    CrosshairRenderHelper(Crosshair host) {
        this.host = host;
    }

    float outlineExpand(float scale) {
        return OUTLINE_EXPAND * scale;
    }

    int outlineColor() {
        return OUTLINE_COLOR;
    }

    int riptideColor() {
        return RIPTIDE_BLOCK_COLOR;
    }

    float updateGap(float targetGap, float scale) {
        animatedGap += (targetGap - animatedGap) * GAP_SMOOTHING;
        return animatedGap * scale;
    }

    void reset() {
        animatedGap = 0f;
    }

    float computeCooldown() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (!host.dynamicGap.get()) return 0f;
        float charge = host.combat.getChargeProgress(mc.player);
        if (charge >= 0.0f) return 1.0f - charge;
        float progress = host.combat.getAttackCooldownProgress();
        progress = Mth.clamp(progress + 0.06f, 0.0f, 1.0f);
        return 1.0f - progress;
    }

    void renderPinwheel(Renderer2D renderer, float centerX, float centerY, float thicknessPx, float lengthPx, int color) {
        float chargeProgress = Mth.clamp(host.combat.getAttackCooldownProgress(), 0.0f, 1.0f);
        float spinDeg = (1.0f - chargeProgress) * PINWHEEL_SPIN_DEGREES;
        for (int i = 0; i < 4; i++) {
            float bladeAngleDeg = 45f + i * 90f + spinDeg;
            drawRotatedBlade(renderer, centerX, centerY, thicknessPx, lengthPx, bladeAngleDeg, color);
        }
    }

    private void drawRotatedBlade(Renderer2D renderer, float cx, float cy, float thicknessPx, float lengthPx, float angleDeg, int color) {
        double rad = Math.toRadians(angleDeg);
        double dirX = Math.cos(rad);
        double dirY = Math.sin(rad);
        double perpX = -dirY;
        double perpY = dirX;
        double innerX = cx;
        double innerY = cy;
        double outerX = cx + dirX * lengthPx;
        double outerY = cy + dirY * lengthPx;
        double halfThickness = thicknessPx * 0.5;
        double[] points = new double[]{
                innerX - perpX * halfThickness, innerY - perpY * halfThickness,
                outerX - perpX * halfThickness, outerY - perpY * halfThickness,
                outerX + perpX * halfThickness, outerY + perpY * halfThickness,
                innerX + perpX * halfThickness, innerY + perpY * halfThickness
        };
        renderer.polygon(points, 4, color);
    }

    void renderRiptideBlockedCross(Renderer2D renderer, float centerX, float centerY, float thicknessPx, float lengthPx, boolean outline, float outlinePx, int color) {
        float crossLength = lengthPx * RIPTIDE_CROSS_LENGTH_FACTOR;
        double hX = centerX - crossLength;
        double hY = centerY - thicknessPx * 0.5f;
        double vX = centerX - thicknessPx * 0.5f;
        double vY = centerY - crossLength;
        drawSegment(renderer, hX, hY, crossLength * 2.0f, thicknessPx, color, outline, outlinePx);
        drawSegment(renderer, vX, vY, thicknessPx, crossLength * 2.0f, color, outline, outlinePx);
    }

    void drawSegment(Renderer2D renderer, double x, double y, double w, double h, int color, boolean outline, float outlinePx) {
        if (outline) renderer.quad(x - outlinePx, y - outlinePx, w + outlinePx * 2.0f, h + outlinePx * 2.0f, OUTLINE_COLOR);
        renderer.quad(x, y, w, h, color);
    }
}
