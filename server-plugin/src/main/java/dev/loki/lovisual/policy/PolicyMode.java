package dev.loki.lovisual.policy;

import java.util.Locale;

/**
 * How the mod must treat a denied feature: {@link #LOCK} refuses to enable it
 * and shows a lock in the ClickGui, {@link #SUGGEST} only warns the player.
 */
public enum PolicyMode {
    LOCK,
    SUGGEST;

    /**
     * Parses a config value. A missing value falls back to the conservative
     * {@link #LOCK}; an unknown value is an admin error and throws.
     *
     * @throws IllegalArgumentException when {@code raw} is present but not {@code lock}/{@code suggest}
     */
    public static PolicyMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LOCK;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "lock" -> LOCK;
            case "suggest" -> SUGGEST;
            default -> throw new IllegalArgumentException("unknown mode: " + raw + " (expected lock|suggest)");
        };
    }
}
