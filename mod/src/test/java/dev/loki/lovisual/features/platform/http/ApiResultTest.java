package dev.loki.lovisual.features.platform.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JSON/status mapping logic of {@link ApiResult}, covering exactly the
 * response shapes the backend plans document: 2xx bodies, {@code {"error":
 * "..."}} error bodies, 404/202 markers used by the device-token poll and
 * shared-config lookup, and unparseable/empty bodies.
 */
public final class ApiResultTest {

    @Test
    void twoHundredWithBodyIsSuccess() {
        ApiResult result = ApiResult.of(200, "{\"device_token\":\"lvd_abc\"}");
        assertTrue(result.success());
        assertEquals("lvd_abc", result.asObject().get("device_token").getAsString());
        assertNull(result.error());
    }

    @Test
    void nonTwoHundredWithErrorBodyExposesMessage() {
        ApiResult result = ApiResult.of(400, "{\"error\":\"bad request\"}");
        assertFalse(result.success());
        assertEquals("bad request", result.error());
    }

    @Test
    void nonTwoHundredWithoutParsableBodyFallsBackToStatusCode() {
        ApiResult result = ApiResult.of(500, "not json");
        assertFalse(result.success());
        assertEquals("HTTP 500", result.error());
        assertNull(result.body());
    }

    @Test
    void emptyBodyOnSuccessIsHandled() {
        ApiResult result = ApiResult.of(204, "");
        assertTrue(result.success());
        assertNull(result.body());
        assertNull(result.asObject());
    }

    @Test
    void fourOhFourIsNotFoundAndTwoOhTwoIsPending() {
        assertTrue(ApiResult.of(404, "{\"error\":\"unknown\"}").isNotFound());
        assertTrue(ApiResult.of(202, "{}").isPending());
        assertFalse(ApiResult.of(200, "{}").isNotFound());
        assertFalse(ApiResult.of(200, "{}").isPending());
    }

    @Test
    void networkErrorHasNoStatusCodeAndCarriesTheMessage() {
        ApiResult result = ApiResult.networkError("timed out");
        assertFalse(result.success());
        assertEquals(-1, result.statusCode());
        assertEquals("timed out", result.error());
    }

    @Test
    void networkErrorFallsBackToGenericMessageWhenNoneGiven() {
        ApiResult result = ApiResult.networkError(null);
        assertEquals("Network error", result.error());
    }
}
