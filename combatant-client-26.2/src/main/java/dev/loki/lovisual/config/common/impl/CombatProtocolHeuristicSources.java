/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.common.impl;

import dev.loki.lovisual.config.common.CommonBooleanGroupSchema;
import dev.loki.lovisual.util.combat.protocol.CombatProtocolHeuristics;

import java.util.Map;

public final class CombatProtocolHeuristicSources implements CommonBooleanGroupSchema {
    @Override
    public String commonI18nKey() {
        return "combat.protocol_heuristics";
    }

    @Override
    public Map<String, Boolean> defaults() {
        return CombatProtocolHeuristics.defaultSourceToggles();
    }
}
