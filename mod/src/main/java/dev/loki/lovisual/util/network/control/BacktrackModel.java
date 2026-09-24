package dev.loki.lovisual.util.network.control;

import dev.loki.lovisual.config.values.mode.EnumValue;

/**
 * Data types for {@link BacktrackController}: the target-acquisition mode and the
 * immutable timing/range configuration. Kept out of the controller so the stateful
 * machine stays focused on packet flow.
 */
public final class BacktrackModel {

    private BacktrackModel() {
    }

    public enum TargetMode implements EnumValue.IdProvider {
        ATTACK("attack"),
        RANGE("range");

        private final String id;

        TargetMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public record Config(
            double minRange,
            double maxRange,
            int minDelayMs,
            int maxDelayMs,
            int minNextBacktrackDelayMs,
            int maxNextBacktrackDelayMs,
            int trackingBufferMs,
            int chancePercent,
            boolean pauseOnHurtTime,
            int pauseHurtTime,
            int lastAttackTimeToWorkMs,
            TargetMode targetMode
    ) {
        public Config {
            if (targetMode == null) {
                targetMode = TargetMode.ATTACK;
            }
        }

        public static Config defaults() {
            return new Config(0.0, 3.0, 100, 150, 0, 10, 500, 100, false, 3, 1000, TargetMode.ATTACK);
        }
    }
}
