package dev.loki.lovisual.mixinhandlers.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import dev.loki.lovisual.config.values.collection.ItemCooldownRulesValue;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.pvp.PvpChatEvent;
import dev.loki.lovisual.events.impl.pvp.PvpTabEvent;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.combat.pvp.PvpCooldowns;
import dev.loki.lovisual.features.module.modules.misc.sound.TotemSound;
import dev.loki.lovisual.features.module.modules.visuals.fx.TotemFX;
import dev.loki.lovisual.util.logging.ServerDumpUtil;
import dev.loki.lovisual.util.player.interaction.NetworkStatsUtil;
import dev.loki.lovisual.util.pvp.client.CooldownsState;
import dev.loki.lovisual.util.pvp.opponents.OpponentCooldownManager;
import dev.loki.lovisual.util.pvp.opponents.TotemPopCounter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat, tab-metric and totem-pop packet bodies for {@link ClientPacketListenerMixin}.
 * Physically under {@code handlers/} but declares the parent package for direct access.
 */
public final class ClientPacketListenerPvpHandler {

    // "TPS: 20.0" или "TPS: *20.0"
    private static final Pattern TPS_PATTERN =
            Pattern.compile("TPS:\\s*\\*?(\\d+(?:\\.\\d+)?)");
    // "Пинг: 57"
    private static final Pattern PING_PATTERN =
            Pattern.compile("Пинг:\\s*(\\d+)");

    private ClientPacketListenerPvpHandler() {
    }

    public static void captureTab(ClientboundTabListPacket packet) {
        String header = packet.header() == null ? "" : packet.header().getString();
        String footer = packet.footer() == null ? "" : packet.footer().getString();
        ServerDumpUtil.dumpTabText("HEADER", packet.header());
        ServerDumpUtil.dumpTabText("FOOTER", packet.footer());
        if (Events.BUS.hasListeners(PvpTabEvent.class)) {
            Events.BUS.post(new PvpTabEvent(header, footer));
        }
    }

    public static void onSystemChat(ClientboundSystemChatPacket packet) {
        String msg = packet.content().getString();

        // PvP state listeners via events; legacy direct setters disabled.
        if (msg != null && !msg.isBlank() && Events.BUS.hasListeners(PvpChatEvent.class)) {
            Events.BUS.post(new PvpChatEvent(msg, PvpChatEvent.Source.GAME_MESSAGE));
        }
    }

    public static void onPlayerChat(ClientboundPlayerChatPacket packet) {
        String msg = packet == null ? null : packet.body().content();
        if (msg == null || msg.isBlank()) return;
        if (Events.BUS.hasListeners(PvpChatEvent.class)) {
            Events.BUS.post(new PvpChatEvent(msg, PvpChatEvent.Source.CHAT_MESSAGE));
        }
    }

    public static void parseTabMetrics(Component text) {
        if (text == null) return;

        String raw = text.getString();
        if (raw.isEmpty()) return;

        Matcher tps = TPS_PATTERN.matcher(raw);
        if (tps.find()) {
            try {
                NetworkStatsUtil.updateTabTps(Float.parseFloat(tps.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }

        Matcher ping = PING_PATTERN.matcher(raw);
        if (ping.find()) {
            try {
                NetworkStatsUtil.updateTabPing(Integer.parseInt(ping.group(1)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static void onTotemStatus(ClientboundEntityEventPacket packet, ClientPacketListener listener) {
        if (packet.getEventId() == 35) { // 35 = сработал тотем бессмертия
            Minecraft client = Minecraft.getInstance();

            Entity entity = packet.getEntity(listener.getLevel());
            if (entity == null) return;
            TotemSound totemSound = Modules.get(TotemSound.class);
            if (totemSound != null) totemSound.onTotemPop(entity);
            if (entity.equals(client.player)) {
                PvpCooldowns cooldowns = Modules.get(PvpCooldowns.class);
                if (cooldowns != null) {
                    cooldowns.tryStartLocalCooldown(Items.TOTEM_OF_UNDYING, ItemCooldownRulesValue.Trigger.TOTEM_POP);
                }
                TotemFX fx = Modules.get(TotemFX.class);
                if (fx != null) {
                    fx.onTotemPop();
                }
                return;
            }
            if (entity instanceof Player player) {
                TotemPopCounter.recordPop(player);
                if (CooldownsState.shouldTrackOpponents()) {
                    OpponentCooldownManager.recordUse(player.getUUID(), Items.TOTEM_OF_UNDYING);
                }
            }
        }
    }

    /*
     * Disabled legacy hooks kept for reference (the old sound/inventory-driven
     * opponent-cooldown and PvP-state systems). They are wired out of the mixin and
     * intentionally inert; re-enable by restoring bodies into the matching
     * ClientPacketListenerMixin injection.
     *
     * handleSystemChat -> PvP enter/exit:
     *   if (msg.contains("Вы вошли в режим PVP")) CooldownsState.MANAGER.enterPvp();
     *   else if (msg.contains("PVP окончено")) CooldownsState.MANAGER.exitPvp();
     *
     * handleSoundEvent (chorus / ender pearl / totem use) -> OpponentCooldownManager.start(
     *   uuid, Items.CHORUS_FRUIT | ENDER_PEARL | TOTEM_OF_UNDYING) guarded by
     *   CooldownsState.shouldTrackOpponents(), SoundEvents match and
     *   SoundEntityResolver.isUnambiguousOpponent(pos, 1.6).
     *
     * handleContainerSetSlot / handleContainerContent ->
     *   CooldownsState.PENDING.consumeIfConfirmed(slot, count) then
     *   CooldownsState.MANAGER.commitConfirmedUse(item, startedAtMs).
     */
}
