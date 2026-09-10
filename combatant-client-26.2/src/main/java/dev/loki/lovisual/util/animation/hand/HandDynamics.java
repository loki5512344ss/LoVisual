/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation.hand;
import dev.loki.lovisual.util.animation.hand.state.HandAnimState;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import dev.loki.lovisual.features.module.modules.visuals.items.HoldMyItems;
import dev.loki.lovisual.render.helpers.util.TickDelta;

/**
 * Клей между рендером руки и чистой математикой динамики: собирает ввод
 * (дельты взгляда, скорость, использование предмета), собирает позу и
 * применяет её к матрице вокруг ванильных трансформов. Состояние не хранит —
 * оно живёт в {@link HandAnimState}, обновляемом раз за кадр.
 */
public final class HandDynamics {

    /** Смещения/углы слабее этого считаются нулевыми и не применяются. */
    private static final float SIGNIFICANT_EPSILON = 1.0e-4f;

    private HandDynamics() {
    }

    /** Один шаг динамики за кадр; вызывается из начала submitHandsWithItems. */
    public static void beginFrame(HandAnimState state, AbstractClientPlayer player, float tickDelta) {
        state.beginFrame(
                player,
                Minecraft.getInstance().level,
                player.getViewYRot(tickDelta),
                player.getViewXRot(tickDelta),
                player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND,
                player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND,
                TickDelta.fixedDeltaTicks()
        );
    }

    /**
     * Применяет позу динамики к матрице руки: pushPose, затем смещение и
     * повороты вокруг точки взгляда до ванильных трансформов. Вызывающий
     * обязан сделать popPose, если вернулось true.
     *
     * @return true, если был pushPose и нужен popPose
     */
    public static boolean apply(
            HoldMyItems module, HandAnimState state, AbstractClientPlayer player,
            float tickDelta, InteractionHand hand, PoseStack matrices
    ) {
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        float bobStrength = module.walkBob.get() ? module.bobStrength.get() : 0.0f;
        float driftStrength = module.useDrift.get() ? module.driftStrength.get() : 0.0f;

        boolean usingThisHand = player.isUsingItem() && player.getUsedItemHand() == hand;
        float drift = usingThisHand ? state.drift(mainHand) : 0.0f;

        HandPoseMath.Pose pose = HandPoseMath.sway(
                state.sway().yawLag(), state.sway().pitchLag(), module.swayIntensity.get());
        if (bobStrength > 0.0f) {
            pose = HandPoseMath.add(pose, HandPoseMath.bob(
                    player.walkAnimation.position(tickDelta),
                    BobMath.amplitude(player.walkAnimation.speed(tickDelta)),
                    bobStrength));
        }
        if (driftStrength > 0.0f && drift > 0.0f) {
            boolean rightArm = (player.getMainArm() == HumanoidArm.RIGHT) == mainHand;
            pose = HandPoseMath.add(pose, HandPoseMath.drift(drift, driftStrength, rightArm));
        }
        if (module.joinAppear.get()) {
            pose = HandPoseMath.add(pose, HandPoseMath.joinDrop(state.joinProgress()));
        }
        if (!pose.isSignificant(SIGNIFICANT_EPSILON)) {
            return false;
        }

        matrices.pushPose();
        matrices.translate(pose.dx(), pose.dy(), pose.dz());
        if (pose.rx() != 0.0f) matrices.mulPose(Axis.XP.rotationDegrees(pose.rx()));
        if (pose.ry() != 0.0f) matrices.mulPose(Axis.YP.rotationDegrees(pose.ry()));
        if (pose.rz() != 0.0f) matrices.mulPose(Axis.ZP.rotationDegrees(pose.rz()));
        return true;
    }
}
