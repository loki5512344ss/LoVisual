package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * One lookup's HTTP session. A 404 is a plain "not found"; a 429, 5xx or network error is remembered as
 * transient so the caller can tell "nothing exists" from "the service was busy, try again later".
 */
final class LyricHttp {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final String AGENT = "Mozilla/5.0 (X11; Linux x86_64) LoVisual/1.0";
    private static final int ATTEMPTS = 2;

    private boolean transientFailure;

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    boolean sawTransientFailure() {
        return transientFailure;
    }

    /** @return the response body, or null when there is none (missing or failed). */
    String get(String url) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", AGENT)
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 200) return response.body();
                if (status == 404) return null;
                if (status != 429 && status < 500) return null;
            } catch (IOException e) {
                // network trouble is worth retrying
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (attempt < ATTEMPTS) pause();
        }
        transientFailure = true;
        return null;
    }

    private static void pause() {
        try {
            Thread.sleep(350);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
