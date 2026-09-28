package dev.loki.lovisual.features.platform;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Pure normalization for a configured backend base URL: trims whitespace and a
 * trailing slash, and falls back to a caller-supplied default whenever the
 * stored value is blank or fails to parse as an absolute URI with a host.
 * Kept dependency-free (no HTTP client, no Minecraft types) so it can be unit
 * tested in isolation and reused once real API calls are added later.
 */
public final class BackendEndpoint {

    private BackendEndpoint() {
    }

    public static String normalize(String rawValue, String fallback) {
        String candidate = stripTrailingSlash(rawValue);
        if (isValidBaseUrl(candidate)) {
            return candidate;
        }
        return stripTrailingSlash(fallback);
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static boolean isValidBaseUrl(String value) {
        if (value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value);
            return uri.isAbsolute() && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
