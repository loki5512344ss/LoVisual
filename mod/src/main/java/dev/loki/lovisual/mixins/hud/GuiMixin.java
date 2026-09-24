package dev.loki.lovisual.mixins.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into vanilla {@link Hud} state extraction. All bodies live in the {@code handlers} package:
 * overlay toggles ({@link GuiOverlayHandler}), custom bar/hotbar/health rendering
 * ({@link GuiHudBarHandler}), heart-layout redirects ({@link GuiHealthLayoutHandler}) and held-item
 * tooltip recoloring ({@link GuiTooltipHandler}).
 */
@Mixin(Hud.class)
public abstract class GuiMixin {

    @Shadow
    private int toolHighlightTimer;
    @Shadow
    private ItemStack lastToolHighlight;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void lovisual$debug2dGuiStateProbe(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, CallbackInfo ci) {
        // RenderThread2DDebugRenderer.extractGuiStateProbe(ctx);
    }

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$hideScoreboardSidebar(GuiGraphicsExtractor context, Objective objective, CallbackInfo ci) {
        GuiOverlayHandler.hideScoreboardSidebar(ci);
    }

    @Inject(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$filterOverlayMessage(Component message, boolean tinted, CallbackInfo ci) {
        GuiOverlayHandler.filterOverlayMessage(message, tinted, ci);
    }

    @Inject(method = "extractEffects(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$hideVanillaPotions(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        GuiOverlayHandler.hideVanillaPotions(ci);
    }

    @Inject(method = "extractCrosshair(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$replaceCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        GuiOverlayHandler.replaceCrosshair(ci);
    }

    @Inject(method = "extractVignette(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$killVignette(GuiGraphicsExtractor context, Entity entity, CallbackInfo ci) {
        GuiOverlayHandler.killVignette(ci);
    }

    @Inject(method = "extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$customHotbar(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, CallbackInfo ci) {
        GuiHudBarHandler.renderHotbar(ctx, tickCounter, ci);
    }

    @Inject(method = "extractItemHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void lovisual$customBarAfterVanillaHotbar(GuiGraphicsExtractor ctx, DeltaTracker tickCounter, CallbackInfo ci) {
        GuiHudBarHandler.afterVanillaHotbar(ctx, tickCounter);
    }

    @Inject(method = "extractPortalOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$disableVanillaPortalOverlay(GuiGraphicsExtractor context, float nauseaStrength, CallbackInfo ci) {
        GuiOverlayHandler.disablePortalOverlay(ci);
    }

    @Inject(method = "extractSleepOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$disableVanillaSleepOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        GuiOverlayHandler.disableSleepOverlay(ci);
    }

    @Inject(method = "extractHearts(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V", at = @At("HEAD"), cancellable = true)
    private void lovisual$renderHealthBar(GuiGraphicsExtractor context,
                                          Player player,
                                          int x, int y, int lines,
                                          int regeneratingHeartIndex, float maxHealth,
                                          int lastHealth, int health,
                                          int absorption, boolean blinking,
                                          CallbackInfo ci) {
        GuiHudBarHandler.renderHealth(context, player, x, y, maxHealth, ci);
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void lovisual$colorizeHeldItemTooltip(GuiGraphicsExtractor context, CallbackInfo ci) {
        GuiTooltipHandler.colorizeHeldItem(this.toolHighlightTimer, this.lastToolHighlight, this.minecraft, ci);
    }

    @Redirect(method = "extractPlayerHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAbsorptionAmount()F"))
    private float lovisual$ignoreAbsorptionForLayout(Player player) {
        return GuiHealthLayoutHandler.ignoreAbsorptionForLayout(player);
    }

    @Redirect(method = "extractPlayerHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double lovisual$clampMaxHealthForLayout(Player player, Holder<Attribute> attribute) {
        return GuiHealthLayoutHandler.clampMaxHealthForLayout(player, attribute);
    }
}
