package dev.loki.lovisual.util.session.microsoft.auth;

import com.google.gson.JsonObject;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.session.microsoft.MicrosoftJson;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Shared HTTP transport, request builders, JSON parsing and error-mapping plumbing for
 * {@link MicrosoftAuthService} and its flow helpers. Owns the singleton {@link HttpClient} and its
 * daemon executor; exposes only package-static utilities.
 */
final class MicrosoftHttp {
    static final String CLIENT_ID = "54fd49e4-2103-4044-9603-2b028c814ec3";
    static final String USER_AGENT = "LoVisual AltManager";
    static final Duration TIMEOUT = Duration.ofSeconds(Long.getLong("lovisual.microsoftAuth.timeout", 15L));

    static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        private int index;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "LoVisualMicrosoftAuth-" + (++index));
            thread.setDaemon(true);
            return thread;
        }
    });

    static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NEVER)
            .executor(EXECUTOR)
            .build();

    private MicrosoftHttp() {
    }

    static HttpRequest formPost(String uri, String payload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
    }

    static HttpRequest jsonPost(String uri, JsonObject payload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(MicrosoftJson.GSON.toJson(payload)))
                .build();
    }

    static CompletableFuture<JsonObject> sendJson(HttpRequest request, String action) {
        return sendRaw(request, action).thenApply(response -> {
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw badStatus(action, response);
            }
            return parseJson(response.body(), action);
        });
    }

    static CompletableFuture<HttpResponse<String>> sendRaw(HttpRequest request, String action) {
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .exceptionally(t -> {
                    throw new MicrosoftAuthException("Unable to " + action, t);
                });
    }

    static JsonObject parseJson(String body, String action) {
        try {
            JsonObject json = MicrosoftJson.GSON.fromJson(body, JsonObject.class);
            return Objects.requireNonNull(json, "response body is null");
        } catch (Throwable t) {
            throw new MicrosoftAuthException("Unable to " + action + ": invalid JSON response", t);
        }
    }

    static String encode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    static String cleanToken(String token, String name) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
        return token.trim();
    }

    static <T> CompletableFuture<T> logFailures(String action, CompletableFuture<T> future) {
        return future.whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                logAuthFailure(action, throwable);
            }
        });
    }

    static <T> CompletableFuture<T> failedLoggedFuture(String action, Throwable throwable) {
        logAuthFailure(action, throwable);
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    static MicrosoftAuthException xstsUnauthorized(HttpResponse<String> response) {
        try {
            JsonObject json = parseJson(response.body(), "parse XSTS unauthorized response");
            long error = MicrosoftJson.number(json, "XErr");
            if (error == 2148916233L) {
                return new MicrosoftAuthException("Microsoft account has no linked Xbox account", "lovisual.auth.microsoft.no_xbox");
            }
            if (error == 2148916235L) {
                return new MicrosoftAuthException("Xbox Live is not available for this account region", "lovisual.auth.microsoft.xbox_unavailable");
            }
            if (error == 2148916236L || error == 2148916237L || error == 2148916238L) {
                return new MicrosoftAuthException("Microsoft account is not allowed to use Xbox Live for Minecraft", "lovisual.auth.microsoft.xbox_adult_required");
            }
            return new MicrosoftAuthException("Unknown XSTS authorization error: " + error);
        } catch (MicrosoftAuthException e) {
            return e;
        } catch (Throwable t) {
            return new MicrosoftAuthException("XSTS authorization failed with HTTP 401", t);
        }
    }

    static MicrosoftAuthException badStatus(String action, HttpResponse<String> response) {
        return new MicrosoftAuthException("Unable to " + action + ": HTTP " + response.statusCode() + " body=" + redact(response.body()));
    }

    private static void logAuthFailure(String action, Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof DeviceAuthPendingException) {
            return;
        }

        String message = cause.getMessage();
        DebugLog.error("[AltManager][MicrosoftAuth] Failed to %s%s", cause, action,
                message == null || message.isBlank() ? "" : ": " + message);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? new MicrosoftAuthException("Unknown Microsoft authentication failure") : current;
    }

    private static String redact(String body) {
        if (body == null) {
            return "<null>";
        }
        if (body.length() > 512) {
            return body.substring(0, 512) + "...";
        }
        return body;
    }
}
