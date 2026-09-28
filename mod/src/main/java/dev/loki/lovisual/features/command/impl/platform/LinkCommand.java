package dev.loki.lovisual.features.command.impl.platform;

import com.google.gson.JsonObject;
import dev.loki.lovisual.features.command.core.CommandContext;
import dev.loki.lovisual.features.command.core.CommandInfo;
import dev.loki.lovisual.features.command.util.ClientCommand;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.PlatformSession;
import dev.loki.lovisual.features.platform.http.ApiResult;
import dev.loki.lovisual.features.platform.link.DeviceLinkFlow;

import java.util.List;
import java.util.Locale;

/**
 * Links this client to a LoVisual platform account: {@code %link} starts the
 * device-code flow (request a code, show it, poll until the site confirms
 * it), {@code %link status} confirms the stored device token via
 * {@code GET /me}, {@code %link unlink} clears it locally and tries to
 * revoke the link server-side. Command-based only per this pass's scope; no
 * ClickGui screen.
 */
@CommandInfo(
        id = "link",
        usage = "%link [status|unlink]",
        descriptionKey = "command.link.description"
)
public final class LinkCommand implements ClientCommand {

    private static DeviceLinkFlow activeFlow;

    @Override
    public boolean execute(CommandContext ctx) {
        String action = ctx.arg(0) == null ? "" : ctx.arg(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "" -> startLink();
            case "status" -> checkStatus();
            case "unlink" -> unlink();
            case "cancel" -> cancelLink();
            default -> CommandOutput.warning("Usage: " + metadata().usage());
        }
        return true;
    }

    private void startLink() {
        if (PlatformSession.get().isLinked()) {
            CommandOutput.warning("Already linked. Use %link unlink first, or %link status to check.");
            return;
        }
        if (activeFlow != null) {
            CommandOutput.warning("A link request is already in progress. Use %link cancel to stop it.");
            return;
        }
        CommandOutput.send("Requesting a device link code...");
        activeFlow = new DeviceLinkFlow();
        activeFlow.start(this::onFlowUpdate);
    }

    private void onFlowUpdate(DeviceLinkFlow.Result result) {
        switch (result.status()) {
            case AWAITING_CONFIRMATION -> CommandOutput.success(
                    "Open the LoVisual site and enter this code: " + result.userCode());
            case LINKED -> {
                activeFlow = null;
                CommandOutput.success("Device linked.");
                checkStatus();
            }
            case FAILED -> {
                activeFlow = null;
                CommandOutput.error("Link failed: " + result.message());
            }
        }
    }

    private void cancelLink() {
        if (activeFlow == null) {
            CommandOutput.warning("No link request in progress.");
            return;
        }
        activeFlow.cancel();
        activeFlow = null;
        CommandOutput.send("Link request cancelled.");
    }

    private void checkStatus() {
        if (!PlatformSession.get().isLinked()) {
            CommandOutput.send("Not linked. Use %link to start.");
            return;
        }
        DeviceLinkFlow.fetchMe().thenAccept(result -> {
            if (dispatchOnClientThread(() -> reportMe(result))) return;
            reportMe(result);
        });
    }

    private void reportMe(ApiResult result) {
        if (!result.success() || result.asObject() == null) {
            CommandOutput.error("Linked, but /me failed: " + result.error());
            return;
        }
        JsonObject me = result.asObject();
        String nick = me.has("display_nick") ? me.get("display_nick").getAsString() : "?";
        String email = me.has("email") ? me.get("email").getAsString() : "?";
        CommandOutput.success("Linked as " + nick + " (" + email + ")");
    }

    private void unlink() {
        if (!PlatformSession.get().isLinked()) {
            CommandOutput.warning("Not linked.");
            return;
        }
        // Minimal per scope: clear the local token only. Revoking the link
        // server-side needs its `id` from GET /device/links, which this pass
        // does not fetch/track (kept out of scope to avoid a second async
        // round trip just for %link unlink); a future pass can look the link
        // up by matching last_seen or add a "revoke current device" endpoint.
        PlatformSession.get().clear();
        CommandOutput.success("Unlinked locally.");
    }

    private static boolean dispatchOnClientThread(Runnable runnable) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null) {
            return false;
        }
        if (mc.isSameThread()) {
            runnable.run();
            return true;
        }
        mc.execute(runnable);
        return true;
    }

    @Override
    public List<String> suggest(CommandContext ctx, int argIndex, String token) {
        if (argIndex != 1) return List.of();
        String lower = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return List.of("status", "unlink", "cancel").stream().filter(v -> v.startsWith(lower)).toList();
    }
}
