package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.KineticLyrics;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LrcParser;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;

/**
 * Fetches synced lyrics from lrclib.net (no API key required).
 */
public final class LrcLibClient {
    private static final String API_GET = "https://lrclib.net/api/get";
    private static final String API_SEARCH = "https://lrclib.net/api/search";

    private static final String USER_AGENT = "LoVisual/1.0";

    private static final Map<String, List<LyricLine>> LYRICS_CACHE = new ConcurrentHashMap<>();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private LrcLibClient() {
    }

    public static CompletableFuture<List<LyricLine>> fetchLyricsAsync(String trackName, String artistName, boolean debugMode) {
        if (trackName == null || trackName.isBlank()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String cacheKey = (trackName.trim() + " - " + (artistName != null ? artistName.trim() : "")).toLowerCase();
        List<LyricLine> cached = LYRICS_CACHE.get(cacheKey);
        if (cached != null) {
            if (debugMode) {
                DebugLog.info("[KineticLyrics] Cache hit: %s (%d lines)", cacheKey, cached.size());
            }
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            List<LyricLine> lines = tryFetchGet(trackName, artistName, debugMode);
            if (!lines.isEmpty()) {
                LYRICS_CACHE.put(cacheKey, lines);
                return lines;
            }

            lines = tryFetchSearch(trackName, artistName, debugMode);
            if (!lines.isEmpty()) {
                LYRICS_CACHE.put(cacheKey, lines);
                return lines;
            }

            return Collections.<LyricLine>emptyList();
        });
    }

    private static List<LyricLine> tryFetchGet(String trackName, String artistName, boolean debugMode) {
        String cleanTrack = cleanTrackName(trackName);
        String cleanArtist = artistName != null ? cleanTrackName(artistName) : "";

        String query = String.format("?track_name=%s&artist_name=%s",
                URLEncoder.encode(cleanTrack, StandardCharsets.UTF_8),
                URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8));

        return executeRequest(API_GET + query, debugMode);
    }

    private static List<LyricLine> tryFetchSearch(String trackName, String artistName, boolean debugMode) {
        String cleanTrack = cleanTrackName(trackName);
        String cleanArtist = artistName != null ? cleanTrackName(artistName) : "";
        String searchQuery = (cleanTrack + " " + cleanArtist).trim();

        String query = "?q=" + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_SEARCH + query))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(14))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
                for (JsonElement elem : array) {
                    if (elem.isJsonObject()) {
                        JsonObject obj = elem.getAsJsonObject();
                        if (obj.has("syncedLyrics") && !obj.get("syncedLyrics").isJsonNull()) {
                            String syncedLrc = obj.get("syncedLyrics").getAsString();
                            List<LyricLine> parsed = LrcParser.parse(syncedLrc);
                            if (!parsed.isEmpty()) {
                                if (debugMode) {
                                    DebugLog.info("[KineticLyrics] Found lyrics via search fallback");
                                }
                                return parsed;
                            }
                        }
                    }
                }
            } else if (response.statusCode() == 404) {
                if (debugMode) {
                    DebugLog.info("[KineticLyrics] Search: track not found (404)");
                }
            }
        } catch (Exception e) {
            if (debugMode) {
                DebugLog.error("[KineticLyrics] Search fallback failed", e);
            }
        }
        return Collections.emptyList();
    }

    private static List<LyricLine> executeRequest(String url, boolean debugMode) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofSeconds(14))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("syncedLyrics") && !json.get("syncedLyrics").isJsonNull()) {
                        String syncedLrc = json.get("syncedLyrics").getAsString();
                        return LrcParser.parse(syncedLrc);
                    }
                    if (debugMode) {
                        DebugLog.info("[KineticLyrics] No syncedLyrics in response");
                    }
                    return Collections.emptyList();
                } else if (response.statusCode() == 404) {
                    if (debugMode) {
                        DebugLog.info("[KineticLyrics] Track not found (404)");
                    }
                    return Collections.emptyList();
                } else {
                    if (debugMode) {
                        DebugLog.info("[KineticLyrics] Attempt %d returned %d", attempt, response.statusCode());
                    }
                }
            } catch (Exception e) {
                if (debugMode) {
                    DebugLog.error("[KineticLyrics] Attempt " + attempt + " failed", e);
                }
                if (attempt < 2) {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    public static String cleanTrackName(String trackName) {
        if (trackName == null) return "";
        return trackName
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("\\[.*?\\]", "")
                .trim();
    }
}
