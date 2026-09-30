package dev.loki.lovisual.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyConfigTest {

    @Test
    void defaultsAreEnabledLockWithExampleDeny() {
        PolicyConfig cfg = PolicyConfig.defaults();
        assertTrue(cfg.enabled());
        assertEquals(PolicyMode.LOCK, cfg.mode());
        assertFalse(cfg.reason().isBlank());
        assertFalse(cfg.deny().isEmpty());
    }

    @Test
    void fromMapParsesAllFields() {
        PolicyConfig cfg = PolicyConfig.fromMap(Map.of(
                "enabled", true,
                "mode", "suggest",
                "reason", "custom rules",
                "deny", List.of("esp", "freecam")
        ));
        assertTrue(cfg.enabled());
        assertEquals(PolicyMode.SUGGEST, cfg.mode());
        assertEquals("custom rules", cfg.reason());
        assertEquals(List.of("esp", "freecam"), cfg.deny());
    }

    @Test
    void fromMapMissingModeDefaultsToLock() {
        PolicyConfig cfg = PolicyConfig.fromMap(Map.of("deny", List.of("esp")));
        assertEquals(PolicyMode.LOCK, cfg.mode());
        assertEquals(List.of("esp"), cfg.deny());
    }

    @Test
    void fromMapUnknownModeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> PolicyConfig.fromMap(Map.of("mode", "banana")));
    }

    @Test
    void fromMapDisabledWithEmptyDeny() {
        PolicyConfig cfg = PolicyConfig.fromMap(Map.of(
                "enabled", false,
                "mode", "lock",
                "deny", List.of()
        ));
        assertFalse(cfg.enabled());
        assertTrue(cfg.deny().isEmpty());
    }

    @Test
    void loadWritesDefaultConfigWhenMissing(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        PolicyConfig cfg = PolicyConfig.load(file);
        assertTrue(Files.exists(file));
        assertEquals(PolicyConfig.defaults(), cfg);
        assertEquals(cfg, PolicyConfig.load(file));
    }

    @Test
    void loadParsesYamlFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, """
                enabled: false
                mode: suggest
                reason: "no cheats"
                deny:
                  - esp
                  - hitboxes
                """);
        PolicyConfig cfg = PolicyConfig.load(file);
        assertFalse(cfg.enabled());
        assertEquals(PolicyMode.SUGGEST, cfg.mode());
        assertEquals("no cheats", cfg.reason());
        assertEquals(List.of("esp", "hitboxes"), cfg.deny());
    }

    @Test
    void loadThrowsOnUnknownMode(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "mode: banana\n");
        assertThrows(IllegalArgumentException.class, () -> PolicyConfig.load(file));
    }

    @Test
    void loadOfEmptyFileFallsBackToDefaults(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "");
        assertEquals(PolicyConfig.defaults(), PolicyConfig.load(file));
    }
}
