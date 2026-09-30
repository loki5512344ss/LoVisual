package dev.loki.lovisual.features.platform.serverpolicy;

/**
 * Static holder for the active server policy. {@link #allows} is the single
 * gate the module lifecycle consults; ClickGui reads {@link #isLocked} for the
 * card lock and {@link #suggestWarn} for the warn-but-allow path.
 */
public final class ServerPolicyState {

    private static volatile ServerPolicy current;
    private static volatile long receivedAtMs;

    private ServerPolicyState() {
    }

    /** Installs a parsed policy (replacing any previous one). */
    public static void apply(ServerPolicy policy) {
        if (policy == null) {
            return;
        }
        current = policy;
        receivedAtMs = System.currentTimeMillis();
    }

    /** Drops the policy — called on disconnect so singleplayer is unaffected. */
    public static void clear() {
        current = null;
        receivedAtMs = 0L;
    }

    public static ServerPolicy current() {
        return current;
    }

    public static long receivedAtMs() {
        return receivedAtMs;
    }

    public static boolean active() {
        return current != null;
    }

    /** True when the module may be enabled: no policy, no lock, or not denied. */
    public static boolean allows(String moduleId) {
        ServerPolicy policy = current;
        return policy == null || !policy.isLocked() || !policy.denies(moduleId);
    }

    /** True when the module is listed by a {@code lock} policy. */
    public static boolean isLocked(String moduleId) {
        ServerPolicy policy = current;
        return policy != null && policy.isLocked() && policy.denies(moduleId);
    }

    /** True when the module is listed by a {@code suggest} policy. */
    public static boolean suggestWarn(String moduleId) {
        ServerPolicy policy = current;
        return policy != null && !policy.isLocked() && policy.denies(moduleId);
    }

    /** The proxy-supplied reason text, for messages; empty when no policy. */
    public static String reason() {
        ServerPolicy policy = current;
        return policy == null ? "" : policy.reason();
    }
}
