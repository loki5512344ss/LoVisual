/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl;

import dev.loki.lovisual.events.Event;

import java.util.List;

public class CombatProtocolBossbarEvent extends Event {
    public final List<String> names;
    public final long timeMs;

    public CombatProtocolBossbarEvent(List<String> names) {
        this(names, System.currentTimeMillis());
    }

    public CombatProtocolBossbarEvent(List<String> names, long timeMs) {
        this.names = names == null ? List.of() : List.copyOf(names);
        this.timeMs = timeMs;
    }
}
