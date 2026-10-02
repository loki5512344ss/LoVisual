package dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.source;

import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.LyricLine;
import dev.loki.lovisual.features.module.modules.visuals.lyrics.lrc.match.TrackQuery;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds timed lyrics for a track: lrclib first (exact lookup, then ranked searches, unsynced text gets an
 * estimated timeline), NetEase as the fallback. Only successes are cached; a result that failed because a
 * service was busy says so, so the caller retries instead of giving up on the song.
 */
public final class LyricsFinder {
    /** @param retryLater no lyrics were found, but a service was busy or unreachable, so asking again may help */
    public record Result(List<LyricLine> lines, boolean retryLater) {
        static final Result NOTHING = new Result(List.of(), false);
    }

    private static final Map<String, List<LyricLine>> CACHE = new ConcurrentHashMap<>();

    private LyricsFinder() {
    }

    public static CompletableFuture<Result> findAsync(String title, String artist, long durationSeconds, boolean debug) {
        TrackQuery query = new TrackQuery(title, artist, durationSeconds);
        if (query.title().isEmpty()) return CompletableFuture.completedFuture(Result.NOTHING);

        String key = (query.title() + " - " + query.artist()).toLowerCase();
        List<LyricLine> cached = CACHE.get(key);
        if (cached != null) return CompletableFuture.completedFuture(new Result(cached, false));

        return CompletableFuture.supplyAsync(() -> {
            LyricHttp http = new LyricHttp();
            List<LyricLine> lines = LrcLibSource.find(query, http);
            String source = "lrclib";
            if (lines.isEmpty()) {
                lines = NeteaseSource.find(query, http);
                source = "netease";
            }
            if (debug) {
                DebugLog.info("[KineticLyrics] %s: %d lines via %s%s", key, lines.size(), source,
                        http.sawTransientFailure() ? " (a service was busy)" : "");
            }
            if (!lines.isEmpty()) {
                CACHE.put(key, lines);
                return new Result(lines, false);
            }
            return new Result(List.of(), http.sawTransientFailure());
        });
    }
}
