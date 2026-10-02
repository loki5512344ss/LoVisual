package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails.pose;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Собирает {@link WingTipLocator.Pose} из живого игрока так же, как это делает отрисовка элитры. */
public final class ElytraPoses {

    private ElytraPoses() {
    }

    public static WingTipLocator.Pose of(AbstractClientPlayer player, float partial) {
        Vec3 pos = player.getPosition(partial);
        Vec3 delta = player.avatarState().deltaMovementOnPreviousTick().lerp(player.getDeltaMovement(), partial);
        Vec3 view = player.getViewVector(partial);
        float flying = WingTipLocator.flyingYRot(delta.x, delta.z, view.x, view.z);
        float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        var wing = player.elytraAnimationState;
        return new WingTipLocator.Pose(pos.x, pos.y, pos.z, bodyYaw, player.getXRot(partial), player.getScale(),
                player.isCrouching(), player.isAutoSpinAttack(), player.getFallFlyingTicks() + partial, flying,
                player.tickCount + partial, wing.getRotX(partial), wing.getRotY(partial), wing.getRotZ(partial));
    }

    /** Скорость игрока за тик, блоки (для ширины и прозрачности ленты). */
    public static float speed(AbstractClientPlayer player) {
        return (float) player.getDeltaMovement().length();
    }
}
