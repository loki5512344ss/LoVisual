/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl.network;

import lombok.Getter;
import net.minecraft.network.protocol.Packet;
import dev.loki.lovisual.events.Event;
import dev.loki.lovisual.util.network.control.BlinkManager;
import dev.loki.lovisual.util.network.packet.TransferOrigin;

public class BlinkPacketEvent extends Event {
    private final Packet<?> packet;
    @Getter
    private final TransferOrigin origin;
    @Getter
    private BlinkManager.Action action = BlinkManager.Action.FLUSH;

    public BlinkPacketEvent(Packet<?> packet, TransferOrigin origin) {
        this.packet = packet;
        this.origin = origin;
    }

    @SuppressWarnings("unchecked")
    public <T extends Packet<?>> T getPacket() {
        return (T) packet;
    }

    public void setAction(BlinkManager.Action action) {
        if (action == null || this.action == action || this.action.getPriority() >= action.getPriority()) {
            return;
        }
        this.action = action;
    }
}
