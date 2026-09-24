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
