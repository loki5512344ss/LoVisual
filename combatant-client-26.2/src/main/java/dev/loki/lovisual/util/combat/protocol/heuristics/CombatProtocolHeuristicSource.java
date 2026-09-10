/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.combat.protocol.heuristics;

public enum CombatProtocolHeuristicSource {
    MESSAGES("messages"),
    OVERLAY("overlay"),
    BOSSBAR("bossbar");

    private final String key;

    CombatProtocolHeuristicSource(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
