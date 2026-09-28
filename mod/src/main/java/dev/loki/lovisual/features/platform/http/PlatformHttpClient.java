package dev.loki.lovisual.features.platform.http;

import com.google.gson.JsonObject;
import dev.loki.lovisual.features.platform.PlatformConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Small reusable client for talking to the LoVisual gateway. Resolves the
 * base URL via {@link PlatformConfig}/{@link dev.loki.lovisual.features.platform.BackendEndpoint},
 * sends/receives JSON, attaches the device token as a bearer header when one
 * is supplied, and always runs the blocking network call off the caller's
 * thread (mirrors {@code SkinManager}'s dedicated-executor pattern) so it is
 * safe to call from the render/client thread. Non-2xx responses are mapped
 * into {@link ApiResult} rather than thrown as exceptions.
 */
public final class PlatformHttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    private static final Executor EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "LoVisual-PlatformHttp");
        thread.setDaemon(true);
        return thread;
    });

    private PlatformHttpClient() {
    }

    public static CompletableFuture<ApiResult> get(String path, String bearerToken) {
        return send(requestBuilder(path).GET(), bearerToken);
    }

    public static CompletableFuture<ApiResult> post(String path, JsonObject body, String bearerToken) {
        String json = body == null ? "{}" : body.toString();
        HttpRequest.Builder builder = requestBuilder(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        return send(builder, bearerToken);
    }

    public static CompletableFuture<ApiResult> put(String path, JsonObject body, String bearerToken) {
        String json = body == null ? "{}" : body.toString();
        HttpRequest.Builder builder = requestBuilder(path)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));
        return send(builder, bearerToken);
    }

    public static CompletableFuture<ApiResult> delete(String path, String bearerToken) {
        return send(requestBuilder(path).DELETE(), bearerToken);
    }

    private static HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder(URI.create(PlatformConfig.get().getBackendBaseUrl() + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
    }

    private static CompletableFuture<ApiResult> send(HttpRequest.Builder builder, String bearerToken) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        HttpRequest request = builder.build();
        return CompletableFuture.supplyAsync(() -> executeBlocking(request), EXECUTOR);
    }

    private static ApiResult executeBlocking(HttpRequest request) {
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return ApiResult.of(response.statusCode(), response.body());
        } catch (IOException e) {
            return ApiResult.networkError(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ApiResult.networkError("Interrupted");
        }
    }
}
