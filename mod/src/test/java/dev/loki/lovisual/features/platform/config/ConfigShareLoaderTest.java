package dev.loki.lovisual.features.platform.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ConfigShareLoaderTest {

    @Test
    void godModeCodeIsCaseAndWhitespaceInsensitive() {
        assertTrue(ConfigShareLoader.isGodMode("IDDQD"));
        assertTrue(ConfigShareLoader.isGodMode("  iddqd "));
    }

    @Test
    void otherCodesAreNotGodMode() {
        assertFalse(ConfigShareLoader.isGodMode("IDKFA"));
        assertFalse(ConfigShareLoader.isGodMode(null));
        assertFalse(ConfigShareLoader.isGodMode(""));
    }
}
