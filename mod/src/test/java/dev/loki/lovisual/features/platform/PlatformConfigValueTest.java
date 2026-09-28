package dev.loki.lovisual.features.platform;

import dev.loki.lovisual.config.values.primitive.StringValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the {@code backendBaseUrl} setting the way {@link PlatformConfig}
 * declares it (same name/default), without touching the singleton or its
 * on-disk config file. {@link dev.loki.lovisual.config.io.ConfigSerializer}
 * persists a config object purely through each {@code ConfigValue}'s
 * {@code toJson}/{@code fromJson}, so exercising that pair here is exactly
 * what "round-trips through the config serializer" means, matching the style
 * of dev.loki.lovisual.config.values.PrimitiveValuesTest.
 */
public final class PlatformConfigValueTest {

    @Test
    void defaultsToProductionBackendUrlWhenNothingIsConfigured() {
        StringValue value = new StringValue("backendBaseUrl", PlatformConfig.DEFAULT_BACKEND_BASE_URL);
        assertEquals(PlatformConfig.DEFAULT_BACKEND_BASE_URL, value.get());
        assertEquals(PlatformConfig.DEFAULT_BACKEND_BASE_URL, BackendEndpoint.normalize(value.get(), PlatformConfig.DEFAULT_BACKEND_BASE_URL));
    }

    @Test
    void overriddenValueRoundTripsThroughToJsonFromJson() {
        StringValue value = new StringValue("backendBaseUrl", PlatformConfig.DEFAULT_BACKEND_BASE_URL);
        value.fromJson("https://staging.lovisual.example");

        Object serialized = value.toJson();
        assertEquals("https://staging.lovisual.example", serialized);

        StringValue reloaded = new StringValue("backendBaseUrl", PlatformConfig.DEFAULT_BACKEND_BASE_URL);
        reloaded.fromJson(serialized);
        assertEquals("https://staging.lovisual.example", reloaded.get());
        assertEquals("https://staging.lovisual.example", BackendEndpoint.normalize(reloaded.get(), PlatformConfig.DEFAULT_BACKEND_BASE_URL));
    }

    @Test
    void malformedStoredValueFallsBackAfterRoundTrip() {
        StringValue value = new StringValue("backendBaseUrl", PlatformConfig.DEFAULT_BACKEND_BASE_URL);
        value.fromJson("not-a-url");

        Object serialized = value.toJson();
        StringValue reloaded = new StringValue("backendBaseUrl", PlatformConfig.DEFAULT_BACKEND_BASE_URL);
        reloaded.fromJson(serialized);

        assertEquals(PlatformConfig.DEFAULT_BACKEND_BASE_URL,
                BackendEndpoint.normalize(reloaded.get(), PlatformConfig.DEFAULT_BACKEND_BASE_URL));
    }
}
