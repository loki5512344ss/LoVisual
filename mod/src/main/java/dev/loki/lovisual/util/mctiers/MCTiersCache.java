package dev.loki.lovisual.util.mctiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Асинхронный кэш профилей mctiers.com (публичный REST API, без ключа):
 * {@code GET https://mctiers.com/api/search_profile/<username>} возвращает
 * {@code rankings.<mode>.{tier,retired}} на игровой режим (vanilla/sword/
 * uhc/pot/neth_pot/smp/axe/mace). Паттерн кэша и HTTP-вызова — как у
 * {@link dev.loki.lovisual.features.account.SkinManager} (та же ниша:
 * не блокировать рендер, тихо ронять сеть).
 */
public enum MCTiersCache {
    ;

    private static final String API_URL = "https://mctiers.com/api/search_profile/%s";
    private static final long TTL_MS = 5 * 60_000L;

    private static final Map<String, CachedProfile> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> LOADING = new ConcurrentHashMap<>();
    private static final Executor EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "LoVisual-MCTiersLookup");
        thread.setDaemon(true);
        return thread;
    });

    /** @return тир для режима, либо null если ещё не загружен/нет ранга — запрос уходит в фон сам. */
    public static MCTiersTier getTier(String playerName, String apiMode) {
        String key = normalize(playerName);
        if (key.isEmpty()) return null;

        CachedProfile cached = CACHE.get(key);
        if (cached != null && System.currentTimeMillis() - cached.fetchedAtMs < TTL_MS) {
            return cached.rankings.get(apiMode);
        }

        if (LOADING.putIfAbsent(key, Boolean.TRUE) == null) {
            CompletableFuture.runAsync(() -> load(playerName, key), EXECUTOR);
        }
        return cached != null ? cached.rankings.get(apiMode) : null;
    }

    public static void clearCache() {
        CACHE.clear();
        LOADING.clear();
    }

    private static void load(String playerName, String key) {
        try {
            JsonObject profile = fetchProfile(playerName);
            if (profile == null) return;

            Map<String, MCTiersTier> rankings = new ConcurrentHashMap<>();
            if (profile.has("rankings") && profile.get("rankings").isJsonObject()) {
                JsonObject rankingsJson = profile.getAsJsonObject("rankings");
                for (String mode : rankingsJson.keySet()) {
                    JsonObject ranking = rankingsJson.getAsJsonObject(mode);
                    if (ranking == null || !ranking.has("tier")) continue;
                    int tier = ranking.get("tier").getAsInt();
                    boolean retired = ranking.has("retired") && ranking.get("retired").getAsBoolean();
                    rankings.put(mode, new MCTiersTier(tier, retired));
                }
            }
            CACHE.put(key, new CachedProfile(rankings, System.currentTimeMillis()));
        } catch (Throwable ignored) {
        } finally {
            LOADING.remove(key);
        }
    }

    private static JsonObject fetchProfile(String playerName) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI
                    .create(String.format(API_URL, playerName))
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "LoVisual");

            if (connection.getResponseCode() != 200) {
                connection.disconnect();
                return null;
            }

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            } finally {
                connection.disconnect();
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private record CachedProfile(Map<String, MCTiersTier> rankings, long fetchedAtMs) {
    }
}
