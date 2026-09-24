package dev.loki.lovisual.util.network.packet;

import net.minecraft.network.protocol.Packet;
import dev.loki.lovisual.util.network.packet.TransferOrigin;

public record PacketSnapshot(Packet<?> packet, TransferOrigin origin, long timestamp) {
}
