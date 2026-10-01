package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles;

/** A circle born at the take-off point; {@code spawnMs} is GameClock time. */
public record JumpCircle(double x, double y, double z, long spawnMs) {
}
