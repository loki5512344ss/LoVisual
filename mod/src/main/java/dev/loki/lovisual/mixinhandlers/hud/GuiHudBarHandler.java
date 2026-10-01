package dev.loki.lovisual.mixinhandlers.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomBar;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomHealthBar;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomXpBarRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.jump.CustomJumpBarRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.locator.panel.CustomLocatorRenderer;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.hotbar.CustomHotbar;
import dev.loki.lovisual.mixininterface.render.IGuiGraphics;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.core.frame.RenderPhase;
import dev.loki.lovisual.render.engine.core.frame.RenderPhaseScope;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;

/**
 * Custom HUD replacement rendering for {@link GuiMixin}: hotbar, XP/jump/locator bars and the
 * health bar. All passes are emitted through a deferred {@link Renderer2D} layer so they land after
 * vanilla misc overlays, then drop a stratum marker via {@link IGuiGraphics}.
 */
public final class GuiHudBarHandler {
    private GuiHudBarHandler() {
    }

    public static final Renderer2D.Deferred2DLayer VANILLA_HUD_REPLACEMENT_LAYER =
            Renderer2D.Deferred2DLayer.HUD_AFTER_MISC_OVERLAYS;

    public static void renderHotbar(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        CustomHotbar hotbar = CustomHotbar.get();
        CustomBar bar = CustomBar.get();
        if (hotbar == null && bar == null) return;

        boolean hotbarEnabled = hotbar != null && hotbar.isHudHotbarEnabled();
        boolean barEnabled = bar != null && bar.isHudBarEnabled() && bar.isHotbarXpBarEnabled();
        if (!hotbarEnabled && !barEnabled) return;
        if (!hotbarEnabled) return;

        boolean useJump = barEnabled && CustomJumpBarRenderer.shouldUseJumpBar(mc);
        boolean useLocator = barEnabled && !useJump && CustomBar.shouldUseLocator(mc, bar);
        boolean drawXp = barEnabled && !useJump && !useLocator;

        LoVisualRenderSystem.ensureFrameContext();
        try (RenderPhaseScope ignored = LoVisualRenderSystem.phase(RenderPhase.HUD_MAIN, "2d:hud:custom_hotbar")) {
            Renderer2D.withDeferredLayer(VANILLA_HUD_REPLACEMENT_LAYER, () -> {
                ViewportContext.beginUnscaled(ctx);
                Renderer2D.COLOR.begin();
                if (hotbarEnabled) {
                    CustomHotbar.renderBackground(Renderer2D.COLOR, mc);
                }
                if (drawXp) {
                    CustomXpBarRenderer.renderXpBarBackground(Renderer2D.COLOR, mc);
                }
                if (barEnabled && useJump) {
                    CustomJumpBarRenderer.renderJumpBarBackground(Renderer2D.COLOR, mc);
                }
                if (barEnabled && useLocator) {
                    CustomLocatorRenderer.renderLocatorBackground(Renderer2D.COLOR, mc);
                }
                Renderer2D.COLOR.render();
                ViewportContext.end(ctx);

                if (hotbarEnabled) {
                    CustomHotbar.renderItems(ctx, tickCounter, mc);
                }
            });
            appendVanillaHudReplacementMarker(ctx);

            if (drawXp) {
                CustomXpBarRenderer.renderXpBarText(ctx, mc);
            }
            if (hotbarEnabled) {
                ci.cancel();
            }
        }
    }

    public static void afterVanillaHotbar(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        CustomHotbar hotbar = CustomHotbar.get();
        if (hotbar != null && hotbar.isHudHotbarEnabled()) return;

        CustomBar bar = CustomBar.get();
        if (bar == null || !bar.isHudBarEnabled() || !bar.isHotbarXpBarEnabled()) return;

        renderCustomBarOnly(ctx, tickCounter, mc, bar);
    }

    public static void renderHealth(GuiGraphicsExtractor context, Player player, int x, int y, float maxHealth, CallbackInfo ci) {
        if (!Lifecycle.canRunHud()) return;
        if (!(player instanceof LocalPlayer clientPlayer)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        if (!CustomHealthBar.shouldRenderHealthBar(mc, clientPlayer, maxHealth)) return;

        LoVisualRenderSystem.ensureFrameContext();
        try (RenderPhaseScope ignored = LoVisualRenderSystem.phase(RenderPhase.HUD_MAIN, "2d:hud:custom_health")) {
            Renderer2D.withDeferredLayer(VANILLA_HUD_REPLACEMENT_LAYER, () -> {
                ViewportContext.beginUnscaled(context);
                Renderer2D.COLOR.begin();
                CustomHealthBar.HealthTextInfo textInfo =
                        CustomHealthBar.renderHealthBarBackground(
                                Renderer2D.COLOR,
                                mc,
                                clientPlayer,
                                x,
                                y,
                                maxHealth
                        );
                CustomHealthBar.renderHealthBarText(textInfo);
                Renderer2D.COLOR.render();
                ViewportContext.end(context);
            });
            appendVanillaHudReplacementMarker(context);
            ci.cancel();
        }
    }

    private static void renderCustomBarOnly(GuiGraphicsExtractor ctx,
                                            DeltaTracker tickCounter,
                                            Minecraft mc,
                                            CustomBar bar) {
        boolean useJump = CustomJumpBarRenderer.shouldUseJumpBar(mc);
        boolean useLocator = !useJump && CustomBar.shouldUseLocator(mc, bar);
        boolean drawXp = !useJump && !useLocator;

        LoVisualRenderSystem.ensureFrameContext();
        try (RenderPhaseScope ignored = LoVisualRenderSystem.phase(RenderPhase.HUD_MAIN, "2d:hud:custom_bar")) {
            Renderer2D.withDeferredLayer(VANILLA_HUD_REPLACEMENT_LAYER, () -> {
                ViewportContext.beginUnscaled(ctx);
                Renderer2D.COLOR.begin();
                if (drawXp) {
                    CustomXpBarRenderer.renderXpBarBackground(Renderer2D.COLOR, mc);
                }
                if (useJump) {
                    CustomJumpBarRenderer.renderJumpBarBackground(Renderer2D.COLOR, mc);
                }
                if (useLocator) {
                    CustomLocatorRenderer.renderLocatorBackground(Renderer2D.COLOR, mc);
                }
                Renderer2D.COLOR.render();
                ViewportContext.end(ctx);

                if (drawXp) {
                    CustomXpBarRenderer.renderXpBarText(ctx, mc);
                }
            });
            appendVanillaHudReplacementMarker(ctx);
        }
    }

    private static void appendVanillaHudReplacementMarker(GuiGraphicsExtractor ctx) {
        if (!(ctx instanceof IGuiGraphics graphics)) return;
        ctx.nextStratum();
        graphics.lovisual$addDeferredHudMarker(VANILLA_HUD_REPLACEMENT_LAYER);
        ctx.nextStratum();
    }
}
