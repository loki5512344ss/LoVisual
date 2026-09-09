package dev.loki.lovisual.features.module.modules.combat.combatlog;

/** A single attributed hit: either dealt by the local player or taken by them. */
public record CombatLogEntry(String otherName, float damage, boolean dealtByMe, long timestampMs) {
}
