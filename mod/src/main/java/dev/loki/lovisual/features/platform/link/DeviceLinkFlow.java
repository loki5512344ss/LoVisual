package dev.loki.lovisual.features.platform.link;

import com.google.gson.JsonObject;
import dev.loki.lovisual.features.platform.PlatformSession;
import dev.loki.lovisual.features.platform.http.ApiResult;
import dev.loki.lovisual.features.platform.http.PlatformHttpClient;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Drives LoVisual's own OAuth-device-flow-style login against the gateway:
 * {@code POST /device/code} to get a short user code, then polls
 * {@code POST /device/token} with the {@code device_code} until it is
 * confirmed on the site, expires, or is cancelled. Structurally the same
 * shape as {@code AltMicrosoftAuth}'s Microsoft device-code flow, but talks
 * to LoVisual's own backend and stores only a device token via
 * {@link PlatformSession} (never a password).
 */
public final class DeviceLinkFlow {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "LoVisual-DeviceLinkPoll");
        thread.setDaemon(true);
        return thread;
    });

    private static final long POLL_INTERVAL_SECONDS = 4;

    private volatile boolean cancelled;
    private ScheduledFuture<?> pollTask;

    public void start(Consumer<Result> onUpdate) {
        cancelled = false;
        PlatformHttpClient.post("/device/code", new JsonObject(), null)
                .thenAccept(result -> dispatch(() -> handleCodeResponse(result, onUpdate)));
    }

    public void cancel() {
        cancelled = true;
        if (pollTask != null) {
            pollTask.cancel(false);
        }
    }

    private void handleCodeResponse(ApiResult result, Consumer<Result> onUpdate) {
        if (cancelled) {
            return;
        }
        if (!result.success() || result.asObject() == null) {
            onUpdate.accept(Result.failure(result.error() == null ? "Failed to request a device code" : result.error()));
            return;
        }
        JsonObject body = result.asObject();
        String deviceCode = text(body, "device_code");
        String userCode = text(body, "user_code");
        long expiresIn = body.has("expires_in") ? body.get("expires_in").getAsLong() : 600L;
        if (deviceCode.isBlank() || userCode.isBlank()) {
            onUpdate.accept(Result.failure("Malformed /device/code response"));
            return;
        }
        onUpdate.accept(Result.awaitingConfirmation(userCode));
        long deadline = System.currentTimeMillis() + Math.max(1, expiresIn) * 1000L;
        schedulePoll(deviceCode, deadline, onUpdate);
    }

    private void schedulePoll(String deviceCode, long deadline, Consumer<Result> onUpdate) {
        if (cancelled) {
            return;
        }
        if (System.currentTimeMillis() >= deadline) {
            onUpdate.accept(Result.failure("Device code expired"));
            return;
        }
        pollTask = SCHEDULER.schedule(() -> poll(deviceCode, deadline, onUpdate), POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void poll(String deviceCode, long deadline, Consumer<Result> onUpdate) {
        if (cancelled) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("device_code", deviceCode);
        PlatformHttpClient.post("/device/token", body, null)
                .thenAccept(result -> dispatch(() -> handlePollResponse(result, deviceCode, deadline, onUpdate)));
    }

    private void handlePollResponse(ApiResult result, String deviceCode, long deadline, Consumer<Result> onUpdate) {
        if (cancelled) {
            return;
        }
        if (result.isNotFound()) {
            onUpdate.accept(Result.failure("Device code expired or unknown"));
            return;
        }
        if (result.isPending()) {
            schedulePoll(deviceCode, deadline, onUpdate);
            return;
        }
        if (!result.success() || result.asObject() == null) {
            schedulePoll(deviceCode, deadline, onUpdate);
            return;
        }
        String deviceToken = text(result.asObject(), "device_token");
        if (deviceToken.isBlank()) {
            schedulePoll(deviceCode, deadline, onUpdate);
            return;
        }
        PlatformSession.get().setDeviceToken(deviceToken);
        onUpdate.accept(Result.linked());
    }

    private static void dispatch(Runnable runnable) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.isSameThread()) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }

    private static String text(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    public enum Status { AWAITING_CONFIRMATION, LINKED, FAILED }

    public record Result(Status status, String userCode, String message) {
        static Result awaitingConfirmation(String userCode) {
            return new Result(Status.AWAITING_CONFIRMATION, userCode, null);
        }

        static Result linked() {
            return new Result(Status.LINKED, null, null);
        }

        static Result failure(String message) {
            return new Result(Status.FAILED, null, message);
        }
    }

    /**
     * Revokes the link server-side ({@code POST /device/revoke}, the token is the
     * proof) after dropping the local token. The local token is always cleared
     * even if the call fails or an older backend answers 404; {@code onDone}
     * runs on the client thread with the server's answer.
     */
    public static void unlink(Consumer<ApiResult> onDone) {
        String token = PlatformSession.get().getDeviceToken();
        PlatformSession.get().clear();
        JsonObject body = new JsonObject();
        body.addProperty("device_token", token);
        PlatformHttpClient.post("/device/revoke", body, null)
                .thenAccept(result -> dispatch(() -> onDone.accept(result)));
    }

    /** Fire-and-forget {@code GET /me} using the currently stored device token. */
    public static CompletableFuture<ApiResult> fetchMe() {
        return PlatformHttpClient.get("/me", PlatformSession.get().getDeviceToken());
    }
}
