/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.gui.hud.HudRenderUtil;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.core.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.SvgRenderOptions;
import dev.loki.lovisual.util.proxy.ProxyBackend;
import dev.loki.lovisual.util.proxy.ProxyEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class JoinMultiplayerScreenMixin {
    @Unique
    private static final float LOVISUAL_PROXY_BUTTON_X = 8f;
    @Unique
    private static final float LOVISUAL_PROXY_BUTTON_Y = 8f;
    @Unique
    private static final float LOVISUAL_PROXY_BUTTON_W = 24f;
    @Unique
    private static final float LOVISUAL_PROXY_BUTTON_H = 24f;
    @Unique
    private float lovisual$proxyHover;
    @Unique
    private float lovisual$proxyPress;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("RETURN"))
    private void lovisual$renderProxyEntry(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!(self instanceof JoinMultiplayerScreen)) return;
        float dt = AnimationUtility.deltaTime();
        boolean hovered = lovisual$inside(mouseX, mouseY, LOVISUAL_PROXY_BUTTON_X, LOVISUAL_PROXY_BUTTON_Y,
                LOVISUAL_PROXY_BUTTON_W, LOVISUAL_PROXY_BUTTON_H);
        lovisual$proxyHover = lovisual$animate(lovisual$proxyHover, hovered ? 1f : 0f, dt, 11f);
        lovisual$proxyPress = lovisual$animate(lovisual$proxyPress, 0f, dt, 8f);

        ViewportContext.beginScaled(context);
        Renderer2D.COLOR.begin();
        try {
            lovisual$drawProxyButton();
        } finally {
            Renderer2D.COLOR.render();
            ViewportContext.end(context);
        }
    }

    @Unique
    private void lovisual$drawProxyButton() {
        float hover = AnimationUtility.easeOutCubic(lovisual$proxyHover);
        float press = AnimationUtility.easeOutCubic(lovisual$proxyPress);
        Themes.Theme theme = Theme.theme();
        boolean enabled = ProxyBackend.isEnabled();
        ProxyEntry active = ProxyBackend.activeProxy();
        boolean configured = active.isConfigured();

        float size = LOVISUAL_PROXY_BUTTON_W * (1.0f + hover * 0.035f - press * 0.025f);
        float x = LOVISUAL_PROXY_BUTTON_X + (LOVISUAL_PROXY_BUTTON_W - size) * 0.5f;
        float y = LOVISUAL_PROXY_BUTTON_Y + (LOVISUAL_PROXY_BUTTON_H - size) * 0.5f - hover * 0.35f;
        float radius = 5.0f;
        float centerX = x + size * 0.5f;
        float centerY = y + size * 0.5f;
        int accent = enabled && configured ? theme.accent() : 0x8A94A3;
        int baseTop = enabled && configured ? 0x222A20 : 0x1B2028;
        int baseBottom = enabled && configured ? 0x121812 : 0x10141B;
        int fillTop = HudRenderUtil.mixColor(withAlpha(baseTop, 226), withAlpha(accent, 96), (enabled && configured ? 0.18f : 0.06f) + hover * 0.12f);
        int fillBottom = HudRenderUtil.mixColor(withAlpha(baseBottom, 236), withAlpha(accent, 70), (enabled && configured ? 0.12f : 0.04f) + hover * 0.08f);
        int stroke = HudRenderUtil.mixColor(withAlpha(0x46505D, 180), withAlpha(accent, 218), (enabled && configured ? 0.38f : 0.10f) + hover * 0.35f);
        int iconColor = enabled && configured
                ? HudRenderUtil.mixColor(withAlpha(0xFFFFFF, 245), withAlpha(theme.accent(), 255), 0.18f)
                : withAlpha(0xC5CCD6, 235);

        Renderer2D.COLOR.roundedRectSoftShadow(x, y + 0.8f, size, size, radius,
                7.5f, 0.026f + hover * 0.012f, withAlpha(0x060810, 96));
        Renderer2D.COLOR.roundedRectGradientQuad(x, y, size, size, radius, 1f,
                fillTop, fillTop, fillBottom, fillBottom);
        Renderer2D.COLOR.roundedRectStrokeGradient(x + 0.5f, y + 0.5f, size - 1.0f, size - 1.0f,
                radius - 0.5f, 1f, 0.55f, HudRenderUtil.mixColor(stroke, 0xFFFFFFFF, 0.08f), stroke, 90f);
        Renderer2D.COLOR.svg("shield-user", centerX - 5.6f, centerY - 5.6f, 11.2f, 11.2f, SvgRenderOptions.overrideColor(iconColor));
    }

    @Unique
    private static float lovisual$animate(float current, float target, float dt, float speed) {
        float next = AnimationUtility.approach(current, target, dt, speed);
        return AnimationUtility.snap(next, target, 0.001f);
    }

    @Unique
    private static boolean lovisual$inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Unique
    private static int withAlpha(int rgb, int alpha) {
        return (rgb & 0x00FFFFFF) | (Mth.clamp(alpha, 0, 255) << 24);
    }

}
