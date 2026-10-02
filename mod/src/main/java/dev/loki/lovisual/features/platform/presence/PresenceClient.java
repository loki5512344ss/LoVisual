package dev.loki.lovisual.features.platform.presence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.loki.lovisual.features.platform.PlatformSession;
import dev.loki.lovisual.features.platform.http.ApiResult;
import dev.loki.lovisual.features.platform.http.ClientThread;
import dev.loki.lovisual.features.platform.http.PlatformHttpClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Poll loop of GUI presence ({@code POST /presence/gui}): one request publishes the local menu state and returns the
 * state of nearby players. Must be driven from the client thread; responses hop back onto it, so {@link #remote()}
 * is only touched there. Adaptive cadence: fast while a menu (local or remote) is live, slow heartbeat otherwise.
 */
public final class PresenceClient {

    private static final long FAST_MS = 350L;
    private static final long IDLE_MS = 2000L;
    private static final long ERROR_BACKOFF_MS = 8000L;
    private static final long AUTH_BACKOFF_MS = 30000L;
    private static final long FORGET_MS = 15000L;

    private final Map<UUID, PresenceTimeline> remote = new HashMap<>();
    private boolean inFlight;
    private boolean anyLive;
    private long nextAt;

    public Map<UUID, PresenceTimeline> remote() {
        return remote;
    }

    public void clear() {
        remote.clear();
        anyLive = false;
        nextAt = 0L;
    }

    public boolean due(long now, boolean localOpen) {
        return !inFlight && now >= nextAt && PlatformSession.get().isLinked();
    }

    /** Next send interval for the current state; exposed so the caller can decide how often to sample. */
    public long interval(boolean localOpen) {
        return localOpen || anyLive ? FAST_MS : IDLE_MS;
    }

    /**
     * @param local   local menu state to publish, or null for a "LoVisual is running" heartbeat (ignored when
     *                {@code publish} is false)
     * @param publish false for a read-only poll
     */
    public void sync(long now, String server, UUID self, PresenceCodec.Gui local, boolean publish, List<UUID> want) {
        inFlight = true;
        JsonObject body = new JsonObject();
        body.addProperty("server", server);
        body.addProperty("mc", self.toString());
        body.addProperty("publish", publish);
        if (publish && local != null) body.addProperty("gui", PresenceCodec.encode(local));
        JsonArray wanted = new JsonArray();
        want.forEach(id -> wanted.add(id.toString()));
        body.add("want", wanted);
        boolean localOpen = local != null && !local.closed();
        PlatformHttpClient.post("/presence/gui", body, PlatformSession.get().getDeviceToken())
                .thenAccept(result -> ClientThread.run(() -> onResult(result, localOpen)));
    }

    private void onResult(ApiResult result, boolean localOpen) {
        inFlight = false;
        long now = System.currentTimeMillis();
        if (!result.success()) {
            nextAt = now + (result.statusCode() == 401 || result.statusCode() == 409 ? AUTH_BACKOFF_MS : ERROR_BACKOFF_MS);
            return;
        }
        JsonObject obj = result.asObject();
        JsonElement players = obj == null ? null : obj.get("players");
        if (players != null && players.isJsonArray()) {
            for (JsonElement e : players.getAsJsonArray()) apply(e, now);
        }
        for (Iterator<PresenceTimeline> it = remote.values().iterator(); it.hasNext(); ) {
            if (!it.next().present(now - FORGET_MS + PresenceTimeline.STALE_MS * 4)) it.remove();
        }
        anyLive = remote.values().stream().anyMatch(t -> t.live(now));
        nextAt = now + interval(localOpen);
    }

    private void apply(JsonElement e, long now) {
        if (!e.isJsonObject()) return;
        JsonObject o = e.getAsJsonObject();
        try {
            UUID id = UUID.fromString(o.get("mc").getAsString());
            long age = o.has("age") ? o.get("age").getAsLong() : 0L;
            JsonElement g = o.get("gui");
            PresenceCodec.Gui gui = g == null || g.isJsonNull() ? null : PresenceCodec.decode(g.getAsString());
            if (gui == null) gui = new PresenceCodec.Gui(-1, false, false, true, PresenceCodec.NO_MODULE, List.of());
            remote.computeIfAbsent(id, k -> new PresenceTimeline()).add(now - age, gui);
        } catch (RuntimeException ignored) {
            // malformed entry from the server: skip it, the rest of the response is still valid
        }
    }
}
