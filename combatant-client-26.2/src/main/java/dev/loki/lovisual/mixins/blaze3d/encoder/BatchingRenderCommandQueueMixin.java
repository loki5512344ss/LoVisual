/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.blaze3d.encoder;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import dev.loki.lovisual.render.fade.ViewObstructionFadeContext;
import dev.loki.lovisual.render.engine.msaa.MsaaWorldTarget;

import java.util.Arrays;

@Mixin(SubmitNodeCollection.class)
public abstract class BatchingRenderCommandQueueMixin {
    @Unique
    private static final int LOVISUAL_VIEW_FADE_DITHER_MARK = 0x00010001;
    @Unique
    private static final int LOVISUAL_VIEW_FADE_A2C_MARK = 0x00010100;

    @Unique
    private static int lovisual$markViewObstructionFade(int argb, float alpha, boolean msaaActive) {
        int rgb = (argb & 0x00FEFEFE) | (msaaActive ? LOVISUAL_VIEW_FADE_A2C_MARK : LOVISUAL_VIEW_FADE_DITHER_MARK);
        int alphaByte = Math.round(255.0f * Mth.clamp(alpha, 0.0f, 1.0f));
        return rgb | ((alphaByte & 0xFF) << 24);
    }

    @Unique
    private static boolean lovisual$shouldApplyViewObstructionFade() {
        return ViewObstructionFadeContext.isActive() && ViewObstructionFadeContext.alpha() < 0.99f;
    }

    @Unique
    private static int lovisual$applyFadeToArgb(int color) {
        int baseColor = color == -1 ? 0xFFFFFFFF : ((color & 0x00FFFFFF) | 0xFF000000);
        return lovisual$markViewObstructionFade(baseColor, ViewObstructionFadeContext.alpha(), MsaaWorldTarget.isActive());
    }

    @ModifyVariable(method = "submitModel", at = @At("HEAD"), argsOnly = true, index = 7)
    private int lovisual$applyViewObstructionFadeAlpha(int tintedColor) {
        if (!lovisual$shouldApplyViewObstructionFade()) {
            return tintedColor;
        }
        return lovisual$applyFadeToArgb(tintedColor);
    }

    @ModifyVariable(method = "submitItem", at = @At("HEAD"), argsOnly = true, index = 6)
    private int[] lovisual$applyViewObstructionFadeAlphaToItemTints(int[] tints) {
        if (!lovisual$shouldApplyViewObstructionFade()) {
            return tints;
        }

        int sentinel = lovisual$applyFadeToArgb(-1);
        if (tints == null || tints.length == 0) {
            return new int[]{sentinel};
        }

        int[] faded = Arrays.copyOf(tints, tints.length);
        faded[0] = lovisual$applyFadeToArgb(faded[0]);
        for (int i = 1; i < faded.length; i++) {
            faded[i] = lovisual$applyFadeToArgb(faded[i]);
        }
        return faded;
    }

}
