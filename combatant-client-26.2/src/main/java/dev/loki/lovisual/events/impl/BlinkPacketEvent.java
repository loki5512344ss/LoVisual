/*
 * This file is part of the Combatant Client distribution.
 * Combatant modifications copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce).
 * Copyright (c) 2015-2026 CCBlueX.
 *
 * LiquidBounce portions are licensed under GPLv3-or-later.
 * Combatant modifications are licensed under GPLv3.
 * See THIRD_PARTY_NOTICES.md for details.
 */

package dev.loki.lovisual.events.impl;

import lombok.Getter;
import net.minecraft.network.protocol.Packet;
import dev.loki.lovisual.events.Event;
import dev.loki.lovisual.util.network.BlinkManager;
import dev.loki.lovisual.util.network.TransferOrigin;

/**
 * Adapted from LiquidBounce's BlinkPacketEvent.
 */
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
