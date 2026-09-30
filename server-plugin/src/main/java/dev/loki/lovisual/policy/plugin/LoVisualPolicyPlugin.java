package dev.loki.lovisual.policy.plugin;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.google.inject.Inject;
import dev.loki.lovisual.policy.PolicyConfig;
import dev.loki.lovisual.policy.PolicyMessage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity-side source of the {@code lovisual:policy} channel: loads
 * {@code config.yml} once, pushes the policy on join/backend switch and in
 * answer to the mod's {@code {"type":"request"}} message.
 */
@Plugin(id = "lovisual-policy", name = "LoVisual Policy", version = "1.0.0",
        authors = {"loki"})
public final class LoVisualPolicyPlugin {

    private static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from(PolicyMessage.CHANNEL);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final Map<UUID, String> currentServer = new ConcurrentHashMap<>();

    /** Null when {@code config.yml} is invalid — then nothing is sent. */
    private volatile PolicyConfig config;

    @Inject
    public LoVisualPolicyPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        proxy.getChannelRegistrar().register(CHANNEL);
        CommandManager commands = proxy.getCommandManager();
        commands.register(commands.metaBuilder("lvpolicy").plugin(this).build(),
                new PolicyCommand(this));
        reload();
        logger.info("LoVisual policy loaded: {}", status());
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        sendPolicy(event.getPlayer());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        currentServer.put(event.getPlayer().getUniqueId(),
                event.getServer().getServerInfo().getName());
        sendPolicy(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        currentServer.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())
                || !(event.getSource() instanceof Player player)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (PolicyMessage.isRequest(new String(event.getData(), StandardCharsets.UTF_8))) {
            sendPolicy(player);
        }
    }

    /** Re-reads {@code config.yml} and pushes the fresh policy to everyone online. */
    public void reload() {
        try {
            config = PolicyConfig.load(dataDirectory.resolve("config.yml"));
        } catch (IOException | IllegalArgumentException e) {
            config = null;
            logger.error("Invalid config.yml, no policy will be sent until it is fixed", e);
        }
        for (Player player : proxy.getAllPlayers()) {
            sendPolicy(player);
        }
    }

    /** A one-line answer for {@code /lvpolicy status}. */
    public String status() {
        PolicyConfig cfg = config;
        if (cfg == null) {
            return "config invalid (see log), nothing is sent";
        }
        if (!cfg.enabled()) {
            return "disabled";
        }
        return cfg.mode() + ", deny=" + cfg.deny().size() + " modules, reason=" + cfg.reason();
    }

    private void sendPolicy(Player player) {
        PolicyConfig cfg = config;
        if (cfg == null || !cfg.enabled()) {
            return;
        }
        String server = currentServer.getOrDefault(player.getUniqueId(), "");
        byte[] payload = PolicyMessage.encodePolicy(cfg, server).getBytes(StandardCharsets.UTF_8);
        player.sendPluginMessage(CHANNEL, payload);
    }
}
