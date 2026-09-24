package dev.loki.lovisual.util.network.control;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.events.impl.network.BlinkPacketEvent;
import dev.loki.lovisual.util.network.packet.TransferOrigin;

/**
 * Blink packet interception for {@link BacktrackController}: decides whether an
 * incoming packet should pass, be queued or be flushed, and decodes the target's
 * tracked position from movement packets. Pure classification and geometry live in
 * {@link BacktrackEngine}; cancellation eligibility in {@link BacktrackTargeting}.
 * Package-private helper (impl subfolder, {@code control} package).
 */
final class BacktrackPacketFlow {

    private BacktrackPacketFlow() {
    }

    static void onBlinkPacket(BacktrackController c, BlinkPacketEvent event) {
        if (!c.enabled || event == null || event.getOrigin() != TransferOrigin.INCOMING) {
            return;
        }

        Packet<?> packet = event.getPacket();
        boolean shouldCancel = BacktrackTargeting.shouldCancelPackets(c);
        boolean hasQueuedIncoming = BacktrackEngine.hasQueuedIncoming();

        if (packet == null) {
            if (shouldCancel || hasQueuedIncoming) {
                event.setAction(BlinkManager.Action.PASS);
            }
            return;
        }

        if (!hasQueuedIncoming && !shouldCancel) {
            return;
        }

        if (BacktrackEngine.shouldAlwaysPass(packet)) {
            event.setAction(BlinkManager.Action.PASS);
            return;
        }

        if (BacktrackEngine.shouldClearOnSafetyPacket(packet)) {
            BacktrackTargeting.clear(c, true);
            event.setAction(BlinkManager.Action.PASS);
            return;
        }

        Entity currentTarget = c.target;
        if (currentTarget == null) {
            return;
        }

        Vec3 nextTargetPos = getPacketTargetPosition(c, packet, currentTarget);
        if (nextTargetPos == null) {
            event.setAction(BlinkManager.Action.PASS);
            return;
        }

        if (BacktrackEngine.boxedDistanceSq(currentTarget, nextTargetPos)
                < BacktrackEngine.boxedDistanceSq(currentTarget, currentTarget.position())) {
            event.setAction(BlinkManager.Action.FLUSH);
            return;
        }

        event.setAction(BlinkManager.Action.QUEUE);
    }

    private static Vec3 getPacketTargetPosition(BacktrackController c, Packet<?> packet, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (target == null || mc == null || mc.level == null) {
            return null;
        }

        if (packet instanceof ClientboundMoveEntityPacket entityPacket
                && BacktrackEngine.isTargetEntityPacket(entityPacket, target, mc)
                && entityPacket.hasPosition()) {
            Vec3 pos = c.delayedPosition.decode(
                    entityPacket.getXa(),
                    entityPacket.getYa(),
                    entityPacket.getZa()
            );
            c.delayedPosition.setBase(pos);
            return pos;
        }
        if (packet instanceof ClientboundTeleportEntityPacket positionPacket && positionPacket.id() == target.getId()) {
            PositionMoveRotation current = new PositionMoveRotation(
                    c.delayedPosition.getBase(),
                    target.getDeltaMovement(),
                    target.getYRot(),
                    target.getXRot()
            );
            Vec3 pos = PositionMoveRotation.calculateAbsolute(current, positionPacket.change(), positionPacket.relatives()).position();
            c.delayedPosition.setBase(pos);
            return pos;
        }
        if (packet instanceof ClientboundEntityPositionSyncPacket syncPacket && syncPacket.id() == target.getId()) {
            Vec3 pos = syncPacket.values().position();
            c.delayedPosition.setBase(pos);
            return pos;
        }
        return null;
    }
}
