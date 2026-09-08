/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.HoldMyItems;
import dev.loki.lovisual.util.animation.hand.HandDynamics;
import dev.loki.lovisual.features.module.modules.visuals.ViewModel;
import dev.loki.lovisual.util.animation.EquipProgressComposer;
import dev.loki.lovisual.util.animation.hand.HandAnimState;

@Environment(EnvType.CLIENT)
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Unique private boolean lovisual$handPosePushed;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lovisual$skipTickWithoutPlayer(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player == null) {
            ci.cancel();
        }
    }

    // Динамика рук (sway/боб/дрейф/появление): состояние шагает ровно раз за
    // кадр в HoldMyItems.onFrame — вне рендер-пути, поэтому повторный проход
    // руки (захват маски Chams) видит те же значения, что и основной проход.
    // Здесь состояние только читается и применяется до ванильных трансформов.
    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void lovisual$applyHandDynamics(
            AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand,
            float swingProgress, ItemStack item, float equipProgress, PoseStack matrices,
            SubmitNodeCollector queue, int light, CallbackInfo ci
    ) {
        HoldMyItems holdMyItems = Modules.get(HoldMyItems.class);
        if (holdMyItems == null || !holdMyItems.isEnabled()) return;
        if (hand == InteractionHand.OFF_HAND && !holdMyItems.offhandToo.get()) return;
        HandAnimState handAnim = holdMyItems.handAnim();
        if (handAnim == null) return;

        lovisual$handPosePushed = HandDynamics.apply(holdMyItems, handAnim, player, tickDelta, hand, matrices);
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void lovisual$restoreHandDynamics(
            AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand,
            float swingProgress, ItemStack item, float equipProgress, PoseStack matrices,
            SubmitNodeCollector queue, int light, CallbackInfo ci
    ) {
        if (!lovisual$handPosePushed) return;
        lovisual$handPosePushed = false;
        matrices.popPose();
    }

    @Inject(
            method = "submitArmWithItem",
            at = @At("HEAD")
    )
    private void lovisual$applyOffsets(
            AbstractClientPlayer player,
            float tickDelta,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            CallbackInfo ci
    ) {
        ViewModel viewModel = Modules.get(ViewModel.class);
        if (viewModel == null || !viewModel.isEnabled()) return;

        matrices.translate(
                viewModel.swingX.get(),
                viewModel.swingY.get(),
                viewModel.swingZ.get() + (isInLiquid(player) ? viewModel.liquidOffsetZ.get() : 0.0f)
        );
    }

    // Четвёртый float среди аргументов (tickDelta, pitch, swingProgress, equipProgress) —
    // equipProgress; индекс 7 указывал бы на matrices (PoseStack), из-за чего модификаторы
    // не применялись. ViewModel и HoldMyItems сводятся в одну цепочку:
    // кривая на полном диапазоне, затем занижение амплитуды (EquipProgressComposer).
    @ModifyVariable(
            method = "submitArmWithItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 3)
    private float lovisual$composeEquipProgress(
            float equipProgress, AbstractClientPlayer player, float tickDelta,
            float pitch, InteractionHand hand) {
        ViewModel viewModel = Modules.get(ViewModel.class);
        HoldMyItems holdMyItems = Modules.get(HoldMyItems.class);
        boolean lowerEnabled = viewModel != null && viewModel.isEnabled();
        boolean curveEnabled = holdMyItems != null && holdMyItems.isEnabled()
                && !(hand == InteractionHand.OFF_HAND && !holdMyItems.offhandToo.get());
        if (!lowerEnabled && !curveEnabled) return equipProgress;
        return EquipProgressComposer.compose(
                equipProgress,
                curveEnabled,
                curveEnabled ? holdMyItems.easeStrength.get() : 1.0f,
                curveEnabled ? holdMyItems.swayIntensity.get() : 1.0f,
                lowerEnabled,
                lowerEnabled ? viewModel.equipLowering.get() : 1.0f
        );
    }

    @Unique
    private boolean isInLiquid(AbstractClientPlayer player) {
        return player != null && (player.isInWater() || player.isInLava());
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void lovisual$scaleMiniItems(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext ctx,
            PoseStack ms,
            SubmitNodeCollector queue,
            int light,
            CallbackInfo ci
    ) {
        ViewModel viewModel = Modules.get(ViewModel.class);
        if (viewModel == null || !viewModel.isEnabled()) return;
        if (!ctx.firstPerson()) return;

        if (viewModel.shouldScale(stack)) {
            viewModel.applyMini(ms, stack);
        }
    }

    @Inject(
            method = "swingArm",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lovisual$customSwing(
            float swingProgress,
            PoseStack ms,
            int direction,
            HumanoidArm arm,
            CallbackInfo ci
    ) {
        ViewModel viewModel = Modules.get(ViewModel.class);
        if (viewModel == null || !viewModel.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!viewModel.shouldSwing(stack))
            return;

        if (viewModel.shouldBypassRotationTransform(stack))
            return;

        // Cancel vanilla swing entirely
        ci.cancel();

        viewModel.applySwingAnimation(swingProgress, ms, arm);
    }
}

