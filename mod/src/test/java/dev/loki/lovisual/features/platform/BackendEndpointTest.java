package dev.loki.lovisual.features.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure normalization tests for {@link BackendEndpoint}: no config file I/O
 * and no singleton involved, matching the value-object test style used by
 * dev.loki.lovisual.config.values.PrimitiveValuesTest.
 */
public final class BackendEndpointTest {

    private static final String FALLBACK = "https://api.lovisual.example";

    @Test
    void keepsAValidHttpsUrlUnchanged() {
        assertEquals("https://api.lovisual.com", BackendEndpoint.normalize("https://api.lovisual.com", FALLBACK));
    }

    @Test
    void trimsWhitespaceAndTrailingSlash() {
        assertEquals("https://api.lovisual.com", BackendEndpoint.normalize("  https://api.lovisual.com/  ", FALLBACK));
    }

    @Test
    void stripsMultipleTrailingSlashes() {
        assertEquals("https://api.lovisual.com", BackendEndpoint.normalize("https://api.lovisual.com///", FALLBACK));
    }

    @Test
    void fallsBackWhenValueIsBlank() {
        assertEquals(FALLBACK, BackendEndpoint.normalize("", FALLBACK));
        assertEquals(FALLBACK, BackendEndpoint.normalize("   ", FALLBACK));
        assertEquals(FALLBACK, BackendEndpoint.normalize(null, FALLBACK));
    }

    @Test
    void fallsBackWhenValueIsMalformed() {
        assertEquals(FALLBACK, BackendEndpoint.normalize("not a url", FALLBACK));
        assertEquals(FALLBACK, BackendEndpoint.normalize("just-a-hostname", FALLBACK));
        assertEquals(FALLBACK, BackendEndpoint.normalize("://missing-scheme", FALLBACK));
    }

    @Test
    void fallbackItselfIsAlsoNormalized() {
        assertEquals("https://api.lovisual.example", BackendEndpoint.normalize(null, "https://api.lovisual.example/"));
    }
}
