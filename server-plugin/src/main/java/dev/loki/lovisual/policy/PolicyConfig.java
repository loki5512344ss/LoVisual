package dev.loki.lovisual.policy;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The proxy-side policy: which mod features are denied and how strictly.
 * Lives in {@code plugins/lovisual-policy/config.yml}; written with defaults
 * on first run. Invalid content (e.g. an unknown mode) throws so the plugin
 * can log the error instead of silently pushing a broken policy.
 */
public record PolicyConfig(boolean enabled, PolicyMode mode, String reason, List<String> deny) {

    private static final String DEFAULT_REASON = "Правила сервера";
    private static final List<String> EXAMPLE_DENY =
            List.of("esp", "freecam", "hitboxes", "fullbright");

    static final String DEFAULT_YAML = """
            # LoVisual server policy: features denied to clients on this proxy.
            # mode: lock     — denied modules are switched off and cannot be enabled;
            # mode: suggest  — the mod only warns the player.
            enabled: true
            mode: lock
            reason: "Правила сервера"
            deny:
              - esp
              - freecam
              - hitboxes
              - fullbright
            """;

    public PolicyConfig {
        reason = reason == null ? "" : reason;
        deny = deny == null ? List.of() : List.copyOf(deny);
    }

    public static PolicyConfig defaults() {
        return new PolicyConfig(true, PolicyMode.LOCK, DEFAULT_REASON, EXAMPLE_DENY);
    }

    /**
     * Loads {@code config.yml}, writing {@link #DEFAULT_YAML} first when it is
     * absent. An empty file falls back to {@link #defaults()}.
     */
    public static PolicyConfig load(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, DEFAULT_YAML, StandardCharsets.UTF_8);
            return defaults();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<?, ?> map = new Yaml().load(reader);
            if (map == null || map.isEmpty()) {
                return defaults();
            }
            return fromMap(map);
        }
    }

    /** Builds a config from parsed YAML/map values with per-key fallbacks. */
    public static PolicyConfig fromMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return defaults();
        }
        Object enabledRaw = map.get("enabled");
        boolean enabled = enabledRaw == null
                || (enabledRaw instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(enabledRaw)));
        PolicyMode mode = PolicyMode.parse(map.get("mode") == null ? null : String.valueOf(map.get("mode")));
        Object reasonRaw = map.get("reason");
        String reason = reasonRaw == null ? DEFAULT_REASON : String.valueOf(reasonRaw);
        return new PolicyConfig(enabled, mode, reason, denyList(map.get("deny")));
    }

    private static List<String> denyList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item != null) {
                out.add(String.valueOf(item));
            }
        }
        return out;
    }
}
