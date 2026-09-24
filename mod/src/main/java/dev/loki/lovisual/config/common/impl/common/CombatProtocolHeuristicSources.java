package dev.loki.lovisual.config.common.impl.common;

import dev.loki.lovisual.config.common.CommonBooleanGroupSchema;
import dev.loki.lovisual.util.combat.protocol.heuristics.CombatProtocolHeuristics;

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
