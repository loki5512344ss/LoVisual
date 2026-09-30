package dev.loki.lovisual.util.aiming.rotation.internal;

import dev.loki.lovisual.events.impl.network.PacketEvent;
import dev.loki.lovisual.util.aiming.data.Rotation;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

/**
 * Tracks the rotation the server sees, updated from outbound and inbound
 * movement packets. Kept apart from the facade so packet wiring stays small.
 */
public final class RotationPackets {

    private Rotation actualServerRotation = Rotation.ZERO;
    private Rotation theoreticalServerRotation = Rotation.ZERO;

    public Rotation actual() {
        return actualServerRotation;
    }

    public void onSend(PacketEvent.Send e) {
        if (e.isCancelled()) return;
        var packet = e.getPacket();

        Rotation rot = null;
        if (packet instanceof ServerboundMovePlayerPacket move) {
            if (!move.hasRotation()) return;
            rot = new Rotation(move.getYRot(0.0f), move.getXRot(0.0f), true);
        } else if (packet instanceof ServerboundUseItemPacket use) {
            rot = new Rotation(use.getYRot(), use.getXRot(), true);
        }

        if (rot != null) {
            actualServerRotation = rot;
            theoreticalServerRotation = rot;
        }
    }

    public void onReceive(PacketEvent.Receive e) {
        if (!(e.getPacket() instanceof ClientboundPlayerPositionPacket packet)) return;
        Rotation rot = new Rotation(packet.change().yRot(), packet.change().xRot(), true);
        theoreticalServerRotation = rot;
        actualServerRotation = rot;
    }

    public Rotation theoretical() {
        return theoreticalServerRotation;
    }
}
