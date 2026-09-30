package dev.loki.lovisual.features.platform.serverpolicy;

import java.util.List;

/**
 * A parsed server policy from the {@code lovisual:policy} channel: which
 * modules the proxy denies ({@link Mode#LOCK} = refuse to enable and lock the
 * ClickGui card, {@link Mode#SUGGEST} = warn only).
 */
public record ServerPolicy(Mode mode, String reason, List<String> deny, String server) {

    public enum Mode {
        LOCK,
        SUGGEST
    }

    public ServerPolicy {
        mode = mode == null ? Mode.LOCK : mode;
        reason = reason == null ? "" : reason;
        deny = deny == null ? List.of() : List.copyOf(deny);
        server = server == null ? "" : server;
    }

    public boolean isLocked() {
        return mode == Mode.LOCK;
    }

    public boolean denies(String moduleId) {
        return moduleId != null && deny.contains(moduleId);
    }
}
